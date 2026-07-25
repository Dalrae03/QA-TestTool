// 전송 어댑터 — renderer.js의 REST 호출을 Supabase(PostgREST) 쿼리로 라우팅한다.
// renderer.js는 `window.desktopApi.request({url, method, headers, body})` 를 호출하고
// `{ ok, status, data }` 를 기대한다. 여기서 URL의 경로를 파싱해 해당 핸들러로 보낸다.
//
// 구현 범위(1차 슬라이스): 부팅 로드 + 테스트케이스 관리 화면 전체
//   projects / area-tags / server-environments / test-configurations / users /
//   folders / testcases(+versions, +audit-logs, +defect link) / defects
// 미구현 엔드포인트(플랜/스위트/런/대시보드/Jira/첨부/엑셀/백업)는 501을 돌려주며,
// 이 기능들은 후속 슬라이스에서 채운다.

import { supabase } from "../supabaseClient.js";
import {
  nn, projectToResponse, envToResponse, cfgToResponse, areaTagToResponse,
  defectToResponse, userToResponse, tcToResponse, tcRequestToRow, buildFolderTree,
  TC_SELECT,
} from "./mappers.js";
import { registerRunRoutes } from "./runs.js";

export class HttpError extends Error {
  constructor(status, message) { super(message); this.status = status; }
}

// supabase-js 결과에서 error를 던지고 data를 반환
export function ok(res) {
  if (res.error) {
    const status = res.status && res.status >= 400 ? res.status : 400;
    throw new HttpError(status, res.error.message);
  }
  return res.data;
}

// ── 라우트 테이블 ──────────────────────────────────────────────────
const routes = [];
export function on(method, pattern, handler) {
  routes.push({ method, parts: pattern.split("/").filter(Boolean), handler });
}
function match(method, pathname) {
  const segs = pathname.split("/").filter(Boolean);
  for (const r of routes) {
    if (r.method !== method || r.parts.length !== segs.length) continue;
    const params = {};
    let matched = true;
    for (let i = 0; i < r.parts.length; i++) {
      const p = r.parts[i];
      if (p.startsWith(":")) params[p.slice(1)] = decodeURIComponent(segs[i]);
      else if (p !== segs[i]) { matched = false; break; }
    }
    if (matched) return { handler: r.handler, params };
  }
  return null;
}

// ── 공용 헬퍼 ──────────────────────────────────────────────────────
async function listOrdered(table, mapFn, { eq } = {}) {
  let q = supabase.from(table).select("*");
  if (eq) for (const [k, v] of Object.entries(eq)) if (v != null) q = q.eq(k, v);
  q = q.order("id", { ascending: true });
  return (ok(await q) || []).map(mapFn);
}
async function getOne(table, id, mapFn) {
  const res = await supabase.from(table).select("*").eq("id", id).maybeSingle();
  const row = ok(res);
  if (!row) throw new HttpError(404, `${table} #${id} 을(를) 찾을 수 없습니다.`);
  return mapFn(row);
}
async function insertReturning(table, row, mapFn) {
  return mapFn(ok(await supabase.from(table).insert(row).select("*").single()));
}
async function updateReturning(table, id, row, mapFn) {
  const res = await supabase.from(table).update(row).eq("id", id).select("*").maybeSingle();
  const updated = ok(res);
  if (!updated) throw new HttpError(404, `${table} #${id} 을(를) 찾을 수 없습니다.`);
  return mapFn(updated);
}
async function removeById(table, id) {
  ok(await supabase.from(table).delete().eq("id", id));
  return null; // 204
}

// ── 테스트케이스 전용 헬퍼 ─────────────────────────────────────────
async function fetchTestCase(id) {
  const res = await supabase.from("test_cases").select(TC_SELECT).eq("id", id).maybeSingle();
  const row = ok(res);
  if (!row) throw new HttpError(404, `테스트케이스 #${id} 을(를) 찾을 수 없습니다.`);
  return tcToResponse(row);
}
async function syncAreaTags(tcId, ids) {
  if (ids === undefined) return; // 요청에 없으면 건드리지 않음
  ok(await supabase.from("test_case_area_tags").delete().eq("test_case_id", tcId));
  const list = (ids || []).filter((x) => x != null);
  if (list.length) {
    const rows = list.map((area_tag_id) => ({ test_case_id: tcId, area_tag_id }));
    ok(await supabase.from("test_case_area_tags").insert(rows));
  }
}

