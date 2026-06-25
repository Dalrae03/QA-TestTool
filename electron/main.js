const path = require("path");
const fs = require("fs");
const { app, BrowserWindow, ipcMain, shell, dialog } = require("electron");

const isMac = process.platform === "darwin";
const REQUEST_TIMEOUT_MS = 15000;
const MAX_ATTACHMENT_BYTES = 50 * 1024 * 1024;

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

async function fetchWithTimeout(url, options = {}) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
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
    const headers = {
      ...(options?.headers ?? {})
    };

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
    const response = await fetchWithTimeout(url, { method: "POST", body: formData });
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

    const response = await fetchWithTimeout(url, { method: "POST", body: formData });
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

    const response = await fetchWithTimeout(url, { method: "GET" });
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

app.whenReady().then(() => {
  ipcMain.handle("desktop:get-config", () => ({
    platform: process.platform,
    version: app.getVersion(),
    defaultApiBaseUrl: "http://localhost:8080"
  }));
  ipcMain.handle("desktop:request", proxyApiRequest);
  ipcMain.handle("desktop:upload-excel", uploadExcel);
  ipcMain.handle("desktop:upload-attachment", uploadAttachment);
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
