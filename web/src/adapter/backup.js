// 데이터 백업/복구 — 전체 테이블을 JSON 스냅샷으로 내보내고, 그 파일로 복구한다.
// (Spring BackupService의 zip 백업을 웹용 JSON 백업으로 대체 — 포맷은 Spring과 호환되지 않음)
//
// 복구 주의: 스키마 PK가 `generated always as identity`라 원본 id를 그대로 삽입할 수 없다.
// 따라서 부모→자식 순으로 삽입하며 old id → new id 매핑을 만들고 FK를 재작성한다.
// ⚠️ 복구는 전체 데이터를 덮어쓰는 파괴적 작업이며, 이 환경에서 직접 검증되지 않았으니
//    실제 사용 전 throwaway 데이터로 먼저 확인할 것.

import { supabase } from "../supabaseClient.js";

// 부모 → 자식 순서 (복구 삽입 순서, 삭제는 역순)
const TABLES = [
  { name: "projects", id: true },
  { name: "users", id: true },
  { name: "server_environments", id: true },
  { name: "defects", id: true },
  { name: "jira_settings", id: true },
  { name: "area_tags", id: true, fk: { project_id: "projects" } },
  { name: "test_configurations", id: true, fk: { server_environment_id: "server_environments" } },
  { name: "test_folders", id: true, selfParent: true, fk: { project_id: "projects" } },
  { name: "test_plans", id: true, fk: { project_id: "projects" } },
  { name: "test_cases", id: true, fk: { folder_id: "test_folders", server_environment_id: "server_environments", test_configuration_id: "test_configurations", project_id: "projects" } },
  { name: "test_suites", id: true, fk: { test_plan_id: "test_plans", project_id: "projects" } },
  { name: "executions", id: true, fk: { project_id: "projects", test_plan_id: "test_plans", test_suite_id: "test_suites", test_configuration_id: "test_configurations" } },
  { name: "test_runs", id: true, fk: { test_case_id: "test_cases" } },
  { name: "execution_items", id: true, fk: { execution_id: "executions", test_case_id: "test_cases", source_suite_id: "test_suites", source_folder_id: "test_folders" } },
  { name: "execution_item_history", id: true, fk: { execution_item_id: "execution_items" } },
  { name: "test_case_versions", id: true, fk: { test_case_id: "test_cases", folder_id: "test_folders" } },
  { name: "attachments", id: true, poly: { typeCol: "entity_type", idCol: "entity_id", map: { TEST_CASE: "test_cases", DEFECT: "defects", TEST_RUN: "test_runs", EXECUTION_ITEM: "execution_items" } } },
  { name: "audit_logs", id: true, poly: { typeCol: "entity_type", idCol: "entity_id", map: { TEST_CASE: "test_cases", TEST_RUN: "test_runs", DEFECT: "defects" } } },
  { name: "test_case_area_tags", id: false, pk: "test_case_id", fk: { test_case_id: "test_cases", area_tag_id: "area_tags" } },
  { name: "test_case_defects", id: false, pk: "test_case_id", fk: { test_case_id: "test_cases", defect_id: "defects" } },
  { name: "test_case_jira_requirements", id: false, pk: "test_case_id", fk: { test_case_id: "test_cases" } },
  { name: "test_plan_core_test_cases", id: false, pk: "test_plan_id", fk: { test_plan_id: "test_plans", test_case_id: "test_cases" } },
  { name: "test_suite_test_cases", id: false, pk: "test_suite_id", fk: { test_suite_id: "test_suites", test_case_id: "test_cases" } },
];

// ── 내보내기 ───────────────────────────────────────────────────────
export async function downloadBackupImpl(options) {
  try {
    const tables = {};
    for (const t of TABLES) {
      const { data, error } = await supabase.from(t.name).select("*");
      if (error) return { ok: false, status: 500, data: { message: `백업 실패(${t.name}): ${error.message}` } };
      tables[t.name] = data || [];
    }
    const backup = { format: "tms-web-backup", version: 1, exportedAt: new Date().toISOString(), tables };
    const blob = new Blob([JSON.stringify(backup)], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    const suggested = options && options.suggestedName ? options.suggestedName.replace(/\.zip$/i, ".json") : null;
    a.href = url;
    a.download = suggested || `tms-backup-${new Date().toISOString().slice(0, 10)}.json`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 5000);
    return { ok: true, status: 200, savedPath: a.download, data: null };
  } catch (e) {
    return { ok: false, status: 500, data: { message: e.message } };
  }
}

