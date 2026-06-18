#!/usr/bin/env node
// Spring Boot + Electron 동시 실행 스크립트
// 사용: npm run dev:start

import { spawn } from 'node:child_process';
import { connect } from 'node:net';
import { join } from 'node:path';
import { tmpdir } from 'node:os';

const BACKEND_PORT = 8080;
const BACKEND_READY_TIMEOUT_MS = 120_000;
const POLL_INTERVAL_MS = 1000;

function isPortOpen(port) {
  return new Promise(resolve => {
    const socket = connect({ port, host: '127.0.0.1' });
    socket.once('connect', () => {
      socket.destroy();
      resolve(true);
    });
    socket.once('error', () => resolve(false));
  });
}

function waitForPort(port, timeoutMs) {
  return new Promise((resolve, reject) => {
    const deadline = Date.now() + timeoutMs;
    const check = async () => {
      if (await isPortOpen(port)) {
        resolve();
      } else if (Date.now() > deadline) {
        reject(new Error(`백엔드가 ${timeoutMs / 1000}초 내에 기동되지 않았습니다.`));
      } else {
        setTimeout(check, POLL_INTERVAL_MS);
      }
    };
    check();
  });
}

// ── Spring Boot 기동 ──────────────────────────────────────────────
let backend = null;
let backendExit = null;
if (await isPortOpen(BACKEND_PORT)) {
  console.log(`[dev:start] 기존 백엔드를 사용합니다 (port ${BACKEND_PORT}).`);
} else {
  console.log('[dev:start] Spring Boot 기동 중...');
  backend = spawn('mvn', ['spring-boot:run', '-q'], {
    stdio: ['ignore', 'pipe', 'pipe'],
    env: { ...process.env },
  });

  backend.stdout.on('data', d => process.stdout.write(`[backend] ${d}`));
  backend.stderr.on('data', d => process.stderr.write(`[backend] ${d}`));
  backendExit = new Promise((_, reject) => {
    backend.once('exit', code => {
      reject(new Error(`Spring Boot가 준비 전에 종료됐습니다 (code=${code ?? 'signal'}).`));
    });
  });
}

// ── 8080 열릴 때까지 대기 후 Electron 실행 ───────────────────────
try {
  const portReady = waitForPort(BACKEND_PORT, BACKEND_READY_TIMEOUT_MS);
  await (backendExit ? Promise.race([portReady, backendExit]) : portReady);
} catch (e) {
  console.error('[dev:start]', e.message);
  backend?.kill();
  process.exit(1);
}

console.log(`[dev:start] 백엔드 준비 완료 (port ${BACKEND_PORT}). Electron 실행 중...`);
const npmCommand = process.platform === 'win32' ? 'npm.cmd' : 'npm';
const electronProfile = join(tmpdir(), 'tms-electron-dev');
const frontend = spawn(npmCommand, ['run', 'desktop:dev', '--', `--user-data-dir=${electronProfile}`], {
  stdio: 'inherit',
  env: { ...process.env },
});

frontend.on('exit', code => {
  console.log(`[dev:start] Electron 종료.${backend ? ' 백엔드도 종료합니다.' : ''}`);
  backend?.kill();
  process.exit(code ?? 0);
});

process.on('SIGINT', () => {
  frontend.kill();
  backend?.kill();
  process.exit(0);
});
