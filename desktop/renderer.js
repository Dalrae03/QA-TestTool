// ── 상수 ─────────────────────────────────────────────────────────
const FOLDER_KEY        = "tms.folders";
const FOLDER_ASSIGN_KEY = "tms.folderAssignments";

// ── 상태 ─────────────────────────────────────────────────────────
const state = {
  apiBaseUrl: "http://localhost:8080",
  selectedId: null,
  testCases: [],
  testRuns: [],
  areaTags: [],
  selectedTagIds: [],
  currentExpected: "",
  folders: [],              // { id, name, parentId, collapsed }
  folderAssignments: {},    // { "tcId": folderId | "unclassified" | null }
  selectedFolderId: "all",
  runsContext: "folder",   // "folder" | "tc" — 실행 기록 탭에서 보여줄 대상
  unclassifiedCollapsed: false,
  filters: { status: "", os: "", type: "", areaTagId: "", keyword: "" }
};

// ── elements ─────────────────────────────────────────────────────
const elements = {
  apiBaseUrl:        document.getElementById("apiBaseUrl"),
  connectButton:     document.getElementById("connectButton"),
  refreshButton:     document.getElementById("refreshButton"),
  newButton:         document.getElementById("newButton"),
  duplicateButton:   document.getElementById("duplicateButton"),
  deleteButton:      document.getElementById("deleteButton"),
  resetButton:       document.getElementById("resetButton"),
  list:              document.getElementById("testCaseList"),
  listState:         document.getElementById("listState"),
  mainSearchCount:   document.getElementById("mainSearchCount"),
  editorPanel:       document.getElementById("editorPanel"),
  flowHint:          document.getElementById("flowHint"),
  form:              document.getElementById("testCaseForm"),
  testRunForm:       document.getElementById("testRunForm"),
  runState:          document.getElementById("runState"),
  runMode:           document.getElementById("runMode"),
  saveRunButton:     document.getElementById("saveRunButton"),
  testRunList:       document.getElementById("testRunList"),
  runStats:          document.getElementById("runStats"),
  editorTitle:       document.getElementById("editorTitle"),
  dtCaseId:          document.getElementById("dtCaseId"),
  dtPills:           document.getElementById("dtPills"),
  formMode:          document.getElementById("formMode"),
  platformPill:      document.getElementById("platform-pill"),
  versionPill:       document.getElementById("version-pill"),
  testCaseId:        document.getElementById("testCaseId"),
  tcFolder:          document.getElementById("tcFolder"),
  type:              document.getElementById("type"),
  priority:          document.getElementById("priority"),
  tcStatus:          document.getElementById("tcStatus"),
  statusSelector:    document.getElementById("statusSelector"),
  title:             document.getElementById("title"),
  description:       document.getElementById("description"),
  precondition:      document.getElementById("precondition"),
  stepsList:         document.getElementById("stepsList"),
  addStepButton:     document.getElementById("addStepButton"),
  notes:             document.getElementById("notes"),
  envOs:             document.getElementById("envOs"),
  envBrowser:        document.getElementById("envBrowser"),
  envDevice:         document.getElementById("envDevice"),
  selectedTagChips:  document.getElementById("selectedTagChips"),
  tagSelect:         document.getElementById("tagSelect"),
  addTagButton:      document.getElementById("addTagButton"),
  newTagInput:       document.getElementById("newTagInput"),
  createTagButton:   document.getElementById("createTagButton"),
  runStatus:         document.getElementById("runStatus"),
  actualResult:      document.getElementById("actualResult"),
  runNotes:          document.getElementById("runNotes"),
  filterKeyword:     document.getElementById("filterKeyword"),
  filterType:        document.getElementById("filterType"),
  filterAreaTag:     document.getElementById("filterAreaTag"),
  statusFilterPills: document.getElementById("statusFilterPills"),
  osFilterPills:     document.getElementById("osFilterPills"),
  detailEmpty:       document.getElementById("detailEmpty"),
  detailEditor:      document.getElementById("detailEditor"),
  runPanelCaseId:    document.getElementById("runPanelCaseId"),
  runPanelCaseTitle: document.getElementById("runPanelCaseTitle")
};

const requiredFieldConfigs = [
  { element: elements.type,         label: "Type",         getValue: () => elements.type.value },
  { element: elements.priority,     label: "Priority",     getValue: () => elements.priority.value },
  { element: elements.title,        label: "Title",        getValue: () => elements.title.value.trim() },
  { element: elements.description,  label: "Description",  getValue: () => elements.description.value.trim() },
  { element: elements.precondition, label: "Precondition", getValue: () => elements.precondition.value.trim() }
];

// ══════════════════════════════════════════════════════════════════
// 뷰 / 탭 전환
// ══════════════════════════════════════════════════════════════════

function switchView(v) {
  document.querySelectorAll(".app-view").forEach(el => el.classList.remove("active"));
  const el = document.getElementById("view-" + v);
  if (el) el.classList.add("active");
  const map = { dashboard: "navDash", testcases: "navTC", settings: "navSet" };
  document.querySelectorAll(".nav-tab").forEach(b => b.classList.remove("active"));
  const t = document.getElementById(map[v]);
  if (t) t.classList.add("active");
  if (v === "dashboard") renderDashboard();
}

function switchTcTab(t) {
  ["list","detail","runs"].forEach(p => {
    const el = document.getElementById("tcp-" + p);
    if (el) el.classList.remove("show");
  });
  ["tctab-list","tctab-detail","tctab-runs"].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.classList.remove("on");
  });
  const target = document.getElementById("tcp-" + t);
  if (target) target.classList.add("show");
  const tabMap = { list:"tctab-list", detail:"tctab-detail", runs:"tctab-runs" };
  const tabEl = document.getElementById(tabMap[t]);
  if (tabEl) tabEl.classList.add("on");

  if (t === "runs") {
    const folderPanel = document.getElementById("folderRunsPanel");
    const tcPanel     = document.getElementById("runPanel");
    if (state.runsContext === "folder") {
      if (folderPanel) folderPanel.style.display = "";
      if (tcPanel)     tcPanel.style.display = "none";
      renderFolderRunsOverview();
    } else {
      if (folderPanel) folderPanel.style.display = "none";
      if (tcPanel)     tcPanel.style.display = "";
    }
  }
}

// ══════════════════════════════════════════════════════════════════
// 검색 필터 / 정렬 (UI)
// ══════════════════════════════════════════════════════════════════

function toggleFilterBar() {
  const bar = document.getElementById("tcFilterBar");
  const btn = document.getElementById("filterToggleBtn");
  const open = bar.classList.toggle("show");
  btn.style.background  = open ? "var(--accent-glow)" : "";
  btn.style.borderColor = open ? "var(--accent)" : "";
  btn.style.color       = open ? "var(--accent)" : "";
}

function toggleSortMenu(e) { e.stopPropagation(); document.getElementById("sortMenu").classList.toggle("show"); }
function setSort(el, label) {
  document.querySelectorAll(".sort-item").forEach(i => i.classList.remove("active"));
  el.classList.add("active");
  document.getElementById("sortMenu").classList.remove("show");
  _toast("정렬: " + label);
}
document.addEventListener("click", () => { const m = document.getElementById("sortMenu"); if (m) m.classList.remove("show"); });

// ══════════════════════════════════════════════════════════════════
// 유틸
// ══════════════════════════════════════════════════════════════════

function escapeHtml(v) {
  return String(v ?? "")
    .replaceAll("&","&amp;").replaceAll("<","&lt;").replaceAll(">","&gt;")
    .replaceAll('"',"&quot;").replaceAll("'","&#39;");
}
function formatDateTime(v) {
  return new Date(v).toLocaleString("ko-KR", { year:"numeric",month:"2-digit",day:"2-digit",hour:"2-digit",minute:"2-digit" });
}
function getApiBaseUrl() {
  return (elements.apiBaseUrl ? elements.apiBaseUrl.value.trim().replace(/\/$/, "") : "") || state.apiBaseUrl;
}
function updateStatus(msg) {
  if (!elements.listState) return;
  elements.listState.style.display = msg ? "block" : "none";
  elements.listState.textContent = msg;
}
function updateRunStatus(msg) { if (elements.runState) elements.runState.textContent = msg; }
function _toast(msg, isError = false) {
  const el = document.createElement("div");
  el.style.cssText = `position:fixed;top:16px;left:50%;transform:translateX(-50%);background:${isError ? "#dc2626" : "var(--accent)"};color:#fff;padding:8px 20px;border-radius:8px;font-size:12px;font-weight:500;z-index:9999;box-shadow:var(--shadow-md);pointer-events:none;max-width:400px;text-align:center`;
  el.textContent = msg;
  document.body.appendChild(el);
  setTimeout(() => el.remove(), 2500);
}

// ══════════════════════════════════════════════════════════════════
// 폴더 — localStorage 영속
// ══════════════════════════════════════════════════════════════════

function loadFolders() {
  try {
    const raw  = localStorage.getItem(FOLDER_KEY);
    const rawA = localStorage.getItem(FOLDER_ASSIGN_KEY);
    state.folders           = raw  ? JSON.parse(raw)  : [];
    state.folderAssignments = rawA ? JSON.parse(rawA) : {};
  } catch (_e) { state.folders = []; state.folderAssignments = {}; }
}
function persistFolders() {
  localStorage.setItem(FOLDER_KEY,        JSON.stringify(state.folders));
  localStorage.setItem(FOLDER_ASSIGN_KEY, JSON.stringify(state.folderAssignments));
}

// ── 폴더 유틸 ─────────────────────────────────────────────────────

