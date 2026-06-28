const path = require("path");
const fs = require("fs");
const { app, BrowserWindow, ipcMain, shell, dialog } = require("electron");

const isMac = process.platform === "darwin";
const REQUEST_TIMEOUT_MS = 15000;
// 백업 export/restore 는 데이터 양에 따라 오래 걸릴 수 있어 별도의 넉넉한 타임아웃을 둔다.
const BACKUP_TIMEOUT_MS = 10 * 60 * 1000;
const MAX_ATTACHMENT_BYTES = 50 * 1024 * 1024;
const MAX_BACKUP_BYTES = 1024 * 1024 * 1024;

// 백엔드와 공유하는 API 토큰. dev-start 가 생성해 환경변수로 주입한다.
// 비어 있으면 헤더를 붙이지 않으며, 백엔드도 검증을 비활성화한다.
const API_TOKEN = process.env.TMS_API_TOKEN || "";

function withAuthHeaders(headers = {}) {
  return API_TOKEN ? { ...headers, "X-TMS-Token": API_TOKEN } : { ...headers };
}

function parseHttpUrl(rawUrl) {
  if (!rawUrl || typeof rawUrl !== "string") {
    throw new Error("요청 URL이 없습니다.");
  }
  const url = new URL(rawUrl);
  if (!["http:", "https:"].includes(url.protocol)) {
    throw new Error("http 또는 https 주소만 사용할 수 있습니다.");
  }
  return url;
}

async function fetchWithTimeout(url, options = {}, timeoutMs = REQUEST_TIMEOUT_MS) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    return await fetch(url, { ...options, signal: controller.signal });
  } finally {
    clearTimeout(timer);
  }
}

function errorResponse(status, message) {
  return { ok: false, status, data: { message } };
}

async function readResponse(response) {
  const contentType = response.headers.get("content-type") ?? "";
  if (response.status === 204) {
    return null;
  }
  return contentType.includes("application/json")
    ? await response.json()
    : await response.text();
}

function createMainWindow() {
  const window = new BrowserWindow({
    width: 1440,
    height: 920,
    minWidth: 1120,
    minHeight: 760,
    show: false,
    backgroundColor: "#ebe5da",
    title: "QA TestTool",
    webPreferences: {
      preload: path.join(__dirname, "preload.js"),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true
    }
  });

  window.once("ready-to-show", () => {
    window.show();
  });

  window.webContents.setWindowOpenHandler(({ url }) => {
    try {
      const parsed = new URL(url);
      if (["http:", "https:", "mailto:"].includes(parsed.protocol)) {
        shell.openExternal(url);
      }
    } catch (_error) {}
    return { action: "deny" };
  });

  window.webContents.on("will-navigate", event => {
    event.preventDefault();
  });

  window.loadFile(path.join(__dirname, "..", "desktop", "index.html"));
}

async function proxyApiRequest(_event, options) {
  try {
    const method = options?.method ?? "GET";
    const url = parseHttpUrl(options?.url);
    const headers = withAuthHeaders(options?.headers ?? {});

    const fetchOptions = {
      method,
      headers
    };

    if (options?.body !== undefined && options?.body !== null) {
      fetchOptions.body = options.body;
    }

    const response = await fetchWithTimeout(url, fetchOptions);
    const data = await readResponse(response);

    return {
      ok: response.ok,
      status: response.status,
      data
    };
  } catch (error) {
    const message = error.name === "AbortError"
      ? "백엔드 서버 응답 시간이 초과되었습니다."
      : `백엔드 서버에 연결할 수 없습니다. ${error.message}`;
    return errorResponse(503, message);
  }
}

// 엑셀 임포트: 파일 경로를 받아 FormData 로 백엔드에 POST
async function uploadExcel(_event, options) {
  try {
    const url = parseHttpUrl(options?.url);
    const filePath = options?.filePath;
    if (!filePath) return errorResponse(400, "filePath가 필요합니다.");
    const stat = fs.statSync(filePath);
    if (stat.size > 50 * 1024 * 1024) return errorResponse(413, "50MB 이하 파일만 업로드할 수 있습니다.");
    const buffer = fs.readFileSync(filePath);
    const fileName = path.basename(filePath);
    const formData = new FormData();
    formData.append("file", new Blob([buffer]), fileName);
    if (options?.projectId) formData.append("projectId", String(options.projectId));
    const response = await fetchWithTimeout(url, { method: "POST", body: formData, headers: withAuthHeaders() });
    const data = await readResponse(response);
    return { ok: response.ok, status: response.status, data };
  } catch (error) {
    return errorResponse(503, `엑셀 업로드 실패: ${error.message}`);
  }
}

