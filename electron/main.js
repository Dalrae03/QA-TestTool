const path = require("path");
const fs = require("fs");
const { app, BrowserWindow, ipcMain, shell, dialog } = require("electron");

const isMac = process.platform === "darwin";

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
    shell.openExternal(url);
    return { action: "deny" };
  });

  window.loadFile(path.join(__dirname, "..", "desktop", "index.html"));
}

async function proxyApiRequest(_event, options) {
  try {
    const method = options?.method ?? "GET";
    const url = options?.url;
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

    const response = await fetch(url, fetchOptions);
    const contentType = response.headers.get("content-type") ?? "";
    let data = null;

    if (response.status !== 204) {
      data = contentType.includes("application/json")
        ? await response.json()
        : await response.text();
    }

    return {
      ok: response.ok,
      status: response.status,
      data
    };
  } catch (error) {
    return {
      ok: false,
      status: 503,
      data: {
        message: "백엔드 서버에 연결할 수 없습니다. Spring Boot 서버가 8080 포트에서 실행 중인지 확인하세요."
      }
    };
  }
}

// 첨부파일: 멀티파트 업로드 — 네이티브 파일 선택 후 FormData 로 백엔드에 POST
async function uploadAttachment(_event, options) {
  try {
    const url = options?.url;
    if (!url) {
      return { ok: false, status: 400, data: { message: "업로드 URL이 없습니다." } };
    }

    const result = await dialog.showOpenDialog({
      title: "첨부할 파일 선택",
      properties: ["openFile"]
    });
    if (result.canceled || result.filePaths.length === 0) {
      return { ok: false, canceled: true, status: 0, data: null };
    }

    const filePath = result.filePaths[0];
    const buffer = fs.readFileSync(filePath);
    const fileName = path.basename(filePath);

    const formData = new FormData();
    formData.append("file", new Blob([buffer]), fileName);

    const response = await fetch(url, { method: "POST", body: formData });
    const contentType = response.headers.get("content-type") ?? "";
    let data = null;
    if (response.status !== 204) {
      data = contentType.includes("application/json")
        ? await response.json()
        : await response.text();
    }
    return { ok: response.ok, status: response.status, data };
  } catch (error) {
    return { ok: false, status: 503, data: { message: "파일 업로드에 실패했습니다." } };
  }
}

// 첨부파일: 다운로드 — 백엔드에서 받아 사용자가 선택한 위치에 저장
async function downloadAttachment(_event, options) {
  try {
    const url = options?.url;
    if (!url) {
      return { ok: false, status: 400, data: { message: "다운로드 URL이 없습니다." } };
    }

    const saveResult = await dialog.showSaveDialog({
      title: "첨부파일 저장",
      defaultPath: options?.suggestedName || "attachment"
    });
    if (saveResult.canceled || !saveResult.filePath) {
      return { ok: false, canceled: true, status: 0, data: null };
    }

    const response = await fetch(url, { method: "GET" });
    if (!response.ok) {
      return { ok: false, status: response.status, data: { message: `HTTP ${response.status}` } };
    }
    const arrayBuffer = await response.arrayBuffer();
    fs.writeFileSync(saveResult.filePath, Buffer.from(arrayBuffer));
    return { ok: true, status: response.status, savedPath: saveResult.filePath, data: null };
  } catch (error) {
    return { ok: false, status: 503, data: { message: "파일 다운로드에 실패했습니다." } };
  }
}

app.whenReady().then(() => {
  ipcMain.handle("desktop:get-config", () => ({
    platform: process.platform,
    version: app.getVersion(),
    defaultApiBaseUrl: "http://localhost:8080"
  }));
  ipcMain.handle("desktop:request", proxyApiRequest);
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
