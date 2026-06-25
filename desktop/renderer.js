// ── 상수 ─────────────────────────────────────────────────────────
const FOLDER_KEY              = "tms.folders";
const FOLDER_ASSIGN_KEY       = "tms.folderAssignments";
const SUITE_FOLDER_KEY        = "tms.suiteFolders";
const SUITE_FOLDER_ASSIGN_KEY = "tms.suiteFolderAssignments";
const FOLDER_COLLAPSED_KEY    = "tms.folderCollapsed";
const FOLDER_MIGRATED_KEY     = "tms.foldersMigrated";
const USER_ROLE_LABELS = {
  ADMIN: "관리자",
  QA_LEAD: "QA 리드",
  QA: "QA",
  DEVELOPER: "개발자",
  VIEWER: "조회자"
};

// ── 상태 ─────────────────────────────────────────────────────────
const state = {
  apiBaseUrl: "http://localhost:8080",
  selectedId: null,
  testCases: [],       // 현재 필터 적용된 목록 (필터 없으면 전체)
  allTestCases: [],    // 필터 무관한 전체 목록 (대시보드·폴더 배정에서 사용)
  testRuns: [],
  testPlans: [],
  testSuites: [],
  selectedPlanId: null,
  selectedSuiteId: null,
  executions: [],            // 테스트런(실행 사이클) 목록
  selectedExecutionId: null,
  runSourceMode: "suite",    // 새 테스트런 모달 — "suite" | "cases"
  areaTags: [],
  allDefects: [],
  serverEnvironments: [],
  testConfigurations: [],
  selectedConfigurationId: null,
  users: [],
  selectedUserId: null,
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
  assignee:          document.getElementById("assignee"),
  tcVersion:         document.getElementById("tcVersion"),
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
  runPanelCaseTitle: document.getElementById("runPanelCaseTitle"),
  auditSection:      document.getElementById("auditSection"),
  auditList:         document.getElementById("auditList"),
  auditRefreshBtn:   document.getElementById("auditRefreshBtn"),
  versionSection:    document.getElementById("versionSection"),
  versionList:       document.getElementById("versionList"),
  versionRefreshBtn: document.getElementById("versionRefreshBtn")
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
  const map = { dashboard: "navDash", testcases: "navTC", plans: "navPlans", runs: "navRuns", settings: "navSet" };
  document.querySelectorAll(".nav-tab").forEach(b => b.classList.remove("active"));
  const t = document.getElementById(map[v]);
  if (t) t.classList.add("active");
  if (v === "dashboard") renderDashboard();
  if (v === "plans") loadTestPlans();
  if (v === "runs") loadExecutions();
  if (v === "settings") { loadTestConfigurations(); loadUsers(); loadAreaTags(); }
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
function normalizeApiBaseUrl(rawValue, fallback = state.apiBaseUrl) {
  let value = String(rawValue ?? "").trim();
  if (!value) value = fallback;
  if (!/^[a-z][a-z0-9+.-]*:\/\//i.test(value)) value = `http://${value}`;
  const url = new URL(value);
  if (!["http:", "https:"].includes(url.protocol)) {
    throw new Error("백엔드 주소는 http 또는 https로 시작해야 합니다.");
  }
  url.hash = "";
  url.search = "";
  return url.href.replace(/\/$/, "");
}
function getApiBaseUrl() {
  return normalizeApiBaseUrl(elements.apiBaseUrl?.value, state.apiBaseUrl);
}
function updateStatus(msg) {
  if (!elements.listState) return;
  elements.listState.style.display = msg ? "block" : "none";
  elements.listState.textContent = msg;
}
function updateRunStatus(msg) { if (elements.runState) elements.runState.textContent = msg; }
function _toast(msg, isError = false) {
  let container = document.getElementById("toastContainer");
  if (!container) {
    container = document.createElement("div");
    container.id = "toastContainer";
    container.style.cssText = "position:fixed;top:16px;left:50%;transform:translateX(-50%);z-index:9999;display:flex;flex-direction:column;gap:8px;align-items:center;pointer-events:none";
    document.body.appendChild(container);
  }
  const el = document.createElement("div");
  el.style.cssText = `background:${isError ? "#dc2626" : "var(--accent)"};color:#fff;padding:8px 20px;border-radius:8px;font-size:12px;font-weight:500;box-shadow:var(--shadow-md);pointer-events:none;max-width:400px;text-align:center;animation:rise .2s ease both`;
  el.textContent = msg;
  container.appendChild(el);
  setTimeout(() => el.remove(), 2500);
}

// ══════════════════════════════════════════════════════════════════
// 폴더 — DB API 영속 (collapsed 상태만 localStorage)
// ══════════════════════════════════════════════════════════════════

function _getFolderCollapsed() {
  try { return JSON.parse(localStorage.getItem(FOLDER_COLLAPSED_KEY) || "{}"); } catch { return {}; }
}
function _saveFolderCollapsed() {
  const map = {};
  for (const f of state.folders) if (f.collapsed) map[String(f.id)] = true;
  localStorage.setItem(FOLDER_COLLAPSED_KEY, JSON.stringify(map));
}

function _flattenFolderTree(nodes, collapsed) {
  const result = [];
  for (const n of nodes) {
    result.push({ id: n.id, name: n.name, parentId: n.parentId ?? null, collapsed: !!collapsed[String(n.id)] });
    if (n.children?.length) result.push(..._flattenFolderTree(n.children, collapsed));
  }
  return result;
}

async function loadFolders() {
  try {
    const qs = state.currentProjectId ? `?projectId=${state.currentProjectId}` : "";
    const tree = await request(`/api/folders${qs}`);
    const collapsed = _getFolderCollapsed();
    state.folders = _flattenFolderTree(tree, collapsed);
  } catch (_e) { state.folders = []; }
  _rebuildFolderAssignments();
}

function _rebuildFolderAssignments() {
  state.folderAssignments = {};
  for (const tc of state.allTestCases) {
    if (tc.folderId) state.folderAssignments[String(tc.id)] = tc.folderId;
  }
}

function persistFolders() {
  _saveFolderCollapsed();
}

// 최초 1회: localStorage의 폴더/배정 데이터를 DB로 마이그레이션
async function migrateLocalStorageFolders() {
  if (localStorage.getItem(FOLDER_MIGRATED_KEY)) return;
  const oldFolders = JSON.parse(localStorage.getItem(FOLDER_KEY) || "[]");
  const oldAssign  = JSON.parse(localStorage.getItem(FOLDER_ASSIGN_KEY) || "{}");
  if (oldFolders.length === 0) { localStorage.setItem(FOLDER_MIGRATED_KEY, "1"); return; }

  const idMap = {};
  // 루트 폴더 먼저
  for (const f of oldFolders.filter(f => !f.parentId)) {
    try {
      const created = await request("/api/folders", { method: "POST", body: JSON.stringify({ name: f.name, parentId: null }) });
      idMap[String(f.id)] = created.id;
    } catch (_e) {}
  }
  // 하위 폴더 (최대 5 depth)
  let remaining = oldFolders.filter(f => f.parentId);
  for (let pass = 0; pass < 5 && remaining.length; pass++) {
    const next = [];
    for (const f of remaining) {
      const newParentId = idMap[String(f.parentId)];
      if (newParentId != null) {
        try {
          const created = await request("/api/folders", { method: "POST", body: JSON.stringify({ name: f.name, parentId: newParentId }) });
          idMap[String(f.id)] = created.id;
        } catch (_e) {}
      } else { next.push(f); }
    }
    remaining = next;
  }
  // 폴더 배정 마이그레이션
  for (const [tcId, oldFolderId] of Object.entries(oldAssign)) {
    const newFolderId = idMap[String(oldFolderId)];
    if (newFolderId != null) {
      try { await request(`/api/testcases/${tcId}/folder`, { method: "PATCH", body: JSON.stringify({ folderId: newFolderId }) }); } catch (_e) {}
    }
  }
  localStorage.setItem(FOLDER_MIGRATED_KEY, "1");
  _toast("폴더 구조를 서버에 마이그레이션했습니다.");
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
  const confirm = async () => {
    if (done) return; done = true;
    const name = input.value.trim();
    wrap.remove();
    if (name) {
      try {
        await request("/api/folders", { method: "POST", body: JSON.stringify({ name, parentId: parentId || null, projectId: state.currentProjectId || null }) });
        await loadFolders();
      } catch (e) { _toast(`폴더 생성 실패: ${e.message}`, true); }
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

async function deleteFolder(folderId) {
  if (!window.confirm("폴더를 삭제할까요? (테스트케이스는 미분류로 이동됩니다)")) return;
  const toDelete = [folderId, ...getAllSubFolderIds(folderId)];

  // 폴더에 속한 TC들을 먼저 미분류로 이동
  for (const tcId of Object.keys(state.folderAssignments)) {
    if (toDelete.includes(state.folderAssignments[tcId])) {
      try { await request(`/api/testcases/${tcId}/folder`, { method: "PATCH", body: JSON.stringify({ folderId: null }) }); } catch (_e) {}
    }
  }
  // 하위 폴더부터 순서대로 삭제 (leaf → root)
  const ordered = toDelete.slice().reverse();
  for (const id of ordered) {
    try { await request(`/api/folders/${id}`, { method: "DELETE" }); } catch (_e) {}
  }

  if (toDelete.includes(state.selectedFolderId)) selectFolder("all", "전체");
  await loadTestCases();
  await loadFolders();
  renderFolderTree(); renderFolderSelect(); renderList();
}

async function renameFolder(folderId) {
  const folder = state.folders.find(f => f.id === folderId);
  if (!folder) return;
  const newName = window.prompt("폴더 이름 변경", folder.name);
  if (!newName || !newName.trim() || newName.trim() === folder.name) return;
  try {
    await request(`/api/folders/${folderId}`, { method: "PUT", body: JSON.stringify({ name: newName.trim(), parentId: folder.parentId || null }) });
    await loadFolders();
    renderFolderTree(); renderFolderSelect();
  } catch (e) { _toast(`이름 변경 실패: ${e.message}`, true); }
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

async function moveFolderBefore(srcId, tgtId) {
  if (srcId === tgtId || isDescendant(srcId, tgtId)) return;
  const src = state.folders.find(f => f.id === srcId);
  const tgt = state.folders.find(f => f.id === tgtId);
  if (!src || !tgt) return;
  try {
    await request(`/api/folders/${srcId}`, { method: "PUT", body: JSON.stringify({ name: src.name, parentId: tgt.parentId }) });
    await loadFolders();
  } catch (e) { _toast(`폴더 이동 실패: ${e.message}`, true); return; }
  renderFolderTree(); renderFolderSelect(); renderList();
}
async function moveFolderAfter(srcId, tgtId) {
  if (srcId === tgtId || isDescendant(srcId, tgtId)) return;
  const src = state.folders.find(f => f.id === srcId);
  const tgt = state.folders.find(f => f.id === tgtId);
  if (!src || !tgt) return;
  try {
    await request(`/api/folders/${srcId}`, { method: "PUT", body: JSON.stringify({ name: src.name, parentId: tgt.parentId }) });
    await loadFolders();
  } catch (e) { _toast(`폴더 이동 실패: ${e.message}`, true); return; }
  renderFolderTree(); renderFolderSelect(); renderList();
}
async function moveFolderInto(srcId, tgtId) {
  if (srcId === tgtId || isDescendant(srcId, tgtId)) return;
  const src = state.folders.find(f => f.id === srcId);
  if (!src) return;
  try {
    await request(`/api/folders/${srcId}`, { method: "PUT", body: JSON.stringify({ name: src.name, parentId: tgtId }) });
    await loadFolders();
  } catch (e) { _toast(`폴더 이동 실패: ${e.message}`, true); return; }
  renderFolderTree(); renderFolderSelect(); renderList();
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
      if (_dragTcId) { const tid = _dragTcId; delete state.folderAssignments[String(tid)]; persistFolders(); _syncEditorFolder(tid, ""); _dragTcId = null; renderFolderTree(); renderList(); _toast("미분류로 이동됐습니다."); request(`/api/testcases/${tid}/folder`, { method: "PATCH", body: JSON.stringify({ folderId: null }) }).catch(() => {}); }
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
      if (_dragTcId) { const tid = _dragTcId; delete state.folderAssignments[String(tid)]; persistFolders(); _syncEditorFolder(tid, ""); _dragTcId = null; renderFolderTree(); renderList(); _toast("미분류로 이동됐습니다."); request(`/api/testcases/${tid}/folder`, { method: "PATCH", body: JSON.stringify({ folderId: null }) }).catch(() => {}); }
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
    node.addEventListener("drop", async e => {
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
        request(`/api/testcases/${tid}/folder`, { method: "PATCH", body: JSON.stringify({ folderId: folder.id }) }).catch(() => {});
        return;
      }
      // 폴더 이동
      if (!_dragFolderId || _dragFolderId === folder.id) { _dragFolderId = null; return; }
      const rect = node.getBoundingClientRect(); const zone = (e.clientY - rect.top) / rect.height;
      if (zone < 0.25)      await moveFolderBefore(_dragFolderId, folder.id);
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

// 메인 통계 카드 / 우선순위 분포 / 태그 칩은 서버(DashboardService)가 집계한 값을 그대로 쓴다 —
// 클라이언트에서 전체 테스트케이스 배열을 매번 훑어 계산하지 않는다.
async function renderDashboard() {
  const g = id => document.getElementById(id);
  let stats = null;
  try {
    const qs = state.currentProjectId ? `?projectId=${state.currentProjectId}` : "";
    stats = await request(`/api/dashboard/stats${qs}`);
  } catch (_e) { stats = null; }

  const total = stats?.totalTestCases ?? 0;
  const byStatus = stats?.testCasesByStatus ?? {};
  const byPriority = stats?.testCasesByPriority ?? {};
  const byAreaTag = stats?.testCasesByAreaTag ?? {};

  if (g("dashStatTotal"))     g("dashStatTotal").textContent     = total;
  if (g("dashStatReady"))     g("dashStatReady").textContent     = byStatus.READY ?? 0;
  if (g("dashStatIssues"))    g("dashStatIssues").textContent    = byStatus.REVIEW_NEEDED ?? 0;
  if (g("dashStatCompleted")) g("dashStatCompleted").textContent = byStatus.COMPLETED ?? 0;

  const hi = byPriority.HIGH ?? 0, mid = byPriority.MEDIUM ?? 0, lo = byPriority.LOW ?? 0;
  ["dashPriHighCnt","dashPriMidCnt","dashPriLoCnt"].forEach((id,i)=>{ if(g(id)) g(id).textContent=[hi,mid,lo][i]; });
  ["dashPriHigh","dashPriMid","dashPriLo"].forEach((id,i)=>{ if(g(id)) g(id).style.width=total?`${([hi,mid,lo][i]/total)*100}%`:"0%"; });

  const tc2 = g("dashTagChips");
  if (tc2) { const e = Object.entries(byAreaTag); tc2.innerHTML = e.length===0 ? '<span style="font-size:12px;color:var(--text-muted)">태그 없음</span>' : e.map(([n,c])=>`<span class="tag-chip">${escapeHtml(n)} (${c})</span>`).join(""); }

  // 검토 필요 이슈 목록은 제목 등 상세가 필요해 이미 받아온 전체 목록에서 추려낸다(집계 DTO에는 제목이 없음).
  const issues = state.allTestCases.filter(t=>t.status==="REVIEW_NEEDED");
  if (g("dashIssueCount")) g("dashIssueCount").textContent = `${issues.length}건`;
  const il = g("dashIssueList");
  if (il) { const pc = { HIGH:"b-hi",MEDIUM:"b-mid",LOW:"b-lo" }; il.innerHTML = issues.length===0 ? '<p style="font-size:12px;color:var(--text-muted)">검토가 필요한 이슈가 없습니다.</p>' : issues.map(tc=>`<div class="issue-item" onclick="switchView('testcases')"><div class="issue-item-title">${escapeHtml(tc.title)}</div><div class="issue-item-meta"><span class="badge b-review">검토 필요</span><span class="badge ${pc[tc.priority]||"b-mid"}">${escapeHtml(tc.priority??"MEDIUM")}</span></div></div>`).join(""); }

  renderDashboardRuns();
  renderDashboardAuditLogsFromStats(stats);
  renderDashboardProjects();
}

// 대시보드 테스트런 현황 — 전체 런 집계 + 통과율
async function renderDashboardRuns() {
  const g = id => document.getElementById(id);
  const body = g("dashRunBody"), empty = g("dashRunEmpty");
  if (!body || !empty) return;
  let runs = [];
  try {
    const qs = state.currentProjectId ? `?projectId=${state.currentProjectId}` : "";
    runs = await request(`/api/test-runs${qs}`, { method: "GET" });
  } catch (_e) { runs = []; }
  if (runs.length === 0) { body.hidden = true; empty.hidden = false; return; }
  empty.hidden = true; body.hidden = false;

  const sum = runs.reduce((a, r) => ({
    total:    a.total    + (r.total    || 0),
    passed:   a.passed   + (r.passed   || 0),
    failed:   a.failed   + (r.failed   || 0),
    blocked:  a.blocked  + (r.blocked  || 0),
    retest:   a.retest   + (r.retest   || 0),
    untested: a.untested + (r.untested || 0)
  }), { total: 0, passed: 0, failed: 0, blocked: 0, retest: 0, untested: 0 });
  const executed = sum.total - sum.untested;
  const passRate = executed ? Math.round((sum.passed / executed) * 100) : 0;

  g("dashRunTotal").textContent  = runs.length;
  g("dashRunActive").textContent = runs.filter(r => r.status === "IN_PROGRESS").length;
  g("dashRunDone").textContent   = runs.filter(r => r.status === "COMPLETED").length;
  g("dashRunPass").textContent   = `${passRate}%`;
  renderSegmentBar(g("dashRunBar"), sum);
  g("dashRunChips").innerHTML =
    `<span class="suite-run-chip pass">통과 ${sum.passed}</span>` +
    `<span class="suite-run-chip fail">실패 ${sum.failed}</span>` +
    `<span class="suite-run-chip block">차단 ${sum.blocked}</span>` +
    (sum.retest ? `<span class="suite-run-chip retest">재테스트 ${sum.retest}</span>` : "") +
    `<span class="suite-run-prog" style="margin-left:4px">미실행 ${sum.untested}</span>`;
}

// ══════════════════════════════════════════════════════════════════
// Status 셀렉터
// ══════════════════════════════════════════════════════════════════

function initStatusSelector() {
  elements.statusSelector.addEventListener("click", async e => {
    const btn = e.target.closest(".status-btn"); if (!btn) return;
    elements.statusSelector.querySelectorAll(".status-btn").forEach(b => b.classList.remove("active"));
    btn.classList.add("active"); elements.tcStatus.value = btn.dataset.value;
    const tcId = elements.testCaseId.value;
    if (tcId) {
      try {
        await request(`/api/testcases/${tcId}/status`, { method: "PATCH", body: JSON.stringify({ status: btn.dataset.value }) });
        const inFiltered = state.testCases.find(t => String(t.id) === String(tcId));
        if (inFiltered) inFiltered.status = btn.dataset.value;
        const inAll = state.allTestCases.find(t => String(t.id) === String(tcId));
        if (inAll) inAll.status = btn.dataset.value;
        renderList();
        await loadAuditLogs(tcId);
      } catch (_e) {}
    }
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
  const assignee = tc.assignee ? `<span class="badge user-role-badge">담당 ${escapeHtml(tc.assignee)}</span>` : "";
  elements.dtPills.innerHTML = `<span class="badge ${sMap[s]||"b-draft"}">${escapeHtml(sLbl[s]||s)}</span><span class="badge ${pMap[p]||"b-mid"}">${escapeHtml(pLbl[p]||p)}</span><span class="badge ${tc.type==="FUNCTIONAL"?"b-func":"b-nf"}">${escapeHtml(tc.type||"")}</span>${assignee}${(tc.areaTags||[]).map(t=>`<span class="badge b-tag">${escapeHtml(t.name)}</span>`).join("")}`;
}

// ══════════════════════════════════════════════════════════════════
// 영역 태그
// ══════════════════════════════════════════════════════════════════

async function loadAreaTags() {
  try {
    const qs = state.currentProjectId ? `?projectId=${state.currentProjectId}` : "";
    state.areaTags = await request(`/api/area-tags${qs}`,{method:"GET"});
    renderTagSelect(); renderFilterAreaTagSelect(); renderAreaTagManageList();
  }
  catch (_e) { state.areaTags = []; }
}

function renderAreaTagManageList() {
  const list = document.getElementById("areaTagManageList");
  if (!list) return;
  list.innerHTML = "";
  if (state.areaTags.length === 0) {
    list.innerHTML = '<div class="plan-empty">등록된 영역 태그가 없습니다.</div>';
    return;
  }
  state.areaTags.forEach(tag => {
    const row = document.createElement("div");
    row.className = "area-tag-row";
    row.innerHTML = `<span class="area-tag-name">${escapeHtml(tag.name)}</span><button type="button" class="btn btn-sm btn-danger area-tag-delete-btn" data-id="${tag.id}">삭제</button>`;
    list.appendChild(row);
  });
  list.querySelectorAll(".area-tag-delete-btn").forEach(btn => {
    btn.addEventListener("click", () => deleteAreaTag(Number(btn.dataset.id)));
  });
}

async function deleteAreaTag(id) {
  const tag = state.areaTags.find(t => t.id === id);
  if (!tag) return;
  if (!window.confirm(`"${tag.name}" 태그를 삭제할까요?\n이 태그가 연결된 테스트 케이스에서도 제거됩니다.`)) return;
  try {
    await request(`/api/area-tags/${id}`, { method: "DELETE" });
    _toast(`"${tag.name}" 태그를 삭제했습니다.`);
    await loadAreaTags();
    await loadTestCases();
  } catch (e) { _toast(`태그 삭제 실패: ${e.message}`, true); }
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
    const created = await request("/api/area-tags",{method:"POST",body:JSON.stringify({name:trimmed, projectId: state.currentProjectId || null})});
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
    renderServerEnvManageList();
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

async function loadUsers() {
  try {
    state.users = await request("/api/users", { method: "GET" });
    renderUserList();
    renderAssigneeSelects();
  } catch (error) {
    state.users = [];
    renderUserList();
    renderAssigneeSelects();
    _toast(`사용자 조회 실패: ${error.message}`, true);
  }
}

function renderAssigneeSelects() {
  const selects = [elements.assignee, document.getElementById("runAssigneeInput")].filter(Boolean);
  const activeUsers = state.users.filter(user => user.active);
  selects.forEach(select => {
    const previous = select.value;
    select.innerHTML = '<option value="">담당자 없음</option>';
    activeUsers.forEach(user => {
      const option = document.createElement("option");
      option.value = user.name;
      option.textContent = `${user.name} · ${USER_ROLE_LABELS[user.role] || user.role}`;
      select.appendChild(option);
    });
    if (previous && !activeUsers.some(user => user.name === previous)) {
      const option = document.createElement("option");
      option.value = previous;
      option.textContent = `${previous} (비활성/외부)`;
      select.appendChild(option);
    }
    select.value = previous || "";
  });
}

function renderUserList() {
  const list = document.getElementById("userList");
  if (!list) return;
  list.innerHTML = "";
  if (state.users.length === 0) {
    list.innerHTML = '<div class="plan-empty">등록된 사용자가 없습니다.</div>';
    return;
  }
  state.users.forEach(user => {
    const button = document.createElement("button");
    button.type = "button";
    button.className = `configuration-list-item${user.id === state.selectedUserId ? " active" : ""}`;
    button.innerHTML = `
      <div class="plan-item-name">${escapeHtml(user.name)}</div>
      <span class="user-email">${escapeHtml(user.email || "이메일 없음")}</span>
      <div class="plan-item-meta">
        <span class="badge ${user.active ? "b-done" : "b-tag"}">${user.active ? "ACTIVE" : "INACTIVE"}</span>
        <span class="badge user-role-badge">${escapeHtml(USER_ROLE_LABELS[user.role] || user.role)}</span>
      </div>`;
    button.addEventListener("click", () => editUser(user));
    list.appendChild(button);
  });
}

function resetUserForm() {
  state.selectedUserId = null;
  document.getElementById("userForm").reset();
  document.getElementById("userId").value = "";
  document.getElementById("userRole").value = "QA";
  document.getElementById("userActive").checked = true;
  document.getElementById("deleteUserButton").disabled = true;
  renderUserList();
  document.getElementById("userName").focus();
}

function editUser(user) {
  state.selectedUserId = user.id;
  document.getElementById("userId").value = user.id;
  document.getElementById("userName").value = user.name;
  document.getElementById("userEmail").value = user.email || "";
  document.getElementById("userRole").value = user.role;
  document.getElementById("userActive").checked = user.active;
  document.getElementById("deleteUserButton").disabled = !user.active;
  renderUserList();
}

async function saveUser(event) {
  event.preventDefault();
  const id = document.getElementById("userId").value;
  const payload = {
    name: document.getElementById("userName").value.trim(),
    email: document.getElementById("userEmail").value.trim() || null,
    role: document.getElementById("userRole").value,
    active: document.getElementById("userActive").checked
  };
  try {
    const saved = await request(id ? `/api/users/${id}` : "/api/users", {
      method: id ? "PUT" : "POST", body: JSON.stringify(payload)
    });
    state.selectedUserId = saved.id;
    _toast(id ? "사용자를 수정했습니다." : "사용자를 생성했습니다.");
    await loadUsers();
    editUser(state.users.find(user => user.id === saved.id));
  } catch (error) { _toast(`사용자 저장 실패: ${error.message}`, true); }
}

async function deactivateUser() {
  if (!state.selectedUserId || !window.confirm("선택한 사용자를 비활성화할까요? 기존 담당자 기록은 유지됩니다.")) return;
  try {
    await request(`/api/users/${state.selectedUserId}`, { method: "DELETE" });
    _toast("사용자를 비활성화했습니다.");
    await loadUsers();
    const user = state.users.find(item => item.id === state.selectedUserId);
    if (user) editUser(user);
    else resetUserForm();
  } catch (error) { _toast(`사용자 비활성화 실패: ${error.message}`, true); }
}

// ══════════════════════════════════════════════════════════════════
// 필터
// ══════════════════════════════════════════════════════════════════

let _filterDebounceTimer = null;

function _scheduleFilterFetch() {
  clearTimeout(_filterDebounceTimer);
  _filterDebounceTimer = setTimeout(fetchFilteredTestCases, 300);
}

async function fetchFilteredTestCases() {
  const p = new URLSearchParams();
  if (state.filters.status)    p.set("status",    state.filters.status);
  if (state.filters.os)        p.set("os",        state.filters.os);
  if (state.filters.type)      p.set("type",      state.filters.type);
  if (state.filters.areaTagId) p.set("areaTagId", state.filters.areaTagId);
  if (state.filters.keyword)   p.set("keyword",   state.filters.keyword);
  if (state.currentProjectId)  p.set("projectId", state.currentProjectId);
  const qs = p.toString();
  if (!qs) {
    // 필터 없음 → 전체 목록 복원
    state.testCases = state.allTestCases;
    renderFolderTree(); renderList();
    return;
  }
  try {
    // allTestCases는 건드리지 않음 — 대시보드는 항상 전체 기준
    state.testCases = await request(`/api/testcases?${qs}`, { method: "GET" });
    renderFolderTree(); renderList();
  } catch (e) { updateStatus(`필터 조회 실패: ${e.message}`); }
}

function initFilters() {
  elements.statusFilterPills.addEventListener("click", e => {
    const pill = e.target.closest(".filter-pill"); if (!pill) return;
    elements.statusFilterPills.querySelectorAll(".filter-pill").forEach(p => p.classList.remove("active")); pill.classList.add("active");
    state.filters.status = pill.dataset.value; _scheduleFilterFetch();
  });
  elements.osFilterPills.addEventListener("click", e => {
    const pill = e.target.closest(".filter-pill"); if (!pill) return;
    elements.osFilterPills.querySelectorAll(".filter-pill").forEach(p => p.classList.remove("active")); pill.classList.add("active");
    state.filters.os = pill.dataset.value; _scheduleFilterFetch();
  });
  elements.filterType.addEventListener("change",    () => { state.filters.type      = elements.filterType.value;    _scheduleFilterFetch(); });
  elements.filterAreaTag.addEventListener("change", () => { state.filters.areaTagId = elements.filterAreaTag.value; _scheduleFilterFetch(); });
  elements.filterKeyword.addEventListener("input",  () => { state.filters.keyword   = elements.filterKeyword.value.trim(); _scheduleFilterFetch(); });
}

// 폴더 필터만 클라이언트에서 처리 (서버는 subfolder 재귀 미지원)
function applyFilters(testCases) {
  return testCases;
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
  if (elements.assignee) elements.assignee.value = "";
  if (elements.tcVersion) elements.tcVersion.value = "";
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
  if (elements.auditSection) elements.auditSection.style.display = "none";
  if (elements.auditList) elements.auditList.innerHTML = "";
  if (elements.versionSection) elements.versionSection.style.display = "none";
  if (elements.versionList) elements.versionList.innerHTML = "";
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
  if (elements.assignee) elements.assignee.value = testCase.assignee || "";
  if (elements.tcVersion) elements.tcVersion.value = testCase.version || "";
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
  await loadTestCaseVersions(testCase.id);
  await loadAuditLogs(testCase.id);
}

const AUDIT_ACTION_LABELS = {
  CREATED: "생성",
  UPDATED: "수정",
  STATUS_CHANGED: "상태 변경",
  MOVED: "폴더 이동",
  DEFECT_LINKED: "결함 연결",
  DEFECT_UNLINKED: "결함 해제",
  VERSION_CREATED: "버전 생성",
  VERSION_RESTORED: "버전 복원",
  DELETED: "삭제"
};

const AUDIT_FIELD_LABELS = {
  testCase: "테스트케이스",
  type: "유형",
  priority: "우선순위",
  status: "상태",
  title: "제목",
  description: "설명",
  precondition: "사전조건",
  steps: "단계",
  notes: "메모",
  os: "OS",
  browser: "브라우저",
  device: "디바이스",
  assignee: "담당자",
  version: "버전",
  folder: "폴더",
  serverEnvironment: "서버 환경",
  testConfiguration: "Configuration",
  areaTags: "영역 태그",
  defects: "결함"
};

async function loadAuditLogs(testCaseId = elements.testCaseId?.value) {
  if (!elements.auditSection || !elements.auditList) return;
  if (!testCaseId) {
    elements.auditSection.style.display = "none";
    elements.auditList.innerHTML = "";
    return;
  }
  elements.auditSection.style.display = "";
  elements.auditList.innerHTML = '<p class="audit-empty">변경이력을 불러오는 중입니다.</p>';
  try {
    const logs = await request(`/api/testcases/${testCaseId}/audit-logs`, { method: "GET" });
    renderAuditLogs(logs);
  } catch (e) {
    elements.auditList.innerHTML = `<p class="audit-empty">변경이력 조회 실패: ${escapeHtml(e.message)}</p>`;
  }
}

function renderAuditLogs(logs) {
  if (!elements.auditList) return;
  if (!logs || logs.length === 0) {
    elements.auditList.innerHTML = '<p class="audit-empty">아직 기록된 변경이력이 없습니다.</p>';
    return;
  }
  // 최신 20건까지만 보여준다 — 그 이상은 화면이 무한히 길어지는 대신 목록 안에서 스크롤한다.
  const capped = logs.slice(0, 20);
  elements.auditList.innerHTML = capped.map(log => {
    const hasValues = log.oldValue !== null || log.newValue !== null;
    const oldValue = log.oldValue ?? "없음";
    const newValue = log.newValue ?? "없음";
    return `
      <div class="audit-item">
        <div class="audit-time">${escapeHtml(formatDateTime(log.createdAt))}</div>
        <div class="audit-main">
          <div class="audit-summary">
            <span class="badge b-tag">${escapeHtml(AUDIT_ACTION_LABELS[log.action] || log.action)}</span>
            <span>${escapeHtml(AUDIT_FIELD_LABELS[log.fieldName] || log.fieldName)}</span>
            <span style="color:var(--text-muted);font-weight:500">${escapeHtml(log.actor || "system")}</span>
          </div>
          ${hasValues ? `<div class="audit-values"><span class="audit-value" title="${escapeHtml(oldValue)}">${escapeHtml(oldValue)}</span><span class="audit-arrow">→</span><span class="audit-value" title="${escapeHtml(newValue)}">${escapeHtml(newValue)}</span></div>` : ""}
        </div>
      </div>`;
  }).join("");
}

async function loadTestCaseVersions(testCaseId = elements.testCaseId?.value) {
  if (!elements.versionSection || !elements.versionList) return;
  if (!testCaseId) {
    elements.versionSection.style.display = "none";
    elements.versionList.innerHTML = "";
    return;
  }
  elements.versionSection.style.display = "";
  elements.versionList.innerHTML = '<p class="version-empty">버전을 불러오는 중입니다.</p>';
  try {
    const versions = await request(`/api/testcases/${testCaseId}/versions`, { method: "GET" });
    renderTestCaseVersions(versions);
  } catch (e) {
    elements.versionList.innerHTML = `<p class="version-empty">버전 조회 실패: ${escapeHtml(e.message)}</p>`;
  }
}

function renderTestCaseVersions(versions) {
  if (!elements.versionList) return;
  if (!versions || versions.length === 0) {
    elements.versionList.innerHTML = '<p class="version-empty">아직 저장된 버전이 없습니다.</p>';
    return;
  }
  const latestNumber = Math.max(...versions.map(v => v.versionNumber || 0));
  // 최신 20건까지만 보여준다 — 그 이상은 화면이 무한히 길어지는 대신 목록 안에서 스크롤한다.
  const capped = versions.slice(0, 20);
  elements.versionList.innerHTML = capped.map(version => {
    const latest = version.versionNumber === latestNumber;
    const env = [version.os, version.browser, version.device].filter(Boolean).join(" / ") || "환경 없음";
    const tags = version.areaTagNames ? ` · ${version.areaTagNames}` : "";
    return `
      <div class="version-item">
        <div class="version-no">v${escapeHtml(version.versionNumber)}</div>
        <div class="version-main">
          <div class="version-title">
            <span>${escapeHtml(version.label || `v${version.versionNumber}`)}</span>
            ${latest ? '<span class="badge b-ready">현재</span>' : ""}
            <span class="badge b-tag">${escapeHtml(version.status || "")}</span>
          </div>
          <div class="version-meta" title="${escapeHtml(version.title || "")}">${escapeHtml(formatDateTime(version.createdAt))} · ${escapeHtml(version.changeSummary || "스냅샷")} · ${escapeHtml(env)}${escapeHtml(tags)}</div>
        </div>
        <button type="button" class="btn btn-sm version-restore-btn" data-version-id="${version.id}" ${latest ? "disabled" : ""}>복원</button>
      </div>`;
  }).join("");
  elements.versionList.querySelectorAll(".version-restore-btn").forEach(button => {
    button.addEventListener("click", () => restoreTestCaseVersion(button.dataset.versionId));
  });
}

async function restoreTestCaseVersion(versionId) {
  const testCaseId = elements.testCaseId.value;
  if (!testCaseId || !versionId) return;
  if (!confirm("선택한 버전으로 현재 테스트케이스를 복원할까요? 현재 상태도 새 버전으로 기록됩니다.")) return;
  try {
    const restored = await request(`/api/testcases/${testCaseId}/versions/${versionId}/restore`, { method: "POST" });
    _toast(`TC-${String(restored.id).padStart(3,"0")} 버전을 복원했습니다.`);
    await loadTestCases();
    const fresh = state.allTestCases.find(tc => tc.id === restored.id) || restored;
    await populateForm(fresh);
  } catch (e) {
    _toast(`버전 복원 실패: ${e.message}`, true);
  }
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
  const tc = state.allTestCases.find(item => String(item.id) === id);
  if (!tc) { _toast("복제할 테스트케이스를 찾을 수 없습니다.", true); return; }
  elements.testCaseId.value = ""; elements.type.value = tc.type||"FUNCTIONAL"; elements.priority.value = tc.priority||"MEDIUM";
  setStatusSelectorValue("DRAFT");
  elements.title.value = `${tc.title} 사본`; elements.description.value = tc.description||""; elements.precondition.value = tc.precondition||"";
  renderSteps(parseSteps(tc.steps));
  elements.notes.value = tc.notes||""; elements.envOs.value = tc.os||""; elements.envBrowser.value = tc.browser||""; elements.envDevice.value = tc.device||"";
  if (elements.assignee) elements.assignee.value = tc.assignee || "";
  if (elements.tcVersion) elements.tcVersion.value = tc.version || "";
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
    assignee:     elements.assignee?.value.trim() || null,
    version:      elements.tcVersion?.value.trim() || null,
    serverEnvironmentId: elements.envServer.value ? Number(elements.envServer.value) : null,
    testConfigurationId: elements.testConfiguration.value ? Number(elements.testConfiguration.value) : null,
    folderId: elements.tcFolder?.value ? Number(elements.tcFolder.value) : null,
    projectId: state.currentProjectId || null
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
  if (isNew && !formFolderId &&
      state.selectedFolderId &&
      state.selectedFolderId !== "all" &&
      state.selectedFolderId !== "unclassified") {
    effectiveFolderId = state.selectedFolderId;
  }

  if (effectiveFolderId) state.folderAssignments[key] = effectiveFolderId;
  else delete state.folderAssignments[key];
  persistFolders();

  const folderId = effectiveFolderId ? Number(effectiveFolderId) : null;
  request(`/api/testcases/${tcId}/folder`, { method: "PATCH", body: JSON.stringify({ folderId }) }).catch(() => {});
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

// 실행 이력(테스트런 결과) 한 줄 — 항상 읽기 전용. 결과 자체는 테스트런 화면에서만 기록/수정한다.
function _buildRunItemLi(entry) {
  const li = document.createElement("li"); li.className = "run-item";
  const cls = RESULT_CLASS[entry.status] || "untested";
  const versionText = entry.versionLabel || (entry.versionNumber ? `v${entry.versionNumber}` : null);
  const reasonLabel = REASON_REQUIRED_LABEL[entry.status] || "사유";
  li.innerHTML =
    `<div class="run-dot" style="background:${{PASSED:"var(--c-pass)",FAILED:"var(--c-hi)",BLOCKED:"#777",RETEST:"#b45309"}[entry.status]||"#ccc"}"></div>` +
    `<div class="run-info">` +
      `<div class="run-hd">` +
        `<span class="run-status run-status-${escapeHtml(cls)}">${escapeHtml(RESULT_LABEL[entry.status] || entry.status)}</span>` +
        (versionText ? `<span class="run-item-version">${escapeHtml(versionText)}</span>` : "") +
        `<span class="run-date">${escapeHtml(formatDateTime(entry.executedAt))}</span>` +
      `</div>` +
      `<div class="run-note run-item-exec-link" data-exec-id="${entry.executionId}">📋 ${escapeHtml(entry.executionName)} (${escapeHtml(EXEC_STATUS_LABEL[entry.executionStatus] || entry.executionStatus)})</div>` +
      (entry.comment ? `<div class="run-note" style="color:var(--text-muted);margin-top:2px">${escapeHtml(entry.comment)}</div>` : "") +
      (entry.failureReason ? `<div class="run-item-failure">${escapeHtml(reasonLabel)}: ${escapeHtml(entry.failureReason)}</div>` : "") +
    `</div>`;
  li.querySelector(".run-item-exec-link").addEventListener("click", () => {
    switchView("runs");
    openExecution(entry.executionId);
  });
  return li;
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
      runsByTcId[tc.id] = await request(`/api/test-runs/items/by-test-case/${tc.id}`, { method: "GET" });
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
      <div class="rs"><div class="rs-n" style="color:#b45309">${retest}</div><div class="rs-l">재테스트</div></div>
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
  runs.forEach(run => list.appendChild(_buildRunItemLi(run)));
  detail.appendChild(list);
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
    const planQs = state.currentProjectId ? `?projectId=${state.currentProjectId}` : "";
    state.testPlans = await request(`/api/test-plans${planQs}`);
    await loadTestCases();
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
    const runLabel = plan.completedRunCount > 0 ? `완료 ${plan.completedRunCount}건` : "완료 없음";
    button.innerHTML = `<div class="plan-item-name">${escapeHtml(plan.name)}</div><div class="plan-item-meta"><span class="badge ${planStatusClass(plan.status)}">${escapeHtml(plan.status)}</span><span>테스트런 ${runLabel}</span></div>`;
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
  if (!list) return;   // 스위트 컬럼이 없으면 skip
  const plan  = state.testPlans.find(item => item.id === state.selectedPlanId);
  const titleEl = document.getElementById("suiteColumnTitle");
  if (titleEl) titleEl.textContent = plan ? `${plan.name} 스위트` : "테스트 스위트";
  const noplan = !plan;
  const suiteBtn = document.getElementById("newSuiteButton");
  if (suiteBtn) suiteBtn.disabled = noplan;
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
  document.getElementById("suiteRunPanel").hidden = true;
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
  const g = id => document.getElementById(id);
  if (g("planRiskItems"))       g("planRiskItems").value       = plan?.riskItems ?? "";
  if (g("planScope"))           g("planScope").value           = plan?.scope ?? "";
  if (g("planTeamSize"))        g("planTeamSize").value        = plan?.teamSize ?? "";
  if (g("planTeamMembers"))     g("planTeamMembers").value     = plan?.teamMembers ?? "";
  if (g("planQualityCriteria")) g("planQualityCriteria").value = plan?.qualityCriteria ?? "";
  if (g("planBudget"))          g("planBudget").value          = plan?.budget ?? "";
  if (g("planNotes"))           g("planNotes").value           = plan?.planNotes ?? "";
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
  document.getElementById("suiteRunFromEditButton").hidden = !suite;
  renderSuiteCasePicker(suite?.testCases?.map(testCase => testCase.id) ?? []);
}

// ══════════════════════════════════════════════════════════════════
// 스위트 실행 — 케이스별 PASS/FAIL/BLOCK 기록 (TestRun API 재사용)
// ══════════════════════════════════════════════════════════════════

const SUITE_RUN_LABEL = { PASSED: "통과", FAILED: "실패", BLOCKED: "차단" };

function showSuiteRun(suite = null) {
  if (!state.selectedPlanId) return;
  const resolved = suite || state.testSuites.find(s => s.id === state.selectedSuiteId);
  if (!resolved) { showPlanForm(state.testPlans.find(p => p.id === state.selectedPlanId)); return; }
  hidePlanEditors();
  document.getElementById("suiteRunPanel").hidden = false;
  document.getElementById("suiteRunTitle").textContent = `${resolved.name} 빠른 실행`;
  loadSuiteRun(resolved);
}

// 통과/실패/차단/재테스트를 비율대로 이어 붙인 세그먼트 진행바. 미실행은 빈 트랙으로 남는다.
function renderSegmentBar(barEl, c = {}) {
  if (!barEl) return;
  const total = c.total || 0;
  const seg = (n, cls) => n > 0 ? `<div class="bar-seg ${cls}" style="width:${(n / total) * 100}%"></div>` : "";
  barEl.innerHTML = total === 0 ? "" :
    seg(c.passed, "pass") + seg(c.failed, "fail") + seg(c.blocked, "block") + seg(c.retest, "retest");
}

async function loadSuiteRun(suite) {
  const cases = suite.testCases ?? [];
  const cont = document.getElementById("suiteRunCases");
  if (cases.length === 0) {
    document.getElementById("suiteRunStats").textContent = "";
    renderSegmentBar(document.getElementById("suiteRunBar"), { total: 0 });
    cont.innerHTML = '<div class="plan-empty">이 스위트에 배정된 테스트케이스가 없습니다. 편집에서 케이스를 추가하세요.</div>';
    return;
  }
  cont.innerHTML = '<div class="plan-empty">실행 이력을 불러오는 중…</div>';
  // 각 케이스의 최신 실행 결과 조회 (응답은 executedAt 내림차순)
  const latest = {};
  await Promise.all(cases.map(async tc => {
    try {
      const runs = await request(`/api/testcases/${tc.id}/runs`, { method: "GET" });
      latest[tc.id] = runs[0] ?? null;
    } catch (_e) { latest[tc.id] = null; }
  }));
  renderSuiteRun(suite, latest);
}

function renderSuiteRun(suite, latest) {
  const cases = suite.testCases ?? [];
  const total = cases.length;
  const executed = cases.filter(tc => latest[tc.id]).length;
  const pass  = cases.filter(tc => latest[tc.id]?.status === "PASSED").length;
  const fail  = cases.filter(tc => latest[tc.id]?.status === "FAILED").length;
  const block = cases.filter(tc => latest[tc.id]?.status === "BLOCKED").length;

  const pct = total ? Math.round((executed / total) * 100) : 0;
  renderSegmentBar(document.getElementById("suiteRunBar"), { total, passed: pass, failed: fail, blocked: block });
  document.getElementById("suiteRunStats").innerHTML =
    `<span class="suite-run-prog">${executed}/${total} 실행 (${pct}%)</span>` +
    `<span class="suite-run-chip pass">통과 ${pass}</span>` +
    `<span class="suite-run-chip fail">실패 ${fail}</span>` +
    `<span class="suite-run-chip block">차단 ${block}</span>`;

  const cont = document.getElementById("suiteRunCases");
  cont.innerHTML = "";
  cases.forEach(tc => {
    const run = latest[tc.id];
    const statusKey = run ? run.status.toLowerCase() : "none";
    const badge = run
      ? `<span class="suite-run-badge ${statusKey}">${SUITE_RUN_LABEL[run.status] || run.status}</span><span class="suite-run-when">${escapeHtml(formatDateTime(run.executedAt))}</span>`
      : `<span class="suite-run-badge none">미실행</span>`;
    const row = document.createElement("div");
    row.className = "suite-run-case";
    row.innerHTML =
      `<div class="suite-run-case-hd">` +
        `<span class="suite-run-tc">TC-${String(tc.id).padStart(3, "0")}</span>` +
        `<span class="suite-run-name" title="${escapeHtml(tc.title)}">${escapeHtml(tc.title)}</span>` +
        badge +
      `</div>` +
      `<div class="suite-run-case-actions">` +
        `<input class="form-input suite-run-note" placeholder="실제 결과 / 비고 (선택)">` +
        `<button type="button" class="btn btn-sm suite-run-btn pass" data-s="PASSED">통과</button>` +
        `<button type="button" class="btn btn-sm suite-run-btn fail" data-s="FAILED">실패</button>` +
        `<button type="button" class="btn btn-sm suite-run-btn block" data-s="BLOCKED">차단</button>` +
      `</div>`;
    const note = row.querySelector(".suite-run-note");
    row.querySelectorAll(".suite-run-btn").forEach(btn => {
      btn.addEventListener("click", () => recordSuiteCaseRun(suite, tc, btn.dataset.s, note.value.trim(), latest));
    });
    cont.appendChild(row);
  });
}

async function recordSuiteCaseRun(suite, testCase, status, note, latest) {
  const actualResult = note || `${SUITE_RUN_LABEL[status]} 처리`;
  try {
    const created = await request(`/api/testcases/${testCase.id}/runs`, {
      method: "POST",
      body: JSON.stringify({ status, actualResult, notes: note || null })
    });
    latest[testCase.id] = created;
    renderSuiteRun(suite, latest);
    _toast(`TC-${String(testCase.id).padStart(3, "0")} ${SUITE_RUN_LABEL[status]} 기록됨`);
  } catch (e) { _toast(`실행 기록 실패: ${e.message}`, true); }
}

function renderSuiteCasePicker(selectedIds) {
  const picker = document.getElementById("suiteCasePicker");
  const selected = new Set(selectedIds);
  picker.innerHTML = "";
  if (state.allTestCases.length === 0) {
    picker.innerHTML = '<div class="plan-empty">배정할 테스트케이스가 없습니다.</div>';
    return;
  }
  state.allTestCases.forEach(testCase => {
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
    description: document.getElementById("planDescription").value.trim() || null,
    riskItems: document.getElementById("planRiskItems")?.value.trim() || null,
    scope: document.getElementById("planScope")?.value.trim() || null,
    teamSize: Number(document.getElementById("planTeamSize")?.value) || null,
    teamMembers: document.getElementById("planTeamMembers")?.value.trim() || null,
    qualityCriteria: document.getElementById("planQualityCriteria")?.value.trim() || null,
    budget: document.getElementById("planBudget")?.value.trim() || null,
    planNotes: document.getElementById("planNotes")?.value.trim() || null,
    projectId: state.currentProjectId || null
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
// 테스트런 (실행 사이클) — 스위트 스냅샷 + 케이스별 결과 기록
// ══════════════════════════════════════════════════════════════════

const RESULT_LABEL = { UNTESTED: "미실행", PASSED: "통과", FAILED: "실패", BLOCKED: "차단", RETEST: "재테스트" };
const RESULT_CLASS = { UNTESTED: "untested", PASSED: "passed", FAILED: "failed", BLOCKED: "blocked", RETEST: "retest" };
const EXEC_STATUS_LABEL = { IN_PROGRESS: "진행 중", COMPLETED: "완료" };

async function loadExecutions() {
  state.apiBaseUrl = getApiBaseUrl();
  try {
    const qs = state.currentProjectId ? `?projectId=${state.currentProjectId}` : "";
    state.executions = await request(`/api/test-runs${qs}`, { method: "GET" });
    renderExecutionList();
    if (state.selectedExecutionId && state.executions.some(e => e.id === state.selectedExecutionId)) {
      await openExecution(state.selectedExecutionId);
    } else {
      state.selectedExecutionId = null;
      document.getElementById("runDetail").hidden = true;
      document.getElementById("runDetailEmpty").hidden = false;
    }
  } catch (e) {
    state.executions = []; renderExecutionList();
    state.selectedExecutionId = null;
    document.getElementById("runDetail").hidden = true;
    document.getElementById("runDetailEmpty").hidden = false;
    _toast(`테스트런 조회 실패: ${e.message}`, true);
  }
}

// 테스트 플랜별로 고정된 색상을 배정 — 사이드바에서 같은 플랜에 속한 런을 한눈에 구분.
const PLAN_COLOR_VARS = ["--c-plan-1", "--c-plan-2", "--c-plan-3", "--c-plan-4", "--c-plan-5", "--c-plan-6", "--c-plan-7", "--c-plan-8"];
function planColorVar(planId) {
  if (!planId) return null;
  let hash = 0;
  for (const ch of String(planId)) hash = (hash * 31 + ch.charCodeAt(0)) % PLAN_COLOR_VARS.length;
  return `var(${PLAN_COLOR_VARS[hash]})`;
}

function buildRunCycleItem(exec) {
  const item = document.createElement("button");
  item.type = "button";
  const planColor = planColorVar(exec.testPlanId);
  item.className = `run-cycle-item${exec.id === state.selectedExecutionId ? " active" : ""}${planColor ? " has-plan" : ""}`;
  if (planColor) item.style.setProperty("--plan-color", planColor);
  const done = exec.total - exec.untested;
  item.innerHTML =
    `<div class="run-cycle-name">${escapeHtml(exec.name)}</div>` +
    `<div class="run-cycle-meta">` +
      `<span class="badge ${exec.status === "COMPLETED" ? "b-pass" : "b-tag"}">${EXEC_STATUS_LABEL[exec.status] || exec.status}</span>` +
      `<span>${done}/${exec.total} (${exec.progressPct}%)</span>` +
      (exec.failed ? `<span class="run-cycle-fail">실패 ${exec.failed}</span>` : "") +
      (exec.blocked ? `<span class="run-cycle-block">차단 ${exec.blocked}</span>` : "") +
      (exec.retest ? `<span class="run-cycle-retest">재테스트 ${exec.retest}</span>` : "") +
    `</div>`;
  item.addEventListener("click", () => openExecution(exec.id));
  return item;
}

// 같은 테스트 플랜에 속한 런들을 플랜 단위 섹션으로 묶어서 표시 — 플랜이 없는 런은 "플랜 미지정" 섹션으로.
function groupExecutionsByPlan(executions) {
  const groups = [];
  const indexByKey = new Map();
  executions.forEach(exec => {
    const key = exec.testPlanId ?? "__none__";
    if (!indexByKey.has(key)) {
      indexByKey.set(key, groups.length);
      groups.push({ key, planId: exec.testPlanId ?? null, planName: exec.testPlanId ? (exec.planName || "이름 없는 플랜") : "플랜 미지정", items: [] });
    }
    groups[indexByKey.get(key)].items.push(exec);
  });
  return groups;
}

function renderExecutionList() {
  const list = document.getElementById("runList");
  const empty = document.getElementById("runEmpty");
  document.getElementById("runCount").textContent = state.executions.length;
  list.innerHTML = "";
  if (state.executions.length === 0) { empty.hidden = false; return; }
  empty.hidden = true;
  const groups = groupExecutionsByPlan(state.executions);
  groups.forEach(group => {
    const planColor = planColorVar(group.planId);
    const header = document.createElement("div");
    header.className = `run-plan-group-hd${planColor ? " has-plan" : ""}`;
    if (planColor) header.style.setProperty("--plan-color", planColor);
    header.innerHTML = `<span class="run-plan-group-dot"></span><span class="run-plan-group-name">${escapeHtml(group.planName)}</span><span class="badge b-tag">${group.items.length}</span>`;
    list.appendChild(header);
    group.items.forEach(exec => list.appendChild(buildRunCycleItem(exec)));
  });
}

async function openExecution(id) {
  state.selectedExecutionId = id;
  try {
    const exec = await request(`/api/test-runs/${id}`, { method: "GET" });
    renderExecutionDetail(exec);
    renderExecutionList();
  } catch (e) { _toast(`테스트런 조회 실패: ${e.message}`, true); }
}

// 결과 분포 도넛 차트 (완료 리포트용)
function donutSvg(c) {
  const total = c.total || 0;
  const r = 54, cx = 70, cy = 70, sw = 20, C = 2 * Math.PI * r;
  const segs = [
    { n: c.passed,  color: "var(--c-pass)" },
    { n: c.failed,  color: "var(--c-fail)" },
    { n: c.blocked, color: "#b45309" },
    { n: c.retest,  color: "#d97706" },
    { n: c.untested, color: "#e5e5e3" }
  ].filter(s => s.n > 0);
  let offset = 0;
  const arcs = total === 0
    ? `<circle cx="${cx}" cy="${cy}" r="${r}" fill="none" stroke="#e5e5e3" stroke-width="${sw}"/>`
    : segs.map(s => {
        const len = (s.n / total) * C;
        const el = `<circle cx="${cx}" cy="${cy}" r="${r}" fill="none" stroke="${s.color}" stroke-width="${sw}" stroke-dasharray="${len} ${C - len}" stroke-dashoffset="${-offset}" transform="rotate(-90 ${cx} ${cy})"/>`;
        offset += len;
        return el;
      }).join("");
  const executed = total - c.untested;
  const passRate = executed ? Math.round((c.passed / executed) * 100) : 0;
  return `<svg width="140" height="140" viewBox="0 0 140 140" class="run-donut">${arcs}` +
    `<text x="${cx}" y="${cy - 1}" text-anchor="middle" class="run-donut-pct">${passRate}%</text>` +
    `<text x="${cx}" y="${cy + 17}" text-anchor="middle" class="run-donut-sub">통과율</text></svg>`;
}

// 런 상세 헤더의 플랜 배지 — 클릭하면 인라인 드롭다운으로 바뀌어 즉시 플랜을 재배정할 수 있다.
async function ensureTestPlansLoaded() {
  if (state.testPlans && state.testPlans.length > 0) return state.testPlans;
  try { state.testPlans = await request("/api/test-plans", { method: "GET" }); } catch (_e) { state.testPlans = []; }
  return state.testPlans;
}

function renderRunPlanBadge(exec) {
  const wrap = document.getElementById("runPlanBadgeWrap");
  if (!wrap) return;
  const planColor = planColorVar(exec.testPlanId);
  const badge = document.createElement("button");
  badge.type = "button";
  badge.className = `run-plan-badge${planColor ? " has-plan" : ""}`;
  if (planColor) badge.style.setProperty("--plan-color", planColor);
  badge.innerHTML = `<span class="run-plan-badge-dot"></span>${escapeHtml(exec.planName || "플랜 없음")}`;
  badge.title = "클릭해서 소속 플랜 변경";
  badge.addEventListener("click", async () => {
    const plans = await ensureTestPlansLoaded();
    const select = document.createElement("select");
    select.className = "form-input run-plan-badge-select";
    select.innerHTML = `<option value="">플랜 없음</option>` +
      plans.map(p => `<option value="${p.id}"${p.id === exec.testPlanId ? " selected" : ""}>${escapeHtml(p.name)}</option>`).join("");
    wrap.innerHTML = "";
    wrap.appendChild(select);
    select.focus();
    let committed = false;
    const revert = () => { if (!committed) { wrap.innerHTML = ""; wrap.appendChild(badge); } };
    select.addEventListener("blur", revert);
    select.addEventListener("change", async () => {
      committed = true;
      const newPlanId = select.value ? Number(select.value) : null;
      try {
        const updated = await request(`/api/test-runs/${exec.id}/plan`, {
          method: "PATCH",
          body: JSON.stringify({ testPlanId: newPlanId })
        });
        state.currentExec = updated;
        renderRunPlanBadge(updated);
        renderRunPlanSummary(updated);
        const idx = state.executions.findIndex(e => e.id === updated.id);
        if (idx >= 0) { state.executions[idx] = { ...state.executions[idx], ...summaryOf(updated) }; renderExecutionList(); }
        _toast("플랜을 변경했습니다.");
      } catch (e) {
        _toast(`플랜 변경 실패: ${e.message}`, true);
        wrap.innerHTML = ""; wrap.appendChild(badge);
      }
    });
  });
  wrap.innerHTML = "";
  wrap.appendChild(badge);
}

function renderExecutionDetail(exec) {
  document.getElementById("runDetailEmpty").hidden = true;
  document.getElementById("runDetail").hidden = false;
  document.getElementById("runDetailName").textContent = exec.name;
  renderRunPlanBadge(exec);
  renderRunPlanSummary(exec);
  const created = exec.createdAt ? formatDateTime(exec.createdAt) : "";
  const isCompleted = exec.status === "COMPLETED";
  const completedAt = exec.completedAt ? formatDateTime(exec.completedAt) : "";
  document.getElementById("runDetailMeta").textContent =
    `${exec.suiteName || "테스트케이스 직접 선택"} · 생성 ${created}${exec.assignee ? " · " + exec.assignee : ""}` +
    (isCompleted && completedAt ? ` · 완료 ${completedAt}` : "");

  const statusBadge = document.getElementById("runDetailStatus");
  statusBadge.textContent = EXEC_STATUS_LABEL[exec.status] || exec.status;
  statusBadge.className = `badge ${isCompleted ? "b-pass" : "b-tag"}`;

  document.getElementById("runCompleteButton").textContent = isCompleted ? "다시 열기" : "완료 처리";

  const done = exec.total - exec.untested;
  const counts = { total: exec.total, passed: exec.passed, failed: exec.failed, blocked: exec.blocked, retest: exec.retest, untested: exec.untested };

  const report = document.getElementById("runDetailReport");

  // ── 런 대시보드: 도넛 + 요약 — 진행 중·완료 상태 모두 항상 표시 ──
  report.hidden = false;
  report.innerHTML =
    `<div class="run-report-chart">${donutSvg(counts)}</div>` +
    `<div class="run-report-summary">` +
      `<div class="run-report-line"><span class="run-report-num">${done}/${exec.total}</span><span class="run-report-cap">${isCompleted ? "실행 완료" : "실행됨"}</span></div>` +
      `<div class="suite-run-stats" style="margin-top:6px">${runChips(exec)}</div>` +
      (completedAt ? `<div class="run-report-when">완료 ${completedAt}</div>` : "") +
    `</div>`;

  state.currentExec = exec;
  const cont = document.getElementById("runDetailItems");
  cont.innerHTML = "";
  const buildFn = window._patchedBuildRunItemRow || buildRunItemRow;
  (exec.items || []).forEach(item => cont.appendChild(buildFn(item, isCompleted)));
}

function runChips(exec) {
  return `<span class="suite-run-chip pass">통과 ${exec.passed}</span>` +
    `<span class="suite-run-chip fail">실패 ${exec.failed}</span>` +
    `<span class="suite-run-chip block">차단 ${exec.blocked}</span>` +
    `<span class="suite-run-chip retest">재테스트 ${exec.retest}</span>` +
    (exec.untested ? `<span class="suite-run-chip">미실행 ${exec.untested}</span>` : "");
}

// 진행바 + 집계 칩 + 대시보드 도넛 갱신 (진행 중 화면 전용) — 결과 기록 시 전체 재렌더 없이 헤더만 업데이트.
function renderRunProgress(exec) {
  const done = exec.total - exec.untested;
  const report = document.getElementById("runDetailReport");
  if (report && !report.hidden) {
    const counts = { total: exec.total, passed: exec.passed, failed: exec.failed, blocked: exec.blocked, retest: exec.retest, untested: exec.untested };
    report.querySelector(".run-report-chart").innerHTML = donutSvg(counts);
    report.querySelector(".run-report-num").textContent = `${done}/${exec.total}`;
    report.querySelector(".suite-run-stats").innerHTML = runChips(exec);
  }
}

// 테스트케이스 원본 상세 — 펼침 패널에서 재조회 없이 재사용.
const tcDetailCache = new Map();
async function fetchTestCaseDetail(testCaseId) {
  if (tcDetailCache.has(testCaseId)) return tcDetailCache.get(testCaseId);
  const tc = await request(`/api/testcases/${testCaseId}`, { method: "GET" });
  tcDetailCache.set(testCaseId, tc);
  return tc;
}

const PRIORITY_LABEL = { HIGH: "높음", MEDIUM: "보통", LOW: "낮음" };
const CASE_OS_LABEL = { MAC: "Mac", WINDOWS: "Windows", LINUX: "Linux", IOS: "iOS", ANDROID: "Android" };
const CASE_BROWSER_LABEL = { CHROME: "Chrome", FIREFOX: "Firefox", SAFARI: "Safari", EDGE: "Edge", SAMSUNG_INTERNET: "Samsung Internet", NONE: "없음 (앱/API)" };
const CASE_DEVICE_LABEL = { DESKTOP: "Desktop", MOBILE: "Mobile", TABLET: "Tablet" };

function caseDetailEnvironmentHtml(tc) {
  if (tc.testConfiguration) {
    const c = tc.testConfiguration;
    const parts = [
      c.os ? CASE_OS_LABEL[c.os] || c.os : null,
      c.browser ? CASE_BROWSER_LABEL[c.browser] || c.browser : null,
      c.device ? CASE_DEVICE_LABEL[c.device] || c.device : null,
      c.serverEnvironment?.name
    ].filter(Boolean);
    return `<div class="case-detail-field"><strong>테스트 환경</strong><p>${escapeHtml(c.name)}${parts.length ? ` (${parts.map(escapeHtml).join(" · ")})` : ""}</p></div>`;
  }
  const parts = [
    tc.os ? CASE_OS_LABEL[tc.os] || tc.os : null,
    tc.browser ? CASE_BROWSER_LABEL[tc.browser] || tc.browser : null,
    tc.device ? CASE_DEVICE_LABEL[tc.device] || tc.device : null,
    tc.serverEnvironment?.name
  ].filter(Boolean);
  return parts.length ? `<div class="case-detail-field"><strong>테스트 환경</strong><p>${parts.map(escapeHtml).join(" · ")}</p></div>` : "";
}

function caseDetailDefectsHtml(defects) {
  if (!defects || defects.length === 0) return "";
  const rows = defects.map(d => {
    const jira = d.jiraKey ? `<span class="defect-jira-key">${escapeHtml(d.jiraKey)}</span>` : "";
    const link = d.externalUrl ? `<a href="${escapeHtml(d.externalUrl)}" target="_blank" rel="noreferrer" class="defect-ext-link">↗ 외부 링크</a>` : "";
    return `<div class="case-detail-defect-row">` +
      `<span class="badge sev-${d.severity}">${_sevLabel[d.severity] ?? d.severity}</span>` +
      `<span class="badge dst-${d.status}">${_dstLabel[d.status] ?? d.status}</span>` +
      `<span class="case-detail-defect-title">${escapeHtml(d.title)}</span>${jira}${link}` +
    `</div>`;
  }).join("");
  return `<div class="case-detail-field"><strong>연결된 결함 (${defects.length})</strong>${rows}</div>`;
}

// 케이스 펼침 패널 — 원본 테스트케이스 상세 + 이번 런에서의 실행 결과를 함께 보여준다.
async function renderCaseDetailBody(bodyEl, item) {
  bodyEl.innerHTML = `<div class="case-detail-loading">불러오는 중…</div>`;
  let tc, attachments;
  try {
    tc = await fetchTestCaseDetail(item.testCaseId);
    attachments = await request(`/api/testcases/${item.testCaseId}/attachments`, { method: "GET" }).catch(() => []);
  } catch (e) {
    bodyEl.innerHTML = `<div class="case-detail-loading">테스트케이스 상세를 불러오지 못했습니다: ${escapeHtml(e.message)}</div>`;
    return;
  }
  const steps = parseSteps(tc.steps);
  bodyEl.innerHTML =
    `<div class="case-detail-meta-row">` +
      `<span class="case-detail-field"><strong>우선순위</strong>${escapeHtml(PRIORITY_LABEL[tc.priority] || tc.priority || "-")}</span>` +
      `<span class="case-detail-field"><strong>유형</strong>${escapeHtml(tc.type || "-")}</span>` +
    `</div>` +
    (tc.description ? `<div class="case-detail-field"><strong>설명</strong><p>${escapeHtml(tc.description)}</p></div>` : "") +
    (tc.precondition ? `<div class="case-detail-field"><strong>사전조건</strong><p>${escapeHtml(tc.precondition)}</p></div>` : "") +
    (steps.length && steps[0] ? `<div class="case-detail-field"><strong>테스트 스텝</strong><ol class="case-detail-steps">${steps.map(s => `<li>${escapeHtml(s)}</li>`).join("")}</ol></div>` : "") +
    caseDetailEnvironmentHtml(tc) +
    (tc.notes ? `<div class="case-detail-field"><strong>메모</strong><p>${escapeHtml(tc.notes)}</p></div>` : "") +
    caseDetailDefectsHtml(tc.defects) +
    `<div class="case-detail-field"><strong>첨부파일</strong><div class="case-detail-attachments"></div></div>` +
    (item.comment ? `<div class="case-detail-field"><strong>비고</strong><p>${escapeHtml(item.comment)}</p></div>` : "") +
    (item.failureReason ? `<div class="case-detail-failure"><strong>${escapeHtml(REASON_REQUIRED_LABEL[item.status] || "사유")}</strong><p>${escapeHtml(item.failureReason)}</p></div>` : "");
  renderAttachmentList(bodyEl.querySelector(".case-detail-attachments"), attachments, () => renderCaseDetailBody(bodyEl, item));
}

// 실행 결과 행 한 줄 — 초기 렌더와 기록 후 인플레이스 교체에서 공용으로 쓴다.
function buildRunItemRow(item, isCompleted) {
  const row = document.createElement("div");
  row.className = `suite-run-case${isCompleted ? " is-locked" : ""}`;
  row.dataset.itemId = item.id;
  row.dataset.status = item.status;
  const cls = RESULT_CLASS[item.status] || "untested";
  const versionText = item.versionLabel || (item.versionNumber ? `v${item.versionNumber}` : null);
  const head =
    `<div class="suite-run-case-hd">` +
      `<span class="suite-run-tc">TC-${String(item.testCaseId).padStart(3, "0")}</span>` +
      `<span class="case-detail-toggle">▸</span>` +
      `<span class="suite-run-name" title="${escapeHtml(item.caseTitle)}">${escapeHtml(item.caseTitle)}</span>` +
      `<span class="suite-run-badge ${cls}">${RESULT_LABEL[item.status] || item.status}</span>` +
      (versionText ? `<span class="run-item-version" title="이 런이 생성될 때 케이스의 버전">${escapeHtml(versionText)}</span>` : "") +
      (item.executedAt ? `<span class="suite-run-when">${escapeHtml(formatDateTime(item.executedAt))}</span>` : "") +
    `</div>`;
  const detailBody = `<div class="case-detail-body" hidden></div>`;

  function attachToggle() {
    const nameEl = row.querySelector(".suite-run-name");
    const bodyEl = row.querySelector(".case-detail-body");
    nameEl.addEventListener("click", () => {
      const expanding = bodyEl.hidden;
      bodyEl.hidden = !expanding;
      row.classList.toggle("expanded", expanding);
      if (expanding && !bodyEl.dataset.loaded) {
        bodyEl.dataset.loaded = "1";
        renderCaseDetailBody(bodyEl, item);
      }
    });
  }

  if (isCompleted) {
    // 읽기 전용: 결과 + 비고 텍스트만, 단 상세는 펼쳐서 볼 수 있음
    row.innerHTML = head + (item.comment ? `<div class="run-item-comment">${escapeHtml(item.comment)}</div>` : "") + detailBody;
    attachToggle();
    return row;
  }
  const act = (s) => item.status === s ? " active" : "";
  row.innerHTML = head +
    `<div class="suite-run-case-actions">` +
      `<input class="form-input suite-run-note" placeholder="비고 (선택)" value="${escapeHtml(item.comment || "")}">` +
      `<button type="button" class="btn btn-sm suite-run-btn pass${act("PASSED")}" data-s="PASSED">통과</button>` +
      `<button type="button" class="btn btn-sm suite-run-btn fail${act("FAILED")}" data-s="FAILED">실패</button>` +
      `<button type="button" class="btn btn-sm suite-run-btn block${act("BLOCKED")}" data-s="BLOCKED">차단</button>` +
      `<button type="button" class="btn btn-sm suite-run-btn retest${act("RETEST")}" data-s="RETEST">재테스트</button>` +
    `</div>` + detailBody;
  const note = row.querySelector(".suite-run-note");
  row.querySelectorAll(".suite-run-btn").forEach(btn => {
    btn.addEventListener("click", () => recordExecutionItem(row, btn.dataset.s, note.value.trim()));
  });
  attachToggle();
  return row;
}

async function recordExecutionItem(rowEl, clickedStatus, comment) {
  const exec = state.currentExec;
  if (!exec) return;
  const itemId = Number(rowEl.dataset.itemId);
  // 같은 결과를 다시 누르면 '미실행'으로 되돌린다 — 오클릭 복구.
  const target = rowEl.dataset.status === clickedStatus ? "UNTESTED" : clickedStatus;
  const buttons = rowEl.querySelectorAll(".suite-run-btn");
  buttons.forEach(b => { b.disabled = true; });           // 중복 제출 방지
  try {
    const updated = await request(`/api/test-runs/${exec.id}/items/${itemId}`, {
      method: "PATCH",
      body: JSON.stringify({ status: target, comment: comment || null })
    });
    state.currentExec = updated;
    const updatedItem = (updated.items || []).find(it => it.id === itemId);
    if (updatedItem) rowEl.replaceWith(buildRunItemRow(updatedItem, false)); // 해당 행만 교체 → 스크롤·타 행 비고 보존
    renderRunProgress(updated);
    const idx = state.executions.findIndex(e => e.id === updated.id);
    if (idx >= 0) { state.executions[idx] = { ...state.executions[idx], ...summaryOf(updated) }; renderExecutionList(); }
    _toast(target === "UNTESTED" ? "미실행으로 되돌렸습니다." : `${RESULT_LABEL[target]} 기록됨`);
  } catch (e) {
    buttons.forEach(b => { b.disabled = false; });
    _toast(`결과 기록 실패: ${e.message}`, true);
  }
}

function summaryOf(exec) {
  const { items, ...rest } = exec;
  return rest;
}

async function toggleExecutionComplete() {
  const exec = await request(`/api/test-runs/${state.selectedExecutionId}`, { method: "GET" }).catch(() => null);
  if (!exec) return;
  const next = exec.status === "COMPLETED" ? "IN_PROGRESS" : "COMPLETED";
  try {
    await request(`/api/test-runs/${exec.id}`, {
      method: "PUT",
      body: JSON.stringify({ name: exec.name, description: exec.description || null, status: next, assignee: exec.assignee || null })
    });
    _toast(next === "COMPLETED" ? "테스트런을 완료 처리했습니다." : "테스트런을 다시 열었습니다.");
    await loadExecutions();
  } catch (e) { _toast(`상태 변경 실패: ${e.message}`, true); }
}

async function deleteSelectedExecution() {
  if (!state.selectedExecutionId || !window.confirm("이 테스트런을 삭제할까요? 기록된 결과도 함께 삭제됩니다.")) return;
  try {
    await request(`/api/test-runs/${state.selectedExecutionId}`, { method: "DELETE" });
    state.selectedExecutionId = null;
    _toast("테스트런을 삭제했습니다.");
    await loadExecutions();
  } catch (e) { _toast(`삭제 실패: ${e.message}`, true); }
}

// ── 새 테스트런 모달 ───────────────────────────────────────────────

async function openNewRunModal() {
  state.apiBaseUrl = getApiBaseUrl();
  await loadUsers();
  try {
    const planQs = state.currentProjectId ? `?projectId=${state.currentProjectId}` : "";
    state.testPlans = await request(`/api/test-plans${planQs}`, { method: "GET" });
  } catch (_e) { state.testPlans = []; }
  const planSel = document.getElementById("runPlanSelect");
  planSel.innerHTML = '<option value="">-- 플랜 없이 생성 --</option>' +
    state.testPlans.map(p => `<option value="${p.id}">${escapeHtml(p.name)}</option>`).join("");
  document.getElementById("runNameInput").value = "";
  document.getElementById("runAssigneeInput").value = "";
  _runTcPickerAllTcs = [];
  _runTcPickerSelectedIds = new Set();
  setRunSourceMode("suite");
  await populateRunSuiteSelect(null);
  document.getElementById("newRunModal").hidden = false;
}

async function populateRunSuiteSelect(planId) {
  const suiteSel = document.getElementById("runSuiteSelect");
  suiteSel.innerHTML = '<option value="">스위트를 선택하세요</option>';
  try {
    const qs = state.currentProjectId ? `?projectId=${state.currentProjectId}` : "";
    const suites = await request(`/api/suites${qs}`, { method: "GET" });
    if (suites.length === 0) {
      suiteSel.innerHTML += '<option value="" disabled>스위트 없음</option>';
      return;
    }
    suites.forEach(s => {
      const count = s.testCases?.length || 0;
      const opt = document.createElement("option");
      opt.value = s.id;
      opt.disabled = count === 0;
      opt.textContent = `${s.name} (${count}건${s.testPlanName ? " · " + s.testPlanName : ""}${count === 0 ? " · 생성 불가" : ""})`;
      suiteSel.appendChild(opt);
    });
    const first = suites.find(s => (s.testCases?.length || 0) > 0);
    if (first) suiteSel.value = String(first.id);
  } catch (e) { suiteSel.innerHTML += '<option value="" disabled>조회 실패</option>'; }
}

async function createExecution() {
  const createButton = document.getElementById("newRunCreateButton");
  const planId = document.getElementById("runPlanSelect").value ? Number(document.getElementById("runPlanSelect").value) : null;
  let payload;
  if (state.runSourceMode === "cases") {
    if (_runTcPickerSelectedIds.size === 0) { _toast("테스트케이스를 1개 이상 선택하세요.", true); return; }
    payload = {
      testCaseIds: [..._runTcPickerSelectedIds],
      testPlanId: planId,
      projectId: state.currentProjectId || null,
      name: document.getElementById("runNameInput").value.trim() || null,
      assignee: document.getElementById("runAssigneeInput").value.trim() || null
    };
  } else {
    const suiteId = Number(document.getElementById("runSuiteSelect").value);
    if (!suiteId) { _toast("스위트를 선택하세요.", true); return; }
    payload = {
      suiteId,
      name: document.getElementById("runNameInput").value.trim() || null,
      assignee: document.getElementById("runAssigneeInput").value.trim() || null
    };
  }
  try {
    createButton.disabled = true;
    createButton.textContent = "생성 중...";
    const created = await request("/api/test-runs", { method: "POST", body: JSON.stringify(payload) });
    closeNewRunModal();
    state.selectedExecutionId = created.id;
    _toast(`테스트런 '${created.name}' 생성됨 (${created.total}건)`);
    await loadExecutions();
  } catch (e) { _toast(`테스트런 생성 실패: ${e.message}`, true); }
  finally {
    createButton.disabled = false;
    createButton.textContent = "생성";
  }
}

function closeNewRunModal() {
  document.getElementById("newRunModal").hidden = true;
}

// ══════════════════════════════════════════════════════════════════
// API
// ══════════════════════════════════════════════════════════════════

async function request(path, options = {}) {
  state.apiBaseUrl = normalizeApiBaseUrl(state.apiBaseUrl);
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
    const qs = state.currentProjectId ? `?projectId=${state.currentProjectId}` : "";
    const all = await request(`/api/testcases${qs}`, { method: "GET" });
    state.allTestCases = all;
    state.testCases    = all;
    _rebuildFolderAssignments();
    const hasFilter = state.filters.status || state.filters.os || state.filters.type || state.filters.areaTagId || state.filters.keyword;
    if (hasFilter) {
      await fetchFilteredTestCases();   // 필터 재적용
    } else {
      renderFolderTree(); renderList();
    }
    _stopRetry();
  } catch (e) {
    state.allTestCases = []; state.testCases = []; renderFolderTree(); renderList();
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
        state.allTestCases = tcs;
        state.testCases = tcs;
        _rebuildFolderAssignments();
        _stopRetry();
        await loadProjects();
        await loadAreaTags();
        await loadServerEnvironments();
        await loadTestConfigurations();
        await loadFolders();
        renderFolderTree(); renderList();
        updateStatus(`${tcs.length}개 로드됨`);
      });
    } catch (_e) {}
  }, 3000);
}
function _stopRetry() {
  if (_retryTimer) { clearInterval(_retryTimer); _retryTimer = null; }
}

// 테스트케이스 1건의 실행 이력 — 테스트런(Execution)을 통해 기록된 결과만 보여준다(버전 스냅샷 포함).
async function loadTestRuns(testCaseId) {
  updateRunStatus("실행 이력을 불러오는 중입니다.");
  try {
    state.testRuns = await request(`/api/test-runs/items/by-test-case/${testCaseId}`, { method: "GET" });
    renderTestRuns();
    updateRunStatus(state.testRuns.length === 0 ? "아직 실행 이력이 없습니다." : `총 ${state.testRuns.length}개의 실행 이력이 있습니다.`);
  } catch (e) {
    state.testRuns = []; renderTestRuns(); updateRunStatus(`조회 실패: ${e.message}`);
  }
}

function renderTestRuns() {
  elements.testRunList.innerHTML = "";
  if (elements.runStats) {
    const g = id => document.getElementById(id);
    const pass    = state.testRuns.filter(r=>r.status==="PASSED").length;
    const fail    = state.testRuns.filter(r=>r.status==="FAILED").length;
    const block   = state.testRuns.filter(r=>r.status==="BLOCKED").length;
    const retest  = state.testRuns.filter(r=>r.status==="RETEST").length;
    if (g("rsPass"))   g("rsPass").textContent   = pass;
    if (g("rsFail"))   g("rsFail").textContent   = fail;
    if (g("rsBlock"))  g("rsBlock").textContent  = block;
    if (g("rsRetest")) g("rsRetest").textContent = retest;
    elements.runStats.style.display = state.testRuns.length > 0 ? "grid" : "none";
  }
  state.testRuns.forEach(entry => elements.testRunList.appendChild(_buildRunItemLi(entry)));
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

  try {
    if (id) {
      // ── 수정 ──
      const updated = await request(`/api/testcases/${id}`, { method: "PUT", body: JSON.stringify(payload) });
      saveFolderAssignment(updated.id);
      _toast(`TC-${String(updated.id).padStart(3,"0")} 수정 완료`);
      setFlowStage("saved", "수정이 반영됐습니다.");
      await loadTestCases();
      const refreshed = state.allTestCases.find(tc => String(tc.id) === id);
      if (refreshed) await populateForm(refreshed);
    } else {
      // ── 생성 ──
      const created = await request("/api/testcases", { method: "POST", body: JSON.stringify(payload) });
      saveFolderAssignment(created.id, true);  // isNew: 선택된 폴더 자동 배정
      _toast(`TC-${String(created.id).padStart(3,"0")} 생성 완료`);
      setFlowStage("saved", "저장됐습니다. 실행 결과를 기록할 수 있습니다.");
      await loadTestCases();
      const createdItem = state.allTestCases.find(tc => tc.id === created.id);
      if (createdItem) { await populateForm(createdItem); elements.actualResult.focus(); }
    }
  } catch (e) {
    console.error("[Save] failed:", e);
    _toast(`저장 실패: ${e.message}`, true);
  }
}

async function handleDelete() {
  const id = elements.testCaseId.value; if (!id) return;
  const hasRuns = state.testRuns.length > 0;
  const msg = hasRuns
    ? `실행 기록이 남아있습니다. 테스트케이스 TC-${String(id).padStart(3,"0")}를 삭제할까요?`
    : `테스트케이스 TC-${String(id).padStart(3,"0")}를 삭제할까요?`;
  if (!window.confirm(msg)) return;
  try {
    await request(`/api/testcases/${id}`, { method: "DELETE" });
    delete state.folderAssignments[id]; persistFolders();
    _toast(`TC-${String(id).padStart(3,"0")} 삭제 완료`);
    hideEditor(); setSelected(null); state.testRuns = []; elements.testCaseId.value = "";
    switchTcTab("list"); await loadTestCases();
  } catch (e) { _toast(`삭제 실패: ${e.message}`, true); }
}

async function verifyConnection() {
  try {
    state.apiBaseUrl = getApiBaseUrl();
    if (elements.apiBaseUrl) elements.apiBaseUrl.value = state.apiBaseUrl;
    localStorage.setItem("tms.apiBaseUrl", state.apiBaseUrl);
    await request("/api/testcases",{method:"GET"});
    _toast(`연결 성공: ${state.apiBaseUrl}`);
    // 기동 시점에 백엔드가 꺼져 있었다면 프로젝트 목록 등 초기 로딩이 조용히 실패한 채로 남아있을 수 있다 —
    // 연결에 성공하면 항상 전체 상태를 다시 불러와 동기화한다.
    await loadProjects();
    await refreshAllForProjectSwitch();
  }
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

  try {
    state.apiBaseUrl = normalizeApiBaseUrl(localStorage.getItem("tms.apiBaseUrl"), config.defaultApiBaseUrl);
  } catch (_e) {
    state.apiBaseUrl = normalizeApiBaseUrl(config.defaultApiBaseUrl);
    localStorage.setItem("tms.apiBaseUrl", state.apiBaseUrl);
  }
  if (elements.apiBaseUrl) elements.apiBaseUrl.value = state.apiBaseUrl;

  initStatusSelector();
  initFilters();

  elements.form.addEventListener("submit", handleSubmit);
  elements.refreshButton.addEventListener("click",   loadTestCases);
  elements.auditRefreshBtn?.addEventListener("click", () => loadAuditLogs());
  elements.versionRefreshBtn?.addEventListener("click", () => loadTestCaseVersions());
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
  document.getElementById("newSuiteButton")?.addEventListener("click", () => showSuiteForm());
  document.getElementById("newSuiteFolderButton")?.addEventListener("click", addSuiteFolder);
  document.getElementById("planForm").addEventListener("submit", savePlan);
  document.getElementById("suiteForm").addEventListener("submit", saveSuite);
  document.getElementById("deletePlanButton").addEventListener("click", deleteSelectedPlan);
  document.getElementById("deleteSuiteButton").addEventListener("click", deleteSelectedSuite);
  document.getElementById("suiteEditButton").addEventListener("click", () => showSuiteForm(state.testSuites.find(s => s.id === state.selectedSuiteId)));
  document.getElementById("suiteRunFromEditButton").addEventListener("click", () => showSuiteRun());

  document.getElementById("newRunButton").addEventListener("click", openNewRunModal);
  document.getElementById("manageSuitesButton")?.addEventListener("click", openSuiteManagerModal);
  document.getElementById("newRunCancelButton").addEventListener("click", closeNewRunModal);
  document.getElementById("newRunCloseButton").addEventListener("click", closeNewRunModal);
  document.getElementById("newRunCreateButton").addEventListener("click", createExecution);
  document.getElementById("runPlanSelect").addEventListener("change", e => populateRunSuiteSelect(e.target.value ? Number(e.target.value) : null));
  document.getElementById("runSourceTabSuite").addEventListener("click", () => setRunSourceMode("suite"));
  document.getElementById("runSourceTabCases").addEventListener("click", () => setRunSourceMode("cases"));
  document.getElementById("runCompleteButton").addEventListener("click", toggleExecutionComplete);
  document.getElementById("runDeleteButton").addEventListener("click", deleteSelectedExecution);
  document.getElementById("newRunModal").addEventListener("click", e => { if (e.target.id === "newRunModal") closeNewRunModal(); });
  document.addEventListener("keydown", e => {
    if (e.key === "Escape" && !document.getElementById("newRunModal").hidden) closeNewRunModal();
  });

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
  document.getElementById("newUserButton").addEventListener("click", resetUserForm);
  document.getElementById("userForm").addEventListener("submit", saveUser);
  document.getElementById("deleteUserButton").addEventListener("click", deactivateUser);

  requiredFieldConfigs.forEach(({ element }) => {
    if (!element) return;
    element.addEventListener("input",  () => element.setCustomValidity(""));
    element.addEventListener("change", () => element.setCustomValidity(""));
  });
  elements.apiBaseUrl.addEventListener("change", () => {
    try {
      state.apiBaseUrl = getApiBaseUrl();
      elements.apiBaseUrl.value = state.apiBaseUrl;
      localStorage.setItem("tms.apiBaseUrl", state.apiBaseUrl);
    } catch (e) {
      _toast(e.message, true);
      elements.apiBaseUrl.value = state.apiBaseUrl;
    }
  });

  // 초기 상태
  hideEditor(); switchTcTab("list");
  await loadProjects();
  await loadAreaTags();
  await loadServerEnvironments();
  await loadTestConfigurations();
  await loadUsers();
  await loadTestCases();          // TC 먼저 로드해야 folderAssignments 재구성 가능
  await migrateLocalStorageFolders(); // localStorage → DB 최초 1회 마이그레이션
  await loadFolders();            // DB에서 폴더 트리 로드
  renderFolderTree(); renderFolderSelect(); renderList();
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

// ══════════════════════════════════════════════════════════════════
// 사이드바 토글 (#1)
// ══════════════════════════════════════════════════════════════════

function toggleSidebar() {
  const sidebar = document.getElementById("navSidebar");
  if (!sidebar) return;
  const collapsed = sidebar.classList.toggle("collapsed");
  localStorage.setItem("tms.sidebarCollapsed", collapsed ? "1" : "");
}

(function initSidebarState() {
  if (localStorage.getItem("tms.sidebarCollapsed")) {
    const s = document.getElementById("navSidebar");
    if (s) s.classList.add("collapsed");
  }
})();

// ══════════════════════════════════════════════════════════════════
// 프로젝트 (#14)
// ══════════════════════════════════════════════════════════════════

state.currentProjectId = null;

async function loadProjects() {
  try {
    const projects = await request("/api/projects");
    const sel = document.getElementById("projectSelect");
    if (!sel) return;
    if (!projects || projects.length === 0) {
      sel.innerHTML = '<option value="">프로젝트 없음</option>';
      state.currentProjectId = null;
      return;
    }
    const saved = localStorage.getItem("tms.currentProjectId");
    const savedId = saved ? Number(saved) : null;
    const exists = savedId && projects.some(p => p.id === savedId);
    state.currentProjectId = exists ? savedId : projects[0].id;
    sel.innerHTML = projects.map(p => `<option value="${p.id}">${escapeHtml(p.name)}</option>`).join("");
    sel.value = String(state.currentProjectId);
  } catch (_e) {}
}

// 프로젝트 전환 시 모든 섹션(테스트케이스/플랜/스위트/런)의 캐시된 상태를 비우고
// 새 프로젝트 기준으로 다시 불러온다 — 어떤 탭이 떠 있든, 나중에 그 탭으로 돌아와도 항상 최신 프로젝트 데이터를 보장한다.
async function refreshAllForProjectSwitch() {
  state.selectedPlanId = null;
  state.selectedSuiteId = null;
  state.testSuites = [];
  state.selectedExecutionId = null;
  state.currentExec = null;
  await loadTestCases();
  await loadFolders();
  await loadAreaTags();
  renderFolderTree(); renderFolderSelect(); renderList();
  await loadTestPlans();
  await loadExecutions();
  renderDashboard();
}

async function onProjectChange() {
  const sel = document.getElementById("projectSelect");
  if (!sel) return;
  state.currentProjectId = Number(sel.value) || null;
  if (state.currentProjectId) localStorage.setItem("tms.currentProjectId", state.currentProjectId);
  await refreshAllForProjectSwitch();
}

function showCreateProjectModal() {
  document.getElementById("newProjectName").value = "";
  document.getElementById("newProjectDesc").value = "";
  document.getElementById("newProjectOwner").value = "";
  document.getElementById("createProjectModal").hidden = false;
}

function closeCreateProjectModal() {
  document.getElementById("createProjectModal").hidden = true;
}

async function createProject() {
  const name = document.getElementById("newProjectName").value.trim();
  if (!name) { _toast("프로젝트 이름을 입력하세요.", true); return; }
  try {
    await request("/api/projects", {
      method: "POST",
      body: JSON.stringify({
        name,
        description: document.getElementById("newProjectDesc").value.trim() || null,
        owner: document.getElementById("newProjectOwner").value.trim() || null
      })
    });
    closeCreateProjectModal();
    _toast(`프로젝트 '${name}'를 생성했습니다.`);
    await loadProjects();
    const sel = document.getElementById("projectSelect");
    if (sel) { sel.value = String(state.currentProjectId); await onProjectChange(); }
  } catch (e) { _toast(`프로젝트 생성 실패: ${e.message}`, true); }
}

// ══════════════════════════════════════════════════════════════════
// 엑셀 임포트 (#2)
// ══════════════════════════════════════════════════════════════════

let _excelFile = null;

function openExcelImportModal() {
  _excelFile = null;
  document.getElementById("excelFileInput").value = "";
  document.getElementById("excelFileInfo").style.display = "none";
  document.getElementById("excelImportResult").style.display = "none";
  document.getElementById("excelImportBtn").disabled = true;
  document.getElementById("excelImportModal").hidden = false;
}

function closeExcelImportModal() {
  document.getElementById("excelImportModal").hidden = true;
}

function excelDragOver(e) {
  e.preventDefault();
  document.getElementById("excelDropZone").classList.add("drag-over");
}

function excelDragLeave(e) {
  e.preventDefault();
  document.getElementById("excelDropZone").classList.remove("drag-over");
}

function excelDrop(e) {
  e.preventDefault();
  document.getElementById("excelDropZone").classList.remove("drag-over");
  const file = e.dataTransfer?.files?.[0];
  if (file) _setExcelFile(file);
}

function excelFileSelected(e) {
  const file = e.target.files?.[0];
  if (file) _setExcelFile(file);
}

function _setExcelFile(file) {
  _excelFile = file;
  const info = document.getElementById("excelFileInfo");
  info.style.display = "";
  info.textContent = `${file.name}  (${(file.size / 1024).toFixed(1)} KB)`;
  document.getElementById("excelImportBtn").disabled = false;
  document.getElementById("excelImportResult").style.display = "none";
}

async function doExcelImport() {
  if (!_excelFile) return;
  const btn = document.getElementById("excelImportBtn");
  btn.disabled = true;
  btn.textContent = "가져오는 중...";
  const result = document.getElementById("excelImportResult");
  result.style.display = "";
  result.innerHTML = '<span style="color:var(--text-muted)">처리 중...</span>';
  try {
    const base = normalizeApiBaseUrl(state.apiBaseUrl);
    // Electron File has .path (native filesystem path)
    const filePath = _excelFile.path;
    if (!filePath) throw new Error("파일 경로를 읽을 수 없습니다. 파일을 다시 선택해주세요.");
    const resp = await window.desktopApi.uploadExcel({
      url: `${base}/api/import/excel`,
      filePath,
      projectId: state.currentProjectId || null
    });
    if (!resp.ok) throw new Error(resp.data?.message || `HTTP ${resp.status}`);
    const data = resp.data;
    result.innerHTML = `
      <div style="font-size:12px;line-height:1.6">
        <div style="color:var(--c-pass)">✅ 폴더 ${data.createdFolders}개, 테스트케이스 ${data.createdCases}건 생성</div>
        ${data.errors?.length ? `<div style="color:var(--c-fail);margin-top:4px">⚠ 오류 ${data.errors.length}건:<br>${data.errors.slice(0,5).map(escapeHtml).join("<br>")}</div>` : ""}
      </div>`;
    await loadTestCases();
    await loadFolders();
    renderFolderTree(); renderFolderSelect(); renderList();
  } catch (e) {
    result.innerHTML = `<div style="color:var(--c-fail);font-size:12px">❌ ${escapeHtml(e.message)}</div>`;
  } finally {
    btn.disabled = false;
    btn.textContent = "가져오기";
  }
}

// ══════════════════════════════════════════════════════════════════
// 대시보드 감사 로그 + 히트맵 (#13)
// ══════════════════════════════════════════════════════════════════

async function refreshDashboardAuditLogs() {
  let stats = null;
  try {
    const qs = state.currentProjectId ? `?projectId=${state.currentProjectId}` : "";
    stats = await request(`/api/dashboard/stats${qs}`);
  } catch (_e) { stats = null; }
  renderDashboardAuditLogsFromStats(stats);
}

function renderDashboardAuditLogsFromStats(stats) {
  const listEl = document.getElementById("dashAuditList");
  const heatEl = document.getElementById("dashHeatmap");
  if (!listEl) return;
  try {
    if (!stats) throw new Error("no stats");

    // 히트맵
    if (heatEl && stats.defectHeatmap?.length) {
      const max = Math.max(...stats.defectHeatmap.map(h => h.count), 1);
      heatEl.innerHTML = stats.defectHeatmap.map(h => {
        const lvl = Math.ceil((h.count / max) * 4);
        return `<div class="heatmap-cell lvl-${lvl}" title="${escapeHtml(h.areaTag)}: ${h.count}건">${escapeHtml(h.areaTag)}<br><strong>${h.count}</strong></div>`;
      }).join("");
    } else if (heatEl) {
      heatEl.innerHTML = '<span style="font-size:12px;color:var(--text-muted)">데이터 없음</span>';
    }

    // 감사 로그
    if (stats.recentAuditLogs?.length) {
      listEl.innerHTML = stats.recentAuditLogs.map(log => `
        <div class="audit-log-item">
          <span class="audit-log-action">${escapeHtml(log.action ?? "")}</span>
          <span class="audit-log-summary">${escapeHtml(log.entityType ?? "")} #${log.entityId ?? ""}</span>
          <span style="margin-left:auto;font-size:10px;color:var(--text-muted)">${log.createdAt ? formatDateTime(log.createdAt) : ""}</span>
        </div>`).join("");
    } else {
      listEl.innerHTML = '<p style="font-size:12px;color:var(--text-muted)">최근 로그 없음</p>';
    }
  } catch (_e) {
    listEl.innerHTML = '<p style="font-size:12px;color:var(--text-muted)">로그 로드 실패</p>';
  }
}

// ══════════════════════════════════════════════════════════════════
// 실패 사유 모달 (#9)
// ══════════════════════════════════════════════════════════════════

let _failureCallback = null;

// 실패/차단/재테스트 공통 사유 모달 — 결과별로 제목/문구만 바꿔서 재사용한다.
const REASON_MODAL_META = {
  FAILED:  { title: "🔴 실패 사유 입력",   desc: "실패 이유 또는 Jira 버그 티켓 URL을 입력하세요.",   confirmLabel: "실패로 기록" },
  BLOCKED: { title: "⛔ 차단 사유 입력",   desc: "테스트를 진행할 수 없었던 이유를 입력하세요.",       confirmLabel: "차단으로 기록" },
  RETEST:  { title: "🔁 재테스트 사유 입력", desc: "재테스트가 필요한 이유를 입력하세요.",               confirmLabel: "재테스트로 기록" }
};

function openFailureReasonModal(callback, kind = "FAILED") {
  _failureCallback = callback;
  const meta = REASON_MODAL_META[kind] || REASON_MODAL_META.FAILED;
  document.getElementById("failureReasonTitle").textContent = meta.title;
  document.getElementById("failureReasonDesc").textContent = meta.desc;
  document.getElementById("failureReasonConfirmBtn").textContent = meta.confirmLabel;
  document.getElementById("failureReasonInput").value = "";
  document.getElementById("failureReasonError").hidden = true;
  document.getElementById("failureReasonModal").style.display = "flex";
}

function cancelFailureReason() {
  document.getElementById("failureReasonModal").style.display = "none";
  _failureCallback = null;
}

function confirmFailureReason() {
  const reason = document.getElementById("failureReasonInput").value.trim();
  if (!reason) { document.getElementById("failureReasonError").hidden = false; return; }
  document.getElementById("failureReasonModal").style.display = "none";
  if (_failureCallback) _failureCallback(reason);
  _failureCallback = null;
}

// ══════════════════════════════════════════════════════════════════
// 대시보드 프로젝트 카드 (#2)
// ══════════════════════════════════════════════════════════════════

async function renderDashboardProjects() {
  const grid = document.getElementById("dashProjectGrid");
  if (!grid) return;
  try {
    const projects = await request("/api/projects");
    if (!projects || projects.length === 0) { grid.innerHTML = '<span style="font-size:12px;color:var(--text-muted)">프로젝트 없음</span>'; return; }
    grid.innerHTML = projects.map(p => `
      <div class="dash-project-card${state.currentProjectId === p.id ? " active" : ""}"
           onclick="switchDashboardProject(${p.id})" title="${escapeHtml(p.description || "")}">
        <div class="dash-project-name">${escapeHtml(p.name)}</div>
        <div class="dash-project-meta">${p.owner ? escapeHtml(p.owner) : "담당자 없음"}</div>
      </div>`).join("");
  } catch (_e) {}
}

async function switchDashboardProject(id) {
  state.currentProjectId = id;
  localStorage.setItem("tms.currentProjectId", id);
  const sel = document.getElementById("projectSelect");
  if (sel) sel.value = String(id);
  await refreshAllForProjectSwitch();
}

// ══════════════════════════════════════════════════════════════════
// 런 상세 — 플랜 요약 표시 (#10)
// ══════════════════════════════════════════════════════════════════

async function renderRunPlanSummary(exec) {
  const el = document.getElementById("runPlanSummary");
  if (!el) return;
  if (!exec.testPlanId) { el.hidden = true; return; }
  try {
    const plan = await request(`/api/test-plans/${exec.testPlanId}`);
    const fields = [
      { label: "플랜 이름", value: plan.name },
      { label: "상태",     value: plan.status },
      { label: "기간",     value: [plan.startDate, plan.endDate].filter(Boolean).join(" ~ ") || null },
      { label: "범위",     value: plan.scope },
      { label: "팀 규모",  value: plan.teamSize ? `${plan.teamSize}명` : null },
      { label: "팀원",     value: plan.teamMembers },
      { label: "리스크",   value: plan.riskItems },
      { label: "품질 기준",value: plan.qualityCriteria },
      { label: "예산",     value: plan.budget },
      { label: "메모",     value: plan.planNotes }
    ].filter(f => f.value);
    el.hidden = false;
    el.innerHTML = `<div class="run-plan-summary-title">📋 연결된 테스트 플랜</div>
      <div class="run-plan-summary-grid">${fields.map(f =>
        `<div class="run-plan-summary-item"><strong>${escapeHtml(f.label)}</strong>${escapeHtml(f.value)}</div>`
      ).join("")}</div>`;
  } catch (_e) { el.hidden = true; }
}

// ══════════════════════════════════════════════════════════════════
// 스위트 관리 모달 (#3)
// ══════════════════════════════════════════════════════════════════

let _smSuites = [];
let _smSelectedId = null;
let _tcPickerSelectedIds = new Set();
let _tcPickerAllTcs = [];

async function openSuiteManagerModal() {
  document.getElementById("suiteManagerModal").hidden = false;
  _smSelectedId = null;
  _tcPickerSelectedIds = new Set();
  await loadSuiteManagerList();
  showSuiteEditorEmpty();
}

function closeSuiteManagerModal() {
  document.getElementById("suiteManagerModal").hidden = true;
}

async function loadSuiteManagerList() {
  try {
    const qs = state.currentProjectId ? `?projectId=${state.currentProjectId}` : "";
    _smSuites = await request(`/api/suites${qs}`);
  } catch (_e) { _smSuites = []; }
  renderSuiteManagerList();
}

function renderSuiteManagerList() {
  const list = document.getElementById("suiteManagerList");
  if (!list) return;
  if (_smSuites.length === 0) {
    list.innerHTML = '<div style="font-size:12px;color:var(--text-muted);padding:12px">스위트가 없습니다.</div>';
    return;
  }
  list.innerHTML = _smSuites.map(s => {
    const cnt = s.testCases?.length || 0;
    return `<div class="suite-manager-item${_smSelectedId === s.id ? " active" : ""}"
                 onclick="selectSuiteInManager(${s.id})">
      ${escapeHtml(s.name)}
      <div class="suite-manager-item-meta">${cnt}개 TC${s.testPlanName ? " · " + escapeHtml(s.testPlanName) : ""}</div>
    </div>`;
  }).join("");
}

function showSuiteEditorEmpty() {
  document.getElementById("suiteEditorEmpty").hidden = false;
  document.getElementById("suiteEditorForm").hidden = true;
}

async function selectSuiteInManager(id) {
  _smSelectedId = id;
  renderSuiteManagerList();
  const suite = _smSuites.find(s => s.id === id);
  if (!suite) return;
  document.getElementById("smSuiteId").value = suite.id;
  document.getElementById("smSuiteName").value = suite.name;
  document.getElementById("smSuiteDesc").value = suite.description || "";
  _tcPickerSelectedIds = new Set((suite.testCases || []).map(tc => tc.id));
  document.getElementById("suiteEditorEmpty").hidden = true;
  document.getElementById("suiteEditorForm").hidden = false;
  document.getElementById("smDeleteBtn").hidden = false;
  await initTcPicker();
}

function startNewSuite() {
  _smSelectedId = null;
  _tcPickerSelectedIds = new Set();
  renderSuiteManagerList();
  document.getElementById("smSuiteId").value = "";
  document.getElementById("smSuiteName").value = "";
  document.getElementById("smSuiteDesc").value = "";
  document.getElementById("suiteEditorEmpty").hidden = true;
  document.getElementById("suiteEditorForm").hidden = false;
  document.getElementById("smDeleteBtn").hidden = true;
  initTcPicker();
}

async function saveSuiteFromManager() {
  const name = document.getElementById("smSuiteName").value.trim();
  if (!name) { _toast("스위트 이름을 입력하세요.", true); return; }
  const id = document.getElementById("smSuiteId").value;
  const payload = {
    name,
    description: document.getElementById("smSuiteDesc").value.trim() || null,
    testCaseIds: [..._tcPickerSelectedIds],
    projectId: state.currentProjectId || null
  };
  try {
    if (id) {
      await request(`/api/suites/${id}`, { method: "PUT", body: JSON.stringify(payload) });
      _toast("스위트를 수정했습니다.");
    } else {
      await request("/api/suites", { method: "POST", body: JSON.stringify(payload) });
      _toast("스위트를 생성했습니다.");
    }
    await loadSuiteManagerList();
    // 새 런 모달 스위트 드롭다운도 갱신
    await populateRunSuiteSelect(null);
  } catch (e) { _toast(`저장 실패: ${e.message}`, true); }
}

async function deleteSuiteFromManager() {
  const id = document.getElementById("smSuiteId").value;
  if (!id || !window.confirm("이 스위트를 삭제할까요?")) return;
  try {
    await request(`/api/suites/${id}`, { method: "DELETE" });
    _toast("스위트를 삭제했습니다.");
    _smSelectedId = null;
    showSuiteEditorEmpty();
    await loadSuiteManagerList();
  } catch (e) { _toast(`삭제 실패: ${e.message}`, true); }
}

// ══════════════════════════════════════════════════════════════════
// TC 피커 — 폴더 그룹 + 체크박스 + 필터 (#4)
// ══════════════════════════════════════════════════════════════════

async function initTcPicker() {
  const qs = state.currentProjectId ? `?projectId=${state.currentProjectId}` : "";
  try {
    _tcPickerAllTcs = await request(`/api/testcases${qs}`);
  } catch (_e) { _tcPickerAllTcs = []; }
  document.getElementById("tcPickerSearch").value = "";
  document.getElementById("tcPickerPriority").value = "";
  document.getElementById("tcPickerStatus").value = "";
  renderTcPickerTree(_tcPickerAllTcs);
}

function filterTcPicker() {
  const keyword  = document.getElementById("tcPickerSearch").value.toLowerCase();
  const priority = document.getElementById("tcPickerPriority").value;
  const status   = document.getElementById("tcPickerStatus").value;
  const filtered = _tcPickerAllTcs.filter(tc => {
    if (keyword  && !tc.title.toLowerCase().includes(keyword))  return false;
    if (priority && tc.priority !== priority)                    return false;
    if (status   && tc.status   !== status)                     return false;
    return true;
  });
  renderTcPickerTree(filtered);
}

function renderTcPickerTree(tcs) {
  const tree = document.getElementById("tcPickerTree");
  if (!tree) return;

  if (tcs.length === 0) {
    tree.innerHTML = '<div class="tc-picker-empty">조건에 맞는 테스트케이스가 없습니다.</div>';
    updateTcPickerCount();
    return;
  }

  // 폴더별 그룹핑
  const groups = {};
  const noFolder = [];
  for (const tc of tcs) {
    const fid = tc.folderId;
    if (fid) {
      if (!groups[fid]) groups[fid] = { name: tc.folderName || `폴더 ${fid}`, tcs: [] };
      groups[fid].tcs.push(tc);
    } else {
      noFolder.push(tc);
    }
  }

  const allGroups = Object.entries(groups).map(([, g]) => g);
  if (noFolder.length) allGroups.push({ name: "미분류", tcs: noFolder });

  let html = "";
  for (const g of allGroups) {
    const ids = g.tcs.map(t => t.id).join(",");
    const selCount = g.tcs.filter(t => _tcPickerSelectedIds.has(t.id)).length;
    const allSel = selCount === g.tcs.length;
    const someSel = selCount > 0 && !allSel;
    html += `<div class="tc-picker-folder" data-folder-tcids="${ids}">
      <input type="checkbox" class="tc-picker-folder-chk" ${allSel ? "checked" : ""}
        ${someSel ? "data-indeterminate='true'" : ""}
        onclick="event.stopPropagation();toggleTcPickerFolderSelect(this)">
      <span class="tc-picker-folder-arrow" onclick="toggleTcPickerFolder(this.closest('.tc-picker-folder'))">▼</span>
      <span onclick="toggleTcPickerFolder(this.closest('.tc-picker-folder'))">📁 ${escapeHtml(g.name)}</span>
      <span style="margin-left:auto;font-size:10px;color:var(--text-muted)">${g.tcs.length}건</span>
    </div>`;
    for (const tc of g.tcs) {
      const chk = _tcPickerSelectedIds.has(tc.id) ? "checked" : "";
      const priCls = { HIGH:"b-hi", MEDIUM:"b-mid", LOW:"b-lo" }[tc.priority] || "b-mid";
      html += `<div class="tc-picker-tc" onclick="toggleTcPickerItem(event,${tc.id})">
        <input type="checkbox" ${chk} data-tcid="${tc.id}" onclick="event.stopPropagation();toggleTcPickerItem(event,${tc.id})">
        <div class="tc-picker-tc-info">
          <div class="tc-picker-tc-title">TC-${String(tc.id).padStart(3,"0")} ${escapeHtml(tc.title)}</div>
          <div class="tc-picker-tc-badges">
            <span class="badge ${priCls}" style="font-size:9px">${tc.priority}</span>
            <span class="badge b-tag" style="font-size:9px">${tc.status}</span>
          </div>
        </div>
      </div>`;
    }
  }
  tree.innerHTML = html;

  // indeterminate 상태는 JS로만 설정 가능
  tree.querySelectorAll(".tc-picker-folder-chk[data-indeterminate='true']").forEach(cb => {
    cb.indeterminate = true;
  });

  updateTcPickerCount();
}

function toggleTcPickerFolder(folderEl) {
  if (!folderEl) return;
  folderEl.classList.toggle("collapsed");
  const isCollapsed = folderEl.classList.contains("collapsed");
  let sib = folderEl.nextElementSibling;
  while (sib && sib.classList.contains("tc-picker-tc")) {
    sib.style.display = isCollapsed ? "none" : "";
    sib = sib.nextElementSibling;
  }
}

function toggleTcPickerFolderSelect(chkEl) {
  const folderEl = chkEl.closest(".tc-picker-folder");
  if (!folderEl) return;
  const ids = (folderEl.dataset.folderTcids || "").split(",").map(Number).filter(Boolean);
  const allSelected = ids.every(id => _tcPickerSelectedIds.has(id));
  if (allSelected) {
    ids.forEach(id => _tcPickerSelectedIds.delete(id));
    chkEl.checked = false;
    chkEl.indeterminate = false;
  } else {
    ids.forEach(id => _tcPickerSelectedIds.add(id));
    chkEl.checked = true;
    chkEl.indeterminate = false;
  }
  // 개별 TC 체크박스 동기화
  ids.forEach(id => {
    const cb = document.querySelector(`.tc-picker-tc input[data-tcid="${id}"]`);
    if (cb) cb.checked = _tcPickerSelectedIds.has(id);
  });
  updateTcPickerCount();
}

function toggleTcPickerItem(event, id) {
  if (_tcPickerSelectedIds.has(id)) {
    _tcPickerSelectedIds.delete(id);
  } else {
    _tcPickerSelectedIds.add(id);
  }
  const chk = document.querySelector(`.tc-picker-tc input[data-tcid="${id}"]`);
  if (chk) chk.checked = _tcPickerSelectedIds.has(id);
  // 해당 TC가 속한 폴더 체크박스 상태 업데이트
  const tcRow = chk?.closest(".tc-picker-tc");
  let folderEl = tcRow?.previousElementSibling;
  while (folderEl && !folderEl.classList.contains("tc-picker-folder")) {
    folderEl = folderEl.previousElementSibling;
  }
  if (folderEl) {
    const ids = (folderEl.dataset.folderTcids || "").split(",").map(Number).filter(Boolean);
    const selCount = ids.filter(i => _tcPickerSelectedIds.has(i)).length;
    const folderChk = folderEl.querySelector(".tc-picker-folder-chk");
    if (folderChk) {
      folderChk.checked = selCount === ids.length;
      folderChk.indeterminate = selCount > 0 && selCount < ids.length;
    }
  }
  updateTcPickerCount();
}

function updateTcPickerCount() {
  const el = document.getElementById("tcPickerCount");
  if (el) el.textContent = _tcPickerSelectedIds.size;
}

// ══════════════════════════════════════════════════════════════════
// 새 테스트런 모달 — 스위트 없이 테스트케이스 직접 선택 (#3)
// 스위트 관리의 tc-picker와 동일한 UX를 별도 상태로 둔다 — 두 모달이 동시에 열리지 않지만,
// DOM id 충돌을 피하려고 함수/상태를 통째로 분리했다.
// ══════════════════════════════════════════════════════════════════

let _runTcPickerAllTcs = [];
let _runTcPickerSelectedIds = new Set();

async function initRunTcPicker() {
  const qs = state.currentProjectId ? `?projectId=${state.currentProjectId}` : "";
  try {
    _runTcPickerAllTcs = await request(`/api/testcases${qs}`);
  } catch (_e) { _runTcPickerAllTcs = []; }
  document.getElementById("runTcPickerSearch").value = "";
  document.getElementById("runTcPickerPriority").value = "";
  document.getElementById("runTcPickerStatus").value = "";
  _runTcPickerSelectedIds = new Set();
  renderRunTcPickerTree(_runTcPickerAllTcs);
}

function filterRunTcPicker() {
  const keyword  = document.getElementById("runTcPickerSearch").value.toLowerCase();
  const priority = document.getElementById("runTcPickerPriority").value;
  const status   = document.getElementById("runTcPickerStatus").value;
  const filtered = _runTcPickerAllTcs.filter(tc => {
    if (keyword  && !tc.title.toLowerCase().includes(keyword))  return false;
    if (priority && tc.priority !== priority)                    return false;
    if (status   && tc.status   !== status)                     return false;
    return true;
  });
  renderRunTcPickerTree(filtered);
}

function renderRunTcPickerTree(tcs) {
  const tree = document.getElementById("runTcPickerTree");
  if (!tree) return;

  if (tcs.length === 0) {
    tree.innerHTML = '<div class="tc-picker-empty">조건에 맞는 테스트케이스가 없습니다.</div>';
    updateRunTcPickerCount();
    return;
  }

  const groups = {};
  const noFolder = [];
  for (const tc of tcs) {
    const fid = tc.folderId;
    if (fid) {
      if (!groups[fid]) groups[fid] = { name: tc.folderName || `폴더 ${fid}`, tcs: [] };
      groups[fid].tcs.push(tc);
    } else {
      noFolder.push(tc);
    }
  }

  const allGroups = Object.entries(groups).map(([, g]) => g);
  if (noFolder.length) allGroups.push({ name: "미분류", tcs: noFolder });

  let html = "";
  for (const g of allGroups) {
    const ids = g.tcs.map(t => t.id).join(",");
    const selCount = g.tcs.filter(t => _runTcPickerSelectedIds.has(t.id)).length;
    const allSel = selCount === g.tcs.length;
    const someSel = selCount > 0 && !allSel;
    html += `<div class="tc-picker-folder" data-folder-tcids="${ids}">
      <input type="checkbox" class="tc-picker-folder-chk" ${allSel ? "checked" : ""}
        ${someSel ? "data-indeterminate='true'" : ""}
        onclick="event.stopPropagation();toggleRunTcPickerFolderSelect(this)">
      <span class="tc-picker-folder-arrow" onclick="toggleTcPickerFolder(this.closest('.tc-picker-folder'))">▼</span>
      <span onclick="toggleTcPickerFolder(this.closest('.tc-picker-folder'))">📁 ${escapeHtml(g.name)}</span>
      <span style="margin-left:auto;font-size:10px;color:var(--text-muted)">${g.tcs.length}건</span>
    </div>`;
    for (const tc of g.tcs) {
      const chk = _runTcPickerSelectedIds.has(tc.id) ? "checked" : "";
      const priCls = { HIGH:"b-hi", MEDIUM:"b-mid", LOW:"b-lo" }[tc.priority] || "b-mid";
      html += `<div class="tc-picker-tc" onclick="toggleRunTcPickerItem(event,${tc.id})">
        <input type="checkbox" ${chk} data-tcid="${tc.id}" onclick="event.stopPropagation();toggleRunTcPickerItem(event,${tc.id})">
        <div class="tc-picker-tc-info">
          <div class="tc-picker-tc-title">TC-${String(tc.id).padStart(3,"0")} ${escapeHtml(tc.title)}</div>
          <div class="tc-picker-tc-badges">
            <span class="badge ${priCls}" style="font-size:9px">${tc.priority}</span>
            <span class="badge b-tag" style="font-size:9px">${tc.status}</span>
          </div>
        </div>
      </div>`;
    }
  }
  tree.innerHTML = html;

  tree.querySelectorAll(".tc-picker-folder-chk[data-indeterminate='true']").forEach(cb => {
    cb.indeterminate = true;
  });

  updateRunTcPickerCount();
}

function toggleRunTcPickerFolderSelect(chkEl) {
  const folderEl = chkEl.closest(".tc-picker-folder");
  if (!folderEl) return;
  const ids = (folderEl.dataset.folderTcids || "").split(",").map(Number).filter(Boolean);
  const allSelected = ids.every(id => _runTcPickerSelectedIds.has(id));
  if (allSelected) {
    ids.forEach(id => _runTcPickerSelectedIds.delete(id));
    chkEl.checked = false;
    chkEl.indeterminate = false;
  } else {
    ids.forEach(id => _runTcPickerSelectedIds.add(id));
    chkEl.checked = true;
    chkEl.indeterminate = false;
  }
  ids.forEach(id => {
    const cb = document.querySelector(`#runTcPickerTree .tc-picker-tc input[data-tcid="${id}"]`);
    if (cb) cb.checked = _runTcPickerSelectedIds.has(id);
  });
  updateRunTcPickerCount();
}

function toggleRunTcPickerItem(event, id) {
  if (_runTcPickerSelectedIds.has(id)) {
    _runTcPickerSelectedIds.delete(id);
  } else {
    _runTcPickerSelectedIds.add(id);
  }
  const chk = document.querySelector(`#runTcPickerTree .tc-picker-tc input[data-tcid="${id}"]`);
  if (chk) chk.checked = _runTcPickerSelectedIds.has(id);
  const tcRow = chk?.closest(".tc-picker-tc");
  let folderEl = tcRow?.previousElementSibling;
  while (folderEl && !folderEl.classList.contains("tc-picker-folder")) {
    folderEl = folderEl.previousElementSibling;
  }
  if (folderEl) {
    const ids = (folderEl.dataset.folderTcids || "").split(",").map(Number).filter(Boolean);
    const selCount = ids.filter(i => _runTcPickerSelectedIds.has(i)).length;
    const folderChk = folderEl.querySelector(".tc-picker-folder-chk");
    if (folderChk) {
      folderChk.checked = selCount === ids.length;
      folderChk.indeterminate = selCount > 0 && selCount < ids.length;
    }
  }
  updateRunTcPickerCount();
}

function updateRunTcPickerCount() {
  const el = document.getElementById("runTcPickerCount");
  if (el) el.textContent = _runTcPickerSelectedIds.size;
}

function setRunSourceMode(mode) {
  state.runSourceMode = mode;
  document.getElementById("runSourceTabSuite").classList.toggle("active", mode === "suite");
  document.getElementById("runSourceTabCases").classList.toggle("active", mode === "cases");
  document.getElementById("runSuiteField").hidden = mode !== "suite";
  document.getElementById("runCasesField").hidden = mode !== "cases";
  if (mode === "cases" && _runTcPickerAllTcs.length === 0) initRunTcPicker();
}

// ══════════════════════════════════════════════════════════════════
// FAIL 클릭 → 실패 사유 모달 연동 (#9)
// ══════════════════════════════════════════════════════════════════

// buildRunItemRow 패치: FAIL 버튼 클릭 시 사유 모달 → recordExecutionItem 호출
const _origBuildRunItemRow = buildRunItemRow;
// eslint-disable-next-line no-global-assign
const REASON_REQUIRED_LABEL = { FAILED: "실패 내용", BLOCKED: "차단 사유", RETEST: "재테스트 사유" };

window._patchedBuildRunItemRow = function(item, isCompleted) {
  const row = _origBuildRunItemRow(item, isCompleted);
  if (isCompleted) {
    // 완료 상태 읽기 전용 — 사유 표시 (실패/차단/재테스트 공통)
    if (item.failureReason) {
      const div = document.createElement("div");
      div.className = "run-item-failure";
      const isUrl = /^https?:\/\//i.test(item.failureReason);
      const label = REASON_REQUIRED_LABEL[item.status] || "사유";
      div.innerHTML = `🔗 ${escapeHtml(label)}: ${isUrl
        ? `<a href="${escapeHtml(item.failureReason)}" target="_blank" rel="noopener">${escapeHtml(item.failureReason)}</a>`
        : escapeHtml(item.failureReason)}`;
      row.appendChild(div);
    }
    return row;
  }
  // 진행 중 — 실패/차단/재테스트 버튼 클릭 시 모두 사유 모달을 거치게 한다.
  const note = row.querySelector(".suite-run-note");
  ["fail", "block", "retest"].forEach(cls => {
    const status = { fail: "FAILED", block: "BLOCKED", retest: "RETEST" }[cls];
    const btn = row.querySelector(`.suite-run-btn.${cls}`);
    if (!btn) return;
    const cloned = btn.cloneNode(true);
    btn.replaceWith(cloned);
    cloned.addEventListener("click", () => {
      const isSameStatus = row.dataset.status === status;
      if (isSameStatus) {
        // 이미 같은 상태 → 미실행으로 되돌림 (사유 모달 불필요)
        recordExecutionItemWithReason(row, "UNTESTED", note?.value?.trim(), "");
      } else {
        openFailureReasonModal((reason) => {
          recordExecutionItemWithReason(row, status, note?.value?.trim(), reason);
        }, status);
      }
    });
  });
  // PASS 버튼만 기존 방식 유지 (사유 불필요)
  const passBtn = row.querySelector(".suite-run-btn.pass");
  if (passBtn) {
    const cloned = passBtn.cloneNode(true);
    passBtn.replaceWith(cloned);
    cloned.addEventListener("click", () => {
      recordExecutionItem(row, cloned.dataset.s, note?.value?.trim());
    });
  }
  return row;
};

async function recordExecutionItemWithReason(rowEl, status, comment, failureReason) {
  const exec = state.currentExec;
  if (!exec) return;
  const itemId = Number(rowEl.dataset.itemId);
  const target = rowEl.dataset.status === status ? "UNTESTED" : status;
  rowEl.querySelectorAll(".suite-run-btn").forEach(b => { b.disabled = true; });
  try {
    const updated = await request(`/api/test-runs/${exec.id}/items/${itemId}`, {
      method: "PATCH",
      body: JSON.stringify({ status: target, comment: comment || null, failureReason: failureReason || null })
    });
    state.currentExec = updated;
    const updatedItem = (updated.items || []).find(it => it.id === itemId);
    if (updatedItem) rowEl.replaceWith(window._patchedBuildRunItemRow(updatedItem, false));
    renderRunProgress(updated);
    const idx = state.executions.findIndex(e => e.id === updated.id);
    if (idx >= 0) { state.executions[idx] = { ...state.executions[idx], ...summaryOf(updated) }; renderExecutionList(); }
    _toast(target === "UNTESTED" ? "미실행으로 되돌렸습니다." : `${RESULT_LABEL[target] || target} 기록됨`);
  } catch (e) {
    rowEl.querySelectorAll(".suite-run-btn").forEach(b => { b.disabled = false; });
    _toast(`결과 기록 실패: ${e.message}`, true);
  }
}

bootstrap();
