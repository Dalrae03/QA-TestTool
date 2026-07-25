// 테스트 플랜 · 스위트 · 테스트런(실행/Execution) · 레거시 케이스별 런 라우트.
// index.js 의 registerRunRoutes() 로 등록된다. (Spring TestPlan/TestSuite/Execution/TestRun 컨트롤러 대응)
//
// 조회(GET)는 현재 스키마로 동작한다. 테스트런 "생성/결과기록"은 마이그레이션 0004가
// 적용돼 있어야 한다(executions.status='READY', execution_items 상태 확장·폴더 스냅샷 컬럼).

import { supabase } from "../supabaseClient.js";
import { on, HttpError, ok } from "./index.js";
import { nn, tcToResponse, TC_SELECT } from "./mappers.js";

const nowIso = () => new Date().toISOString();

// ── 매핑: 플랜 ─────────────────────────────────────────────────────
function planToResponse(p, agg) {
  return {
    id: p.id, name: p.name, status: p.status, assignee: p.assignee,
    startDate: p.start_date, endDate: p.end_date,
    suiteCount: agg.suiteCount, testCaseCount: agg.testCaseCount, completedRunCount: agg.completedRunCount,
    targetSystem: p.target_system, targetVersion: p.target_version,
    testGoal: p.test_goal, testTarget: p.test_target,
    coreTestCases: agg.coreTestCases,
    impactScope: p.impact_scope, commonScope: p.common_scope,
    priorityTargets: p.priority_targets, riskAnalysis: p.risk_analysis,
    testApproach: p.test_approach, testPerspective: p.test_perspective,
    entryCriteria: p.entry_criteria, exitCriteria: p.exit_criteria,
    serverEnvironmentNote: p.server_environment_note, deviceMatrix: p.device_matrix, testData: p.test_data,
    schedule: p.schedule, deliverables: p.deliverables,
    createdAt: p.created_at, updatedAt: p.updated_at,
  };
}
function planRequestToRow(b) {
  return {
    name: b.name, status: b.status, assignee: nn(b.assignee),
    start_date: nn(b.startDate), end_date: nn(b.endDate),
    target_system: nn(b.targetSystem), target_version: nn(b.targetVersion),
    test_goal: nn(b.testGoal), test_target: nn(b.testTarget),
    impact_scope: nn(b.impactScope), common_scope: nn(b.commonScope),
    priority_targets: nn(b.priorityTargets), risk_analysis: nn(b.riskAnalysis),
    test_approach: nn(b.testApproach), test_perspective: nn(b.testPerspective),
    entry_criteria: nn(b.entryCriteria), exit_criteria: nn(b.exitCriteria),
    server_environment_note: nn(b.serverEnvironmentNote), device_matrix: nn(b.deviceMatrix), test_data: nn(b.testData),
    schedule: nn(b.schedule), deliverables: nn(b.deliverables),
    project_id: nn(b.projectId),
  };
}