function getAllSubFolderIds(folderId) {
  const result = [];
  for (const f of state.folders.filter(f => f.parentId === folderId)) {
    result.push(f.id);
    result.push(...getAllSubFolderIds(f.id));
  }
  return result;
}
function isDescendant(potentialParentId, folderId) {
  let cur = state.folders.find(f => f.id === folderId);
  while (cur && cur.parentId) {
    if (cur.parentId === potentialParentId) return true;
    cur = state.folders.find(f => f.id === cur.parentId);
  }
  return false;
}
function getFolderTcCount(folderId) {
  const allIds = [folderId, ...getAllSubFolderIds(folderId)];
  return state.testCases.filter(tc => allIds.includes(state.folderAssignments[String(tc.id)])).length;
}
function getUnclassifiedTcs() {
  return state.testCases.filter(tc => !state.folderAssignments[String(tc.id)]);
}

// ── 폴더 선택 ─────────────────────────────────────────────────────

function selectFolder(folderId, name) {
  state.selectedFolderId = folderId;
  state.runsContext = "folder";
  const bc = document.getElementById("tcBreadcrumb");
  if (bc) bc.textContent = name;
  renderFolderTree();
  renderList();
  switchTcTab("list");   // 폴더 클릭 시 항상 목록 탭으로 전환
}

// ── 폴더 추가 (인라인 편집) ──────────────────────────────────────

function addFolder() {
  const container = document.getElementById("folderTree");
  if (container.querySelector(".folder-name-input")) {
    container.querySelector(".folder-name-input").focus();
    return;
  }
  const wrap = document.createElement("div");
  wrap.className = "folder-node-wrap";
  const node = document.createElement("div");
  node.className = "folder-node";
  node.style.cssText = "padding:6px 10px";
  const ico = document.createElement("span");
  ico.textContent = "📁"; ico.style.fontSize = "14px";
  const input = document.createElement("input");
  input.type = "text";
  input.className = "folder-name-input";
  input.placeholder = "폴더 이름 입력 후 Enter";
  input.style.cssText = "border:none;outline:none;background:transparent;font-size:12px;color:var(--text-primary);flex:1;min-width:0;font-family:var(--font)";
  node.append(ico, input);
  wrap.appendChild(node);
  container.appendChild(wrap);
  input.focus();

  let done = false;
  const confirm = () => {
    if (done) return; done = true;
    const name = input.value.trim();
    wrap.remove();
    if (name) {
      const parentId = (state.selectedFolderId && state.selectedFolderId !== "all" && state.selectedFolderId !== "unclassified")
        ? state.selectedFolderId : null;
      state.folders.push({ id: "f_" + Date.now() + "_" + Math.random().toString(36).slice(2,6), name, parentId, collapsed: false });
      persistFolders();
    }
    renderFolderTree(); renderFolderSelect(); renderList();
  };
  input.addEventListener("keydown", e => {
    if (e.key === "Enter") { e.preventDefault(); confirm(); }
    if (e.key === "Escape") { done = true; wrap.remove(); }
  });
  input.addEventListener("blur", confirm, { once: true });
}

// ── 폴더 삭제 ─────────────────────────────────────────────────────

function deleteFolder(folderId) {
  if (!window.confirm("폴더를 삭제할까요? (테스트케이스는 미분류로 이동됩니다)")) return;
  const toDelete = [folderId, ...getAllSubFolderIds(folderId)];
  state.folders = state.folders.filter(f => !toDelete.includes(f.id));
  for (const tcId of Object.keys(state.folderAssignments)) {
    if (toDelete.includes(state.folderAssignments[tcId])) delete state.folderAssignments[tcId];
  }
  if (toDelete.includes(state.selectedFolderId)) selectFolder("all", "전체");
  persistFolders(); renderFolderTree(); renderFolderSelect(); renderList();
}

// ── 폴더 이동 ─────────────────────────────────────────────────────

let _dragFolderId = null;
let _dragTcId     = null;   // 드래그 중인 테스트케이스 id

function _clearFolderDrop() {
  document.querySelectorAll("#folderTree .drop-line").forEach(l => l.classList.remove("show"));
  document.querySelectorAll("#folderTree .folder-node.drop-on").forEach(n => n.classList.remove("drop-on"));
}

function moveFolderBefore(srcId, tgtId) {
  if (srcId === tgtId || isDescendant(srcId, tgtId)) return;
  const src = state.folders.find(f => f.id === srcId);
  const tgt = state.folders.find(f => f.id === tgtId);
  if (!src || !tgt) return;
  src.parentId = tgt.parentId;
  state.folders = state.folders.filter(f => f.id !== srcId);
  state.folders.splice(state.folders.findIndex(f => f.id === tgtId), 0, src);
  persistFolders(); renderFolderTree(); renderFolderSelect(); renderList();
}
function moveFolderAfter(srcId, tgtId) {
  if (srcId === tgtId || isDescendant(srcId, tgtId)) return;
  const src = state.folders.find(f => f.id === srcId);
  const tgt = state.folders.find(f => f.id === tgtId);
  if (!src || !tgt) return;
  src.parentId = tgt.parentId;
  state.folders = state.folders.filter(f => f.id !== srcId);
  state.folders.splice(state.folders.findIndex(f => f.id === tgtId) + 1, 0, src);
  persistFolders(); renderFolderTree(); renderFolderSelect(); renderList();
}
function moveFolderInto(srcId, tgtId) {
  if (srcId === tgtId || isDescendant(srcId, tgtId)) return;
  const src = state.folders.find(f => f.id === srcId);
  const tgt = state.folders.find(f => f.id === tgtId);
  if (!src || !tgt) return;
  src.parentId = tgtId;
  tgt.collapsed = false;
  persistFolders(); renderFolderTree(); renderFolderSelect(); renderList();
}

// ── 폴더 트리 렌더 ───────────────────────────────────────────────

function renderFolderTree() {
  const container = document.getElementById("folderTree");
  container.innerHTML = "";

  // ① 전체
  const allCnt = state.testCases.length;
  const allWrap = _makeFolderNodeEl(
    "all", "📂", "전체", allCnt, state.selectedFolderId === "all", false, false, 0
  );
  const allNode = allWrap.querySelector(".folder-node");
  allNode.addEventListener("click", () => selectFolder("all", "전체"));
  // TC 드롭 → 미분류로 이동
  allNode.addEventListener("dragover", e => { e.preventDefault(); if (_dragTcId) { _clearFolderDrop(); allNode.classList.add("drop-on"); } });
  allNode.addEventListener("dragleave", () => allNode.classList.remove("drop-on"));
  allNode.addEventListener("drop", e => {
    e.preventDefault(); allNode.classList.remove("drop-on");
    if (_dragTcId) { const tid = _dragTcId; delete state.folderAssignments[String(tid)]; persistFolders(); _syncEditorFolder(tid, ""); _dragTcId = null; renderFolderTree(); renderList(); _toast("미분류로 이동됐습니다."); }
  });
  container.appendChild(allWrap);

  // ② 미분류 (항상 표시)
  const unTcs = getUnclassifiedTcs();
  const unHasTcs = unTcs.length > 0;
  const unCollapsed = state.unclassifiedCollapsed;
  const unWrap = _makeFolderNodeEl(
    "unclassified", "📁", "미분류", unTcs.length,
    state.selectedFolderId === "unclassified",
    unHasTcs, unCollapsed, 0
  );
  const unNode = unWrap.querySelector(".folder-node");
  unNode.addEventListener("click", e => {
    if (e.target.closest(".folder-caret")) return;
    selectFolder("unclassified", "미분류");
  });
  // TC 드롭 → 미분류로 이동
  unNode.addEventListener("dragover", e => { e.preventDefault(); if (_dragTcId) { _clearFolderDrop(); unNode.classList.add("drop-on"); } });
  unNode.addEventListener("dragleave", () => unNode.classList.remove("drop-on"));
  unNode.addEventListener("drop", e => {
    e.preventDefault(); unNode.classList.remove("drop-on");
    if (_dragTcId) { const tid = _dragTcId; delete state.folderAssignments[String(tid)]; persistFolders(); _syncEditorFolder(tid, ""); _dragTcId = null; renderFolderTree(); renderList(); _toast("미분류로 이동됐습니다."); }
  });
  if (unHasTcs) {
    const unCaret = unWrap.querySelector(".folder-caret");
    if (unCaret) unCaret.addEventListener("click", e => {
      e.stopPropagation();
      state.unclassifiedCollapsed = !state.unclassifiedCollapsed;
      renderFolderTree();
    });
  }
  container.appendChild(unWrap);
  if (unHasTcs && !unCollapsed) {
    _renderTcNodes(container, null, 1, true);
  }

  // ③ 사용자 폴더
  _renderFolderNodes(container, null, 0);
}

function _makeFolderNodeEl(id, icon, name, count, isActive, hasChildren, isCollapsed, depth) {
  const wrap = document.createElement("div");
  wrap.className = "folder-node-wrap";
  wrap.dataset.id = id;
  const dropLine = document.createElement("div");
  dropLine.className = "drop-line";
  wrap.appendChild(dropLine);
  const node = document.createElement("div");
  const indentClass = depth === 1 ? "folder-indent" : depth >= 2 ? "folder-indent2" : "";
  node.className = `folder-node ${indentClass}${isActive ? " active" : ""}`;
  node.dataset.id = id;
  const caretHtml = hasChildren
    ? `<span class="folder-caret ${isCollapsed ? "" : "open"}" title="접기/펼치기">▶</span>`
    : `<span class="folder-caret-placeholder"></span>`;
  node.innerHTML = `${caretHtml}<span style="font-size:${depth === 0 ? "14px" : "13px"}">${icon}</span><span class="folder-label">${escapeHtml(name)}</span><span class="folder-cnt">${count}</span>`;
  wrap.appendChild(node);
  return wrap;
}

