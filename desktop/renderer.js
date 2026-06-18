// ── 상수 ─────────────────────────────────────────────────────────
const FOLDER_KEY              = "tms.folders";
const FOLDER_ASSIGN_KEY       = "tms.folderAssignments";
const SUITE_FOLDER_KEY        = "tms.suiteFolders";
const SUITE_FOLDER_ASSIGN_KEY = "tms.suiteFolderAssignments";

// ── 상태 ─────────────────────────────────────────────────────────
const state = {
  apiBaseUrl: "http://localhost:8080",
  selectedId: null,
  testCases: [],
  testRuns: [],
  testPlans: [],
  testSuites: [],
  selectedPlanId: null,
  selectedSuiteId: null,
  areaTags: [],
  allDefects: [],
  serverEnvironments: [],
  testConfigurations: [],
  selectedConfigurationId: null,
  selectedTagIds: [],
  folders: [],              // { id, name, parentId, collapsed }
  folderAssignments: {},    // { "tcId": folderId | null }
  suiteFolders: [],         // { id, name, parentId, collapsed }
  suiteFolderAssignments: {}, // { "suiteId": folderId | null }
  selectedFolderId: "all",
  runsContext: "folder",   // "folder" | "tc" — 실행 기록 탭에서 보여줄 대상
  unclassifiedCollapsed: false,
  folderSearchQuery: "",
  sort: "updated_desc",
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
  envServer:         document.getElementById("envServer"),
  testConfiguration: document.getElementById("testConfiguration"),
  newServerEnvName:  document.getElementById("newServerEnvName"),
  newServerEnvType:  document.getElementById("newServerEnvType"),
  newServerEnvUrl:   document.getElementById("newServerEnvUrl"),
  createServerEnvButton: document.getElementById("createServerEnvButton"),
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
  const map = { dashboard: "navDash", testcases: "navTC", plans: "navPlans", settings: "navSet" };
  document.querySelectorAll(".nav-tab").forEach(b => b.classList.remove("active"));
  const t = document.getElementById(map[v]);
  if (t) t.classList.add("active");
  if (v === "dashboard") renderDashboard();
  if (v === "plans") loadTestPlans();
  if (v === "settings") loadTestConfigurations();
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
function setSort(el, sort, label) {
  document.querySelectorAll(".sort-item").forEach(i => i.classList.remove("active"));
  el.classList.add("active");
  state.sort = sort;
  document.getElementById("sortMenu").classList.remove("show");
  renderList();
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

function _getFolderDepth(folderId) {
  let depth = 0;
  let cur = state.folders.find(f => f.id === folderId);
  while (cur && cur.parentId) { depth++; cur = state.folders.find(f => f.id === cur.parentId); }
  return depth;
}

function _indentClass(depth) {
  if (depth === 1) return "folder-indent";
  if (depth === 2) return "folder-indent2";
  if (depth === 3) return "folder-indent3";
  if (depth >= 4)  return "folder-indent4";
  return "";
}

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

function getVisibleFolderIdsForSearch() {
  const query = state.folderSearchQuery;
  if (!query) return null;

  const visibleIds = new Set();
  state.folders.filter(folder => folder.name.toLocaleLowerCase("ko").includes(query)).forEach(folder => {
    let current = folder;
    while (current) {
      visibleIds.add(current.id);
      current = current.parentId ? state.folders.find(item => item.id === current.parentId) : null;
    }
  });
  return visibleIds;
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

function _openFolderNameInput(parentId) {
  const container = document.getElementById("folderTree");
  if (container.querySelector(".folder-name-input")) {
    container.querySelector(".folder-name-input").focus();
    return;
  }
  const wrap = document.createElement("div");
  wrap.className = "folder-node-wrap";
  const node = document.createElement("div");
  node.className = "folder-node";
  const depth = parentId ? _getFolderDepth(parentId) + 1 : 0;
  const indentClass = _indentClass(depth);
  if (indentClass) node.classList.add(indentClass);
  node.style.cssText = "padding:6px 10px";
  const ico = document.createElement("span");
  ico.textContent = "📁"; ico.style.fontSize = depth === 0 ? "14px" : "13px";
  const input = document.createElement("input");
  input.type = "text";
  input.className = "folder-name-input";
  input.placeholder = "폴더 이름 입력 후 Enter";
  input.style.cssText = "border:none;outline:none;background:transparent;font-size:12px;color:var(--text-primary);flex:1;min-width:0;font-family:var(--font)";
  node.append(ico, input);
  wrap.appendChild(node);

  if (parentId) {
    // 부모 폴더 노드 바로 다음에 삽입
    const parentWrap = container.querySelector(`.folder-node-wrap[data-id="${CSS.escape(String(parentId))}"]`);
    if (parentWrap && parentWrap.nextSibling) container.insertBefore(wrap, parentWrap.nextSibling);
    else container.appendChild(wrap);
  } else {
    container.appendChild(wrap);
  }
  input.focus();

  let done = false;
  const confirm = () => {
    if (done) return; done = true;
    const name = input.value.trim();
    wrap.remove();
    if (name) {
      state.folders.push({ id: "f_" + Date.now() + "_" + Math.random().toString(36).slice(2,6), name, parentId: parentId || null, collapsed: false });
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

function addFolder() {
  const parentId = (state.selectedFolderId && state.selectedFolderId !== "all" && state.selectedFolderId !== "unclassified")
    ? state.selectedFolderId : null;
  _openFolderNameInput(parentId);
}

function addSubFolder(parentId) {
  _openFolderNameInput(parentId);
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

function renameFolder(folderId) {
  const folder = state.folders.find(f => f.id === folderId);
  if (!folder) return;
  const newName = window.prompt("폴더 이름 변경", folder.name);
  if (!newName || !newName.trim() || newName.trim() === folder.name) return;
  folder.name = newName.trim();
  persistFolders(); renderFolderTree(); renderFolderSelect();
}

// ── 폴더 컨텍스트 메뉴 ────────────────────────────────────────────
const _folderCtxMenu = (() => {
  const el = document.createElement("div");
  el.className = "folder-ctx-menu";
  el.innerHTML = `
    <div class="folder-ctx-item" data-action="add-sub">📁 하위 폴더 추가</div>
    <div class="folder-ctx-item" data-action="rename">✏️ 이름 변경</div>
    <div class="folder-ctx-sep"></div>
    <div class="folder-ctx-item danger" data-action="delete">🗑 삭제</div>
  `;
  document.body.appendChild(el);

  let _targetId = null;

  const hide = () => { el.classList.remove("show"); _targetId = null; };

  el.addEventListener("click", e => {
    const item = e.target.closest(".folder-ctx-item");
    if (!item || !_targetId) return;
    const action = item.dataset.action;
    hide();
    if (action === "add-sub")  addSubFolder(_targetId);
    if (action === "rename")   renameFolder(_targetId);
    if (action === "delete")   deleteFolder(_targetId);
  });

  document.addEventListener("click", hide, true);
  document.addEventListener("keydown", e => { if (e.key === "Escape") hide(); });

  return {
    show(folderId, x, y) {
      _targetId = folderId;
      el.style.left = x + "px";
      el.style.top  = y + "px";
      el.classList.add("show");
      // 화면 밖으로 나가면 위로 올리기
      const rect = el.getBoundingClientRect();
      if (rect.bottom > window.innerHeight) el.style.top = (y - rect.height) + "px";
      if (rect.right  > window.innerWidth)  el.style.left = (x - rect.width)  + "px";
    }
  };
})();

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
  const isSearching = Boolean(state.folderSearchQuery);
  const visibleFolderIds = getVisibleFolderIdsForSearch();
  const matchesSpecialFolder = name => name.toLocaleLowerCase("ko").includes(state.folderSearchQuery);

  // ① 전체
  if (!isSearching || matchesSpecialFolder("전체")) {
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
  }

  // ② 미분류 (항상 표시)
  const unTcs = getUnclassifiedTcs();
  const unHasTcs = unTcs.length > 0;
  const unCollapsed = state.unclassifiedCollapsed;
  if (!isSearching || matchesSpecialFolder("미분류")) {
    const unWrap = _makeFolderNodeEl(
      "unclassified", "📁", "미분류", unTcs.length,
      state.selectedFolderId === "unclassified",
      unHasTcs && !isSearching, unCollapsed, 0
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
    if (unHasTcs && !isSearching) {
      const unCaret = unWrap.querySelector(".folder-caret");
      if (unCaret) unCaret.addEventListener("click", e => {
        e.stopPropagation();
        state.unclassifiedCollapsed = !state.unclassifiedCollapsed;
        renderFolderTree();
      });
    }
    container.appendChild(unWrap);
    if (unHasTcs && !unCollapsed && !isSearching) {
      _renderTcNodes(container, null, 1, true);
    }
  }

  // ③ 사용자 폴더
  _renderFolderNodes(container, null, 0, visibleFolderIds);

  if (isSearching && container.children.length === 0) {
    const empty = document.createElement("p");
    empty.className = "folder-search-empty";
    empty.textContent = "일치하는 폴더가 없습니다.";
    container.appendChild(empty);
  }
}

function _makeFolderNodeEl(id, icon, name, count, isActive, hasChildren, isCollapsed, depth) {
  const wrap = document.createElement("div");
  wrap.className = "folder-node-wrap";
  wrap.dataset.id = id;
  const dropLine = document.createElement("div");
  dropLine.className = "drop-line";
  wrap.appendChild(dropLine);
  const node = document.createElement("div");
  const indentClass = _indentClass(depth);
  node.className = `folder-node ${indentClass}${isActive ? " active" : ""}`;
  node.dataset.id = id;
  const caretHtml = hasChildren
    ? `<span class="folder-caret ${isCollapsed ? "" : "open"}" title="접기/펼치기">▶</span>`
    : `<span class="folder-caret-placeholder"></span>`;
  node.innerHTML = `${caretHtml}<span style="font-size:${depth === 0 ? "14px" : "13px"}">${icon}</span><span class="folder-label">${escapeHtml(name)}</span><span class="folder-cnt">${count}</span>`;
  wrap.appendChild(node);
  return wrap;
}

function _renderFolderNodes(container, parentId, depth, visibleFolderIds = null) {
  const isSearching = visibleFolderIds !== null;
  const siblings = state.folders.filter(f =>
    (f.parentId || null) === (parentId || null) && (!visibleFolderIds || visibleFolderIds.has(f.id))
  );
  siblings.forEach(folder => {
    const hasSubFolders = state.folders.some(f => f.parentId === folder.id);
    const hasTcs        = state.testCases.some(tc => state.folderAssignments[String(tc.id)] === folder.id);
    const hasChildren   = hasSubFolders || hasTcs;
    const count         = getFolderTcCount(folder.id);
    const indentClass   = _indentClass(depth);
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
    node.draggable  = !isSearching;

    const caretHtml = hasChildren
      ? `<span class="folder-caret ${folder.collapsed ? "" : "open"}" title="접기/펼치기">▶</span>`
      : `<span class="folder-caret-placeholder"></span>`;
    node.innerHTML = `<span class="drag-handle">⋮⋮</span>${caretHtml}<span style="font-size:${depth===0?"14px":"13px"}">📁</span><span class="folder-label">${escapeHtml(folder.name)}</span>`;

    // 하위 폴더 추가 버튼 (margin-left:auto 로 오른쪽 정렬 시작)
    const addBtn = document.createElement("button");
    addBtn.className = "folder-sub-add-btn";
    addBtn.title = "하위 폴더 추가"; addBtn.textContent = "+";
    addBtn.style.cssText = "padding:0 5px;background:transparent;border:none;color:var(--text-muted);cursor:pointer;font-size:14px;opacity:0;transition:opacity .15s;flex-shrink:0;line-height:1";
    node.appendChild(addBtn);

    // 삭제 버튼
    const delBtn = document.createElement("button");
    delBtn.className = "folder-del-btn";
    delBtn.title = "폴더 삭제"; delBtn.textContent = "✕";
    delBtn.style.cssText = "padding:0 4px;background:transparent;border:none;color:var(--text-muted);cursor:pointer;font-size:10px;opacity:0;transition:opacity .15s;flex-shrink:0;line-height:1";
    node.appendChild(delBtn);

    // 카운트 배지
    const cntBadge = document.createElement("span");
    cntBadge.className = "folder-cnt";
    cntBadge.textContent = count;
    node.appendChild(cntBadge);

    node.addEventListener("mouseenter", () => { addBtn.style.opacity = "1"; delBtn.style.opacity = "1"; cntBadge.style.display = "none"; });
    node.addEventListener("mouseleave", () => { addBtn.style.opacity = "0"; delBtn.style.opacity = "0"; cntBadge.style.display = ""; });

    // 클릭
    node.addEventListener("click", e => {
      if (e.target.closest(".folder-caret") || e.target.closest(".folder-del-btn") || e.target === delBtn || e.target.closest(".folder-sub-add-btn") || e.target === addBtn) return;
      selectFolder(folder.id, folder.name);
    });
    // 카렛
    const caret = node.querySelector(".folder-caret");
    if (caret) caret.addEventListener("click", e => {
      e.stopPropagation(); folder.collapsed = !folder.collapsed; persistFolders(); renderFolderTree();
    });
    // 삭제
    delBtn.addEventListener("click", e => { e.stopPropagation(); deleteFolder(folder.id); });

    // 하위 폴더 추가
    addBtn.addEventListener("click", e => { e.stopPropagation(); addSubFolder(folder.id); });

    // 우클릭 컨텍스트 메뉴
    node.addEventListener("contextmenu", e => {
      e.preventDefault(); e.stopPropagation();
      _folderCtxMenu.show(folder.id, e.clientX, e.clientY);
    });

    // 드래그 (폴더 이동)
    if (!isSearching) {
      node.addEventListener("dragstart", e => {
        _dragFolderId = folder.id; _dragTcId = null; e.dataTransfer.effectAllowed = "move";
        setTimeout(() => node.classList.add("drag-active"), 0);
      });
    }
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
    if (hasChildren && (isSearching || !folder.collapsed)) {
      _renderFolderNodes(container, folder.id, depth + 1, visibleFolderIds);
      if (!isSearching) _renderTcNodes(container, folder.id, depth + 1, false);
    }
  });
}

// ── 폴더 내 테스트케이스 노드 렌더 ───────────────────────────────

function _renderTcNodes(container, folderId, depth, isUnclassified) {
  const tcs = isUnclassified
    ? getUnclassifiedTcs()
    : state.testCases.filter(tc => state.folderAssignments[String(tc.id)] === folderId);

  const indentClass = _indentClass(depth);
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
  _clearSuiteDrop();
  document.querySelectorAll("#folderTree .folder-node.drag-active, #suiteList .folder-node.drag-active").forEach(n => n.classList.remove("drag-active"));
  document.querySelectorAll(".tc-row-dragging").forEach(r => r.classList.remove("tc-row-dragging"));
  _dragFolderId      = null;
  _dragTcId          = null;
  _dragSuiteId       = null;
  _dragSuiteFolderId = null;
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

async function loadServerEnvironments() {
  try {
    state.serverEnvironments = await request("/api/server-environments", { method: "GET" });
    renderServerEnvironmentSelect();
    renderServerEnvManageList();
  } catch (_error) {
    state.serverEnvironments = [];
    renderServerEnvironmentSelect();
    renderServerEnvManageList();
  }
}

function renderServerEnvManageList() {
  const container = document.getElementById("serverEnvManageList");
  if (!container) return;
  container.innerHTML = "";
  if (state.serverEnvironments.length === 0) {
    container.innerHTML = `<p style="font-size:11px;color:var(--text-muted)">등록된 서버 환경이 없습니다.</p>`;
    return;
  }
  state.serverEnvironments.forEach(env => {
    const row = document.createElement("div");
    row.className = "server-env-row";
    row.innerHTML = `
      <span class="server-env-row-name">${escapeHtml(env.name)}</span>
      <span class="badge ${env.active ? "dst-RESOLVED" : "dst-CLOSED"}">${env.type}${env.active ? "" : " · 비활성"}</span>
      <span class="server-env-row-url" title="${escapeHtml(env.baseUrl)}">${escapeHtml(env.baseUrl)}</span>
      <button type="button" class="btn btn-sm se-toggle-btn">${env.active ? "비활성화" : "활성화"}</button>
      <button type="button" class="btn btn-sm se-edit-btn">✎</button>
      <button type="button" class="btn btn-sm btn-danger se-delete-btn">🗑</button>`;
    row.querySelector(".se-toggle-btn").addEventListener("click", () =>
      updateServerEnvironment(env.id, { ...toEnvRequest(env), active: !env.active }));
    row.querySelector(".se-edit-btn").addEventListener("click", () => showServerEnvEdit(row, env));
    row.querySelector(".se-delete-btn").addEventListener("click", () => deleteServerEnvironment(env.id, env.name));
    container.appendChild(row);
  });
}

function toEnvRequest(env) {
  return { name: env.name, type: env.type, baseUrl: env.baseUrl, description: env.description ?? null, active: env.active };
}

function showServerEnvEdit(row, env) {
  row.innerHTML = `
    <input type="text" class="form-input se-name" maxlength="100" value="${escapeHtml(env.name)}" placeholder="환경 이름" style="flex:1;min-width:0">
    <select class="form-input se-type">
      <option value="LOCAL">LOCAL</option><option value="DEVELOPMENT">DEVELOPMENT</option>
      <option value="STAGING">STAGING</option><option value="PRODUCTION">PRODUCTION</option>
    </select>
    <input type="url" class="form-input se-url" maxlength="500" value="${escapeHtml(env.baseUrl)}" placeholder="URL" style="flex:1.4;min-width:0">
    <button type="button" class="btn btn-sm btn-pri se-save-btn">저장</button>
    <button type="button" class="btn btn-sm se-cancel-btn">취소</button>`;
  row.querySelector(".se-type").value = env.type;
  row.querySelector(".se-cancel-btn").addEventListener("click", renderServerEnvManageList);
  row.querySelector(".se-save-btn").addEventListener("click", () => {
    const name = row.querySelector(".se-name").value.trim();
    const baseUrl = row.querySelector(".se-url").value.trim();
    if (!name || !baseUrl) { _toast("이름과 URL을 입력하세요.", true); return; }
    updateServerEnvironment(env.id, {
      name, baseUrl, type: row.querySelector(".se-type").value,
      description: env.description ?? null, active: env.active
    });
  });
}

async function updateServerEnvironment(id, payload) {
  try {
    await request(`/api/server-environments/${id}`, { method: "PUT", body: JSON.stringify(payload) });
    _toast("서버 환경을 수정했습니다.");
    await loadServerEnvironments();
  } catch (e) { _toast(`서버 환경 수정 실패: ${e.message}`, true); }
}

async function deleteServerEnvironment(id, name) {
  if (!confirm(`서버 환경 "${name}"을(를) 삭제할까요?`)) return;
  try {
    await request(`/api/server-environments/${id}`, { method: "DELETE" });
    _toast("서버 환경을 삭제했습니다.");
    await loadServerEnvironments();
  } catch (e) { _toast(`서버 환경 삭제 실패: ${e.message}`, true); }
}

function renderServerEnvironmentSelect() {
  const previous = elements.envServer.value;
  elements.envServer.innerHTML = '<option value="">선택 안 함</option>';
  state.serverEnvironments.forEach(environment => {
    const option = document.createElement("option");
    option.value = environment.id;
    option.textContent = `${environment.name} · ${environment.type}${environment.active ? "" : " (비활성)"}`;
    option.title = environment.baseUrl;
    elements.envServer.appendChild(option);
  });
  if ([...elements.envServer.options].some(option => option.value === previous)) {
    elements.envServer.value = previous;
  }
  renderConfigurationServerSelect();
}

async function createServerEnvironment() {
  const name = elements.newServerEnvName.value.trim();
  const baseUrl = elements.newServerEnvUrl.value.trim();
  if (!name || !baseUrl) {
    _toast("서버 환경 이름과 URL을 입력해주세요.", true);
    return;
  }
  try {
    const created = await request("/api/server-environments", {
      method: "POST",
      body: JSON.stringify({
        name,
        type: elements.newServerEnvType.value,
        baseUrl,
        description: null,
        active: true
      })
    });
    state.serverEnvironments.push(created);
    state.serverEnvironments.sort((a, b) => a.name.localeCompare(b.name, "ko"));
    renderServerEnvironmentSelect();
    elements.envServer.value = String(created.id);
    elements.newServerEnvName.value = "";
    elements.newServerEnvUrl.value = "";
    _toast("서버 환경을 등록했습니다.");
  } catch (error) {
    _toast(`서버 환경 등록 실패: ${error.message}`, true);
  }
}

async function loadTestConfigurations() {
  try {
    state.testConfigurations = await request("/api/test-configurations", { method: "GET" });
    renderTestConfigurationSelect();
    renderConfigurationList();
  } catch (error) {
    state.testConfigurations = [];
    renderTestConfigurationSelect();
    renderConfigurationList();
    _toast(`Configuration 조회 실패: ${error.message}`, true);
  }
}

function renderTestConfigurationSelect() {
  const previous = elements.testConfiguration.value;
  elements.testConfiguration.innerHTML = '<option value="">직접 설정</option>';
  state.testConfigurations.forEach(configuration => {
    const option = document.createElement("option");
    option.value = configuration.id;
    option.textContent = `${configuration.name}${configuration.active ? "" : " (비활성)"}`;
    elements.testConfiguration.appendChild(option);
  });
  if ([...elements.testConfiguration.options].some(option => option.value === previous)) {
    elements.testConfiguration.value = previous;
  }
}

function applySelectedConfiguration() {
  const id = Number(elements.testConfiguration.value);
  const configuration = state.testConfigurations.find(item => item.id === id);
  if (!configuration) return;
  elements.envServer.value = configuration.serverEnvironment?.id
    ? String(configuration.serverEnvironment.id) : "";
  elements.envOs.value = configuration.os || "";
  elements.envBrowser.value = configuration.browser || "";
  elements.envDevice.value = configuration.device || "";
}

function renderConfigurationServerSelect() {
  const select = document.getElementById("configurationServer");
  if (!select) return;
  const previous = select.value;
  select.innerHTML = '<option value="">선택 안 함</option>';
  state.serverEnvironments.forEach(environment => {
    const option = document.createElement("option");
    option.value = environment.id;
    option.textContent = `${environment.name} · ${environment.type}`;
    select.appendChild(option);
  });
  if ([...select.options].some(option => option.value === previous)) select.value = previous;
}

function renderConfigurationList() {
  const list = document.getElementById("configurationList");
  if (!list) return;
  list.innerHTML = "";
  if (state.testConfigurations.length === 0) {
    list.innerHTML = '<div class="plan-empty">등록된 configuration이 없습니다.</div>';
    return;
  }
  state.testConfigurations.forEach(configuration => {
    const button = document.createElement("button");
    button.type = "button";
    button.className = `configuration-list-item${configuration.id === state.selectedConfigurationId ? " active" : ""}`;
    const detail = [configuration.serverEnvironment?.name, configuration.os, configuration.browser, configuration.device]
      .filter(Boolean).join(" · ") || "환경 미지정";
    button.innerHTML = `<div class="plan-item-name">${escapeHtml(configuration.name)}</div><div class="plan-item-meta"><span class="badge ${configuration.active ? "b-done" : "b-tag"}">${configuration.active ? "ACTIVE" : "INACTIVE"}</span><span>${escapeHtml(detail)}</span></div>`;
    button.addEventListener("click", () => editConfiguration(configuration));
    list.appendChild(button);
  });
}

function resetConfigurationForm() {
  state.selectedConfigurationId = null;
  document.getElementById("configurationForm").reset();
  document.getElementById("configurationId").value = "";
  document.getElementById("configurationActive").checked = true;
  document.getElementById("deleteConfigurationButton").disabled = true;
  renderConfigurationList();
  document.getElementById("configurationName").focus();
}

function editConfiguration(configuration) {
  state.selectedConfigurationId = configuration.id;
  document.getElementById("configurationId").value = configuration.id;
  document.getElementById("configurationName").value = configuration.name;
  document.getElementById("configurationServer").value = configuration.serverEnvironment?.id
    ? String(configuration.serverEnvironment.id) : "";
  document.getElementById("configurationOs").value = configuration.os || "";
  document.getElementById("configurationBrowser").value = configuration.browser || "";
  document.getElementById("configurationDevice").value = configuration.device || "";
  document.getElementById("configurationActive").checked = configuration.active;
  document.getElementById("deleteConfigurationButton").disabled = false;
  renderConfigurationList();
}

async function saveConfiguration(event) {
  event.preventDefault();
  const id = document.getElementById("configurationId").value;
  const serverEnvironmentId = document.getElementById("configurationServer").value;
  const payload = {
    name: document.getElementById("configurationName").value.trim(),
    serverEnvironmentId: serverEnvironmentId ? Number(serverEnvironmentId) : null,
    os: document.getElementById("configurationOs").value || null,
    browser: document.getElementById("configurationBrowser").value || null,
    device: document.getElementById("configurationDevice").value || null,
    active: document.getElementById("configurationActive").checked
  };
  try {
    const saved = await request(id ? `/api/test-configurations/${id}` : "/api/test-configurations", {
      method: id ? "PUT" : "POST", body: JSON.stringify(payload)
    });
    state.selectedConfigurationId = saved.id;
    _toast(id ? "Configuration을 수정했습니다." : "Configuration을 생성했습니다.");
    await loadTestConfigurations();
    editConfiguration(state.testConfigurations.find(item => item.id === saved.id));
  } catch (error) { _toast(`Configuration 저장 실패: ${error.message}`, true); }
}

async function deleteConfiguration() {
  if (!state.selectedConfigurationId || !window.confirm("Configuration을 삭제할까요?")) return;
  try {
    await request(`/api/test-configurations/${state.selectedConfigurationId}`, { method: "DELETE" });
    _toast("Configuration을 삭제했습니다.");
    resetConfigurationForm();
    await loadTestConfigurations();
  } catch (error) { _toast(`Configuration 삭제 실패: ${error.message}`, true); }
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
  elements.envOs.value = ""; elements.envBrowser.value = ""; elements.envDevice.value = ""; elements.envServer.value = ""; elements.testConfiguration.value = "";
  if (elements.tcFolder) elements.tcFolder.value = "";
  state.selectedTagIds = [];
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
  // 저장 전에는 결함 연결·첨부파일을 사용할 수 없으므로 섹션을 숨긴다
  const defSection = document.getElementById("defectSection");
  if (defSection) defSection.style.display = "none";
  const attSection = document.getElementById("tcAttachSection");
  if (attSection) attSection.style.display = "none";
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
  elements.envServer.value    = testCase.serverEnvironment?.id ? String(testCase.serverEnvironment.id) : "";
  elements.testConfiguration.value = testCase.testConfiguration?.id ? String(testCase.testConfiguration.id) : "";
  if (elements.tcFolder) elements.tcFolder.value = state.folderAssignments[String(testCase.id)] || "";
  state.selectedTagIds  = (testCase.areaTags ?? []).map(t => t.id);
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
  renderDefects(testCase.defects || []);
  loadTestCaseAttachments(testCase.id);
}

// ══════════════════════════════════════════════════════════════════
// 결함 / Jira
// ══════════════════════════════════════════════════════════════════

const _sevLabel = { CRITICAL:"CRITICAL", MAJOR:"MAJOR", MINOR:"MINOR", TRIVIAL:"TRIVIAL" };
const _dstLabel = { OPEN:"OPEN", IN_PROGRESS:"진행중", RESOLVED:"해결됨", CLOSED:"종료" };

function renderDefects(defects) {
  const section = document.getElementById("defectSection");
  const list    = document.getElementById("defectList");
  const empty   = document.getElementById("defectEmpty");
  if (!section || !list) return;

  // 저장된 케이스에서는 결함 섹션을 항상 노출한다 (추가/연결 가능)
  section.style.display = "";
  list.innerHTML = "";

  if (!defects || defects.length === 0) {
    if (empty) empty.style.display = "";
    return;
  }
  if (empty) empty.style.display = "none";

  defects.forEach(d => {
    const item = document.createElement("div");
    item.className = "defect-item";

    const jiraKeyHtml = d.jiraKey
      ? `<span class="defect-jira-key">${escapeHtml(d.jiraKey)}</span>`
      : `<span style="font-size:11px;color:var(--text-muted)">Jira 미연결</span>`;
    const urlHtml = d.externalUrl
      ? `<a href="${escapeHtml(d.externalUrl)}" target="_blank" rel="noreferrer" class="defect-ext-link" title="${escapeHtml(d.externalUrl)}">↗ 외부 링크</a>`
      : "";

    item.innerHTML = `
      <div class="defect-item-top">
        <span class="badge sev-${d.severity}">${_sevLabel[d.severity] ?? d.severity}</span>
        <span class="badge dst-${d.status}">${_dstLabel[d.status] ?? d.status}</span>
        <span class="defect-title" title="${escapeHtml(d.title)}">${escapeHtml(d.title)}</span>
        ${jiraKeyHtml}
      </div>
      ${d.description ? `<div class="defect-desc">${escapeHtml(d.description)}</div>` : ""}
      <div class="defect-actions">
        ${urlHtml}
        <button type="button" class="btn btn-sm jira-push-btn" data-id="${d.id}" title="TMS 결함 → Jira 이슈 생성 또는 업데이트">↑ Jira Push</button>
        <button type="button" class="btn btn-sm jira-pull-btn" data-id="${d.id}" ${d.jiraKey ? "" : "disabled"} title="Jira 상태 → TMS 결함 상태 동기화">↓ Jira Pull</button>
        <button type="button" class="btn btn-sm jira-link-toggle-btn" data-id="${d.id}">🔗 이슈 연결</button>
        <button type="button" class="btn btn-sm defect-edit-btn" data-id="${d.id}">✎ 수정</button>
        <button type="button" class="btn btn-sm defect-unlink-btn" data-id="${d.id}" title="이 케이스에서 연결만 해제 (결함은 유지)">⊘ 연결 해제</button>
        <button type="button" class="btn btn-sm btn-danger defect-delete-btn" data-id="${d.id}" title="결함을 완전히 삭제">🗑 삭제</button>
      </div>
      <div class="defect-link-row" style="display:none">
        <input type="text" placeholder="Jira 이슈 키 입력 (예: TMS-42)" maxlength="50">
        <button type="button" class="btn btn-sm btn-pri jira-link-confirm-btn" data-id="${d.id}">연결</button>
        <button type="button" class="btn btn-sm jira-link-cancel-btn">취소</button>
      </div>
    `;

    // 이슈 연결 토글
    item.querySelector(".jira-link-toggle-btn").addEventListener("click", () => {
      const row = item.querySelector(".defect-link-row");
      row.style.display = row.style.display === "none" ? "flex" : "none";
    });
    item.querySelector(".jira-link-cancel-btn").addEventListener("click", () => {
      item.querySelector(".defect-link-row").style.display = "none";
    });
    item.querySelector(".jira-push-btn").addEventListener("click", async () => {
      await _jiraAction(`/api/defects/${d.id}/jira/push`, "POST", "Jira Push 완료");
    });
    item.querySelector(".jira-pull-btn").addEventListener("click", async () => {
      await _jiraAction(`/api/defects/${d.id}/jira/pull`, "POST", "Jira Pull 완료");
    });
    item.querySelector(".jira-link-confirm-btn").addEventListener("click", async () => {
      const key = item.querySelector(".defect-link-row input").value.trim();
      if (!key) { _toast("Jira 이슈 키를 입력하세요.", true); return; }
      await _jiraAction(`/api/defects/${d.id}/jira/link`, "POST", `${key} 연결 완료`, { jiraKey: key });
    });
    item.querySelector(".defect-edit-btn").addEventListener("click", () => showDefectEditForm(item, d));
    item.querySelector(".defect-unlink-btn").addEventListener("click", () => unlinkDefect(d.id));
    item.querySelector(".defect-delete-btn").addEventListener("click", () => deleteDefect(d.id, d.title));

    list.appendChild(item);
  });
}

// 인라인 수정 폼
function showDefectEditForm(item, d) {
  item.innerHTML = `
    <div class="defect-create-form" style="display:block;border:none;padding:0;margin:0;background:none">
      <input type="text" class="form-input de-title" maxlength="200" value="${escapeHtml(d.title)}" placeholder="결함 제목 *">
      <textarea class="form-input de-desc" rows="2" placeholder="결함 설명">${escapeHtml(d.description || "")}</textarea>
      <div class="defect-create-row">
        <select class="form-input de-sev">
          <option value="CRITICAL">CRITICAL</option><option value="MAJOR">MAJOR</option>
          <option value="MINOR">MINOR</option><option value="TRIVIAL">TRIVIAL</option>
        </select>
        <select class="form-input de-status">
          <option value="OPEN">OPEN</option><option value="IN_PROGRESS">진행중</option>
          <option value="RESOLVED">해결됨</option><option value="CLOSED">종료</option>
        </select>
        <input type="url" class="form-input de-url" maxlength="500" value="${escapeHtml(d.externalUrl || "")}" placeholder="외부 URL (선택)">
      </div>
      <div class="defect-create-actions">
        <button type="button" class="btn btn-sm de-cancel">취소</button>
        <button type="button" class="btn btn-sm btn-pri de-save">저장</button>
      </div>
    </div>`;
  item.querySelector(".de-sev").value = d.severity;
  item.querySelector(".de-status").value = d.status;
  item.querySelector(".de-cancel").addEventListener("click", () => refreshCurrentTestCase());
  item.querySelector(".de-save").addEventListener("click", async () => {
    const title = item.querySelector(".de-title").value.trim();
    if (!title) { _toast("결함 제목을 입력하세요.", true); return; }
    await updateDefect(d.id, {
      title,
      description: item.querySelector(".de-desc").value.trim() || null,
      severity: item.querySelector(".de-sev").value,
      status: item.querySelector(".de-status").value,
      externalUrl: item.querySelector(".de-url").value.trim() || null
    });
  });
}

async function refreshCurrentTestCase() {
  const id = elements.testCaseId.value;
  if (!id) return;
  const fresh = await request(`/api/testcases/${id}`, { method: "GET" });
  await populateForm(fresh);
}

// 새 결함 생성 후 현재 케이스에 연결
async function createDefectAndLink() {
  const tcId = elements.testCaseId.value;
  if (!tcId) { _toast("먼저 테스트케이스를 저장하세요.", true); return; }
  const title = document.getElementById("defectTitle").value.trim();
  if (!title) { _toast("결함 제목을 입력하세요.", true); return; }
  try {
    const created = await request("/api/defects", {
      method: "POST",
      body: JSON.stringify({
        title,
        description: document.getElementById("defectDescription").value.trim() || null,
        severity: document.getElementById("defectSeverity").value,
        status: document.getElementById("defectStatus").value,
        externalUrl: document.getElementById("defectExternalUrl").value.trim() || null
      })
    });
    await request(`/api/testcases/${tcId}/defects/${created.id}`, { method: "POST" });
    _toast("결함을 생성하고 연결했습니다.");
    hideDefectForms();
    ["defectTitle", "defectDescription", "defectExternalUrl"].forEach(id => { document.getElementById(id).value = ""; });
    await refreshCurrentTestCase();
  } catch (e) { _toast(`결함 생성 실패: ${e.message}`, true); }
}

async function updateDefect(defectId, payload) {
  try {
    await request(`/api/defects/${defectId}`, { method: "PUT", body: JSON.stringify(payload) });
    _toast("결함을 수정했습니다.");
    await refreshCurrentTestCase();
  } catch (e) { _toast(`결함 수정 실패: ${e.message}`, true); }
}

async function unlinkDefect(defectId) {
  const tcId = elements.testCaseId.value;
  if (!tcId) return;
  try {
    await request(`/api/testcases/${tcId}/defects/${defectId}`, { method: "DELETE" });
    _toast("연결을 해제했습니다.");
    await refreshCurrentTestCase();
  } catch (e) { _toast(`연결 해제 실패: ${e.message}`, true); }
}

async function deleteDefect(defectId, title) {
  if (!confirm(`결함 "${title}"을(를) 완전히 삭제할까요? 이 작업은 되돌릴 수 없습니다.`)) return;
  try {
    await request(`/api/defects/${defectId}`, { method: "DELETE" });
    _toast("결함을 삭제했습니다.");
    await refreshCurrentTestCase();
  } catch (e) { _toast(`결함 삭제 실패: ${e.message}`, true); }
}

async function loadAllDefects() {
  try {
    state.allDefects = await request("/api/defects", { method: "GET" });
  } catch (_e) { state.allDefects = []; }
}

async function openDefectLinkForm() {
  hideDefectForms();
  await loadAllDefects();
  const select = document.getElementById("defectLinkSelect");
  const linked = new Set((state.allDefects.length ? collectLinkedDefectIds() : []));
  select.innerHTML = '<option value="">결함을 선택하세요</option>';
  state.allDefects
    .filter(d => !linked.has(d.id))
    .forEach(d => {
      const opt = document.createElement("option");
      opt.value = d.id;
      opt.textContent = `[${d.severity}] ${d.title}${d.jiraKey ? " · " + d.jiraKey : ""}`;
      select.appendChild(opt);
    });
  if (select.options.length === 1) {
    _toast("연결할 수 있는 기존 결함이 없습니다.", true);
    return;
  }
  document.getElementById("defectLinkForm").style.display = "block";
}

function collectLinkedDefectIds() {
  const ids = [];
  document.querySelectorAll("#defectList .defect-unlink-btn").forEach(b => ids.push(Number(b.dataset.id)));
  return ids;
}

async function linkExistingDefect() {
  const tcId = elements.testCaseId.value;
  const defectId = document.getElementById("defectLinkSelect").value;
  if (!tcId || !defectId) { _toast("연결할 결함을 선택하세요.", true); return; }
  try {
    await request(`/api/testcases/${tcId}/defects/${defectId}`, { method: "POST" });
    _toast("결함을 연결했습니다.");
    hideDefectForms();
    await refreshCurrentTestCase();
  } catch (e) { _toast(`결함 연결 실패: ${e.message}`, true); }
}

function hideDefectForms() {
  const c = document.getElementById("defectCreateForm");
  const l = document.getElementById("defectLinkForm");
  if (c) c.style.display = "none";
  if (l) l.style.display = "none";
}

function toggleDefectCreateForm() {
  const c = document.getElementById("defectCreateForm");
  const willShow = c.style.display === "none";
  hideDefectForms();
  c.style.display = willShow ? "block" : "none";
}

// ══════════════════════════════════════════════════════════════════
// 첨부파일
// ══════════════════════════════════════════════════════════════════

function formatFileSize(bytes) {
  if (bytes == null) return "";
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

// container 에 첨부파일 목록을 렌더링한다. reload 는 목록 새로고침 콜백.
function renderAttachmentList(container, items, reload) {
  container.innerHTML = "";
  if (!items || items.length === 0) {
    container.innerHTML = `<p style="font-size:12px;color:var(--text-muted)">첨부된 파일이 없습니다.</p>`;
    return;
  }
  items.forEach(att => {
    const row = document.createElement("div");
    row.className = "attach-item";
    row.innerHTML = `
      <span class="attach-icon">📄</span>
      <span class="attach-name" title="${escapeHtml(att.originalFilename)}">${escapeHtml(att.originalFilename)}</span>
      <span class="attach-size">${formatFileSize(att.fileSize)}</span>
      <button type="button" class="btn btn-sm attach-download-btn">⬇ 다운로드</button>
      <button type="button" class="btn btn-sm btn-danger attach-delete-btn">🗑</button>`;
    row.querySelector(".attach-download-btn").addEventListener("click", () => downloadAttachmentFile(att));
    row.querySelector(".attach-delete-btn").addEventListener("click", () => deleteAttachmentFile(att.id, reload));
    container.appendChild(row);
  });
}

async function loadTestCaseAttachments(tcId) {
  const section = document.getElementById("tcAttachSection");
  const list    = document.getElementById("tcAttachList");
  if (!section || !list) return;
  section.style.display = "";
  try {
    const items = await request(`/api/testcases/${tcId}/attachments`, { method: "GET" });
    renderAttachmentList(list, items, () => loadTestCaseAttachments(tcId));
  } catch (e) {
    list.innerHTML = `<p style="font-size:12px;color:var(--c-hi)">첨부파일을 불러오지 못했습니다.</p>`;
  }
}

async function uploadAttachmentTo(uploadPath, reload) {
  if (!window.desktopApi?.uploadAttachment) {
    _toast("이 환경에서는 파일 업로드를 지원하지 않습니다.", true);
    return;
  }
  try {
    const res = await window.desktopApi.uploadAttachment({ url: `${state.apiBaseUrl}${uploadPath}` });
    if (res?.canceled) return;
    if (!res?.ok) {
      _toast(`업로드 실패: ${res?.data?.message || "HTTP " + res?.status}`, true);
      return;
    }
    _toast("파일을 업로드했습니다.");
    if (reload) await reload();
  } catch (e) { _toast(`업로드 실패: ${e.message}`, true); }
}

async function downloadAttachmentFile(att) {
  if (!window.desktopApi?.downloadAttachment) {
    _toast("이 환경에서는 파일 다운로드를 지원하지 않습니다.", true);
    return;
  }
  try {
    const res = await window.desktopApi.downloadAttachment({
      url: `${state.apiBaseUrl}/api/attachments/${att.id}/download`,
      suggestedName: att.originalFilename
    });
    if (res?.canceled) return;
    if (!res?.ok) { _toast(`다운로드 실패: ${res?.data?.message || "HTTP " + res?.status}`, true); return; }
    _toast("파일을 저장했습니다.");
  } catch (e) { _toast(`다운로드 실패: ${e.message}`, true); }
}

async function deleteAttachmentFile(id, reload) {
  if (!confirm("이 첨부파일을 삭제할까요?")) return;
  try {
    await request(`/api/attachments/${id}`, { method: "DELETE" });
    _toast("첨부파일을 삭제했습니다.");
    if (reload) await reload();
  } catch (e) { _toast(`삭제 실패: ${e.message}`, true); }
}

async function _jiraAction(path, method, successMsg, body) {
  try {
    const opts = { method };
    if (body) opts.body = JSON.stringify(body);
    await request(path, opts);
    _toast(successMsg);
    // 현재 케이스 새로고침
    const id = elements.testCaseId.value;
    if (id) {
      const fresh = await request(`/api/testcases/${id}`, { method: "GET" });
      await populateForm(fresh);
    }
  } catch (e) {
    _toast(`실패: ${e.message}`, true);
  }
}

async function jiraSyncAll() {
  try {
    const result = await request("/api/jira/sync-all", { method: "POST" });
    _toast(`전체 동기화 완료 — 성공 ${result.success}/${result.total}, 실패 ${result.failed}`);
  } catch (e) {
    _toast(`동기화 실패: ${e.message}`, true);
  }
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
  elements.envServer.value = tc.serverEnvironment?.id ? String(tc.serverEnvironment.id) : "";
  elements.testConfiguration.value = tc.testConfiguration?.id ? String(tc.testConfiguration.id) : "";
  if (elements.tcFolder) elements.tcFolder.value = "";
  state.selectedTagIds = (tc.areaTags??[]).map(t=>t.id);
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
  return {
    type:         elements.type.value, priority: elements.priority.value,
    status:       elements.tcStatus.value || "DRAFT",
    title:        elements.title.value.trim(), description: elements.description.value.trim(),
    precondition: elements.precondition.value.trim(),
    steps:        stepsValue,
    notes:        elements.notes.value.trim() || null,
    os:           elements.envOs.value||null, browser: elements.envBrowser.value||null,
    device:       elements.envDevice.value||null, areaTagIds: [...state.selectedTagIds],
    serverEnvironmentId: elements.envServer.value ? Number(elements.envServer.value) : null,
    testConfigurationId: elements.testConfiguration.value ? Number(elements.testConfiguration.value) : null
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

function sortTestCases(testCases) {
  const priorityOrder = { HIGH: 0, MEDIUM: 1, LOW: 2 };
  const statusOrder = { DRAFT: 0, REVIEW_NEEDED: 1, READY: 2, COMPLETED: 3 };
  const byIdDesc = (a, b) => Number(b.id || 0) - Number(a.id || 0);
  const byTitle = (a, b) => String(a.title || "").localeCompare(String(b.title || ""), "ko", { sensitivity: "base" });

  return [...testCases].sort((a, b) => {
    if (state.sort === "title_asc") {
      return byTitle(a, b) || byIdDesc(a, b);
    }
    if (state.sort === "priority_desc") {
      return (priorityOrder[a.priority] ?? 99) - (priorityOrder[b.priority] ?? 99)
        || byTitle(a, b)
        || byIdDesc(a, b);
    }
    if (state.sort === "status_asc") {
      return (statusOrder[a.status] ?? 99) - (statusOrder[b.status] ?? 99)
        || byTitle(a, b)
        || byIdDesc(a, b);
    }

    const aTime = Date.parse(a.updatedAt || a.createdAt || "") || 0;
    const bTime = Date.parse(b.updatedAt || b.createdAt || "") || 0;
    return bTime - aTime || byIdDesc(a, b);
  });
}

function createTableRow(tc) {
  const tr = document.createElement("tr");
  if (tc.id === state.selectedId)    tr.classList.add("sel-row");
  if (tc.status === "REVIEW_NEEDED") tr.classList.add("attn-row");
  const env = [tc.serverEnvironment?.name,tc.os,tc.browser,tc.device].filter(Boolean);
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
    <td><span style="font-size:11px;color:var(--text-muted)">${tc.updatedAt ? escapeHtml(formatDateTime(tc.updatedAt)) : "-"}</span></td>`;
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
  let filtered = sortTestCases(applyFilters(state.testCases));

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
    if (g("rsPass"))   g("rsPass").textContent   = pass;
    if (g("rsFail"))   g("rsFail").textContent   = fail;
    if (g("rsBlock"))  g("rsBlock").textContent  = block;
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

const RUN_DOT_COLOR = { PASSED:"var(--c-pass)", FAILED:"var(--c-hi)", BLOCKED:"#777" };
const RUN_STATUS_LABEL = { PASSED:"PASS", FAILED:"FAIL", BLOCKED:"BLOCKED" };

const RUN_STATUS_OPTIONS_HTML = `
  <option value="PASSED">PASS — 통과</option>
  <option value="FAILED">FAIL — 실패</option>
  <option value="BLOCKED">BLOCKED — 차단</option>`;

// handlers: { onDelete(), onUpdate(payload) }
function _buildRunItemLi(run, handlers) {
  const li = document.createElement("li"); li.className = "run-item";
  _renderRunItemView(li, run, handlers);
  return li;
}

function _renderRunItemView(li, run, handlers) {
  li.classList.remove("run-item-editing");
  li.innerHTML = `<div class="run-dot" style="background:${RUN_DOT_COLOR[run.status]||"#ccc"}"></div><div class="run-info"><div class="run-hd"><span class="run-status run-status-${escapeHtml(run.status.toLowerCase())}">${escapeHtml(RUN_STATUS_LABEL[run.status] || run.status)}</span><span class="run-date">${escapeHtml(formatDateTime(run.executedAt))}</span></div><div class="run-note">${escapeHtml(run.actualResult)}</div>${run.notes?`<div class="run-note" style="color:var(--text-muted);margin-top:2px">${escapeHtml(run.notes)}</div>`:""}<div class="run-attach"><button type="button" class="run-attach-toggle">📎 증적 첨부파일</button><div class="run-attach-body" style="display:none"></div></div></div><button type="button" class="btn btn-sm btn-danger run-del-btn">삭제</button>`;
  li.querySelector(".run-del-btn").addEventListener("click", e => { e.stopPropagation(); handlers.onDelete(); });
  const info = li.querySelector(".run-info");
  info.style.cursor = "pointer";
  info.title = "클릭하여 수정";
  info.addEventListener("click", e => {
    if (e.target.closest(".run-attach")) return;   // 첨부 영역 클릭은 수정 진입 제외
    _renderRunItemEdit(li, run, handlers);
  });

  // 실행 증적(첨부파일) — 토글 시 지연 로드
  const toggle = li.querySelector(".run-attach-toggle");
  const body   = li.querySelector(".run-attach-body");
  toggle.addEventListener("click", e => {
    e.stopPropagation();
    const open = body.style.display === "none";
    body.style.display = open ? "block" : "none";
    if (open) loadRunAttachments(run.id, body);
  });
}

async function loadRunAttachments(runId, container) {
  const tcId = elements.testCaseId.value;
  if (!tcId) return;
  const listPath   = `/api/testcases/${tcId}/runs/${runId}/attachments`;
  const reload     = () => loadRunAttachments(runId, container);
  container.innerHTML = `<p style="font-size:12px;color:var(--text-muted)">불러오는 중…</p>`;
  try {
    const items = await request(listPath, { method: "GET" });
    const listEl = document.createElement("div");
    listEl.className = "attach-list";
    renderAttachmentList(listEl, items, reload);
    const uploadBtn = document.createElement("button");
    uploadBtn.type = "button";
    uploadBtn.className = "btn btn-sm";
    uploadBtn.style.marginTop = "6px";
    uploadBtn.textContent = "＋ 파일 업로드";
    uploadBtn.addEventListener("click", e => { e.stopPropagation(); uploadAttachmentTo(listPath, reload); });
    container.innerHTML = "";
    container.appendChild(listEl);
    container.appendChild(uploadBtn);
  } catch (e) {
    container.innerHTML = `<p style="font-size:12px;color:var(--c-hi)">첨부파일을 불러오지 못했습니다.</p>`;
  }
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
  const filtered = sortTestCases(applyFilters(state.testCases));
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

  card.innerHTML = `
    <div class="frun-card-title">${escapeHtml(tc.title)}</div>
    <div class="state-banner" style="margin-bottom:10px">${runs.length ? `총 ${runs.length}개의 실행 이력이 있습니다.` : `아직 실행 이력이 없습니다.`}</div>
    <div class="run-stats run-stats-three" style="display:grid">
      <div class="rs"><div class="rs-n" style="color:var(--c-pass)">${pass}</div><div class="rs-l">통과</div></div>
      <div class="rs"><div class="rs-n" style="color:var(--c-hi)">${fail}</div><div class="rs-l">실패</div></div>
      <div class="rs"><div class="rs-n">${block}</div><div class="rs-l">차단</div></div>
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
// 스위트 폴더 — localStorage 영속 (planId별)
// ══════════════════════════════════════════════════════════════════

let _dragSuiteId       = null;
let _dragSuiteFolderId = null;

function loadSuiteFolders(planId) {
  if (!planId) { state.suiteFolders = []; state.suiteFolderAssignments = {}; return; }
  try {
    const allFolders = JSON.parse(localStorage.getItem(SUITE_FOLDER_KEY) || "{}");
    const allAssigns = JSON.parse(localStorage.getItem(SUITE_FOLDER_ASSIGN_KEY) || "{}");
    state.suiteFolders           = allFolders[String(planId)] || [];
    state.suiteFolderAssignments = allAssigns[String(planId)] || {};
  } catch (_e) { state.suiteFolders = []; state.suiteFolderAssignments = {}; }
}

function persistSuiteFolders() {
  const planId = state.selectedPlanId;
  if (!planId) return;
  const allFolders = JSON.parse(localStorage.getItem(SUITE_FOLDER_KEY) || "{}");
  const allAssigns = JSON.parse(localStorage.getItem(SUITE_FOLDER_ASSIGN_KEY) || "{}");
  allFolders[String(planId)] = state.suiteFolders;
  allAssigns[String(planId)] = state.suiteFolderAssignments;
  localStorage.setItem(SUITE_FOLDER_KEY, JSON.stringify(allFolders));
  localStorage.setItem(SUITE_FOLDER_ASSIGN_KEY, JSON.stringify(allAssigns));
}

function getAllSubSuiteFolderIds(folderId) {
  const result = [];
  for (const f of state.suiteFolders.filter(f => f.parentId === folderId)) {
    result.push(f.id); result.push(...getAllSubSuiteFolderIds(f.id));
  }
  return result;
}

function getSuiteFolderCount(folderId) {
  const allIds = [folderId, ...getAllSubSuiteFolderIds(folderId)];
  return state.testSuites.filter(s => allIds.includes(state.suiteFolderAssignments[String(s.id)])).length;
}

function getUnclassifiedSuites() {
  return state.testSuites.filter(s => !state.suiteFolderAssignments[String(s.id)]);
}

function addSuiteFolder() {
  if (!state.selectedPlanId) { _toast("플랜을 먼저 선택하세요.", true); return; }
  const container = document.getElementById("suiteList");
  if (container.querySelector(".folder-name-input")) {
    container.querySelector(".folder-name-input").focus(); return;
  }
  const wrap = document.createElement("div"); wrap.className = "folder-node-wrap";
  const node = document.createElement("div"); node.className = "folder-node"; node.style.cssText = "padding:6px 10px";
  const ico = document.createElement("span"); ico.textContent = "📁"; ico.style.fontSize = "14px";
  const input = document.createElement("input");
  input.type = "text"; input.className = "folder-name-input"; input.placeholder = "폴더 이름 입력 후 Enter";
  input.style.cssText = "border:none;outline:none;background:transparent;font-size:12px;color:var(--text-primary);flex:1;min-width:0;font-family:var(--font)";
  node.append(ico, input); wrap.appendChild(node); container.appendChild(wrap); input.focus();
  let done = false;
  const confirm = () => {
    if (done) return; done = true;
    const name = input.value.trim(); wrap.remove();
    if (name) {
      state.suiteFolders.push({ id: "sf_" + Date.now() + "_" + Math.random().toString(36).slice(2, 6), name, parentId: null, collapsed: false });
      persistSuiteFolders();
    }
    renderSuiteList();
  };
  input.addEventListener("keydown", e => {
    if (e.key === "Enter") { e.preventDefault(); confirm(); }
    if (e.key === "Escape") { done = true; wrap.remove(); }
  });
  input.addEventListener("blur", confirm, { once: true });
}

function deleteSuiteFolder(folderId) {
  if (!window.confirm("폴더를 삭제할까요? (스위트는 미분류로 이동됩니다)")) return;
  const toDelete = [folderId, ...getAllSubSuiteFolderIds(folderId)];
  state.suiteFolders = state.suiteFolders.filter(f => !toDelete.includes(f.id));
  for (const sid of Object.keys(state.suiteFolderAssignments)) {
    if (toDelete.includes(state.suiteFolderAssignments[sid])) delete state.suiteFolderAssignments[sid];
  }
  persistSuiteFolders(); renderSuiteList();
}

function _clearSuiteDrop() {
  document.querySelectorAll("#suiteList .folder-node.drop-on").forEach(n => n.classList.remove("drop-on"));
}

function _buildSuiteFolderNode(folder, depth) {
  const wrap = document.createElement("div"); wrap.className = "folder-node-wrap"; wrap.dataset.id = folder.id;
  const folderSuites = state.testSuites.filter(s => state.suiteFolderAssignments[String(s.id)] === folder.id);
  const subFolders   = state.suiteFolders.filter(f => f.parentId === folder.id);
  const hasChildren  = folderSuites.length > 0 || subFolders.length > 0;
  const count        = getSuiteFolderCount(folder.id);
  const indentClass  = depth === 1 ? " folder-indent" : depth >= 2 ? " folder-indent2" : "";
  const node = document.createElement("div");
  node.className = `folder-node${indentClass}`; node.dataset.id = folder.id; node.draggable = true;
  const caretHtml = hasChildren
    ? `<span class="folder-caret ${folder.collapsed ? "" : "open"}">▶</span>`
    : `<span class="folder-caret-placeholder"></span>`;
  node.innerHTML = `<span class="drag-handle">⋮⋮</span>${caretHtml}<span style="font-size:${depth===0?"14px":"13px"}">📁</span><span class="folder-label">${escapeHtml(folder.name)}</span><span class="folder-cnt">${count}</span>`;
  const delBtn = document.createElement("button");
  delBtn.className = "folder-del-btn"; delBtn.title = "폴더 삭제"; delBtn.textContent = "✕";
  delBtn.style.cssText = "margin-left:auto;padding:0 4px;background:transparent;border:none;color:var(--text-muted);cursor:pointer;font-size:10px;opacity:0;transition:opacity .15s;flex-shrink:0";
  node.appendChild(delBtn);
  node.addEventListener("mouseenter", () => delBtn.style.opacity = "1");
  node.addEventListener("mouseleave", () => delBtn.style.opacity = "0");
  delBtn.addEventListener("click", e => { e.stopPropagation(); deleteSuiteFolder(folder.id); });
  const caret = node.querySelector(".folder-caret");
  if (caret) caret.addEventListener("click", e => {
    e.stopPropagation(); folder.collapsed = !folder.collapsed; persistSuiteFolders(); renderSuiteList();
  });
  // 스위트 드롭 → 이 폴더에 배정
  node.addEventListener("dragover", e => { e.preventDefault(); if (_dragSuiteId) { _clearSuiteDrop(); node.classList.add("drop-on"); } });
  node.addEventListener("dragleave", e => { if (!wrap.contains(e.relatedTarget)) node.classList.remove("drop-on"); });
  node.addEventListener("drop", e => {
    e.preventDefault(); node.classList.remove("drop-on");
    if (_dragSuiteId) {
      state.suiteFolderAssignments[String(_dragSuiteId)] = folder.id;
      persistSuiteFolders(); _dragSuiteId = null;
      _toast(`'${folder.name}'으로 이동됐습니다.`); renderSuiteList();
    }
  });
  // 폴더 드래그 (재정렬)
  node.addEventListener("dragstart", e => {
    if (e.target.closest(".folder-caret") || e.target.closest(".folder-del-btn")) { e.preventDefault(); return; }
    _dragSuiteFolderId = folder.id; _dragSuiteId = null; e.dataTransfer.effectAllowed = "move";
    setTimeout(() => node.classList.add("drag-active"), 0);
  });
  wrap.appendChild(node);
  if (!folder.collapsed) {
    subFolders.forEach(sf => wrap.appendChild(_buildSuiteFolderNode(sf, depth + 1)));
    folderSuites.forEach(s => wrap.appendChild(_buildSuiteNodeEl(s, depth + 1)));
  }
  return wrap;
}

function _buildSuiteNodeEl(suite, depth) {
  const wrap = document.createElement("div"); wrap.className = "folder-node-wrap tc-in-folder";
  const indentClass = depth === 0 ? "" : depth === 1 ? " folder-indent" : " folder-indent2";
  const node = document.createElement("div");
  node.className = `folder-node tc-node${indentClass}${suite.id === state.selectedSuiteId ? " active" : ""}`;
  node.title = suite.name; node.draggable = true;
  node.innerHTML = `<span class="tc-drag-handle" title="드래그하여 폴더 이동">⋮⋮</span><span style="font-size:12px;flex-shrink:0">📋</span><span class="tc-node-label">${escapeHtml(suite.name)}</span><span class="folder-cnt">${suite.testCases?.length || 0}건</span>`;
  node.addEventListener("click", () => { state.selectedSuiteId = suite.id; renderSuiteList(); showSuiteForm(suite); });
  node.addEventListener("dragstart", e => {
    _dragSuiteId = suite.id; _dragSuiteFolderId = null;
    e.dataTransfer.effectAllowed = "move"; e.dataTransfer.setData("text/plain", "suite:" + suite.id);
    setTimeout(() => node.classList.add("drag-active"), 0);
  });
  wrap.appendChild(node); return wrap;
}

// ══════════════════════════════════════════════════════════════════
// 테스트 플랜 / 스위트
// ══════════════════════════════════════════════════════════════════

function planStatusClass(status) {
  return { DRAFT: "b-draft", ACTIVE: "b-ready", COMPLETED: "b-done", ARCHIVED: "b-tag" }[status] || "b-tag";
}

async function loadTestPlans() {
  state.apiBaseUrl = getApiBaseUrl();
  try {
    state.testPlans = await request("/api/test-plans");
    if (state.testCases.length === 0) state.testCases = await request("/api/testcases");
    if (state.selectedPlanId && !state.testPlans.some(plan => plan.id === state.selectedPlanId)) {
      state.selectedPlanId = null;
      state.selectedSuiteId = null;
      state.testSuites = [];
    }
    renderPlanList();
    if (state.selectedPlanId) { loadSuiteFolders(state.selectedPlanId); await loadTestSuites(state.selectedPlanId); }
    else renderSuiteList();
  } catch (error) {
    _toast(`플랜 조회 실패: ${error.message}`, true);
  }
}

function renderPlanList() {
  const list = document.getElementById("planList");
  document.getElementById("planCount").textContent = state.testPlans.length;
  list.innerHTML = "";
  if (state.testPlans.length === 0) {
    list.innerHTML = '<div class="plan-empty">아직 테스트 플랜이 없습니다.</div>';
    return;
  }
  state.testPlans.forEach(plan => {
    const button = document.createElement("button");
    button.type = "button";
    button.className = `plan-list-item${plan.id === state.selectedPlanId ? " active" : ""}`;
    button.innerHTML = `<div class="plan-item-name">${escapeHtml(plan.name)}</div><div class="plan-item-meta"><span class="badge ${planStatusClass(plan.status)}">${escapeHtml(plan.status)}</span><span>${plan.suiteCount} suites</span><span>${plan.testCaseCount} cases</span></div>`;
    button.addEventListener("click", () => selectPlan(plan.id));
    list.appendChild(button);
  });
}

async function selectPlan(planId) {
  state.selectedPlanId  = planId;
  state.selectedSuiteId = null;
  loadSuiteFolders(planId);
  renderPlanList();
  showPlanForm(state.testPlans.find(plan => plan.id === planId));
  await loadTestSuites(planId);
}

async function loadTestSuites(planId) {
  try {
    state.testSuites = await request(`/api/test-plans/${planId}/suites`);
    renderSuiteList();
  } catch (error) {
    state.testSuites = [];
    renderSuiteList();
    _toast(`스위트 조회 실패: ${error.message}`, true);
  }
}

function renderSuiteList() {
  const list  = document.getElementById("suiteList");
  const empty = document.getElementById("suiteEmpty");
  const plan  = state.testPlans.find(item => item.id === state.selectedPlanId);
  document.getElementById("suiteColumnTitle").textContent = plan ? `${plan.name} 스위트` : "테스트 스위트";
  const noplan = !plan;
  document.getElementById("newSuiteButton").disabled       = noplan;
  const folderBtn = document.getElementById("newSuiteFolderButton");
  if (folderBtn) folderBtn.disabled = noplan;
  list.innerHTML = "";

  if (!state.selectedPlanId) {
    empty.style.display = "block";
    empty.textContent   = "플랜을 선택하면 스위트를 관리할 수 있습니다.";
    return;
  }

  const unclassified = getUnclassifiedSuites();
  const hasSuites    = state.testSuites.length > 0;
  const hasFolders   = state.suiteFolders.length > 0;

  if (!hasSuites && !hasFolders) {
    empty.style.display = "block";
    empty.textContent   = "아직 테스트 스위트가 없습니다.";
    return;
  }
  empty.style.display = "none";

  // 미분류 드롭 존
  const unWrap = document.createElement("div"); unWrap.className = "folder-node-wrap";
  const unNode = document.createElement("div");
  unNode.className = "folder-node";
  unNode.innerHTML = `<span class="folder-caret-placeholder"></span><span style="font-size:14px">📂</span><span class="folder-label">미분류</span><span class="folder-cnt">${unclassified.length}</span>`;
  unNode.addEventListener("dragover", e => { e.preventDefault(); if (_dragSuiteId) { _clearSuiteDrop(); unNode.classList.add("drop-on"); } });
  unNode.addEventListener("dragleave", () => unNode.classList.remove("drop-on"));
  unNode.addEventListener("drop", e => {
    e.preventDefault(); unNode.classList.remove("drop-on");
    if (_dragSuiteId) {
      delete state.suiteFolderAssignments[String(_dragSuiteId)];
      persistSuiteFolders(); _dragSuiteId = null; _toast("미분류로 이동됐습니다."); renderSuiteList();
    }
  });
  unWrap.appendChild(unNode); list.appendChild(unWrap);
  unclassified.forEach(s => list.appendChild(_buildSuiteNodeEl(s, 1)));

  // 사용자 폴더
  state.suiteFolders.filter(f => !f.parentId).forEach(folder => list.appendChild(_buildSuiteFolderNode(folder, 0)));
}

function hidePlanEditors() {
  document.getElementById("planEditorEmpty").hidden = true;
  document.getElementById("planForm").hidden = true;
  document.getElementById("suiteForm").hidden = true;
}

function showPlanForm(plan = null) {
  hidePlanEditors();
  const form = document.getElementById("planForm");
  form.hidden = false;
  document.getElementById("planFormTitle").textContent = plan ? "테스트 플랜 수정" : "새 테스트 플랜";
  document.getElementById("planId").value = plan?.id ?? "";
  document.getElementById("planName").value = plan?.name ?? "";
  document.getElementById("planStatus").value = plan?.status ?? "DRAFT";
  document.getElementById("planStartDate").value = plan?.startDate ?? "";
  document.getElementById("planEndDate").value = plan?.endDate ?? "";
  document.getElementById("planDescription").value = plan?.description ?? "";
  document.getElementById("deletePlanButton").disabled = !plan;
}

function showSuiteForm(suite = null) {
  if (!state.selectedPlanId) return;
  hidePlanEditors();
  const form = document.getElementById("suiteForm");
  form.hidden = false;
  document.getElementById("suiteFormTitle").textContent = suite ? "테스트 스위트 수정" : "새 테스트 스위트";
  document.getElementById("suiteId").value = suite?.id ?? "";
  document.getElementById("suiteName").value = suite?.name ?? "";
  document.getElementById("suiteDescription").value = suite?.description ?? "";
  document.getElementById("deleteSuiteButton").disabled = !suite;
  renderSuiteCasePicker(suite?.testCases?.map(testCase => testCase.id) ?? []);
}

function renderSuiteCasePicker(selectedIds) {
  const picker = document.getElementById("suiteCasePicker");
  const selected = new Set(selectedIds);
  picker.innerHTML = "";
  if (state.testCases.length === 0) {
    picker.innerHTML = '<div class="plan-empty">배정할 테스트케이스가 없습니다.</div>';
    return;
  }
  state.testCases.forEach(testCase => {
    const label = document.createElement("label");
    label.className = "suite-case-option";
    label.innerHTML = `<input type="checkbox" value="${testCase.id}" ${selected.has(testCase.id) ? "checked" : ""}><span>${escapeHtml(testCase.title)}<small>TC-${String(testCase.id).padStart(3, "0")} · ${escapeHtml(testCase.status)}</small></span>`;
    picker.appendChild(label);
  });
}

async function savePlan(event) {
  event.preventDefault();
  const id = document.getElementById("planId").value;
  const payload = {
    name: document.getElementById("planName").value.trim(),
    status: document.getElementById("planStatus").value,
    startDate: document.getElementById("planStartDate").value || null,
    endDate: document.getElementById("planEndDate").value || null,
    description: document.getElementById("planDescription").value.trim() || null
  };
  try {
    const saved = await request(id ? `/api/test-plans/${id}` : "/api/test-plans", {
      method: id ? "PUT" : "POST", body: JSON.stringify(payload)
    });
    state.selectedPlanId = saved.id;
    _toast(id ? "테스트 플랜을 수정했습니다." : "테스트 플랜을 생성했습니다.");
    await loadTestPlans();
    showPlanForm(state.testPlans.find(plan => plan.id === saved.id));
  } catch (error) { _toast(`플랜 저장 실패: ${error.message}`, true); }
}

async function saveSuite(event) {
  event.preventDefault();
  const id = document.getElementById("suiteId").value;
  const testCaseIds = Array.from(document.querySelectorAll("#suiteCasePicker input:checked")).map(input => Number(input.value));
  const payload = {
    name: document.getElementById("suiteName").value.trim(),
    description: document.getElementById("suiteDescription").value.trim() || null,
    testCaseIds
  };
  try {
    const saved = await request(id ? `/api/test-plans/${state.selectedPlanId}/suites/${id}` : `/api/test-plans/${state.selectedPlanId}/suites`, {
      method: id ? "PUT" : "POST", body: JSON.stringify(payload)
    });
    state.selectedSuiteId = saved.id;
    _toast(id ? "테스트 스위트를 수정했습니다." : "테스트 스위트를 생성했습니다.");
    await loadTestPlans();
    showSuiteForm(state.testSuites.find(suite => suite.id === saved.id));
  } catch (error) { _toast(`스위트 저장 실패: ${error.message}`, true); }
}

async function deleteSelectedPlan() {
  if (!state.selectedPlanId || !window.confirm("플랜과 포함된 스위트를 모두 삭제할까요?")) return;
  try {
    await request(`/api/test-plans/${state.selectedPlanId}`, { method: "DELETE" });
    state.selectedPlanId = null; state.selectedSuiteId = null; state.testSuites = [];
    hidePlanEditors(); document.getElementById("planEditorEmpty").hidden = false;
    _toast("테스트 플랜을 삭제했습니다.");
    await loadTestPlans();
  } catch (error) { _toast(`플랜 삭제 실패: ${error.message}`, true); }
}

async function deleteSelectedSuite() {
  if (!state.selectedPlanId || !state.selectedSuiteId || !window.confirm("테스트 스위트를 삭제할까요?")) return;
  try {
    await request(`/api/test-plans/${state.selectedPlanId}/suites/${state.selectedSuiteId}`, { method: "DELETE" });
    delete state.suiteFolderAssignments[String(state.selectedSuiteId)];
    persistSuiteFolders();
    state.selectedSuiteId = null;
    showPlanForm(state.testPlans.find(plan => plan.id === state.selectedPlanId));
    _toast("테스트 스위트를 삭제했습니다.");
    await loadTestPlans();
  } catch (error) { _toast(`스위트 삭제 실패: ${error.message}`, true); }
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
    _stopRetry();
  } catch (e) {
    state.testCases = []; renderFolderTree(); renderList();
    updateStatus(`연결 실패: ${e.message}`);
    _startRetry();
  }
}

let _retryTimer = null;
function _startRetry() {
  if (_retryTimer) return;
  _retryTimer = setInterval(async () => {
    try {
      await request("/api/testcases", { method: "GET" }).then(async tcs => {
        state.testCases = tcs;
        _stopRetry();
        await loadAreaTags();
        await loadServerEnvironments();
        await loadTestConfigurations();
        renderFolderTree(); renderList();
        updateStatus(`${tcs.length}개 로드됨`);
      });
    } catch (_e) {}
  }, 3000);
}
function _stopRetry() {
  if (_retryTimer) { clearInterval(_retryTimer); _retryTimer = null; }
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
  document.getElementById("jiraSyncAllBtn")?.addEventListener("click", jiraSyncAll);

  // 결함 추가 / 연결
  document.getElementById("addDefectBtn")?.addEventListener("click", toggleDefectCreateForm);
  document.getElementById("linkDefectBtn")?.addEventListener("click", openDefectLinkForm);
  document.getElementById("defectCreateCancelBtn")?.addEventListener("click", hideDefectForms);
  document.getElementById("defectCreateSaveBtn")?.addEventListener("click", createDefectAndLink);
  document.getElementById("defectLinkConfirmBtn")?.addEventListener("click", linkExistingDefect);
  document.getElementById("defectLinkCancelBtn")?.addEventListener("click", hideDefectForms);

  // 테스트케이스 첨부파일 업로드
  document.getElementById("tcAttachUploadBtn")?.addEventListener("click", () => {
    const tcId = elements.testCaseId.value;
    if (!tcId) { _toast("먼저 테스트케이스를 저장하세요.", true); return; }
    uploadAttachmentTo(`/api/testcases/${tcId}/attachments`, () => loadTestCaseAttachments(tcId));
  });

  // 서버 환경 관리 토글
  document.getElementById("toggleServerEnvManageBtn")?.addEventListener("click", () => {
    const list = document.getElementById("serverEnvManageList");
    if (list) list.style.display = list.style.display === "none" ? "block" : "none";
  });
  elements.connectButton.addEventListener("click",   verifyConnection);
  elements.newButton.addEventListener("click",       resetForm);
  elements.duplicateButton.addEventListener("click", duplicateCurrentTestCase);
  elements.resetButton.addEventListener("click",     resetForm);
  elements.deleteButton.addEventListener("click",    handleDelete);
  elements.addStepButton.addEventListener("click",   () => appendStepRow());

  document.getElementById("newPlanButton").addEventListener("click", () => showPlanForm());
  document.getElementById("newSuiteButton").addEventListener("click", () => showSuiteForm());
  document.getElementById("newSuiteFolderButton")?.addEventListener("click", addSuiteFolder);
  document.getElementById("planForm").addEventListener("submit", savePlan);
  document.getElementById("suiteForm").addEventListener("submit", saveSuite);
  document.getElementById("deletePlanButton").addEventListener("click", deleteSelectedPlan);
  document.getElementById("deleteSuiteButton").addEventListener("click", deleteSelectedSuite);

  const fab = document.getElementById("folderAddBtn");
  if (fab) fab.addEventListener("click", addFolder);

  const folderSearchInput = document.getElementById("folderSearchInput");
  if (folderSearchInput) {
    folderSearchInput.addEventListener("input", () => {
      state.folderSearchQuery = folderSearchInput.value.trim().toLocaleLowerCase("ko");
      renderFolderTree();
    });
  }

  elements.addTagButton.addEventListener("click",   () => { const id = Number(elements.tagSelect.value); if (id) { addSelectedTag(id); elements.tagSelect.value = ""; } });
  elements.createTagButton.addEventListener("click",() => createAndAddTag(elements.newTagInput.value));
  elements.newTagInput.addEventListener("keydown",  e => { if (e.key === "Enter") { e.preventDefault(); createAndAddTag(elements.newTagInput.value); } });
  elements.createServerEnvButton.addEventListener("click", createServerEnvironment);
  elements.testConfiguration.addEventListener("change", applySelectedConfiguration);
  document.getElementById("newConfigurationButton").addEventListener("click", resetConfigurationForm);
  document.getElementById("configurationForm").addEventListener("submit", saveConfiguration);
  document.getElementById("deleteConfigurationButton").addEventListener("click", deleteConfiguration);

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
  await loadServerEnvironments();
  await loadTestConfigurations();
  await loadTestCases();
  renderFolderSelect();
}

// ── 사이드바 리사이즈 ─────────────────────────────────────────────
(function initSidebarResize() {
  const handle  = document.getElementById("sidebarResizeHandle");
  const sidebar = document.getElementById("tcSidebar");
  if (!handle || !sidebar) return;

  const MIN_W = 150;
  const MAX_W = 500;
  const STORAGE_KEY = "tms.sidebarWidth";

  const saved = parseInt(localStorage.getItem(STORAGE_KEY), 10);
  if (saved) sidebar.style.width = saved + "px";

  handle.addEventListener("mousedown", e => {
    e.preventDefault();
    handle.classList.add("dragging");
    document.body.style.cursor = "col-resize";
    document.body.style.userSelect = "none";

    const onMove = mv => {
      const w = Math.min(MAX_W, Math.max(MIN_W, mv.clientX - sidebar.getBoundingClientRect().left));
      sidebar.style.width = w + "px";
    };
    const onUp = () => {
      handle.classList.remove("dragging");
      document.body.style.cursor = "";
      document.body.style.userSelect = "";
      localStorage.setItem(STORAGE_KEY, parseInt(sidebar.style.width, 10));
      document.removeEventListener("mousemove", onMove);
      document.removeEventListener("mouseup",   onUp);
    };
    document.addEventListener("mousemove", onMove);
    document.addEventListener("mouseup",   onUp);
  });
})();

bootstrap();