// 여러 플랜의 집계(스위트수/케이스수/완료런수)와 coreTestCases 를 배치 쿼리로 계산한다.
async function assemblePlans(planRows) {
  if (!planRows.length) return [];
  const planIds = planRows.map((p) => p.id);
  const suites = ok(await supabase.from("test_suites").select("id,test_plan_id").in("test_plan_id", planIds)) || [];
  const suiteIds = suites.map((s) => s.id);
  const suiteLinks = suiteIds.length
    ? (ok(await supabase.from("test_suite_test_cases").select("test_suite_id,test_case_id").in("test_suite_id", suiteIds)) || [])
    : [];
  const completedExecs = ok(await supabase.from("executions").select("test_plan_id").in("test_plan_id", planIds).eq("status", "COMPLETED")) || [];
  const coreLinks = ok(await supabase.from("test_plan_core_test_cases").select("test_plan_id,test_case_id").in("test_plan_id", planIds)) || [];
  const coreCaseIds = [...new Set(coreLinks.map((l) => l.test_case_id))];
  const coreTCs = coreCaseIds.length ? (ok(await supabase.from("test_cases").select(TC_SELECT).in("id", coreCaseIds)) || []) : [];
  const coreTCById = new Map(coreTCs.map((r) => [r.id, tcToResponse(r)]));

  const casesBySuite = new Map();
  for (const l of suiteLinks) {
    if (!casesBySuite.has(l.test_suite_id)) casesBySuite.set(l.test_suite_id, []);
    casesBySuite.get(l.test_suite_id).push(l.test_case_id);
  }
  return planRows.map((p) => {
    const planSuiteIds = suites.filter((s) => s.test_plan_id === p.id).map((s) => s.id);
    const caseSet = new Set();
    for (const sid of planSuiteIds) for (const tcId of (casesBySuite.get(sid) || [])) caseSet.add(tcId);
    return planToResponse(p, {
      suiteCount: planSuiteIds.length,
      testCaseCount: caseSet.size,
      completedRunCount: completedExecs.filter((e) => e.test_plan_id === p.id).length,
      coreTestCases: coreLinks.filter((l) => l.test_plan_id === p.id).map((l) => coreTCById.get(l.test_case_id)).filter(Boolean),
    });
  });
}
async function syncCoreTestCases(planId, ids) {
  if (ids === undefined) return;
  ok(await supabase.from("test_plan_core_test_cases").delete().eq("test_plan_id", planId));
  const list = (ids || []).filter((x) => x != null);
  if (list.length) {
    ok(await supabase.from("test_plan_core_test_cases").insert(list.map((tid) => ({ test_plan_id: planId, test_case_id: tid }))));
  }
}
async function getPlanOr404(id) {
  const p = ok(await supabase.from("test_plans").select("*").eq("id", id).maybeSingle());
  if (!p) throw new HttpError(404, `테스트 플랜 #${id} 을(를) 찾을 수 없습니다.`);
  return (await assemblePlans([p]))[0];
}

// ── 매핑: 스위트 ───────────────────────────────────────────────────
const SUITE_SELECT =
  "*,test_plan:test_plans(id,name)," +
  "test_suite_test_cases(position,test_cases(" + TC_SELECT + "))";
function suiteToResponse(s) {
  const testCases = (s.test_suite_test_cases || [])
    .slice()
    .sort((a, b) => (a.position ?? 0) - (b.position ?? 0))
    .map((x) => x.test_cases).filter(Boolean).map(tcToResponse);
  return {
    id: s.id,
    testPlanId: s.test_plan_id ?? null,
    testPlanName: s.test_plan ? s.test_plan.name : null,
    name: s.name, description: s.description,
    testCases,
    createdAt: s.created_at, updatedAt: s.updated_at,
  };
}
async function fetchSuiteOr404(id, planId) {
  let q = supabase.from("test_suites").select(SUITE_SELECT).eq("id", id);
  if (planId != null) q = q.eq("test_plan_id", planId);
  const s = ok(await q.maybeSingle());
  if (!s) throw new HttpError(404, `스위트 #${id} 을(를) 찾을 수 없습니다.`);
  return suiteToResponse(s);
}
async function syncSuiteCases(suiteId, ids) {
  if (ids === undefined) return;
  ok(await supabase.from("test_suite_test_cases").delete().eq("test_suite_id", suiteId));
  const list = (ids || []).filter((x) => x != null);
  if (list.length) {
    ok(await supabase.from("test_suite_test_cases").insert(
      list.map((tid, i) => ({ test_suite_id: suiteId, test_case_id: tid, position: i }))));
  }
}
async function insertSuite(body, planId) {
  const row = { name: body.name, description: nn(body.description), project_id: nn(body.projectId), test_plan_id: planId ?? null };
  const created = ok(await supabase.from("test_suites").insert(row).select("id").single());
  await syncSuiteCases(created.id, body.testCaseIds);
  return fetchSuiteOr404(created.id);
}
async function updateSuite(id, body, planId) {
  const row = { name: body.name, description: nn(body.description) };
  let q = supabase.from("test_suites").update(row).eq("id", id);
  if (planId != null) q = q.eq("test_plan_id", planId);
  const updated = ok(await q.select("id").maybeSingle());
  if (!updated) throw new HttpError(404, `스위트 #${id} 을(를) 찾을 수 없습니다.`);
  await syncSuiteCases(id, body.testCaseIds);
  return fetchSuiteOr404(id);
}