function _renderFolderNodes(container, parentId, depth) {
  const siblings = state.folders.filter(f => (f.parentId || null) === (parentId || null));
  siblings.forEach(folder => {
    const hasSubFolders = state.folders.some(f => f.parentId === folder.id);
    const hasTcs        = state.testCases.some(tc => state.folderAssignments[String(tc.id)] === folder.id);
    const hasChildren   = hasSubFolders || hasTcs;
    const count         = getFolderTcCount(folder.id);
    const indentClass   = depth === 1 ? "folder-indent" : depth >= 2 ? "folder-indent2" : "";
    const isActive      = state.selectedFolderId === folder.id;

    const wrap = document.createElement("div");
    wrap.className = "folder-node-wrap";
    wrap.dataset.id = folder.id; wrap.dataset.depth = depth;
    if (parentId) wrap.dataset.parent = parentId;

    const dropLine = document.createElement("div");
    dropLine.className = "drop-line";
    wrap.appendChild(dropLine);

    const node = document.createElement("div");
    node.className = `folder-node ${indentClass}${isActive ? " active" : ""}`;
    node.dataset.id = folder.id;
    node.draggable  = true;

    const caretHtml = hasChildren
      ? `<span class="folder-caret ${folder.collapsed ? "" : "open"}" title="접기/펼치기">▶</span>`
      : `<span class="folder-caret-placeholder"></span>`;
    node.innerHTML = `<span class="drag-handle">⋮⋮</span>${caretHtml}<span style="font-size:${depth===0?"14px":"13px"}">📁</span><span class="folder-label">${escapeHtml(folder.name)}</span><span class="folder-cnt">${count}</span>`;

    // 삭제 버튼
    const delBtn = document.createElement("button");
    delBtn.className = "folder-del-btn";
    delBtn.title = "폴더 삭제"; delBtn.textContent = "✕";
    delBtn.style.cssText = "margin-left:auto;padding:0 4px;background:transparent;border:none;color:var(--text-muted);cursor:pointer;font-size:10px;opacity:0;transition:opacity .15s;flex-shrink:0";
    node.appendChild(delBtn);
    node.addEventListener("mouseenter", () => delBtn.style.opacity = "1");
    node.addEventListener("mouseleave", () => delBtn.style.opacity = "0");

    // 클릭
    node.addEventListener("click", e => {
      if (e.target.closest(".folder-caret") || e.target.closest(".folder-del-btn") || e.target === delBtn) return;
      selectFolder(folder.id, folder.name);
    });
    // 카렛
    const caret = node.querySelector(".folder-caret");
    if (caret) caret.addEventListener("click", e => {
      e.stopPropagation(); folder.collapsed = !folder.collapsed; persistFolders(); renderFolderTree();
    });
    // 삭제
    delBtn.addEventListener("click", e => { e.stopPropagation(); deleteFolder(folder.id); });

    // 드래그 (폴더 이동)
    node.addEventListener("dragstart", e => {
      _dragFolderId = folder.id; _dragTcId = null; e.dataTransfer.effectAllowed = "move";
      setTimeout(() => node.classList.add("drag-active"), 0);
    });
    node.addEventListener("dragover", e => {
      e.preventDefault();
      // TC 드래그 중 → 폴더 전체를 드롭 대상으로 강조
      if (_dragTcId) {
        _clearFolderDrop(); node.classList.add("drop-on"); return;
      }
      if (_dragFolderId === folder.id) return;
      _clearFolderDrop();
      const rect = node.getBoundingClientRect(); const zone = (e.clientY - rect.top) / rect.height;
      if (zone < 0.25)      dropLine.classList.add("show");
      else if (zone > 0.75) { const nw = wrap.nextElementSibling; if (nw) { const nl = nw.querySelector(".drop-line"); if (nl) nl.classList.add("show"); } else dropLine.classList.add("show"); }
      else                  node.classList.add("drop-on");
    });
    node.addEventListener("dragleave", e => { if (!wrap.contains(e.relatedTarget)) _clearFolderDrop(); });
    node.addEventListener("drop", e => {
      e.preventDefault(); _clearFolderDrop();
      // TC 드롭 → 이 폴더에 배정
      if (_dragTcId) {
        const tid = _dragTcId;
        state.folderAssignments[String(tid)] = folder.id;
        persistFolders();
        _syncEditorFolder(tid, folder.id);
        _dragTcId = null;
        renderFolderTree(); renderList();
        _toast(`'${folder.name}' 폴더로 이동됐습니다.`);
        return;
      }
      // 폴더 이동
      if (!_dragFolderId || _dragFolderId === folder.id) { _dragFolderId = null; return; }
      const rect = node.getBoundingClientRect(); const zone = (e.clientY - rect.top) / rect.height;
      if (zone < 0.25)      moveFolderBefore(_dragFolderId, folder.id);
      else if (zone > 0.75) moveFolderAfter(_dragFolderId, folder.id);
      else                  moveFolderInto(_dragFolderId, folder.id);
      _dragFolderId = null;
    });

    wrap.appendChild(node);
    container.appendChild(wrap);

    // 자식 (펼쳐진 경우)
    if (hasChildren && !folder.collapsed) {
      _renderFolderNodes(container, folder.id, depth + 1);
      _renderTcNodes(container, folder.id, depth + 1, false);
    }
  });
}

// ── 폴더 내 테스트케이스 노드 렌더 ───────────────────────────────

function _renderTcNodes(container, folderId, depth, isUnclassified) {
  const tcs = isUnclassified
    ? getUnclassifiedTcs()
    : state.testCases.filter(tc => state.folderAssignments[String(tc.id)] === folderId);

  const indentClass = depth === 1 ? "folder-indent" : depth >= 2 ? "folder-indent2" : "";
  const sLbl = { DRAFT:"초안", REVIEW_NEEDED:"검토", READY:"준비됨", COMPLETED:"완료" };
  const sCls = { DRAFT:"b-draft", REVIEW_NEEDED:"b-review", READY:"b-ready", COMPLETED:"b-done" };

  tcs.forEach(tc => {
    const wrap = document.createElement("div");
    wrap.className = "folder-node-wrap tc-in-folder";
    const node = document.createElement("div");
    node.className = `folder-node tc-node ${indentClass}${tc.id === state.selectedId ? " active" : ""}`;
    node.title = tc.title;
    node.draggable = true;
    node.innerHTML = `<span class="tc-drag-handle" title="드래그하여 폴더 이동">⋮⋮</span><span style="font-size:12px;flex-shrink:0">📄</span><span class="tc-node-label">${escapeHtml(tc.title)}</span><span class="badge ${sCls[tc.status]||"b-draft"} tc-status-badge">${sLbl[tc.status]||tc.status}</span>`;
    node.addEventListener("click", async () => { await populateForm(tc); switchTcTab("detail"); });
    // 드래그 시작
    node.addEventListener("dragstart", e => {
      _dragTcId = tc.id; _dragFolderId = null;
      e.dataTransfer.effectAllowed = "move";
      e.dataTransfer.setData("text/plain", "tc:" + tc.id);
      setTimeout(() => node.classList.add("drag-active"), 0);
    });
    wrap.appendChild(node); container.appendChild(wrap);
  });
}

document.addEventListener("dragend", () => {
  _clearFolderDrop();
  document.querySelectorAll("#folderTree .folder-node.drag-active").forEach(n => n.classList.remove("drag-active"));
  document.querySelectorAll(".tc-row-dragging").forEach(r => r.classList.remove("tc-row-dragging"));
  _dragFolderId = null;
  _dragTcId     = null;
});

// ── 폴더 선택 드롭다운 (에디터 폼) ──────────────────────────────

function renderFolderSelect() {
  if (!elements.tcFolder) return;
  const prev = elements.tcFolder.value;
  elements.tcFolder.innerHTML = '<option value="">미분류</option>';
  function addOpts(parentId, depth) {
    state.folders.filter(f => (f.parentId || null) === (parentId || null)).forEach(f => {
      const opt = document.createElement("option");
      opt.value = f.id;
      opt.textContent = "  ".repeat(depth) + f.name;
      elements.tcFolder.appendChild(opt);
      addOpts(f.id, depth + 1);
    });
  }
  addOpts(null, 0);
  if ([...elements.tcFolder.options].some(o => o.value === prev)) elements.tcFolder.value = prev;
}

// ══════════════════════════════════════════════════════════════════
// 대시보드
// ══════════════════════════════════════════════════════════════════

