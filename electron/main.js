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

app.whenReady().then(() => {
  ipcMain.handle("desktop:get-config", () => ({
    platform: process.platform,
    version: app.getVersion(),
    defaultApiBaseUrl: "http://localhost:8080"
  }));

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