// ── 매핑: 실행(테스트런) ───────────────────────────────────────────
const RESULT_STATUSES = ["UNTESTED", "NOT_EXECUTED", "IN_PROGRESS", "PASSED", "FAILED", "BLOCKED", "RETEST", "NOT_IMPLEMENTED", "SKIPPED"];
function execCounts(statuses) {
  const c = (s) => statuses.filter((x) => x === s).length;
  const total = statuses.length;
  const untested = c("UNTESTED");
  const progressPct = total === 0 ? 0 : Math.round(((total - untested) * 100) / total);
  return {
    total, untested, passed: c("PASSED"), failed: c("FAILED"), blocked: c("BLOCKED"),
    retest: c("RETEST"), notImplemented: c("NOT_IMPLEMENTED"), progressPct,
  };
}
function execBase(e, counts) {
  return {
    id: e.id, name: e.name, version: e.version, description: e.description,
    testPlanId: e.test_plan_id, planName: e.plan_name,
    testSuiteId: e.test_suite_id, suiteName: e.suite_name,
    testConfigurationId: e.test_configuration_id, configurationName: e.configuration_name,
    environmentDetail: e.environment_detail,
    status: e.status, assignee: e.assignee,
    total: counts.total, untested: counts.untested, passed: counts.passed, failed: counts.failed,
    blocked: counts.blocked, retest: counts.retest, notImplemented: counts.notImplemented,
    progressPct: counts.progressPct,
    createdAt: e.created_at, updatedAt: e.updated_at, completedAt: e.completed_at,
  };
}
function itemToResponse(it, defectCount, historyRows) {
  return {
    id: it.id, testCaseId: it.test_case_id, caseTitle: it.case_title,
    versionNumber: it.version_number, versionLabel: it.version_label,
    sourceSuiteId: it.source_suite_id, sourceSuiteName: it.source_suite_name,
    sourceFolderId: it.source_folder_id ?? null, sourceFolderName: it.source_folder_name ?? null,
    sourceFolderCode: it.source_folder_code ?? null,
    status: it.status, comment: it.comment, failureReason: it.failure_reason,
    defectCount: defectCount || 0, executedAt: it.executed_at,
    history: (historyRows || []).map((h) => ({
      id: h.id, status: h.status, comment: h.comment, failureReason: h.failure_reason, recordedAt: h.recorded_at,
    })),
  };
}
async function getExecutionDetail(id) {
  const e = ok(await supabase.from("executions").select("*").eq("id", id).maybeSingle());
  if (!e) throw new HttpError(404, `테스트런 #${id} 을(를) 찾을 수 없습니다.`);
  const items = ok(await supabase.from("execution_items").select("*").eq("execution_id", id)
    .order("item_order", { ascending: true, nullsFirst: false }).order("id", { ascending: true })) || [];
  const itemIds = items.map((i) => i.id);
  const history = itemIds.length
    ? (ok(await supabase.from("execution_item_history").select("*").in("execution_item_id", itemIds).order("recorded_at", { ascending: true })) || [])
    : [];
  const historyByItem = new Map();
  for (const h of history) {
    if (!historyByItem.has(h.execution_item_id)) historyByItem.set(h.execution_item_id, []);
    historyByItem.get(h.execution_item_id).push(h);
  }
  const caseIds = [...new Set(items.map((i) => i.test_case_id).filter((x) => x != null))];
  const defectCounts = new Map();
  if (caseIds.length) {
    const links = ok(await supabase.from("test_case_defects").select("test_case_id").in("test_case_id", caseIds)) || [];
    for (const l of links) defectCounts.set(l.test_case_id, (defectCounts.get(l.test_case_id) || 0) + 1);
  }
  const base = execBase(e, execCounts(items.map((i) => i.status)));
  base.items = items.map((it) => itemToResponse(it, defectCounts.get(it.test_case_id), historyByItem.get(it.id)));
  return base;
}