function versionToResponse(v) {
  return {
    id: v.id, testCaseId: v.test_case_id, versionNumber: v.version_number,
    label: v.label, changeSummary: v.change_summary, type: v.type,
    priority: v.priority, status: v.status, title: v.title, version: v.version,
    description: v.description, precondition: v.precondition, steps: v.steps,
    expectedResult: v.expected_result, notes: v.notes, os: v.os, browser: v.browser,
    device: v.device, assignee: v.assignee, folderId: v.folder_id, folderName: v.folder_name,
    serverEnvironmentId: null, serverEnvironmentName: null,
    testConfigurationId: null, testConfigurationName: null, areaTagNames: null,
    createdAt: v.created_at,
  };
}
function auditToResponse(a) {
  return {
    id: a.id, entityType: a.entity_type, entityId: a.entity_id, action: a.action,
    fieldName: a.field_name, oldValue: a.old_value, newValue: a.new_value,
    summary: a.summary, actor: a.actor, createdAt: a.created_at,
  };
}

// ── 프로젝트 ───────────────────────────────────────────────────────
on("GET", "/api/projects", () => listOrdered("projects", projectToResponse));
on("POST", "/api/projects", ({ body }) => insertReturning("projects",
  { name: body.name, description: nn(body.description), owner: nn(body.owner) }, projectToResponse));
on("GET", "/api/projects/:id", ({ params }) => getOne("projects", params.id, projectToResponse));
on("PUT", "/api/projects/:id", ({ params, body }) => updateReturning("projects", params.id,
  { name: body.name, description: nn(body.description), owner: nn(body.owner) }, projectToResponse));
on("DELETE", "/api/projects/:id", ({ params }) => removeById("projects", params.id));

// ── 영역 태그 ──────────────────────────────────────────────────────
on("GET", "/api/area-tags", ({ query }) => listOrdered("area_tags", areaTagToResponse,
  { eq: { project_id: query.projectId } }));
on("POST", "/api/area-tags", ({ body }) => insertReturning("area_tags",
  { name: body.name, project_id: nn(body.projectId) }, areaTagToResponse));
on("DELETE", "/api/area-tags/:id", ({ params }) => removeById("area_tags", params.id));

// ── 서버 환경 ──────────────────────────────────────────────────────
const envRow = (b) => ({
  name: b.name, type: b.type, base_url: b.baseUrl, description: nn(b.description),
  active: b.active !== undefined ? b.active : true,
});
on("GET", "/api/server-environments", () => listOrdered("server_environments", envToResponse));
on("POST", "/api/server-environments", ({ body }) => insertReturning("server_environments", envRow(body), envToResponse));
on("GET", "/api/server-environments/:id", ({ params }) => getOne("server_environments", params.id, envToResponse));
on("PUT", "/api/server-environments/:id", ({ params, body }) => updateReturning("server_environments", params.id, envRow(body), envToResponse));
on("DELETE", "/api/server-environments/:id", ({ params }) => removeById("server_environments", params.id));

// ── 테스트 컨피그 ──────────────────────────────────────────────────
const cfgRow = (b) => ({
  name: b.name, server_environment_id: nn(b.serverEnvironmentId),
  os: nn(b.os), os_version: nn(b.osVersion), browser: nn(b.browser),
  browser_version: nn(b.browserVersion), device: nn(b.device),
  runtime_version: nn(b.runtimeVersion), db_version: nn(b.dbVersion),
  active: b.active !== undefined ? b.active : true,
});
const CFG_SELECT = "*,server_environment:server_environments(*)";
on("GET", "/api/test-configurations", async () =>
  (ok(await supabase.from("test_configurations").select(CFG_SELECT).order("id", { ascending: true })) || []).map(cfgToResponse));
on("POST", "/api/test-configurations", async ({ body }) => {
  const id = ok(await supabase.from("test_configurations").insert(cfgRow(body)).select("id").single()).id;
  return cfgToResponse(ok(await supabase.from("test_configurations").select(CFG_SELECT).eq("id", id).single()));
});
on("PUT", "/api/test-configurations/:id", async ({ params, body }) => {
  ok(await supabase.from("test_configurations").update(cfgRow(body)).eq("id", params.id));
  return cfgToResponse(ok(await supabase.from("test_configurations").select(CFG_SELECT).eq("id", params.id).single()));
});
on("DELETE", "/api/test-configurations/:id", ({ params }) => removeById("test_configurations", params.id));