function renderDashboard() {
  const tcs = state.testCases; const total = tcs.length;
  const g = id => document.getElementById(id);
  if (g("dashStatTotal"))     g("dashStatTotal").textContent     = total;
  if (g("dashStatReady"))     g("dashStatReady").textContent     = tcs.filter(t=>t.status==="READY").length;
  if (g("dashStatIssues"))    g("dashStatIssues").textContent    = tcs.filter(t=>t.status==="REVIEW_NEEDED").length;
  if (g("dashStatCompleted")) g("dashStatCompleted").textContent = tcs.filter(t=>t.status==="COMPLETED").length;
  const hi  = tcs.filter(t=>t.priority==="HIGH").length;
  const mid = tcs.filter(t=>t.priority==="MEDIUM").length;
  const lo  = tcs.filter(t=>t.priority==="LOW").length;
  ["dashPriHighCnt","dashPriMidCnt","dashPriLoCnt"].forEach((id,i)=>{ if(g(id)) g(id).textContent=[hi,mid,lo][i]; });
  ["dashPriHigh","dashPriMid","dashPriLo"].forEach((id,i)=>{ if(g(id)) g(id).style.width=total?`${([hi,mid,lo][i]/total)*100}%`:"0%"; });
  const tagMap = {};
  for (const tc of tcs) for (const tag of (tc.areaTags??[])) tagMap[tag.name]=(tagMap[tag.name]||0)+1;
  const tc2 = g("dashTagChips");
  if (tc2) { const e = Object.entries(tagMap); tc2.innerHTML = e.length===0 ? '<span style="font-size:12px;color:var(--text-muted)">태그 없음</span>' : e.map(([n,c])=>`<span class="tag-chip">${escapeHtml(n)} (${c})</span>`).join(""); }
  const issues = tcs.filter(t=>t.status==="REVIEW_NEEDED");
  if (g("dashIssueCount")) g("dashIssueCount").textContent = `${issues.length}건`;
  const il = g("dashIssueList");
  if (il) { const pc = { HIGH:"b-hi",MEDIUM:"b-mid",LOW:"b-lo" }; il.innerHTML = issues.length===0 ? '<p style="font-size:12px;color:var(--text-muted)">검토가 필요한 이슈가 없습니다.</p>' : issues.map(tc=>`<div class="issue-item" onclick="switchView('testcases')"><div class="issue-item-title">${escapeHtml(tc.title)}</div><div class="issue-item-meta"><span class="badge b-review">검토 필요</span><span class="badge ${pc[tc.priority]||"b-mid"}">${escapeHtml(tc.priority??"MEDIUM")}</span></div></div>`).join(""); }
}

// ══════════════════════════════════════════════════════════════════
// Status 셀렉터
// ══════════════════════════════════════════════════════════════════

function initStatusSelector() {
  elements.statusSelector.addEventListener("click", e => {
    const btn = e.target.closest(".status-btn"); if (!btn) return;
    elements.statusSelector.querySelectorAll(".status-btn").forEach(b => b.classList.remove("active"));
    btn.classList.add("active"); elements.tcStatus.value = btn.dataset.value;
  });
}
function setStatusSelectorValue(value) {
  elements.statusSelector.querySelectorAll(".status-btn").forEach(btn => btn.classList.toggle("active", btn.dataset.value === value));
  elements.tcStatus.value = value || "DRAFT";
}

// ══════════════════════════════════════════════════════════════════
// 에디터 표시/숨김 · 헤더
// ══════════════════════════════════════════════════════════════════

function showEditor() {
  elements.detailEmpty.style.display  = "none";
  elements.detailEditor.style.display = "flex";
}
function hideEditor() {
  elements.detailEmpty.style.display  = "";
  elements.detailEditor.style.display = "none";
}
function updateDetailHeader(tc) {
  if (!tc) { elements.dtCaseId.textContent = "새 테스트케이스"; elements.dtPills.innerHTML = ""; return; }
  elements.dtCaseId.textContent = `TC-${String(tc.id).padStart(3,"0")}`;
  const sMap = { DRAFT:"b-draft",REVIEW_NEEDED:"b-review",READY:"b-ready",COMPLETED:"b-done" };
  const sLbl = { DRAFT:"초안",REVIEW_NEEDED:"검토 필요",READY:"준비됨",COMPLETED:"완료" };
  const pMap = { HIGH:"b-hi",MEDIUM:"b-mid",LOW:"b-lo" };
  const pLbl = { HIGH:"높음",MEDIUM:"중간",LOW:"낮음" };
  const s = tc.status??"DRAFT"; const p = tc.priority??"MEDIUM";
  elements.dtPills.innerHTML = `<span class="badge ${sMap[s]||"b-draft"}">${escapeHtml(sLbl[s]||s)}</span><span class="badge ${pMap[p]||"b-mid"}">${escapeHtml(pLbl[p]||p)}</span><span class="badge ${tc.type==="FUNCTIONAL"?"b-func":"b-nf"}">${escapeHtml(tc.type||"")}</span>${(tc.areaTags||[]).map(t=>`<span class="badge b-tag">${escapeHtml(t.name)}</span>`).join("")}`;
}

// ══════════════════════════════════════════════════════════════════
// 영역 태그
// ══════════════════════════════════════════════════════════════════

async function loadAreaTags() {
  try { state.areaTags = await request("/api/area-tags",{method:"GET"}); renderTagSelect(); renderFilterAreaTagSelect(); }
  catch (_e) { state.areaTags = []; }
}
function renderTagSelect() {
  elements.tagSelect.innerHTML = '<option value="">태그 선택…</option>';
  state.areaTags.filter(tag => !state.selectedTagIds.includes(tag.id)).forEach(tag => {
    const opt = document.createElement("option"); opt.value = tag.id; opt.textContent = tag.name; elements.tagSelect.appendChild(opt);
  });
}
function renderFilterAreaTagSelect() {
  const prev = elements.filterAreaTag.value;
  elements.filterAreaTag.innerHTML = '<option value="">태그 전체</option>';
  state.areaTags.forEach(tag => { const opt = document.createElement("option"); opt.value = tag.id; opt.textContent = tag.name; elements.filterAreaTag.appendChild(opt); });
  elements.filterAreaTag.value = prev;
}
function renderSelectedTagChips() {
  elements.selectedTagChips.innerHTML = "";
  state.selectedTagIds.forEach(id => {
    const tag = state.areaTags.find(t => t.id === id); if (!tag) return;
    const chip = document.createElement("span"); chip.className = "tag-chip-item";
    chip.innerHTML = `${escapeHtml(tag.name)}<button type="button" class="tag-chip-remove">×</button>`;
    chip.querySelector(".tag-chip-remove").addEventListener("click", () => { state.selectedTagIds = state.selectedTagIds.filter(i => i !== id); renderSelectedTagChips(); renderTagSelect(); });
    elements.selectedTagChips.appendChild(chip);
  });
}
function addSelectedTag(id) {
  if (!id || state.selectedTagIds.includes(id)) return;
  state.selectedTagIds.push(id); renderSelectedTagChips(); renderTagSelect();
}
async function createAndAddTag(name) {
  const trimmed = name.trim(); if (!trimmed) return;
  try {
    const created = await request("/api/area-tags",{method:"POST",body:JSON.stringify({name:trimmed})});
    state.areaTags.push(created); renderFilterAreaTagSelect(); addSelectedTag(created.id); elements.newTagInput.value = "";
  } catch (_e) {
    const existing = state.areaTags.find(t => t.name.toLowerCase() === trimmed.toLowerCase());
    if (existing) { addSelectedTag(existing.id); elements.newTagInput.value = ""; }
    else _toast(`태그 생성 실패: ${_e.message}`, true);
  }
}

// ══════════════════════════════════════════════════════════════════
// 필터
// ══════════════════════════════════════════════════════════════════

function initFilters() {
  elements.statusFilterPills.addEventListener("click", e => {
    const pill = e.target.closest(".filter-pill"); if (!pill) return;
    elements.statusFilterPills.querySelectorAll(".filter-pill").forEach(p => p.classList.remove("active")); pill.classList.add("active");
    state.filters.status = pill.dataset.value; renderList();
  });
  elements.osFilterPills.addEventListener("click", e => {
    const pill = e.target.closest(".filter-pill"); if (!pill) return;
    elements.osFilterPills.querySelectorAll(".filter-pill").forEach(p => p.classList.remove("active")); pill.classList.add("active");
    state.filters.os = pill.dataset.value; renderList();
  });
  elements.filterType.addEventListener("change",    () => { state.filters.type      = elements.filterType.value;    renderList(); });
  elements.filterAreaTag.addEventListener("change", () => { state.filters.areaTagId = elements.filterAreaTag.value; renderList(); });
  elements.filterKeyword.addEventListener("input",  () => { state.filters.keyword   = elements.filterKeyword.value.trim().toLowerCase(); renderList(); });
}

function applyFilters(testCases) {
  return testCases.filter(tc => {
    if (state.filters.status    && tc.status !== state.filters.status) return false;
    if (state.filters.os        && tc.os !== state.filters.os) return false;
    if (state.filters.type      && tc.type !== state.filters.type) return false;
    if (state.filters.areaTagId) { const tid = Number(state.filters.areaTagId); if (!tc.areaTags?.some(t => t.id === tid)) return false; }
    if (state.filters.keyword)  { const kw = state.filters.keyword; if (!(tc.title||"").toLowerCase().includes(kw) && !(tc.description||"").toLowerCase().includes(kw)) return false; }
    return true;
  });
}

// ══════════════════════════════════════════════════════════════════
// 흐름 단계
// ══════════════════════════════════════════════════════════════════

function setFlowStage(stage, hint) {
  if (elements.editorPanel) elements.editorPanel.dataset.flowStage = stage;
  if (elements.flowHint)    elements.flowHint.textContent = hint;
  const disabled = stage === "draft";
  const rp = document.getElementById("runPanel"); if (rp) rp.classList.toggle("run-panel-disabled", disabled);
  if (elements.saveRunButton) elements.saveRunButton.disabled = disabled;
}
function setSelected(id) {
  state.selectedId = id;
  if (elements.deleteButton)    elements.deleteButton.disabled    = id === null;
  if (elements.duplicateButton) elements.duplicateButton.disabled = id === null;
}

// ══════════════════════════════════════════════════════════════════
// 폼 초기화 / 채우기
// ══════════════════════════════════════════════════════════════════