function buildEnvDetail(c) {
  const parts = [];
  if (c.server_environment && c.server_environment.name) parts.push(c.server_environment.name);
  if (c.os) parts.push(c.os + (c.os_version ? " " + c.os_version : ""));
  if (c.browser) parts.push(c.browser + (c.browser_version ? " " + c.browser_version : ""));
  if (c.device) parts.push(c.device);
  if (c.runtime_version) parts.push(c.runtime_version);
  if (c.db_version) parts.push(c.db_version);
  return parts.length ? parts.join(" · ") : null;
}
async function applyConfig(execRow, testConfigurationId) {
  if (testConfigurationId == null) {
    execRow.test_configuration_id = null; execRow.configuration_name = null; execRow.environment_detail = null;
    return;
  }
  const c = ok(await supabase.from("test_configurations")
    .select("*,server_environment:server_environments(name)").eq("id", testConfigurationId).maybeSingle());
  if (!c) throw new HttpError(404, `TestConfiguration #${testConfigurationId} 을(를) 찾을 수 없습니다.`);
  execRow.test_configuration_id = c.id;
  execRow.configuration_name = c.name;
  execRow.environment_detail = buildEnvDetail(c);
}
async function latestVersions(caseIds) {
  const m = new Map();
  if (!caseIds.length) return m;
  const rows = ok(await supabase.from("test_case_versions").select("test_case_id,version_number,label")
    .in("test_case_id", caseIds).order("version_number", { ascending: false })) || [];
  for (const r of rows) if (!m.has(r.test_case_id)) m.set(r.test_case_id, r);
  return m;
}
function buildItemRow(execId, order, tc, ver, sourceSuite) {
  return {
    execution_id: execId, item_order: order,
    test_case_id: tc.id, case_title: tc.title,
    version_number: ver ? ver.version_number : null,
    version_label: ver ? ver.label : null,
    source_suite_id: sourceSuite ? sourceSuite.id : null,
    source_suite_name: sourceSuite ? sourceSuite.name : null,
    source_folder_id: tc.folder_id ?? null,
    source_folder_name: tc.folder ? tc.folder.name : null,
    source_folder_code: null,
    status: "UNTESTED", comment: null, failure_reason: null, executed_at: null,
  };
}
async function insertItems(rows) {
  if (rows.length) ok(await supabase.from("execution_items").insert(rows));
}

async function createFromSuites(suiteIds, body) {
  const suites = ok(await supabase.from("test_suites")
    .select("*,test_plan:test_plans(id,name),test_suite_test_cases(test_case_id,position,test_cases(id,title,folder_id,folder:test_folders(id,name)))")
    .in("id", suiteIds)) || [];
  const missing = suiteIds.filter((id) => !suites.some((s) => s.id === id));
  if (missing.length) throw new HttpError(404, "존재하지 않는 스위트: " + missing);

  let projectId = body.projectId ?? suites.map((s) => s.project_id).find((x) => x != null) ?? null;

  const mergedCases = new Map();       // caseId -> tc
  const sourceSuiteByCase = new Map(); // caseId -> {id,name}
  for (const s of suites) {
    const links = (s.test_suite_test_cases || []).slice().sort((a, b) => (a.position ?? 0) - (b.position ?? 0));
    for (const l of links) {
      const tc = l.test_cases; if (!tc) continue;
      if (!mergedCases.has(tc.id)) { mergedCases.set(tc.id, tc); sourceSuiteByCase.set(tc.id, { id: s.id, name: s.name }); }
    }
  }
  if (mergedCases.size === 0) throw new HttpError(400, "테스트케이스가 없는 스위트로는 테스트런을 만들 수 없습니다.");

  const single = suites.length === 1;
  const suiteNames = suites.map((s) => s.name).join(", ");
  const today = new Date().toISOString().slice(0, 10);
  const name = (body.name && body.name.trim()) ? body.name.trim() : `${single ? suites[0].name : suiteNames} — ${today}`;

  let planId = null, planName = null;
  if (body.testPlanId != null) {
    const plan = ok(await supabase.from("test_plans").select("id,name").eq("id", body.testPlanId).maybeSingle());
    if (!plan) throw new HttpError(404, "TestPlan not found. id=" + body.testPlanId);
    planId = plan.id; planName = plan.name;
  } else {
    const planIds = [...new Set(suites.map((s) => s.test_plan && s.test_plan.id).filter((x) => x != null))];
    if (planIds.length === 1 && suites.every((s) => s.test_plan)) { planId = suites[0].test_plan.id; planName = suites[0].test_plan.name; }
  }

  const execRow = {
    name: name.slice(0, 200), version: nn(body.version), description: nn(body.description),
    project_id: projectId, test_plan_id: planId, plan_name: planName,
    test_suite_id: single ? suites[0].id : null, suite_name: (single ? suites[0].name : suiteNames).slice(0, 200),
    status: "READY", assignee: nn(body.assignee),
  };
  await applyConfig(execRow, body.testConfigurationId);
  const saved = ok(await supabase.from("executions").insert(execRow).select("id").single());
  const cases = [...mergedCases.values()];
  const vers = await latestVersions(cases.map((c) => c.id));
  await insertItems(cases.map((tc, i) => buildItemRow(saved.id, i, tc, vers.get(tc.id), sourceSuiteByCase.get(tc.id))));
  return getExecutionDetail(saved.id);
}