// ── 사용자 ─────────────────────────────────────────────────────────
const userRow = (b) => ({
  name: b.name, email: nn(b.email), role: b.role,
  active: b.active !== undefined ? b.active : true,
});
on("GET", "/api/users", () => listOrdered("users", userToResponse));
on("POST", "/api/users", ({ body }) => insertReturning("users", userRow(body), userToResponse));
on("GET", "/api/users/:id", ({ params }) => getOne("users", params.id, userToResponse));
on("PUT", "/api/users/:id", ({ params, body }) => updateReturning("users", params.id, userRow(body), userToResponse));
on("DELETE", "/api/users/:id", ({ params }) => removeById("users", params.id));

// ── 결함 ───────────────────────────────────────────────────────────
const defectRow = (b) => ({
  title: b.title, description: nn(b.description), severity: b.severity,
  status: b.status, external_url: nn(b.externalUrl), jira_key: nn(b.jiraKey),
});
on("GET", "/api/defects", () => listOrdered("defects", defectToResponse));
on("POST", "/api/defects", ({ body }) => insertReturning("defects", defectRow(body), defectToResponse));
on("GET", "/api/defects/:id", ({ params }) => getOne("defects", params.id, defectToResponse));
on("PUT", "/api/defects/:id", ({ params, body }) => updateReturning("defects", params.id, defectRow(body), defectToResponse));
on("DELETE", "/api/defects/:id", ({ params }) => removeById("defects", params.id));

// ── 폴더 ───────────────────────────────────────────────────────────
on("GET", "/api/folders", async ({ query }) => {
  let q = supabase.from("test_folders").select("*");
  if (query.projectId != null) q = q.eq("project_id", query.projectId);
  const rows = ok(await q.order("id", { ascending: true })) || [];
  return buildFolderTree(rows);
});
const folderShallow = (r) => ({
  id: r.id, name: r.name, code: r.code ?? null, effectiveCode: r.code ?? null,
  parentId: r.parent_id, children: [], createdAt: r.created_at, updatedAt: r.updated_at,
});
// NOTE: test_folders.code 컬럼이 DB에 없어 insert/update에서 제외(협업자가 컬럼 추가 시 복원).
on("POST", "/api/folders", ({ body }) => insertReturning("test_folders",
  { name: body.name, parent_id: nn(body.parentId), project_id: nn(body.projectId) }, folderShallow));
on("PUT", "/api/folders/:id", ({ params, body }) => updateReturning("test_folders", params.id,
  { name: body.name, parent_id: nn(body.parentId) }, folderShallow));
on("DELETE", "/api/folders/:id", ({ params }) => removeById("test_folders", params.id));