function resetForm() {
  elements.form.reset(); elements.testRunForm.reset();
  elements.testCaseId.value = ""; elements.type.value = "FUNCTIONAL"; elements.priority.value = "MEDIUM"; elements.runStatus.value = "PASSED";
  elements.envOs.value = ""; elements.envBrowser.value = ""; elements.envDevice.value = "";
  if (elements.tcFolder) elements.tcFolder.value = "";
  state.selectedTagIds = []; state.currentExpected = "";
  renderSelectedTagChips(); renderTagSelect();
  setStatusSelectorValue("DRAFT"); renderSteps(); clearValidationErrors();
  setSelected(null); state.testRuns = []; state.runsContext = "tc";
  elements.editorTitle.textContent = "테스트케이스 작성";
  elements.formMode.textContent    = "새 테스트케이스를 저장할 준비가 됐습니다.";
  updateRunStatus("저장된 테스트케이스를 선택하면 실행 결과를 기록할 수 있습니다.");
  setFlowStage("draft", "초안을 작성한 뒤 저장하면 실행 결과를 기록할 수 있습니다.");
  updateDetailHeader(null);
  if (elements.runPanelCaseTitle) elements.runPanelCaseTitle.textContent = "테스트케이스를 선택하세요";
  if (elements.runPanelCaseId)   elements.runPanelCaseId.textContent = "";
  renderTestRuns(); renderList(); showEditor(); switchTcTab("detail"); elements.title.focus();
}

async function populateForm(testCase) {
  elements.testCaseId.value   = String(testCase.id);
  elements.type.value         = testCase.type        || "FUNCTIONAL";
  elements.priority.value     = testCase.priority    || "MEDIUM";
  setStatusSelectorValue(testCase.status || "DRAFT");
  elements.title.value        = testCase.title       || "";
  elements.description.value  = testCase.description || "";
  elements.precondition.value = testCase.precondition|| "";
  renderSteps(parseSteps(testCase.steps));
  elements.notes.value        = testCase.notes       || "";
  elements.envOs.value        = testCase.os          || "";
  elements.envBrowser.value   = testCase.browser     || "";
  elements.envDevice.value    = testCase.device      || "";
  if (elements.tcFolder) elements.tcFolder.value = state.folderAssignments[String(testCase.id)] || "";
  state.selectedTagIds  = (testCase.areaTags ?? []).map(t => t.id);
  state.currentExpected = testCase.expected || "";
  renderSelectedTagChips(); renderTagSelect();
  setSelected(testCase.id);
  state.runsContext = "tc";
  elements.editorTitle.textContent = testCase.title || `TC-${String(testCase.id).padStart(3,"0")}`;
  elements.formMode.textContent    = "수정 후 저장하면 PUT 요청이 전송됩니다.";
  updateRunStatus("실행 결과를 저장하면 이력에 추가됩니다.");
  setFlowStage("saved", "저장된 케이스입니다. 수정하거나 실행 결과를 기록할 수 있습니다.");
  updateDetailHeader(testCase);
  if (elements.runPanelCaseTitle) elements.runPanelCaseTitle.textContent = testCase.title;
  if (elements.runPanelCaseId)   elements.runPanelCaseId.textContent = `TC-${String(testCase.id).padStart(3,"0")} 실행 기록`;
  renderList();
  renderFolderTree();   // 폴더 트리에서 현재 TC 하이라이트 갱신
  showEditor();
  await loadTestRuns(testCase.id);
}

// ══════════════════════════════════════════════════════════════════
// 복제
// ══════════════════════════════════════════════════════════════════

function duplicateCurrentTestCase() {
  const id = elements.testCaseId.value; if (!id) return;
  const tc = state.testCases.find(item => String(item.id) === id);
  if (!tc) { _toast("복제할 테스트케이스를 찾을 수 없습니다.", true); return; }
  elements.testCaseId.value = ""; elements.type.value = tc.type||"FUNCTIONAL"; elements.priority.value = tc.priority||"MEDIUM";
  setStatusSelectorValue("DRAFT");
  elements.title.value = `${tc.title} 사본`; elements.description.value = tc.description||""; elements.precondition.value = tc.precondition||"";
  renderSteps(parseSteps(tc.steps));
  elements.notes.value = tc.notes||""; elements.envOs.value = tc.os||""; elements.envBrowser.value = tc.browser||""; elements.envDevice.value = tc.device||"";
  if (elements.tcFolder) elements.tcFolder.value = "";
  state.selectedTagIds = (tc.areaTags??[]).map(t=>t.id); state.currentExpected = "";
  renderSelectedTagChips(); renderTagSelect();
  elements.testRunForm.reset(); elements.runStatus.value = "PASSED";
  setSelected(null); state.testRuns = []; state.runsContext = "tc";
  elements.editorTitle.textContent = "테스트케이스 복제 작성"; elements.formMode.textContent = "복제본입니다. 수정 후 저장하세요.";
  updateRunStatus("복제본은 저장 후 실행 결과를 기록할 수 있습니다.");
  setFlowStage("draft","복제본 초안입니다. 수정한 뒤 저장하세요."); updateDetailHeader(null);
  renderTestRuns(); renderList(); showEditor(); switchTcTab("detail"); elements.title.focus(); elements.title.select();
}

// ══════════════════════════════════════════════════════════════════
// 페이로드
// ══════════════════════════════════════════════════════════════════

function getPayload() {
  const stepsValue = getStepsValue();
  // 백엔드가 expected를 @NotBlank로 검증하므로: 기존 값 → steps 내용 → 기본값 순으로 폴백
  const expectedValue = state.currentExpected || stepsValue || "N/A";
  return {
    type:         elements.type.value, priority: elements.priority.value,
    status:       elements.tcStatus.value || "DRAFT",
    title:        elements.title.value.trim(), description: elements.description.value.trim(),
    precondition: elements.precondition.value.trim(),
    steps:        stepsValue, expected: expectedValue,
    notes:        elements.notes.value.trim() || null,
    os:           elements.envOs.value||null, browser: elements.envBrowser.value||null,
    device:       elements.envDevice.value||null, areaTagIds: [...state.selectedTagIds]
  };
}

// ── 에디터에 열린 TC의 폴더 셀렉트 동기화 ────────────────────────
// TC가 드래그로 이동됐을 때, 에디터에 그 TC가 열려있으면 폴더 셀렉트를 즉시 갱신
function _syncEditorFolder(tcId, folderId) {
  if (String(tcId) === elements.testCaseId.value && elements.tcFolder) {
    elements.tcFolder.value = folderId || "";
  }
}

// ── 폴더 배정 저장 ────────────────────────────────────────────────
// isNew=true: 폼이 미분류여도 현재 선택된 폴더에 자동 배정

function saveFolderAssignment(tcId, isNew = false) {
  const formFolderId = elements.tcFolder ? elements.tcFolder.value : "";
  const key = String(tcId);

  let effectiveFolderId = formFolderId;
  // 새 케이스이고 폼이 미분류인데 트리에서 특정 폴더가 선택된 경우 → 그 폴더에 배정
  if (isNew && !formFolderId &&
      state.selectedFolderId &&
      state.selectedFolderId !== "all" &&
      state.selectedFolderId !== "unclassified") {
    effectiveFolderId = state.selectedFolderId;
  }

  if (effectiveFolderId) state.folderAssignments[key] = effectiveFolderId;
  else delete state.folderAssignments[key];
  persistFolders();
}

// ══════════════════════════════════════════════════════════════════
// 리스트 렌더 (테이블)
// ══════════════════════════════════════════════════════════════════

function statusBadge(status) {
  const cls = { DRAFT:"b-draft",REVIEW_NEEDED:"b-review",READY:"b-ready",COMPLETED:"b-done" }[status]||"b-draft";
  const lbl = { DRAFT:"초안",REVIEW_NEEDED:"검토 필요",READY:"준비됨",COMPLETED:"완료" }[status]||status;
  return `<span class="badge ${cls}">${escapeHtml(lbl)}</span>`;
}
function priorityBadge(priority) {
  const cls = { HIGH:"b-hi",MEDIUM:"b-mid",LOW:"b-lo" }[priority]||"b-mid";
  const lbl = { HIGH:"높음",MEDIUM:"중간",LOW:"낮음" }[priority]||priority;
  return `<span class="badge ${cls}">${escapeHtml(lbl)}</span>`;
}

function createTableRow(tc) {
  const tr = document.createElement("tr");
  if (tc.id === state.selectedId)    tr.classList.add("sel-row");
  if (tc.status === "REVIEW_NEEDED") tr.classList.add("attn-row");
  const env = [tc.os,tc.browser,tc.device].filter(Boolean);
  const tags = tc.areaTags ?? [];
  tr.draggable = true;
  tr.innerHTML = `
    <td><span class="tc-id" title="드래그하여 폴더 이동" style="cursor:grab">⋮ TC-${String(tc.id).padStart(3,"0")}</span></td>
    <td><span class="tc-ttl">${escapeHtml(tc.title)}</span>${tc.status==="REVIEW_NEEDED"?'<span class="attn-flag">⚠ 검토 필요</span>':""}</td>
    <td>${statusBadge(tc.status)}</td>
    <td>${priorityBadge(tc.priority)}</td>
    <td><span class="badge ${tc.type==="FUNCTIONAL"?"b-func":"b-nf"}">${tc.type==="FUNCTIONAL"?"기능":"비기능"}</span></td>
    <td>${env.length?`<span class="badge b-tag">${env.map(escapeHtml).join(" · ")}</span>`:'<span style="font-size:11px;color:var(--text-muted)">미지정</span>'}</td>
    <td>${tags.map(t=>`<span class="badge b-tag">${escapeHtml(t.name)}</span>`).join(" ")}</td>
    <td><span style="font-size:11px;color:var(--text-muted)">-</span></td>`;
  // 클릭: 상세 보기
  tr.addEventListener("click", async () => { await populateForm(tc); switchTcTab("detail"); });
  // 드래그: 폴더로 이동
  tr.addEventListener("dragstart", e => {
    _dragTcId = tc.id; _dragFolderId = null;
    e.dataTransfer.effectAllowed = "move";
    e.dataTransfer.setData("text/plain", "tc:" + tc.id);
    tr.classList.add("tc-row-dragging");
  });
  tr.addEventListener("dragend", () => {
    tr.classList.remove("tc-row-dragging");
    _dragTcId = null;
    _clearFolderDrop();
  });
  return tr;
}

