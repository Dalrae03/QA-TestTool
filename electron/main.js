const path = require("path");
const { app, BrowserWindow, ipcMain, shell } = require("electron");

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

app.whenReady().then(() => {
  ipcMain.handle("desktop:get-config", () => ({
    platform: process.platform,
    version: app.getVersion(),
    defaultApiBaseUrl: "http://localhost:8080"
  }));
  ipcMain.handle("desktop:request", proxyApiRequest);

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
