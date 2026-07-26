// 엑셀/CSV 내보내기 — 브라우저에서 SheetJS(xlsx)로 파일을 생성해 다운로드한다.
// (Spring ExcelExportService 이식). renderer 는 downloadAttachment 셰임으로 /api/export/... 를 호출하며,
// index.js 가 export URL을 이 모듈로 라우팅한다.
//
// 지원 타입: test-cases, test-cases-filtered, test-runs, defects, test-plans, combined
// 포맷: xlsx(단일/다중 시트), csv(단일 표만; 다중 선택 CSV는 xlsx로 대체 — 브라우저 zip 회피)

import { supabase } from "../supabaseClient.js";

const nz = (v) => (v == null ? "" : String(v));
const str = (v) => (v == null ? "" : String(v));
function dt(v) {
  if (!v) return "";
  try { return new Date(v).toISOString().slice(0, 16).replace("T", " "); } catch { return String(v); }
}

async function loadXLSX() {
  const mod = await import("xlsx");
  const X = (mod && mod.utils) ? mod : (mod.default || mod);
  if (!X || !X.utils || typeof X.utils.book_new !== "function") {
    throw new Error("엑셀 라이브러리를 로드하지 못했습니다. 'npm install' 확인.");
  }
  return X;
}

// ── 표(Tabular) 빌더 ───────────────────────────────────────────────
async function tabTestCases(projectId, ids) {
  const SEL = "id,title,priority,status,type,os,browser,device,assignee,version,description,precondition,steps,expected,notes,folder:test_folders(name),test_case_area_tags(area_tags(name))";
  let cases;
  const filtered = ids && ids.length > 0;
  if (filtered) {
    const { data } = await supabase.from("test_cases").select(SEL).in("id", ids);
    const byId = new Map((data || []).map((r) => [r.id, r]));
    cases = ids.map((id) => byId.get(id)).filter(Boolean); // 화면 순서 보존
  } else {
    let q = supabase.from("test_cases").select(SEL);
    if (projectId != null) q = q.eq("project_id", projectId);
    const { data } = await q.order("id", { ascending: true });
    cases = data || [];
  }
  const headers = ["ID", "제목", "폴더", "우선순위", "상태", "유형", "OS", "브라우저", "디바이스", "담당자", "버전", "영역태그", "설명", "전제조건", "스텝", "예상결과", "메모"];
  const rows = cases.map((tc) => [
    str(tc.id), nz(tc.title), tc.folder ? nz(tc.folder.name) : "",
    nz(tc.priority), nz(tc.status), nz(tc.type), nz(tc.os), nz(tc.browser), nz(tc.device),
    nz(tc.assignee), nz(tc.version),
    (tc.test_case_area_tags || []).map((x) => x.area_tags && x.area_tags.name).filter(Boolean).join(", "),
    nz(tc.description), nz(tc.precondition), nz(tc.steps), nz(tc.expected), nz(tc.notes),
  ]);
  return { sheetName: filtered ? "테스트케이스(필터)" : "테스트케이스", fileBase: filtered ? "test-cases-filtered" : "test-cases", headers, rows };
}

async function tabTestRuns(projectId) {
  let q = supabase.from("executions").select("id,name,status,plan_name,suite_name,configuration_name,assignee");
  if (projectId != null) q = q.eq("project_id", projectId);
  const { data: execs } = await q.order("created_at", { ascending: false });
  const list = execs || [];
  const ids = list.map((e) => e.id);
  let items = [];
  if (ids.length) {
    const { data } = await supabase.from("execution_items")
      .select("execution_id,case_title,status,failure_reason,comment,version_number,version_label,executed_at,item_order")
      .in("execution_id", ids).order("item_order", { ascending: true }).order("id", { ascending: true });
    items = data || [];
  }
  const byExec = new Map();
  for (const it of items) { if (!byExec.has(it.execution_id)) byExec.set(it.execution_id, []); byExec.get(it.execution_id).push(it); }
  const headers = ["테스트런", "런 상태", "플랜", "스위트", "실행환경", "담당자", "테스트케이스", "결과", "사유/결함", "비고", "버전", "실행일시"];
  const rows = [];
  for (const e of list) {
    for (const it of (byExec.get(e.id) || [])) {
      const ver = it.version_number != null ? `v${it.version_number}${it.version_label ? " " + it.version_label : ""}` : "";
      rows.push([nz(e.name), nz(e.status), nz(e.plan_name), nz(e.suite_name), nz(e.configuration_name), nz(e.assignee),
        nz(it.case_title), nz(it.status), nz(it.failure_reason), nz(it.comment), ver, dt(it.executed_at)]);
    }
  }
  return { sheetName: "테스트런 결과", fileBase: "test-run-results", headers, rows };
}

async function tabDefects() {
  const { data } = await supabase.from("defects").select("*").order("created_at", { ascending: false });
  const headers = ["ID", "제목", "심각도", "상태", "Jira 키", "외부 URL", "설명", "생성일", "수정일"];
  const rows = (data || []).map((d) => [
    str(d.id), nz(d.title), nz(d.severity), nz(d.status), nz(d.jira_key), nz(d.external_url), nz(d.description),
    dt(d.created_at), dt(d.updated_at),
  ]);
  return { sheetName: "결함 목록", fileBase: "defects", headers, rows };
}