// ── 그룹 헤더 행 생성 ────────────────────────────────────────────
function _makeGrpRow(name, count, reviewCount) {
  const tr = document.createElement("tr");
  tr.className = "grp-row";
  const badge = reviewCount > 0
    ? `<span class="badge b-review" style="margin-left:8px;font-size:10px">검토 ${reviewCount}건</span>`
    : "";
  tr.innerHTML = `<td colspan="8">📁 ${escapeHtml(name)}&nbsp;&nbsp;${count}${badge}</td>`;
  return tr;
}

// ── 전체 선택 시 최상위 폴더별 그룹 ────────────────────────────
function _renderGroupedAll(filtered) {
  const topFolders = state.folders.filter(f => !f.parentId);

  topFolders.forEach(folder => {
    const allIds = [folder.id, ...getAllSubFolderIds(folder.id)];
    const tcs = filtered.filter(tc => allIds.includes(state.folderAssignments[String(tc.id)]));
    if (tcs.length === 0) return;
    const reviewCnt = tcs.filter(tc => tc.status === "REVIEW_NEEDED").length;
    elements.list.appendChild(_makeGrpRow(folder.name, tcs.length, reviewCnt));
    tcs.forEach(tc => elements.list.appendChild(createTableRow(tc)));
  });

  // 미분류 그룹
  const unTcs = filtered.filter(tc => !state.folderAssignments[String(tc.id)]);
  if (unTcs.length > 0) {
    const reviewCnt = unTcs.filter(tc => tc.status === "REVIEW_NEEDED").length;
    elements.list.appendChild(_makeGrpRow("미분류", unTcs.length, reviewCnt));
    unTcs.forEach(tc => elements.list.appendChild(createTableRow(tc)));
  }
}

// ── 하위 폴더가 있는 폴더 선택 시 하위 폴더별 그룹 ──────────────
function _renderGroupedBySubFolders(filtered, parentFolderId) {
  const subFolders = state.folders.filter(f => f.parentId === parentFolderId);

  subFolders.forEach(sub => {
    const allIds = [sub.id, ...getAllSubFolderIds(sub.id)];
    const tcs = filtered.filter(tc => allIds.includes(state.folderAssignments[String(tc.id)]));
    if (tcs.length === 0) return;
    const reviewCnt = tcs.filter(tc => tc.status === "REVIEW_NEEDED").length;
    elements.list.appendChild(_makeGrpRow(sub.name, tcs.length, reviewCnt));
    tcs.forEach(tc => elements.list.appendChild(createTableRow(tc)));
  });

  // 부모 폴더에 직접 배정된 TC (하위 폴더에 속하지 않는 것)
  const directTcs = filtered.filter(tc => state.folderAssignments[String(tc.id)] === parentFolderId);
  if (directTcs.length > 0) {
    const parentFolder = state.folders.find(f => f.id === parentFolderId);
    const reviewCnt = directTcs.filter(tc => tc.status === "REVIEW_NEEDED").length;
    elements.list.appendChild(_makeGrpRow((parentFolder?.name || "") + " (기타)", directTcs.length, reviewCnt));
    directTcs.forEach(tc => elements.list.appendChild(createTableRow(tc)));
  }
}

function renderList() {
  elements.list.innerHTML = "";
  let filtered = applyFilters(state.testCases);

  // 폴더 필터
  if (state.selectedFolderId === "unclassified") {
    filtered = filtered.filter(tc => !state.folderAssignments[String(tc.id)]);
  } else if (state.selectedFolderId && state.selectedFolderId !== "all") {
    const allIds = [state.selectedFolderId, ...getAllSubFolderIds(state.selectedFolderId)];
    filtered = filtered.filter(tc => allIds.includes(state.folderAssignments[String(tc.id)]));
  }

  if (elements.mainSearchCount) elements.mainSearchCount.textContent = `${filtered.length}개`;
  if (filtered.length === 0) {
    updateStatus(state.testCases.length === 0 ? "저장된 테스트케이스가 없습니다." : "조건에 맞는 케이스가 없습니다.");
    return;
  }
  updateStatus("");

  const fid = state.selectedFolderId;

  // "전체" 선택 & 폴더가 하나라도 있으면 → 최상위 폴더별 그룹
  if (fid === "all" && state.folders.length > 0) {
    _renderGroupedAll(filtered);
    return;
  }

  // 특정 폴더 선택 & 하위 폴더가 있으면 → 하위 폴더별 그룹
  if (fid && fid !== "all" && fid !== "unclassified" &&
      state.folders.some(f => f.parentId === fid)) {
    _renderGroupedBySubFolders(filtered, fid);
    return;
  }

  // 그 외 (리프 폴더, 미분류) → 단순 평면 목록
  filtered.forEach(tc => elements.list.appendChild(createTableRow(tc)));
}

// ══════════════════════════════════════════════════════════════════
// 실행 기록 렌더
// ══════════════════════════════════════════════════════════════════

function renderTestRuns() {
  elements.testRunList.innerHTML = "";
  if (elements.runStats) {
    const g = id => document.getElementById(id);
    const pass   = state.testRuns.filter(r=>r.status==="PASSED").length;
    const fail   = state.testRuns.filter(r=>r.status==="FAILED").length;
    const block  = state.testRuns.filter(r=>r.status==="BLOCKED").length;
    const retest = state.testRuns.filter(r=>r.status==="RETEST").length;
    if (g("rsPass"))   g("rsPass").textContent   = pass;
    if (g("rsFail"))   g("rsFail").textContent   = fail;
    if (g("rsBlock"))  g("rsBlock").textContent  = block;
    if (g("rsRetest")) g("rsRetest").textContent = retest;
    elements.runStats.style.display = state.testRuns.length > 0 ? "grid" : "none";
  }
  state.testRuns.forEach(run => {
    const li = _buildRunItemLi(run, {
      onDelete: () => deleteTestRun(run.id),
      onUpdate: payload => updateTestRun(run.id, payload)
    });
    elements.testRunList.appendChild(li);
  });
}

const RUN_DOT_COLOR = { PASSED:"var(--c-pass)",FAILED:"var(--c-hi)",BLOCKED:"#ccc",SKIPPED:"#ccc",IN_PROGRESS:"var(--c-ready)",RETEST:"var(--c-review)",NOT_EXECUTED:"#ccc" };

const RUN_STATUS_OPTIONS_HTML = `
  <option value="PASSED">✅ PASSED — 통과</option>
  <option value="FAILED">❌ FAILED — 실패</option>
  <option value="BLOCKED">🚫 BLOCKED — 차단</option>
  <option value="SKIPPED">⏭ SKIPPED — 건너뜀</option>
  <option value="IN_PROGRESS">🔄 IN_PROGRESS</option>
  <option value="RETEST">🔁 RETEST — 재테스트</option>
  <option value="NOT_EXECUTED">⬜ NOT_EXECUTED</option>`;

// handlers: { onDelete(), onUpdate(payload) }
function _buildRunItemLi(run, handlers) {
  const li = document.createElement("li"); li.className = "run-item";
  _renderRunItemView(li, run, handlers);
  return li;
}

function _renderRunItemView(li, run, handlers) {
  li.classList.remove("run-item-editing");
  li.innerHTML = `<div class="run-dot" style="background:${RUN_DOT_COLOR[run.status]||"#ccc"}"></div><div class="run-info"><div class="run-hd"><span class="run-status run-status-${escapeHtml(run.status.toLowerCase())}">${escapeHtml(run.status)}</span><span class="run-date">${escapeHtml(formatDateTime(run.executedAt))}</span></div><div class="run-note">${escapeHtml(run.actualResult)}</div>${run.notes?`<div class="run-note" style="color:var(--text-muted);margin-top:2px">${escapeHtml(run.notes)}</div>`:""}</div><button type="button" class="btn btn-sm btn-danger run-del-btn">삭제</button>`;
  li.querySelector(".run-del-btn").addEventListener("click", e => { e.stopPropagation(); handlers.onDelete(); });
  const info = li.querySelector(".run-info");
  info.style.cursor = "pointer";
  info.title = "클릭하여 수정";
  info.addEventListener("click", () => _renderRunItemEdit(li, run, handlers));
}

function _renderRunItemEdit(li, run, handlers) {
  li.classList.add("run-item-editing");
  li.innerHTML = `
    <div class="run-info" style="flex:1">
      <div class="run-field">
        <label>실행 결과</label>
        <select class="form-input ri-status">${RUN_STATUS_OPTIONS_HTML}</select>
      </div>
      <div class="run-field">
        <label>실제 결과</label>
        <textarea class="form-input ri-actual" rows="3">${escapeHtml(run.actualResult)}</textarea>
      </div>
      <div class="run-field">
        <label>실행 메모</label>
        <textarea class="form-input ri-notes" rows="2">${escapeHtml(run.notes || "")}</textarea>
      </div>
      <div style="display:flex;justify-content:flex-end;gap:6px">
        <button type="button" class="btn btn-sm ri-cancel">취소</button>
        <button type="button" class="btn btn-pri btn-sm ri-save">저장</button>
      </div>
    </div>`;
  li.querySelector(".ri-status").value = run.status;
  li.querySelector(".ri-cancel").addEventListener("click", () => _renderRunItemView(li, run, handlers));
  li.querySelector(".ri-save").addEventListener("click", async () => {
    const status = li.querySelector(".ri-status").value;
    const actualResult = li.querySelector(".ri-actual").value.trim();
    const notes = li.querySelector(".ri-notes").value.trim();
    if (!actualResult) { _toast("실제 결과를 입력해주세요.", true); return; }
    await handlers.onUpdate({ status, actualResult, notes: notes || null });
  });
}

