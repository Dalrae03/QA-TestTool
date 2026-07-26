// 엑셀 임포트 — 브라우저에서 xlsx 파싱 후 폴더/테스트케이스 생성.
// (Spring ExcelImportService.importExcel 이식: 파일명→루트폴더, 시트→하위폴더, 헤더→컬럼매핑)
// window.desktopApi.uploadExcel({ file, projectId }) 셰임으로 호출된다.

import { supabase } from "../supabaseClient.js";

// 헤더(소문자) → 필드명
const ALIASES = {
  "title": "title", "제목": "title", "이름": "title", "name": "title",
  "테스트케이스": "title", "테스트 케이스": "title", "케이스명": "title",
  "description": "description", "설명": "description", "내용": "description",
  "precondition": "precondition", "전제조건": "precondition", "사전조건": "precondition", "전제 조건": "precondition",
  "steps": "steps", "스텝": "steps", "단계": "steps", "테스트단계": "steps", "테스트 단계": "steps", "step": "steps",
  "expectedresult": "expectedResult", "예상결과": "expectedResult", "예상 결과": "expectedResult", "expected": "expectedResult",
  "notes": "notes", "메모": "notes", "비고": "notes", "note": "notes", "노트": "notes",
  "priority": "priority", "우선순위": "priority",
  "status": "status", "상태": "status",
  "type": "type", "유형": "type", "타입": "type",
  "os": "os",
  "browser": "browser", "브라우저": "browser",
  "device": "device", "디바이스": "device",
  "assignee": "assignee", "담당자": "assignee", "작성자": "assignee",
  "version": "version", "버전": "version",
};
const ENUMS = {
  priority: ["HIGH", "MEDIUM", "LOW"],
  status: ["DRAFT", "REVIEW_NEEDED", "READY", "COMPLETED"],
  type: ["FUNCTIONAL", "NON_FUNCTIONAL"],
  os: ["MAC", "WINDOWS", "LINUX", "IOS", "ANDROID"],
  browser: ["CHROME", "FIREFOX", "SAFARI", "EDGE", "SAMSUNG_INTERNET", "NONE"],
  device: ["DESKTOP", "MOBILE", "TABLET"],
};
function parseEnum(field, value, dflt) {
  if (value == null || String(value).trim() === "") return dflt;
  const norm = String(value).toUpperCase().trim().replace(/[ -]/g, "_");
  return ENUMS[field].includes(norm) ? norm : dflt;
}

async function insertFolder(name, parentId, projectId) {
  const { data, error } = await supabase.from("test_folders")
    .insert({ name, parent_id: parentId, project_id: projectId }).select("id").single();
  if (error) throw new Error("폴더 생성 실패: " + error.message);
  return data.id;
}
function buildCaseRow(values, folderId, projectId) {
  return {
    type: parseEnum("type", values.type, "FUNCTIONAL"),
    priority: parseEnum("priority", values.priority, "MEDIUM"),
    status: parseEnum("status", values.status, "DRAFT"),
    title: values.title.trim(),
    description: values.description || "엑셀에서 가져온 테스트케이스",
    precondition: values.precondition || "-",
    steps: values.steps || "-",
    expected: values.expectedResult || "",
    notes: values.notes ?? null,
    os: parseEnum("os", values.os, null),
    browser: parseEnum("browser", values.browser, null),
    device: parseEnum("device", values.device, null),
    assignee: values.assignee ?? null,
    version: values.version ?? null,
    folder_id: folderId,
    project_id: projectId,
  };
}

export async function uploadExcelImpl(options) {
  try {
    const file = options && options.file;
    if (!file) return { ok: false, status: 400, data: { message: "파일을 읽을 수 없습니다." } };
    const projectId = options.projectId ?? null;
    // xlsx는 CJS 패키지 — 번들러 interop에 따라 named/default 위치가 달라질 수 있어 방어적으로 처리.
    const mod = await import("xlsx");
    const XLSX = (mod && typeof mod.read === "function") ? mod : (mod.default || mod);
    if (!XLSX || typeof XLSX.read !== "function") {
      return { ok: false, status: 500, data: { message: "엑셀 파서를 로드하지 못했습니다. 'npm install' 로 xlsx 의존성을 설치했는지 확인하세요." } };
    }
    const wb = XLSX.read(await file.arrayBuffer(), { type: "array" });

    const errors = [];
    let createdFolders = 0, createdCases = 0;
    const rootName = file.name.replace(/\.[^.]+$/, "");
    const rootId = await insertFolder(rootName, null, projectId);
    createdFolders++;

    for (const sheetName of wb.SheetNames) {
      const rows = XLSX.utils.sheet_to_json(wb.Sheets[sheetName], { header: 1, blankrows: false, defval: "" });
      if (rows.length < 2) continue;
      const sheetId = await insertFolder(sheetName, rootId, projectId);
      createdFolders++;

      const header = (rows[0] || []).map((h) => String(h).toLowerCase().trim());
      const colMap = {};
      header.forEach((h, i) => { if (ALIASES[h]) colMap[i] = ALIASES[h]; });

      const caseRows = [];
      for (let r = 1; r < rows.length; r++) {
        const cells = rows[r] || [];
        try {
          const values = {};
          for (const [idx, field] of Object.entries(colMap)) {
            const v = cells[idx];
            if (v != null && String(v).trim() !== "") values[field] = String(v).trim();
          }
          if (!values.title) continue;
          caseRows.push(buildCaseRow(values, sheetId, projectId));
        } catch (e) {
          errors.push(`시트 '${sheetName}' ${r + 1}행: ${e.message}`);
        }
      }
      if (caseRows.length) {
        const { error } = await supabase.from("test_cases").insert(caseRows);
        if (error) errors.push(`시트 '${sheetName}' 저장 실패: ${error.message}`);
        else createdCases += caseRows.length;
      }
    }
    return { ok: true, status: 200, data: { createdFolders, createdCases, errors } };
  } catch (e) {
    return { ok: false, status: 500, data: { message: e.message } };
  }
}