async function tabTestPlans(projectId) {
  let q = supabase.from("test_plans").select("id,name,status,assignee,start_date,end_date");
  if (projectId != null) q = q.eq("project_id", projectId);
  const { data: plans } = await q.order("updated_at", { ascending: false });
  const list = plans || [];
  const planIds = list.map((p) => p.id);
  let suites = [];
  if (planIds.length) {
    const { data } = await supabase.from("test_suites")
      .select("id,name,test_plan_id,test_suite_test_cases(position,test_cases(title,priority,status))")
      .in("test_plan_id", planIds).order("created_at", { ascending: true });
    suites = data || [];
  }
  const suitesByPlan = new Map();
  for (const s of suites) { if (!suitesByPlan.has(s.test_plan_id)) suitesByPlan.set(s.test_plan_id, []); suitesByPlan.get(s.test_plan_id).push(s); }
  const headers = ["테스트플랜", "플랜 상태", "담당자", "기간", "스위트", "테스트케이스", "케이스 우선순위", "케이스 상태"];
  const rows = [];
  const period = (p) => { const s = p.start_date || "", e = p.end_date || ""; return (s || e) ? `${s} ~ ${e}` : ""; };
  for (const p of list) {
    const psuites = suitesByPlan.get(p.id) || [];
    if (!psuites.length) { rows.push([nz(p.name), nz(p.status), nz(p.assignee), period(p), "", "", "", ""]); continue; }
    for (const s of psuites) {
      const cases = (s.test_suite_test_cases || []).slice().sort((a, b) => (a.position ?? 0) - (b.position ?? 0)).map((x) => x.test_cases).filter(Boolean);
      if (!cases.length) { rows.push([nz(p.name), nz(p.status), nz(p.assignee), period(p), nz(s.name), "", "", ""]); continue; }
      for (const tc of cases) rows.push([nz(p.name), nz(p.status), nz(p.assignee), period(p), nz(s.name), nz(tc.title), nz(tc.priority), nz(tc.status)]);
    }
  }
  return { sheetName: "테스트플랜 구조", fileBase: "test-plans", headers, rows };
}

async function buildTab(type, projectId, ids) {
  switch (type) {
    case "test-cases": return tabTestCases(projectId, null);
    case "test-cases-filtered": return tabTestCases(projectId, ids);
    case "test-runs": return tabTestRuns(projectId);
    case "defects": return tabDefects();
    case "test-plans": return tabTestPlans(projectId);
    default: throw new Error("알 수 없는 내보내기 항목: " + type);
  }
}

// ── 파일 생성/다운로드 ─────────────────────────────────────────────
function safeSheetName(raw, used) {
  let base = (raw || "Sheet").replace(/[\\/?*[\]:]/g, "_").slice(0, 31) || "Sheet";
  let name = base, n = 2;
  while (used.has(name)) { const suf = ` (${n++})`; name = base.slice(0, 31 - suf.length) + suf; }
  used.add(name);
  return name;
}
function triggerDownload(blob, filename) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url; a.download = filename;
  document.body.appendChild(a); a.click(); a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 5000);
}

export async function exportImpl(options) {
  try {
    const url = new URL(options.url);
    const seg = url.pathname.split("/").filter(Boolean); // api export <type> excel  |  api export combined
    const qs = url.searchParams;
    const projectId = qs.get("projectId");
    const format = (qs.get("format") || "xlsx").toLowerCase();
    const ids = qs.get("ids") ? qs.get("ids").split(",").map(Number).filter((x) => !Number.isNaN(x)) : null;

    let types;
    if (seg[2] === "combined") {
      types = (qs.get("types") || "").split(",").map((s) => s.trim()).filter(Boolean);
    } else {
      // /api/export/{endpoint}/excel — endpoint는 test-cases/test-runs/defects/test-plans.
      // test-cases는 ids가 있으면 필터본으로 처리.
      const endpoint = seg[2];
      types = [endpoint === "test-cases" && ids && ids.length ? "test-cases-filtered" : endpoint];
    }
    if (!types.length) return { ok: false, status: 400, data: { message: "내보낼 항목이 없습니다." } };

    const tabs = [];
    for (const t of [...new Set(types)]) tabs.push(await buildTab(t, projectId, ids));

    const XLSX = await loadXLSX();
    const stamp = new Date().toISOString().slice(0, 10);

    // CSV 단일 표만 진짜 CSV로. 그 외(다중 표 또는 xlsx)는 xlsx 워크북으로.
    if (format === "csv" && tabs.length === 1) {
      const t = tabs[0];
      const ws = XLSX.utils.aoa_to_sheet([t.headers, ...t.rows]);
      const csv = "﻿" + XLSX.utils.sheet_to_csv(ws);
      triggerDownload(new Blob([csv], { type: "text/csv;charset=utf-8" }), `${t.fileBase}_${stamp}.csv`);
      return { ok: true, status: 200, data: null };
    }

    const wb = XLSX.utils.book_new();
    const used = new Set();
    for (const t of tabs) {
      const ws = XLSX.utils.aoa_to_sheet([t.headers, ...t.rows]);
      XLSX.utils.book_append_sheet(wb, ws, safeSheetName(t.sheetName, used));
    }
    const out = XLSX.write(wb, { type: "array", bookType: "xlsx" });
    const name = tabs.length === 1 ? `${tabs[0].fileBase}_${stamp}.xlsx` : `tms-export_${stamp}.xlsx`;
    triggerDownload(new Blob([out], { type: "application/octet-stream" }), name);
    return { ok: true, status: 200, data: null };
  } catch (e) {
    return { ok: false, status: 500, data: { message: "내보내기 실패: " + e.message } };
  }
}