async function createFromTestCases(body) {
  const ids = [...new Set((body.testCaseIds || []).filter((x) => x != null))];
  if (!ids.length) throw new HttpError(400, "스위트 또는 테스트케이스를 선택해야 합니다.");
  const cases = ok(await supabase.from("test_cases").select("id,title,folder_id,folder:test_folders(id,name)").in("id", ids)) || [];
  const missing = ids.filter((id) => !cases.some((c) => c.id === id));
  if (missing.length) throw new HttpError(400, "존재하지 않는 테스트케이스: " + missing);

  const today = new Date().toISOString().slice(0, 10);
  const name = (body.name && body.name.trim()) ? body.name.trim() : `직접 선택 테스트런 — ${today}`;
  let planId = null, planName = null;
  if (body.testPlanId != null) {
    const plan = ok(await supabase.from("test_plans").select("id,name").eq("id", body.testPlanId).maybeSingle());
    if (!plan) throw new HttpError(404, "TestPlan not found. id=" + body.testPlanId);
    planId = plan.id; planName = plan.name;
  }
  const execRow = {
    name: name.slice(0, 200), version: nn(body.version), description: nn(body.description),
    project_id: nn(body.projectId), test_plan_id: planId, plan_name: planName,
    test_suite_id: null, suite_name: null, status: "READY", assignee: nn(body.assignee),
  };
  await applyConfig(execRow, body.testConfigurationId);
  const saved = ok(await supabase.from("executions").insert(execRow).select("id").single());
  const byId = new Map(cases.map((c) => [c.id, c]));
  const vers = await latestVersions(ids);
  await insertItems(ids.map((id, i) => buildItemRow(saved.id, i, byId.get(id), vers.get(id), null)));
  return getExecutionDetail(saved.id);
}

async function assertOpen(id) {
  const e = ok(await supabase.from("executions").select("status").eq("id", id).maybeSingle());
  if (!e) throw new HttpError(404, `테스트런 #${id} 을(를) 찾을 수 없습니다.`);
  if (e.status === "COMPLETED") throw new HttpError(400, "완료된 테스트런은 다시 열기 전까지 수정할 수 없습니다.");
  return e;
}
async function recordItems(execId, itemIds, status, comment, failureReason) {
  const upd = { status, comment: nn(comment), failure_reason: nn(failureReason), executed_at: nowIso() };
  ok(await supabase.from("execution_items").update(upd).in("id", itemIds).eq("execution_id", execId));
  ok(await supabase.from("execution_item_history").insert(
    itemIds.map((iid) => ({ execution_item_id: iid, status, comment: nn(comment), failure_reason: nn(failureReason) }))));
  if (status !== "UNTESTED") {
    ok(await supabase.from("executions").update({ status: "IN_PROGRESS" }).eq("id", execId).eq("status", "READY"));
  }
}

// ── 매핑: 레거시 케이스별 런(TestRun) ─────────────────────────────
function legacyRunToResponse(r) {
  return {
    id: r.id, testCaseId: r.test_case_id, status: r.status, actualResult: r.actual_result,
    notes: r.notes, assignee: r.assignee, failureReason: r.failure_reason, executedAt: r.executed_at,
  };
}
// ── 테스트케이스 실행 이력(런을 통한) ─────────────────────────────
async function testCaseExecutionHistory(testCaseId) {
  const items = ok(await supabase.from("execution_items")
    .select("*,execution:executions(id,name,status)").eq("test_case_id", testCaseId).order("id", { ascending: false })) || [];
  return items.map((it) => ({
    executionItemId: it.id,
    executionId: it.execution ? it.execution.id : null,
    executionName: it.execution ? it.execution.name : null,
    executionStatus: it.execution ? it.execution.status : null,
    versionNumber: it.version_number, versionLabel: it.version_label,
    status: it.status, comment: it.comment, failureReason: it.failure_reason, executedAt: it.executed_at,
  }));
}