// 첨부파일: 멀티파트 업로드 — 네이티브 파일 선택 후 FormData 로 백엔드에 POST
async function uploadAttachment(_event, options) {
  try {
    const url = parseHttpUrl(options?.url);

    const result = await dialog.showOpenDialog({
      title: "첨부할 파일 선택",
      properties: ["openFile"]
    });
    if (result.canceled || result.filePaths.length === 0) {
      return { ok: false, canceled: true, status: 0, data: null };
    }

    const filePath = result.filePaths[0];
    const stat = fs.statSync(filePath);
    if (stat.size > MAX_ATTACHMENT_BYTES) {
      return errorResponse(413, "첨부파일은 50MB 이하만 업로드할 수 있습니다.");
    }
    const buffer = fs.readFileSync(filePath);
    const fileName = path.basename(filePath);

    const formData = new FormData();
    formData.append("file", new Blob([buffer]), fileName);

    const response = await fetchWithTimeout(url, { method: "POST", body: formData, headers: withAuthHeaders() });
    const data = await readResponse(response);
    return { ok: response.ok, status: response.status, data };
  } catch (error) {
    return errorResponse(503, `파일 업로드에 실패했습니다. ${error.message}`);
  }
}

// 첨부파일: 다운로드 — 백엔드에서 받아 사용자가 선택한 위치에 저장
async function downloadAttachment(_event, options) {
  try {
    const url = parseHttpUrl(options?.url);

    const saveResult = await dialog.showSaveDialog({
      title: "첨부파일 저장",
      defaultPath: options?.suggestedName || "attachment"
    });
    if (saveResult.canceled || !saveResult.filePath) {
      return { ok: false, canceled: true, status: 0, data: null };
    }

    const response = await fetchWithTimeout(url, { method: "GET", headers: withAuthHeaders() });
    if (!response.ok) {
      const data = await readResponse(response);
      return { ok: false, status: response.status, data: typeof data === "object" ? data : { message: `HTTP ${response.status}` } };
    }
    const arrayBuffer = await response.arrayBuffer();
    fs.writeFileSync(saveResult.filePath, Buffer.from(arrayBuffer));
    return { ok: true, status: response.status, savedPath: saveResult.filePath, data: null };
  } catch (error) {
    return errorResponse(503, `파일 다운로드에 실패했습니다. ${error.message}`);
  }
}

// 데이터 백업: 전체 백업 zip 을 받아 사용자가 선택한 위치에 저장 (긴 타임아웃)
async function downloadBackup(_event, options) {
  try {
    const url = parseHttpUrl(options?.url);

    const saveResult = await dialog.showSaveDialog({
      title: "데이터 백업 저장",
      defaultPath: options?.suggestedName || "tms-backup.zip",
      filters: [{ name: "백업 파일", extensions: ["zip"] }]
    });
    if (saveResult.canceled || !saveResult.filePath) {
      return { ok: false, canceled: true, status: 0, data: null };
    }

    const response = await fetchWithTimeout(url, { method: "GET", headers: withAuthHeaders() }, BACKUP_TIMEOUT_MS);
    if (!response.ok) {
      const data = await readResponse(response);
      return { ok: false, status: response.status, data: typeof data === "object" ? data : { message: `HTTP ${response.status}` } };
    }
    const arrayBuffer = await response.arrayBuffer();
    fs.writeFileSync(saveResult.filePath, Buffer.from(arrayBuffer));
    return { ok: true, status: response.status, savedPath: saveResult.filePath, data: null };
  } catch (error) {
    return errorResponse(503, `백업 저장에 실패했습니다. ${error.message}`);
  }
}

// 데이터 복구: 백업 zip 을 선택해 백엔드로 업로드 (긴 타임아웃, 대용량 허용)
async function uploadBackup(_event, options) {
  try {
    const url = parseHttpUrl(options?.url);

    const result = await dialog.showOpenDialog({
      title: "복구할 백업 파일 선택",
      properties: ["openFile"],
      filters: [{ name: "백업 파일", extensions: ["zip"] }]
    });
    if (result.canceled || result.filePaths.length === 0) {
      return { ok: false, canceled: true, status: 0, data: null };
    }

    const filePath = result.filePaths[0];
    const stat = fs.statSync(filePath);
    if (stat.size > MAX_BACKUP_BYTES) {
      return errorResponse(413, "백업 파일은 1GB 이하만 복구할 수 있습니다.");
    }
    const buffer = fs.readFileSync(filePath);
    const fileName = path.basename(filePath);

    const formData = new FormData();
    formData.append("file", new Blob([buffer]), fileName);

    const response = await fetchWithTimeout(url, { method: "POST", body: formData, headers: withAuthHeaders() }, BACKUP_TIMEOUT_MS);
    const data = await readResponse(response);
    return { ok: response.ok, status: response.status, data };
  } catch (error) {
    return errorResponse(503, `백업 복구에 실패했습니다. ${error.message}`);
  }
}

