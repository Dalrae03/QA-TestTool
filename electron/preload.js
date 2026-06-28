const { contextBridge, ipcRenderer } = require("electron");

contextBridge.exposeInMainWorld("desktopApi", {
  getConfig: () => ipcRenderer.invoke("desktop:get-config"),
  request: (options) => ipcRenderer.invoke("desktop:request", options),
  uploadExcel: (options) => ipcRenderer.invoke("desktop:upload-excel", options),
  uploadAttachment: (options) => ipcRenderer.invoke("desktop:upload-attachment", options),
  downloadAttachment: (options) => ipcRenderer.invoke("desktop:download-attachment", options),
  downloadBackup: (options) => ipcRenderer.invoke("desktop:download-backup", options),
  uploadBackup: (options) => ipcRenderer.invoke("desktop:upload-backup", options),
  chooseDirectory: (options) => ipcRenderer.invoke("desktop:choose-directory", options),
  saveBackupToDir: (options) => ipcRenderer.invoke("desktop:save-backup-to-dir", options)
});