// ═══════════════════════════════════════════════════════════════════
export function registerRunRoutes() {
  // ── 테스트 플랜 ──
  on("GET", "/api/test-plans", async ({ query }) => {
    let q = supabase.from("test_plans").select("*");
    if (query.projectId != null) q = q.eq("project_id", query.projectId);
    const rows = ok(await q.order("id", { ascending: true })) || [];
    return assemblePlans(rows);
  });
  on("GET", "/api/test-plans/:id", ({ params }) => getPlanOr404(params.id));
  on("POST", "/api/test-plans", async ({ body }) => {
    const created = ok(await supabase.from("test_plans").insert(planRequestToRow(body)).select("id").single());
    await syncCoreTestCases(created.id, body.coreTestCaseIds);
    return getPlanOr404(created.id);
  });
  on("PUT", "/api/test-plans/:id", async ({ params, body }) => {
    const updated = ok(await supabase.from("test_plans").update(planRequestToRow(body)).eq("id", params.id).select("id").maybeSingle());
    if (!updated) throw new HttpError(404, `테스트 플랜 #${params.id} 을(를) 찾을 수 없습니다.`);
    await syncCoreTestCases(params.id, body.coreTestCaseIds);
    return getPlanOr404(params.id);
  });
  on("DELETE", "/api/test-plans/:id", async ({ params }) => {
    ok(await supabase.from("test_plans").delete().eq("id", params.id));
    return null;
  });

  // ── 플랜 소속 스위트 ──
  on("GET", "/api/test-plans/:planId/suites", async ({ params }) => {
    const rows = ok(await supabase.from("test_suites").select(SUITE_SELECT).eq("test_plan_id", params.planId).order("id", { ascending: true })) || [];
    return rows.map(suiteToResponse);
  });
  on("GET", "/api/test-plans/:planId/suites/:suiteId", ({ params }) => fetchSuiteOr404(params.suiteId, params.planId));
  on("POST", "/api/test-plans/:planId/suites", ({ params, body }) => insertSuite(body, params.planId));
  on("PUT", "/api/test-plans/:planId/suites/:suiteId", ({ params, body }) => updateSuite(params.suiteId, body, params.planId));
  on("DELETE", "/api/test-plans/:planId/suites/:suiteId", async ({ params }) => {
    ok(await supabase.from("test_suites").delete().eq("id", params.suiteId).eq("test_plan_id", params.planId));
    return null;
  });

  // ── 독립 스위트 ──
  on("GET", "/api/suites", async ({ query }) => {
    let q = supabase.from("test_suites").select(SUITE_SELECT);
    if (query.projectId != null) q = q.eq("project_id", query.projectId);
    return (ok(await q.order("id", { ascending: true })) || []).map(suiteToResponse);
  });
  on("GET", "/api/suites/standalone", async ({ query }) => {
    let q = supabase.from("test_suites").select(SUITE_SELECT).is("test_plan_id", null);
    if (query.projectId != null) q = q.eq("project_id", query.projectId);
    return (ok(await q.order("id", { ascending: true })) || []).map(suiteToResponse);
  });
  on("GET", "/api/suites/:id", ({ params }) => fetchSuiteOr404(params.id));
  on("POST", "/api/suites", ({ body }) => insertSuite(body, null));
  on("PUT", "/api/suites/:id", ({ params, body }) => updateSuite(params.id, body, null));
  on("DELETE", "/api/suites/:id", async ({ params }) => {
    ok(await supabase.from("test_suites").delete().eq("id", params.id));
    return null;
  });

  // ── 테스트런(실행) ──
  on("GET", "/api/test-runs", async ({ query }) => {
    let q = supabase.from("executions").select("*");
    if (query.projectId != null) q = q.eq("project_id", query.projectId);
    if (query.assignee) q = q.eq("assignee", query.assignee);
    const execs = ok(await q.order("created_at", { ascending: false })) || [];
    if (!execs.length) return [];
    const ids = execs.map((e) => e.id);
    const items = ok(await supabase.from("execution_items").select("execution_id,status").in("execution_id", ids)) || [];
    const byExec = new Map();
    for (const it of items) {
      if (!byExec.has(it.execution_id)) byExec.set(it.execution_id, []);
      byExec.get(it.execution_id).push(it.status);
    }
    return execs.map((e) => { const b = execBase(e, execCounts(byExec.get(e.id) || [])); b.items = null; return b; });
  });
  on("GET", "/api/test-runs/items/by-test-case/:testCaseId", ({ params }) => testCaseExecutionHistory(params.testCaseId));
  on("GET", "/api/test-runs/:id", ({ params }) => getExecutionDetail(params.id));
  on("POST", "/api/test-runs", ({ body }) => {
    const suiteIds = [...new Set([body.suiteId, ...(body.suiteIds || [])].filter((x) => x != null))];
    return suiteIds.length ? createFromSuites(suiteIds, body) : createFromTestCases(body);
  });
  on("PUT", "/api/test-runs/:id", async ({ params, body }) => {
    const upd = { name: (body.name || "").trim(), description: nn(body.description), status: body.status, assignee: nn(body.assignee) };
    upd.completed_at = body.status === "COMPLETED" ? nowIso() : null;
    const r = ok(await supabase.from("executions").update(upd).eq("id", params.id).select("id").maybeSingle());
    if (!r) throw new HttpError(404, `테스트런 #${params.id} 을(를) 찾을 수 없습니다.`);
    return getExecutionDetail(params.id);
  });
  on("POST", "/api/test-runs/:id/clone", async ({ params }) => {
    const src = ok(await supabase.from("executions").select("*").eq("id", params.id).maybeSingle());
    if (!src) throw new HttpError(404, `테스트런 #${params.id} 을(를) 찾을 수 없습니다.`);
    const srcItems = ok(await supabase.from("execution_items").select("*").eq("execution_id", params.id).order("item_order", { ascending: true })) || [];
    const cloneRow = {
      name: (src.name + " (복제)").slice(0, 200), version: src.version, description: src.description,
      project_id: src.project_id, test_plan_id: src.test_plan_id, plan_name: src.plan_name,
      test_suite_id: src.test_suite_id, suite_name: src.suite_name,
      test_configuration_id: src.test_configuration_id, configuration_name: src.configuration_name,
      environment_detail: src.environment_detail, status: "READY", assignee: src.assignee,
    };
    const saved = ok(await supabase.from("executions").insert(cloneRow).select("id").single());
    const caseIds = [...new Set(srcItems.map((i) => i.test_case_id))];
    const cases = caseIds.length ? (ok(await supabase.from("test_cases").select("id,title,folder_id,folder:test_folders(id,name)").in("id", caseIds)) || []) : [];
    const byId = new Map(cases.map((c) => [c.id, c]));
    const vers = await latestVersions(caseIds);
    const rows = [];
    srcItems.forEach((it, i) => {
      const tc = byId.get(it.test_case_id);
      if (tc) rows.push(buildItemRow(saved.id, i, tc, vers.get(tc.id), it.source_suite_id ? { id: it.source_suite_id, name: it.source_suite_name } : null));
    });
    await insertItems(rows);
    return getExecutionDetail(saved.id);
  });
  on("PATCH", "/api/test-runs/:id/plan", async ({ params, body }) => {
    let planId = null, planName = null;
    if (body.testPlanId != null) {
      const plan = ok(await supabase.from("test_plans").select("id,name").eq("id", body.testPlanId).maybeSingle());
      if (!plan) throw new HttpError(404, "TestPlan not found. id=" + body.testPlanId);
      planId = plan.id; planName = plan.name;
    }
    ok(await supabase.from("executions").update({ test_plan_id: planId, plan_name: planName }).eq("id", params.id));
    return getExecutionDetail(params.id);
  });
  on("PATCH", "/api/test-runs/:id/environment", async ({ params, body }) => {
    const patch = {};
    await applyConfig(patch, body.testConfigurationId);
    ok(await supabase.from("executions").update(patch).eq("id", params.id));
    return getExecutionDetail(params.id);
  });
  on("PATCH", "/api/test-runs/:id/items/:itemId", async ({ params, body }) => {
    await assertOpen(params.id);
    const item = ok(await supabase.from("execution_items").select("id").eq("id", params.itemId).eq("execution_id", params.id).maybeSingle());
    if (!item) throw new HttpError(404, `실행 항목 #${params.itemId} 을(를) 찾을 수 없습니다.`);
    await recordItems(params.id, [Number(params.itemId)], body.status, body.comment, body.failureReason);
    return getExecutionDetail(params.id);
  });
  on("PATCH", "/api/test-runs/:id/bulk-results", async ({ params, body }) => {
    await assertOpen(params.id);
    const ids = (body.itemIds || []).map(Number);
    if (!ids.length) throw new HttpError(400, "대상 항목이 없습니다.");
    await recordItems(params.id, ids, body.status, body.comment, body.failureReason);
    return getExecutionDetail(params.id);
  });
  on("POST", "/api/test-runs/:id/suites", async ({ params, body }) => {
    await assertOpen(params.id);
    const exec = ok(await supabase.from("executions").select("id,project_id").eq("id", params.id).single());
    const suiteIds = [...new Set((body.suiteIds || []).filter((x) => x != null))];
    if (!suiteIds.length) throw new HttpError(400, "추가할 스위트를 선택해야 합니다.");
    const suites = ok(await supabase.from("test_suites")
      .select("id,name,test_suite_test_cases(test_case_id,position,test_cases(id,title,folder_id,folder:test_folders(id,name)))")
      .in("id", suiteIds)) || [];
    const existing = ok(await supabase.from("execution_items").select("test_case_id,item_order").eq("execution_id", params.id)) || [];
    const existingCaseIds = new Set(existing.map((i) => i.test_case_id));
    let order = existing.reduce((m, i) => Math.max(m, i.item_order ?? 0), -1) + 1;
    const rows = [];
    const seen = new Set();
    for (const s of suites) {
      const links = (s.test_suite_test_cases || []).slice().sort((a, b) => (a.position ?? 0) - (b.position ?? 0));
      for (const l of links) {
        const tc = l.test_cases; if (!tc || existingCaseIds.has(tc.id) || seen.has(tc.id)) continue;
        seen.add(tc.id);
        const vers = await latestVersions([tc.id]);
        rows.push(buildItemRow(params.id, order++, tc, vers.get(tc.id), { id: s.id, name: s.name }));
      }
    }
    if (!rows.length) throw new HttpError(400, "추가할 새 테스트케이스가 없습니다.");
    await insertItems(rows);
    return getExecutionDetail(params.id);
  });
  on("DELETE", "/api/test-runs/:id/suites/:suiteId", async ({ params }) => {
    await assertOpen(params.id);
    const items = ok(await supabase.from("execution_items").select("id,source_suite_id").eq("execution_id", params.id)) || [];
    const toRemove = items.filter((i) => String(i.source_suite_id) === String(params.suiteId));
    if (!toRemove.length) throw new HttpError(400, "이 테스트런에는 해당 스위트에서 온 항목이 없습니다.");
    if (toRemove.length === items.length) throw new HttpError(400, "마지막 스위트는 제거할 수 없습니다. 테스트런 자체를 삭제해 주세요.");
    ok(await supabase.from("execution_items").delete().in("id", toRemove.map((i) => i.id)));
    return getExecutionDetail(params.id);
  });
  on("DELETE", "/api/test-runs/:id", async ({ params }) => {
    ok(await supabase.from("executions").delete().eq("id", params.id));
    return null;
  });

  // ── 레거시 케이스별 런(TestRun) ──
  on("GET", "/api/testcases/:tcId/runs", async ({ params }) => {
    const rows = ok(await supabase.from("test_runs").select("*").eq("test_case_id", params.tcId).order("executed_at", { ascending: false })) || [];
    return rows.map(legacyRunToResponse);
  });
  on("POST", "/api/testcases/:tcId/runs", async ({ params, body }) => {
    const row = {
      test_case_id: Number(params.tcId), status: body.status, actual_result: body.actualResult ?? "",
      notes: nn(body.notes), assignee: nn(body.assignee), failure_reason: nn(body.failureReason),
    };
    return legacyRunToResponse(ok(await supabase.from("test_runs").insert(row).select("*").single()));
  });
  on("PUT", "/api/testcases/:tcId/runs/:runId", async ({ params, body }) => {
    const row = {
      status: body.status, actual_result: body.actualResult ?? "",
      notes: nn(body.notes), assignee: nn(body.assignee), failure_reason: nn(body.failureReason),
    };
    const r = ok(await supabase.from("test_runs").update(row).eq("id", params.runId).eq("test_case_id", params.tcId).select("*").maybeSingle());
    if (!r) throw new HttpError(404, `런 #${params.runId} 을(를) 찾을 수 없습니다.`);
    return legacyRunToResponse(r);
  });
  on("DELETE", "/api/testcases/:tcId/runs/:runId", async ({ params }) => {
    ok(await supabase.from("test_runs").delete().eq("id", params.runId).eq("test_case_id", params.tcId));
    return null;
  });
}