// ── 테스트케이스 ───────────────────────────────────────────────────
on("GET", "/api/testcases", async ({ query }) => {
  let q = supabase.from("test_cases").select(TC_SELECT);
  const eqMap = {
    project_id: query.projectId, type: query.type, priority: query.priority,
    status: query.status, os: query.os, browser: query.browser,
    device: query.device, folder_id: query.folderId,
  };
  for (const [k, v] of Object.entries(eqMap)) if (v != null && v !== "") q = q.eq(k, v);
  if (query.keyword) {
    const kw = query.keyword.replace(/[%,]/g, " ");
    q = q.or(`title.ilike.%${kw}%,description.ilike.%${kw}%,steps.ilike.%${kw}%`);
  }
  let rows = ok(await q.order("id", { ascending: true })) || [];
  if (query.areaTagId) {
    const tid = Number(query.areaTagId);
    rows = rows.filter((r) => (r.test_case_area_tags || []).some((x) => x.area_tags && x.area_tags.id === tid));
  }
  return rows.map(tcToResponse);
});
on("POST", "/api/testcases", async ({ body }) => {
  const id = ok(await supabase.from("test_cases").insert(tcRequestToRow(body)).select("id").single()).id;
  await syncAreaTags(id, body.areaTagIds);
  return fetchTestCase(id);
});
on("GET", "/api/testcases/:id", ({ params }) => fetchTestCase(params.id));
on("PUT", "/api/testcases/:id", async ({ params, body }) => {
  ok(await supabase.from("test_cases").update(tcRequestToRow(body)).eq("id", params.id));
  await syncAreaTags(params.id, body.areaTagIds);
  return fetchTestCase(params.id);
});
on("PATCH", "/api/testcases/:id/status", async ({ params, body }) => {
  ok(await supabase.from("test_cases").update({ status: body.status }).eq("id", params.id));
  return fetchTestCase(params.id);
});
on("PATCH", "/api/testcases/:id/folder", async ({ params, body }) => {
  ok(await supabase.from("test_cases").update({ folder_id: nn(body.folderId) }).eq("id", params.id));
  return fetchTestCase(params.id);
});
on("DELETE", "/api/testcases/:id", ({ params }) => removeById("test_cases", params.id));
on("POST", "/api/testcases/:id/defects/:defectId", async ({ params }) => {
  ok(await supabase.from("test_case_defects")
    .upsert({ test_case_id: params.id, defect_id: params.defectId }, { onConflict: "test_case_id,defect_id", ignoreDuplicates: true }));
  return fetchTestCase(params.id);
});
on("DELETE", "/api/testcases/:id/defects/:defectId", async ({ params }) => {
  ok(await supabase.from("test_case_defects").delete()
    .eq("test_case_id", params.id).eq("defect_id", params.defectId));
  return fetchTestCase(params.id);
});
on("GET", "/api/testcases/:id/versions", async ({ params }) =>
  (ok(await supabase.from("test_case_versions").select("*")
    .eq("test_case_id", params.id).order("version_number", { ascending: false })) || []).map(versionToResponse));
on("GET", "/api/testcases/:id/audit-logs", async ({ params }) =>
  (ok(await supabase.from("audit_logs").select("*")
    .eq("entity_type", "TEST_CASE").eq("entity_id", params.id)
    .order("created_at", { ascending: false })) || []).map(auditToResponse));

// ── 디스패치 ───────────────────────────────────────────────────────
export async function handleRequest(options) {
  const { url, method = "GET", body } = options || {};
  let parsed;
  try { parsed = new URL(url); }
  catch { return { ok: false, status: 400, data: { message: "잘못된 요청 URL" } }; }

  const query = Object.fromEntries(parsed.searchParams.entries());
  let parsedBody = null;
  if (body != null) {
    try { parsedBody = typeof body === "string" ? JSON.parse(body) : body; }
    catch { parsedBody = body; }
  }

  const m = match(method, parsed.pathname);
  if (!m) return { ok: false, status: 501, data: { message: `아직 웹 버전에서 지원하지 않는 기능입니다 (${method} ${parsed.pathname}).` } };

  try {
    const data = await m.handler({ params: m.params, query, body: parsedBody });
    return data == null ? { ok: true, status: 204, data: null } : { ok: true, status: 200, data };
  } catch (e) {
    const status = e instanceof HttpError ? e.status : 500;
    console.error(`[adapter] ${method} ${parsed.pathname} →`, status, e.message);
    return { ok: false, status, data: { message: e.message || "서버 오류" } };
  }
}

// ── window.desktopApi 설치 (renderer.js가 기대하는 인터페이스) ─────
const notSupported = (msg) => async () => ({ ok: false, status: 501, data: { message: msg } });
export function installDesktopApi() {
  window.desktopApi = {
    getConfig: async () => ({ platform: "web", version: "0.1.0", defaultApiBaseUrl: "http://localhost:8080" }),
    request: (opts) => handleRequest(opts),
    uploadExcel: notSupported("엑셀 임포트는 후속 단계에서 지원됩니다."),
    uploadAttachment: notSupported("첨부 업로드는 후속 단계(Supabase Storage)에서 지원됩니다."),
    downloadAttachment: notSupported("첨부 다운로드는 후속 단계에서 지원됩니다."),
    downloadBackup: notSupported("백업 내보내기는 후속 단계에서 지원됩니다."),
    uploadBackup: notSupported("백업 복구는 후속 단계에서 지원됩니다."),
    chooseDirectory: async () => ({ canceled: true, dir: null }),
    saveBackupToDir: notSupported("백업은 후속 단계에서 지원됩니다."),
  };
}

// 플랜 · 스위트 · 테스트런(실행) · 레거시 런 라우트 등록 (on/ok/HttpError 초기화 이후 호출)
registerRunRoutes();