async function updateTestRun(runId, payload) {
  const testCaseId = elements.testCaseId.value; if (!testCaseId) return;
  try {
    await request(`/api/testcases/${testCaseId}/runs/${runId}`, { method: "PUT", body: JSON.stringify(payload) });
    updateRunStatus(`실행 결과 #${runId} 수정됐습니다.`);
    await loadTestRuns(testCaseId);
  } catch (e) { updateRunStatus(`수정 실패: ${e.message}`); }
}

// ══════════════════════════════════════════════════════════════════
// 폴더 단위 실행 기록 개요
// ══════════════════════════════════════════════════════════════════

function _getSelectedFolderName() {
  const fid = state.selectedFolderId;
  if (fid === "all")          return "전체";
  if (fid === "unclassified") return "미분류";
  return state.folders.find(f => f.id === fid)?.name || "";
}

// 목록 탭의 그룹핑 로직(_renderGroupedAll / _renderGroupedBySubFolders)과 동일한 기준으로
// { name, tcs } 그룹 배열을 만든다.
function _getFolderRunGroups() {
  const filtered = applyFilters(state.testCases);
  const fid = state.selectedFolderId;
  const groups = [];

  if (fid === "unclassified") {
    const tcs = filtered.filter(tc => !state.folderAssignments[String(tc.id)]);
    if (tcs.length) groups.push({ name: "미분류", tcs });
    return groups;
  }

  if (fid === "all" && state.folders.length > 0) {
    state.folders.filter(f => !f.parentId).forEach(folder => {
      const allIds = [folder.id, ...getAllSubFolderIds(folder.id)];
      const tcs = filtered.filter(tc => allIds.includes(state.folderAssignments[String(tc.id)]));
      if (tcs.length) groups.push({ name: folder.name, tcs });
    });
    const unTcs = filtered.filter(tc => !state.folderAssignments[String(tc.id)]);
    if (unTcs.length) groups.push({ name: "미분류", tcs: unTcs });
    return groups;
  }

  if (fid && fid !== "all" && state.folders.some(f => f.parentId === fid)) {
    state.folders.filter(f => f.parentId === fid).forEach(sub => {
      const allIds = [sub.id, ...getAllSubFolderIds(sub.id)];
      const tcs = filtered.filter(tc => allIds.includes(state.folderAssignments[String(tc.id)]));
      if (tcs.length) groups.push({ name: sub.name, tcs });
    });
    const directTcs = filtered.filter(tc => state.folderAssignments[String(tc.id)] === fid);
    if (directTcs.length) {
      const parentFolder = state.folders.find(f => f.id === fid);
      groups.push({ name: (parentFolder?.name || "") + " (기타)", tcs: directTcs });
    }
    return groups;
  }

  if (fid && fid !== "all") {
    const allIds = [fid, ...getAllSubFolderIds(fid)];
    const tcs = filtered.filter(tc => allIds.includes(state.folderAssignments[String(tc.id)]));
    if (tcs.length) groups.push({ name: state.folders.find(f => f.id === fid)?.name || "", tcs });
    return groups;
  }

  if (filtered.length) groups.push({ name: "전체", tcs: filtered });
  return groups;
}

async function renderFolderRunsOverview() {
  const titleEl = document.getElementById("folderRunsTitle");
  const idEl    = document.getElementById("folderRunsId");
  const stateEl = document.getElementById("folderRunsState");
  const body    = document.getElementById("folderRunsBody");
  if (!body) return;

  const groups = _getFolderRunGroups();
  const totalTcs = groups.reduce((sum, g) => sum + g.tcs.length, 0);

  if (titleEl) titleEl.textContent = _getSelectedFolderName();
  if (idEl)    idEl.textContent    = "실행 기록";

  if (totalTcs === 0) {
    body.innerHTML = "";
    if (stateEl) { stateEl.style.display = "block"; stateEl.textContent = "표시할 테스트케이스가 없습니다."; }
    return;
  }
  if (stateEl) stateEl.style.display = "none";
  body.innerHTML = `<div class="frun-empty">실행 이력을 불러오는 중...</div>`;

  // 그룹에 속한 모든 테스트케이스의 실행 이력을 한 번에 조회
  const allTcs = groups.flatMap(g => g.tcs);
  const runsByTcId = {};
  await Promise.all(allTcs.map(async tc => {
    try {
      runsByTcId[tc.id] = await request(`/api/testcases/${tc.id}/runs`, { method: "GET" });
    } catch (_e) {
      runsByTcId[tc.id] = [];
    }
  }));

  body.innerHTML = "";
  groups.forEach(group => {
    const grpEl = document.createElement("div");
    grpEl.className = "frun-grp";
    grpEl.textContent = `📁 ${group.name} ${group.tcs.length}`;
    body.appendChild(grpEl);
    group.tcs.forEach(tc => body.appendChild(_buildFrunCard(tc, runsByTcId[tc.id] || [])));
  });
}

function _buildFrunCard(tc, runs) {
  const card = document.createElement("div");
  card.className = "frun-card";

  const pass   = runs.filter(r=>r.status==="PASSED").length;
  const fail   = runs.filter(r=>r.status==="FAILED").length;
  const block  = runs.filter(r=>r.status==="BLOCKED").length;
  const retest = runs.filter(r=>r.status==="RETEST").length;

  card.innerHTML = `
    <div class="frun-card-title">${escapeHtml(tc.title)}</div>
    <div class="state-banner" style="margin-bottom:10px">${runs.length ? `총 ${runs.length}개의 실행 이력이 있습니다.` : `아직 실행 이력이 없습니다.`}</div>
    <div class="run-stats" style="display:grid">
      <div class="rs"><div class="rs-n" style="color:var(--c-pass)">${pass}</div><div class="rs-l">통과</div></div>
      <div class="rs"><div class="rs-n" style="color:var(--c-hi)">${fail}</div><div class="rs-l">실패</div></div>
      <div class="rs"><div class="rs-n">${block}</div><div class="rs-l">차단</div></div>
      <div class="rs"><div class="rs-n">${retest}</div><div class="rs-l">재확인</div></div>
    </div>
    <div class="frun-toggle">
      실행 이력 보기
      <span class="frun-arrow">▾</span>
    </div>
    <div class="frun-detail" style="display:none"></div>`;

  const toggle = card.querySelector(".frun-toggle");
  const detail = card.querySelector(".frun-detail");
  toggle.addEventListener("click", () => {
    const open = detail.style.display !== "none";
    if (open) {
      detail.style.display = "none";
      toggle.classList.remove("open");
    } else {
      if (!detail.dataset.built) _buildFrunDetail(detail, tc, runs);
      detail.style.display = "block";
      toggle.classList.add("open");
    }
  });

  return card;
}

function _buildFrunDetail(detail, tc, runs) {
  detail.dataset.built = "1";

  if (runs.length === 0) {
    detail.innerHTML = `<div class="frun-empty">실행 이력이 없습니다.</div>`;
    return;
  }

  const list = document.createElement("ul");
  list.className = "run-list";
  runs.forEach(run => list.appendChild(_buildRunItemLi(run, {
    onDelete: () => _deleteFrunRun(tc, run.id, detail),
    onUpdate: payload => _updateFrunRun(tc, run.id, payload, detail)
  })));
  detail.appendChild(list);
}

async function _updateFrunRun(tc, runId, payload, detail) {
  try {
    await request(`/api/testcases/${tc.id}/runs/${runId}`, { method: "PUT", body: JSON.stringify(payload) });
    const updatedRuns = await request(`/api/testcases/${tc.id}/runs`, { method: "GET" });
    _refreshFrunCard(detail, tc, updatedRuns);
  } catch (e) { _toast(`수정 실패: ${e.message}`, true); }
}

async function _deleteFrunRun(tc, runId, detail) {
  if (!window.confirm(`실행 결과 #${runId}를 삭제할까요?`)) return;
  try {
    await request(`/api/testcases/${tc.id}/runs/${runId}`, { method: "DELETE" });
    const updatedRuns = await request(`/api/testcases/${tc.id}/runs`, { method: "GET" });
    _refreshFrunCard(detail, tc, updatedRuns);
  } catch (e) { _toast(`삭제 실패: ${e.message}`, true); }
}

function _refreshFrunCard(detail, tc, updatedRuns) {
  const card = detail.closest(".frun-card");
  const newCard = _buildFrunCard(tc, updatedRuns);
  card.replaceWith(newCard);
  // 갱신 후에는 펼쳐진 상태를 유지
  newCard.querySelector(".frun-toggle").click();
}

// ══════════════════════════════════════════════════════════════════
// API
// ══════════════════════════════════════════════════════════════════

async function request(path, options = {}) {
  const response = await window.desktopApi.request({
    url: `${state.apiBaseUrl}${path}`,
    method: options.method ?? "GET",
    headers: { "Content-Type": "application/json", ...(options.headers ?? {}) },
    body: options.body ?? null
  });
  if (!response.ok) {
    let msg = `HTTP ${response.status}`;
    try { msg = response.data?.message ?? msg; } catch (_e) {}
    console.error(`[API] ${options.method ?? "GET"} ${path} →`, response.status, response.data);
    throw new Error(msg);
  }
  return response.status === 204 ? null : response.data;
}

async function loadTestCases() {
  state.apiBaseUrl = getApiBaseUrl();
  updateStatus("불러오는 중...");
  try {
    state.testCases = await request("/api/testcases", { method: "GET" });
    renderFolderTree(); renderList();
  } catch (e) {
    state.testCases = []; renderFolderTree(); renderList();
    updateStatus(`연결 실패: ${e.message}`);
  }
}