// 기본 백업 폴더 선택 — 네이티브 디렉터리 선택 다이얼로그
async function chooseDirectory(_event, options) {
  try {
    const result = await dialog.showOpenDialog({
      title: options?.title || "백업 폴더 선택",
      properties: ["openDirectory", "createDirectory"],
      defaultPath: options?.defaultPath || undefined
    });
    if (result.canceled || result.filePaths.length === 0) {
      return { canceled: true, dir: null };
    }
    return { canceled: false, dir: result.filePaths[0] };
  } catch (error) {
    return { canceled: false, dir: null, error: error.message };
  }
}

// 지정한 폴더에 백업을 저장(다이얼로그 없음) + 오래된 백업 자동 정리(최근 keepCount개 유지)
async function saveBackupToDir(_event, options) {
  try {
    const url = parseHttpUrl(options?.url);
    const dir = options?.dir;
    if (!dir) return errorResponse(400, "백업 폴더가 지정되지 않았습니다.");
    if (!fs.existsSync(dir) || !fs.statSync(dir).isDirectory()) {
      return errorResponse(400, "백업 폴더를 찾을 수 없습니다.");
    }

    const response = await fetchWithTimeout(url, { method: "GET", headers: withAuthHeaders() }, BACKUP_TIMEOUT_MS);
    if (!response.ok) {
      const data = await readResponse(response);
      return { ok: false, status: response.status, data: typeof data === "object" ? data : { message: `HTTP ${response.status}` } };
    }
    const arrayBuffer = await response.arrayBuffer();
    const filename = options?.filename || `tms-backup-${backupStamp()}.zip`;
    const savedPath = path.join(dir, filename);
    fs.writeFileSync(savedPath, Buffer.from(arrayBuffer));

    const deleted = pruneOldBackups(dir, Number(options?.keepCount) || 0);
    return { ok: true, status: response.status, savedPath, deleted };
  } catch (error) {
    return errorResponse(503, `백업 저장에 실패했습니다. ${error.message}`);
  }
}

// tms-backup-*.zip 중 최신 keepCount개만 남기고 나머지를 삭제. keepCount<=0 이면 정리 안 함.
function pruneOldBackups(dir, keepCount) {
  if (!keepCount || keepCount <= 0) return 0;
  let deleted = 0;
  try {
    const files = fs.readdirSync(dir)
      .filter(name => /^tms-backup-.*\.zip$/i.test(name))
      .sort(); // 파일명에 날짜시간이 들어가 사전순=시간순
    const removable = files.slice(0, Math.max(0, files.length - keepCount));
    for (const name of removable) {
      try { fs.unlinkSync(path.join(dir, name)); deleted++; } catch (_e) { /* 무시 */ }
    }
  } catch (_e) { /* 무시 */ }
  return deleted;
}

function backupStamp() {
  const d = new Date();
  const p = n => String(n).padStart(2, "0");
  return `${d.getFullYear()}${p(d.getMonth() + 1)}${p(d.getDate())}-${p(d.getHours())}${p(d.getMinutes())}${p(d.getSeconds())}`;
}

app.whenReady().then(() => {
  ipcMain.handle("desktop:get-config", () => ({
    platform: process.platform,
    version: app.getVersion(),
    defaultApiBaseUrl: "http://localhost:8080"
  }));
  ipcMain.handle("desktop:request", proxyApiRequest);
  ipcMain.handle("desktop:upload-excel", uploadExcel);
  ipcMain.handle("desktop:upload-attachment", uploadAttachment);
  ipcMain.handle("desktop:download-backup", downloadBackup);
  ipcMain.handle("desktop:upload-backup", uploadBackup);
  ipcMain.handle("desktop:choose-directory", chooseDirectory);
  ipcMain.handle("desktop:save-backup-to-dir", saveBackupToDir);
  ipcMain.handle("desktop:download-attachment", downloadAttachment);

  createMainWindow();

  app.on("activate", () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createMainWindow();
    }
  });
});

app.on("window-all-closed", () => {
  if (!isMac) {
    app.quit();
  }
});
