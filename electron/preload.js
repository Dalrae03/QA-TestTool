const { contextBridge, ipcRenderer } = require("electron");

contextBridge.exposeInMainWorld("desktopApi", {
  getConfig: () => ipcRenderer.invoke("desktop:get-config"),
  request: (options) => ipcRenderer.invoke("desktop:request", options),
  uploadAttachment: (options) => ipcRenderer.invoke("desktop:upload-attachment", options),
  downloadAttachment: (options) => ipcRenderer.invoke("desktop:download-attachment", options)
});