// ── 복구 ───────────────────────────────────────────────────────────
function pickJsonFile() {
  return new Promise((resolve) => {
    const input = document.createElement("input");
    input.type = "file";
    input.accept = ".json,application/json";
    input.style.display = "none";
    let settled = false;
    const finish = (v) => { if (settled) return; settled = true; window.removeEventListener("focus", onFocus); input.remove(); resolve(v); };
    const onFocus = () => setTimeout(() => { if (!settled && (!input.files || input.files.length === 0)) finish(null); }, 400);
    input.addEventListener("change", () => finish(input.files && input.files[0] ? input.files[0] : null));
    window.addEventListener("focus", onFocus);
    document.body.appendChild(input);
    input.click();
  });
}

function remapRow(row, t, idMaps, stripId) {
  const out = { ...row };
  if (stripId) delete out.id;
  if (t.fk) {
    for (const [col, target] of Object.entries(t.fk)) {
      if (out[col] != null) {
        const m = idMaps[target];
        out[col] = m && m.has(out[col]) ? m.get(out[col]) : null;
      }
    }
  }
  if (t.poly) {
    const target = t.poly.map[out[t.poly.typeCol]];
    if (target && out[t.poly.idCol] != null) {
      const m = idMaps[target];
      if (m && m.has(out[t.poly.idCol])) out[t.poly.idCol] = m.get(out[t.poly.idCol]);
    }
  }
  return out;
}

async function restoreFolders(rows, idMaps) {
  const map = idMaps["test_folders"];
  const projMap = idMaps["projects"];
  const remaining = [...rows];
  while (remaining.length) {
    // 부모가 이미 삽입됐거나 루트인 것들 먼저 (데드락이면 남은 전체를 강제 삽입)
    let batch = remaining.filter((r) => r.parent_id == null || map.has(r.parent_id));
    if (!batch.length) batch = remaining.slice();
    for (const r of batch) {
      const payload = { ...r };
      delete payload.id;
      payload.parent_id = r.parent_id != null && map.has(r.parent_id) ? map.get(r.parent_id) : null;
      payload.project_id = r.project_id != null && projMap.has(r.project_id) ? projMap.get(r.project_id) : null;
      const { data, error } = await supabase.from("test_folders").insert(payload).select("id").single();
      if (error) throw new Error(`복구 실패(test_folders): ${error.message}`);
      map.set(r.id, data.id);
      remaining.splice(remaining.indexOf(r), 1);
    }
  }
}

async function restore(T) {
  // 1) 자식 → 부모 역순으로 전부 삭제
  for (let i = TABLES.length - 1; i >= 0; i--) {
    const t = TABLES[i];
    const filterCol = t.id ? "id" : t.pk;
    const { error } = await supabase.from(t.name).delete().not(filterCol, "is", null);
    if (error) throw new Error(`기존 데이터 삭제 실패(${t.name}): ${error.message}`);
  }

  // 2) 부모 → 자식 순으로 삽입 (id 재매핑)
  const idMaps = {};
  let rows = 0, files = 0;
  for (const t of TABLES) idMaps[t.name] = new Map();

  for (const t of TABLES) {
    const src = T[t.name] || [];
    if (!src.length) continue;

    if (t.selfParent) { await restoreFolders(src, idMaps); rows += src.length; continue; }

    if (t.id) {
      const payload = src.map((r) => remapRow(r, t, idMaps, true));
      const { data, error } = await supabase.from(t.name).insert(payload).select("id");
      if (error) throw new Error(`복구 실패(${t.name}): ${error.message}`);
      src.forEach((r, i) => { if (data[i]) idMaps[t.name].set(r.id, data[i].id); });
      rows += src.length;
      if (t.name === "attachments") files = src.length;
    } else {
      const payload = src.map((r) => remapRow(r, t, idMaps, false));
      const { error } = await supabase.from(t.name).insert(payload);
      if (error) throw new Error(`복구 실패(${t.name}): ${error.message}`);
      rows += src.length;
    }
  }
  return { tables: TABLES.length, rows, files };
}

export async function uploadBackupImpl() {
  try {
    const file = await pickJsonFile();
    if (!file) return { ok: false, canceled: true, status: 0, data: null };
    let backup;
    try { backup = JSON.parse(await file.text()); }
    catch { return { ok: false, status: 400, data: { message: "백업 파일을 읽을 수 없습니다(JSON 형식 아님)." } }; }
    if (!backup || !backup.tables) return { ok: false, status: 400, data: { message: "올바른 TMS 웹 백업 파일이 아닙니다." } };
    const result = await restore(backup.tables);
    return { ok: true, status: 200, data: result };
  } catch (e) {
    return { ok: false, status: 500, data: { message: e.message } };
  }
}
