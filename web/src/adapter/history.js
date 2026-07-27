// 테스트케이스 버전 스냅샷 + 감사(변경 이력) 로그.
// Spring TestCaseService 의 createVersionSnapshot / auditLogService.record(IfChanged) 이식.
// 모든 함수는 best-effort(실패해도 예외를 던지지 않음) — 부가 기록이 실패해도 본 CRUD는 정상 동작해야 한다.
//
// 스키마 참고: test_case_versions 에는 서버환경/컨피그/영역태그 컬럼이 없어(0001 축약본),
// 버전 스냅샷/복원은 핵심 필드(유형·우선순위·상태·제목·본문·환경 enum·폴더)만 다룬다.

import { supabase } from "../supabaseClient.js";

const TC = "TEST_CASE";

function fmt(v) {
  if (v == null || v === "") return "(없음)";
  return String(v);
}

// 감사 로그 한 줄 (best-effort)
export async function logAudit(entityType, entityId, action, fieldName, oldVal, newVal, summary) {
  try {
    await supabase.from("audit_logs").insert({
      entity_type: entityType,
      entity_id: Number(entityId),
      action,
      field_name: fieldName,
      old_value: oldVal == null ? null : String(oldVal),
      new_value: newVal == null ? null : String(newVal),
      summary: summary || `${fieldName} 변경: ${fmt(oldVal)} → ${fmt(newVal)}`,
      actor: null,
    });
  } catch (_e) { /* best-effort */ }
}

// 스냅샷/복원용 TC 데이터(관련 이름 포함) 조회
const SNAP_SELECT =
  "*,folder:test_folders(id,name)," +
  "server_environment:server_environments(id,name)," +
  "test_configuration:test_configurations(id,name)," +
  "test_case_area_tags(area_tags(id,name))";

async function fetchSnap(tcId) {
  const { data } = await supabase.from("test_cases").select(SNAP_SELECT).eq("id", tcId).maybeSingle();
  return data || null;
}

// 감사 비교용 요약 스냅샷 (필드→값)
export function auditSnapshot(row) {
  if (!row) return null;
  const tags = (row.test_case_area_tags || []).map((x) => x.area_tags).filter(Boolean).map((t) => t.name);
  return {
    type: row.type, priority: row.priority, status: row.status, title: row.title,
    description: row.description, precondition: row.precondition, steps: row.steps,
    expectedResult: row.expected, notes: row.notes, os: row.os, browser: row.browser,
    device: row.device, assignee: row.assignee, version: row.version,
    folder: row.folder ? row.folder.name : "미분류",
    serverEnvironment: row.server_environment ? row.server_environment.name : null,
    testConfiguration: row.test_configuration ? row.test_configuration.name : null,
    areaTags: tags.join(", "),
  };
}
export async function auditSnapshotOf(tcId) {
  return auditSnapshot(await fetchSnap(tcId));
}

// 필드별 변경을 감사 로그로 기록 (Spring recordTestCaseUpdates 대응)
const FIELD_LABELS = {
  type: "유형", priority: "우선순위", status: "상태", title: "제목", description: "설명",
  precondition: "전제조건", steps: "스텝", expectedResult: "예상결과", notes: "메모",
  os: "OS", browser: "브라우저", device: "디바이스", assignee: "담당자", version: "버전",
  folder: "폴더", serverEnvironment: "서버환경", testConfiguration: "컨피그", areaTags: "영역태그",
};
const FIELD_ACTION = { status: "STATUS_CHANGED", folder: "MOVED" };
export async function recordTcUpdates(tcId, before, after) {
  if (!before || !after) return;
  for (const field of Object.keys(FIELD_LABELS)) {
    const o = before[field], n = after[field];
    if (String(o ?? "") === String(n ?? "")) continue;
    const action = FIELD_ACTION[field] || "UPDATED";
    await logAudit(TC, tcId, action, field, o, n, `${FIELD_LABELS[field]} 변경: ${fmt(o)} → ${fmt(n)}`);
  }
}

// 버전 스냅샷 생성 (best-effort) — 핵심 필드만. audit VERSION_CREATED 도 남긴다.
export async function snapshotTestCase(tcId, changeSummary) {
  try {
    const row = await fetchSnap(tcId);
    if (!row) return;
    const { data: last } = await supabase.from("test_case_versions")
      .select("version_number").eq("test_case_id", tcId)
      .order("version_number", { ascending: false }).limit(1).maybeSingle();
    const nextVersion = last ? last.version_number + 1 : 1;
    const label = (row.version && String(row.version).trim()) ? row.version : `v${nextVersion}`;
    await supabase.from("test_case_versions").insert({
      test_case_id: tcId,
      version_number: nextVersion,
      label,
      change_summary: changeSummary,
      type: row.type, priority: row.priority, status: row.status, title: row.title,
      version: row.version, description: row.description, precondition: row.precondition,
      steps: row.steps, expected_result: row.expected, notes: row.notes,
      os: row.os, browser: row.browser, device: row.device, assignee: row.assignee,
      folder_id: row.folder_id, folder_name: row.folder ? row.folder.name : null,
    });
    await logAudit(TC, tcId, "VERSION_CREATED", "version", null, label, `버전 '${label}' 생성 (${changeSummary})`);
  } catch (_e) { /* best-effort */ }
}

// 버전 복원 — 스냅샷의 핵심 필드를 test_cases에 되돌린다(환경/컨피그/태그는 스키마에 없어 유지).
// 반환: 복원 성공 여부.
export async function restoreTestCaseVersion(tcId, versionId) {
  const { data: v, error } = await supabase.from("test_case_versions")
    .select("*").eq("id", versionId).eq("test_case_id", tcId).maybeSingle();
  if (error || !v) return false;
  const before = await auditSnapshotOf(tcId);
  const { error: upErr } = await supabase.from("test_cases").update({
    type: v.type, priority: v.priority, status: v.status, title: v.title,
    version: v.version, description: v.description, precondition: v.precondition,
    steps: v.steps, expected: v.expected_result ?? "", notes: v.notes,
    os: v.os, browser: v.browser, device: v.device, assignee: v.assignee,
    folder_id: v.folder_id,
  }).eq("id", tcId);
  if (upErr) return false;
  await recordTcUpdates(tcId, before, await auditSnapshotOf(tcId));
  await logAudit(TC, tcId, "VERSION_RESTORED", "version", null, v.label, `버전 '${v.label}' 복원`);
  await snapshotTestCase(tcId, `버전 복원: ${v.label}`);
  return true;
}
