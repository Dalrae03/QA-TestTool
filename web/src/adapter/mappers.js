// Supabase 행(snake_case) ↔ Spring REST 응답(camelCase, 중첩 객체) 매핑.
// renderer.js는 기존 Spring DTO 모양을 그대로 기대하므로, 여기서 정확히 그 모양으로 되돌려준다.

// 빈 문자열/undefined → null (enum CHECK 제약 위반 방지)
export const nn = (v) => (v === "" || v === undefined ? null : v);

// ── 참조 엔티티 ────────────────────────────────────────────────────
export function projectToResponse(p) {
  if (!p) return null;
  return {
    id: p.id, name: p.name, description: p.description, owner: p.owner,
    createdAt: p.created_at, updatedAt: p.updated_at,
  };
}

export function envToResponse(e) {
  if (!e) return null;
  return {
    id: e.id, name: e.name, type: e.type, baseUrl: e.base_url,
    description: e.description, active: e.active,
    createdAt: e.created_at, updatedAt: e.updated_at,
  };
}

export function cfgToResponse(c) {
  if (!c) return null;
  return {
    id: c.id, name: c.name,
    serverEnvironment: envToResponse(c.server_environment),
    os: c.os, osVersion: c.os_version,
    browser: c.browser, browserVersion: c.browser_version,
    device: c.device, runtimeVersion: c.runtime_version, dbVersion: c.db_version,
    active: c.active, createdAt: c.created_at, updatedAt: c.updated_at,
  };
}

export function areaTagToResponse(t) {
  if (!t) return null;
  return { id: t.id, name: t.name };
}

export function defectToResponse(d) {
  if (!d) return null;
  return {
    id: d.id, title: d.title, description: d.description,
    severity: d.severity, status: d.status,
    externalUrl: d.external_url, jiraKey: d.jira_key,
    createdAt: d.created_at, updatedAt: d.updated_at,
  };
}

export function userToResponse(u) {
  if (!u) return null;
  return {
    id: u.id, name: u.name, email: u.email, role: u.role, active: u.active,
    createdAt: u.created_at, updatedAt: u.updated_at,
  };
}

// ── 테스트케이스 ───────────────────────────────────────────────────
// GET 시 사용할 embed select 문자열 (junction table 경유로 결정적으로 임베드).
export const TC_SELECT =
  "*," +
  "folder:test_folders(id,name)," + // NOTE: test_folders.code 컬럼 미존재 → 임베드에서 제외(폴백 "TC")
  "server_environment:server_environments(*)," +
  "test_configuration:test_configurations(*,server_environment:server_environments(*))," +
  "test_case_area_tags(area_tags(id,name))," +
  "test_case_defects(defects(*))," +
  "test_case_jira_requirements(jira_key)";

// 표시 ID: 폴더 코드(있으면) 접두사 + 3자리 내부 id. 코드 없으면 "TC".
function buildDisplayId(row) {
  const code = row.folder && row.folder.code ? String(row.folder.code).trim() : "";
  const prefix = code ? code : "TC";
  return `${prefix}-${String(row.id).padStart(3, "0")}`;
}

export function tcToResponse(row) {
  if (!row) return null;
  const areaTags = (row.test_case_area_tags || [])
    .map((x) => x.area_tags).filter(Boolean).map(areaTagToResponse);
  const defects = (row.test_case_defects || [])
    .map((x) => x.defects).filter(Boolean).map(defectToResponse);
  const jiraKeys = (row.test_case_jira_requirements || []).map((x) => x.jira_key);
  return {
    id: row.id,
    displayId: buildDisplayId(row),
    type: row.type, priority: row.priority, status: row.status,
    title: row.title, description: row.description, precondition: row.precondition,
    steps: row.steps, expectedResult: row.expected, notes: row.notes,
    os: row.os, browser: row.browser, device: row.device,
    assignee: row.assignee, version: row.version,
    currentVersionLabel: row.version,
    folderId: row.folder_id,
    folderName: row.folder ? row.folder.name : null,
    serverEnvironment: envToResponse(row.server_environment),
    testConfiguration: cfgToResponse(row.test_configuration),
    areaTags, defects, jiraRequirementKeys: jiraKeys,
    createdAt: row.created_at, updatedAt: row.updated_at,
  };
}

// CreateTestCaseRequest / UpdateTestCaseRequest → test_cases 행 컬럼
export function tcRequestToRow(body) {
  return {
    type: body.type,
    priority: body.priority,
    status: nn(body.status) || "DRAFT",
    title: body.title,
    description: body.description ?? "",
    precondition: body.precondition ?? "",
    steps: body.steps ?? "",
    expected: body.expectedResult ?? "",
    notes: nn(body.notes),
    os: nn(body.os),
    browser: nn(body.browser),
    device: nn(body.device),
    assignee: nn(body.assignee),
    version: nn(body.version),
    folder_id: nn(body.folderId),
    server_environment_id: nn(body.serverEnvironmentId),
    test_configuration_id: nn(body.testConfigurationId),
    project_id: nn(body.projectId),
  };
}

// ── 폴더 트리 ──────────────────────────────────────────────────────
// 평면 목록 → 부모/자식 중첩 트리. effectiveCode는 자신 코드 또는 가장 가까운 상위 코드.
export function buildFolderTree(rows) {
  const byId = new Map();
  for (const r of rows) {
    byId.set(r.id, {
      id: r.id, name: r.name, code: r.code ?? null, parent_id: r.parent_id,
      created_at: r.created_at, updated_at: r.updated_at, _children: [],
    });
  }
  const roots = [];
  for (const node of byId.values()) {
    if (node.parent_id != null && byId.has(node.parent_id)) {
      byId.get(node.parent_id)._children.push(node);
    } else {
      roots.push(node);
    }
  }
  const effectiveCode = (node) => {
    let cur = node;
    while (cur) {
      if (cur.code && String(cur.code).trim()) return String(cur.code).trim();
      cur = cur.parent_id != null ? byId.get(cur.parent_id) : null;
    }
    return null;
  };
  const toResp = (node) => ({
    id: node.id, name: node.name, code: node.code,
    effectiveCode: effectiveCode(node),
    parentId: node.parent_id,
    children: node._children.map(toResp),
    createdAt: node.created_at, updatedAt: node.updated_at,
  });
  return roots.map(toResp);
}