async function loadTestRuns(testCaseId) {
  updateRunStatus("실행 이력을 불러오는 중입니다.");
  try {
    state.testRuns = await request(`/api/testcases/${testCaseId}/runs`, { method: "GET" });
    renderTestRuns();
    updateRunStatus(state.testRuns.length === 0 ? "아직 실행 이력이 없습니다." : `총 ${state.testRuns.length}개의 실행 이력이 있습니다.`);
  } catch (e) {
    state.testRuns = []; renderTestRuns(); updateRunStatus(`조회 실패: ${e.message}`);
  }
}

// ══════════════════════════════════════════════════════════════════
// 폼 제출 / CRUD
// ══════════════════════════════════════════════════════════════════

async function handleSubmit(event) {
  event.preventDefault();
  state.apiBaseUrl = getApiBaseUrl();

  // ── 유효성 검사 ──
  clearValidationErrors();
  for (const { element, label, getValue } of requiredFieldConfigs) {
    if (!getValue()) {
      _toast(`${label} 항목을 입력해주세요.`, true);
      element.setCustomValidity(`${label}은(는) 필수 입력입니다.`);
      try { element.reportValidity(); } catch (_e) {}
      return;
    }
  }
  if (!validateSteps()) return;

  const payload = getPayload();
  const id      = elements.testCaseId.value;
  console.log("[Save] payload:", payload, "id:", id);

  try {
    if (id) {
      // ── 수정 ──
      const updated = await request(`/api/testcases/${id}`, { method: "PUT", body: JSON.stringify(payload) });
      saveFolderAssignment(updated.id);
      _toast(`TC-${String(updated.id).padStart(3,"0")} 수정 완료`);
      setFlowStage("saved", "수정이 반영됐습니다.");
      await loadTestCases();
      const refreshed = state.testCases.find(tc => String(tc.id) === id);
      if (refreshed) await populateForm(refreshed);
    } else {
      // ── 생성 ──
      const created = await request("/api/testcases", { method: "POST", body: JSON.stringify(payload) });
      saveFolderAssignment(created.id, true);  // isNew: 선택된 폴더 자동 배정
      _toast(`TC-${String(created.id).padStart(3,"0")} 생성 완료`);
      setFlowStage("saved", "저장됐습니다. 실행 결과를 기록할 수 있습니다.");
      await loadTestCases();
      const createdItem = state.testCases.find(tc => tc.id === created.id);
      if (createdItem) { await populateForm(createdItem); elements.actualResult.focus(); }
    }
  } catch (e) {
    console.error("[Save] failed:", e);
    _toast(`저장 실패: ${e.message}`, true);
  }
}

async function handleTestRunSubmit(event) {
  event.preventDefault();
  const testCaseId = elements.testCaseId.value;
  if (!testCaseId) { updateRunStatus("저장된 테스트케이스를 먼저 선택하세요."); return; }
  if (!elements.actualResult.value.trim()) { _toast("실제 결과를 입력해주세요.", true); return; }
  try {
    const created = await request(`/api/testcases/${testCaseId}/runs`, {
      method: "POST",
      body: JSON.stringify({ status: elements.runStatus.value, actualResult: elements.actualResult.value.trim(), notes: elements.runNotes.value.trim() || null })
    });
    updateRunStatus(`실행 결과 #${created.id} 저장됐습니다.`);
    setFlowStage("run", "실행 결과가 기록됐습니다.");
    elements.testRunForm.reset(); elements.runStatus.value = "PASSED";
    await loadTestRuns(testCaseId);
  } catch (e) { updateRunStatus(`저장 실패: ${e.message}`); }
}

async function deleteTestRun(runId) {
  const testCaseId = elements.testCaseId.value; if (!testCaseId) return;
  if (!window.confirm(`실행 결과 #${runId}를 삭제할까요?`)) return;
  try {
    await request(`/api/testcases/${testCaseId}/runs/${runId}`, { method: "DELETE" });
    updateRunStatus(`실행 결과 #${runId} 삭제됐습니다.`); await loadTestRuns(testCaseId);
  } catch (e) { updateRunStatus(`삭제 실패: ${e.message}`); }
}

async function handleDelete() {
  const id = elements.testCaseId.value; if (!id) return;
  if (!window.confirm(`테스트케이스 #${id}를 삭제할까요?`)) return;
  try {
    await request(`/api/testcases/${id}`, { method: "DELETE" });
    delete state.folderAssignments[id]; persistFolders();
    _toast(`TC-${String(id).padStart(3,"0")} 삭제 완료`);
    hideEditor(); setSelected(null); state.testRuns = []; elements.testCaseId.value = "";
    switchTcTab("list"); await loadTestCases();
  } catch (e) { _toast(`삭제 실패: ${e.message}`, true); }
}

async function verifyConnection() {
  state.apiBaseUrl = getApiBaseUrl();
  try { await request("/api/testcases",{method:"GET"}); _toast(`연결 성공: ${state.apiBaseUrl}`); }
  catch (e) { _toast(`연결 실패: ${e.message}`, true); }
}

// ══════════════════════════════════════════════════════════════════
// 검증
// ══════════════════════════════════════════════════════════════════

function clearValidationErrors() {
  requiredFieldConfigs.forEach(({ element }) => { if (element) element.setCustomValidity(""); });
}
function validateSteps() {
  const inputs = Array.from(elements.stepsList.querySelectorAll(".step-input"));
  if (!inputs.some(inp => inp.value.trim())) {
    _toast("Steps를 최소 1개 이상 입력해주세요.", true);
    if (inputs[0]) inputs[0].focus();
    return false;
  }
  return true;
}

// ══════════════════════════════════════════════════════════════════
// Steps (단계만)
// ══════════════════════════════════════════════════════════════════

function createStepRow(stepValue = "") {
  const row = document.createElement("div"); row.className = "step-row";
  const index = document.createElement("span"); index.className = "step-index";
  const input = document.createElement("textarea");
  input.className = "step-input"; input.placeholder = "단계를 입력하세요."; input.value = stepValue; input.rows = 2;
  input.addEventListener("input", clearValidationErrors);
  const btn = document.createElement("button"); btn.type = "button"; btn.className = "step-remove-btn"; btn.textContent = "삭제";
  btn.addEventListener("click", () => { row.remove(); if (elements.stepsList.children.length === 0) appendStepRow(); renumberSteps(); clearValidationErrors(); });
  row.append(index, input, btn);
  return row;
}
function appendStepRow(v = "") { elements.stepsList.appendChild(createStepRow(v)); renumberSteps(); }
function renderSteps(steps = [""]) { elements.stepsList.innerHTML = ""; steps.forEach(s => appendStepRow(s)); }
function renumberSteps() { elements.stepsList.querySelectorAll(".step-row").forEach((row,i) => row.querySelector(".step-index").textContent = i + 1); }
function getStepsValue() { return Array.from(elements.stepsList.querySelectorAll(".step-input")).map(inp => inp.value.trim()).filter(Boolean).join("\n"); }
function parseSteps(v) { const s = String(v??"").split("\n").map(s=>s.trim()).filter(Boolean); return s.length > 0 ? s : [""]; }

// ══════════════════════════════════════════════════════════════════
// 부트스트랩
// ══════════════════════════════════════════════════════════════════

async function bootstrap() {
  const config = await window.desktopApi.getConfig();
  if (elements.platformPill) elements.platformPill.textContent = config.platform;
  if (elements.versionPill)  elements.versionPill.textContent  = `v${config.version}`;
  ["platform-pill-info","version-pill-info"].forEach((id,i) => {
    const el = document.getElementById(id); if (el) el.textContent = [config.platform, `v${config.version}`][i];
  });

  state.apiBaseUrl = localStorage.getItem("tms.apiBaseUrl") || config.defaultApiBaseUrl;
  if (elements.apiBaseUrl) elements.apiBaseUrl.value = state.apiBaseUrl;

  loadFolders();
  initStatusSelector();
  initFilters();

  elements.form.addEventListener("submit", handleSubmit);
  elements.testRunForm.addEventListener("submit", handleTestRunSubmit);
  elements.refreshButton.addEventListener("click",   loadTestCases);
  elements.connectButton.addEventListener("click",   verifyConnection);
  elements.newButton.addEventListener("click",       resetForm);
  elements.duplicateButton.addEventListener("click", duplicateCurrentTestCase);
  elements.resetButton.addEventListener("click",     resetForm);
  elements.deleteButton.addEventListener("click",    handleDelete);
  elements.addStepButton.addEventListener("click",   () => appendStepRow());

  const fab = document.querySelector(".folder-add-btn");
  if (fab) fab.addEventListener("click", addFolder);

  elements.addTagButton.addEventListener("click",   () => { const id = Number(elements.tagSelect.value); if (id) { addSelectedTag(id); elements.tagSelect.value = ""; } });
  elements.createTagButton.addEventListener("click",() => createAndAddTag(elements.newTagInput.value));
  elements.newTagInput.addEventListener("keydown",  e => { if (e.key === "Enter") { e.preventDefault(); createAndAddTag(elements.newTagInput.value); } });

  requiredFieldConfigs.forEach(({ element }) => {
    if (!element) return;
    element.addEventListener("input",  () => element.setCustomValidity(""));
    element.addEventListener("change", () => element.setCustomValidity(""));
  });
  elements.apiBaseUrl.addEventListener("change", () => {
    state.apiBaseUrl = getApiBaseUrl(); localStorage.setItem("tms.apiBaseUrl", state.apiBaseUrl);
  });

  // 초기 상태
  hideEditor(); switchTcTab("list");
  await loadAreaTags();
  await loadTestCases();
  renderFolderSelect();
}

bootstrap();
