// 첨부파일 — Supabase Storage('attachments' 버킷) + attachments 테이블(메타데이터).
// 목록/삭제는 라우트(request 경유), 업로드/다운로드는 window.desktopApi 셰임으로 처리한다.
// (renderer.js는 업로드/다운로드를 desktopApi.uploadAttachment/downloadAttachment 로 호출)
//
// ⚠️ 스토리지 버킷·정책은 마이그레이션 0005 적용 필요(DB 소유자).

import { supabase } from "../supabaseClient.js";
import { on, ok } from "./index.js";

const BUCKET = "attachments";
const MAX_BYTES = 50 * 1024 * 1024;

function attToResponse(r) {
  return {
    id: r.id, entityType: r.entity_type, entityId: r.entity_id,
    originalFilename: r.original_filename, contentType: r.content_type,
    fileSize: r.file_size, createdAt: r.created_at,
  };
}
async function listAttachments(entityType, entityId) {
  const rows = ok(await supabase.from("attachments").select("*")
    .eq("entity_type", entityType).eq("entity_id", entityId).order("id", { ascending: true })) || [];
  return rows.map(attToResponse);
}

// 업로드 경로에서 (엔티티 타입, id) 도출
function parseUploadTarget(pathname) {
  const s = pathname.split("/").filter(Boolean);
  if (s.length === 4 && s[1] === "testcases" && s[3] === "attachments") return { type: "TEST_CASE", id: Number(s[2]) };
  if (s.length === 4 && s[1] === "defects" && s[3] === "attachments") return { type: "DEFECT", id: Number(s[2]) };
  if (s.length === 6 && s[1] === "testcases" && s[3] === "runs" && s[5] === "attachments") return { type: "TEST_RUN", id: Number(s[4]) };
  if (s.length === 6 && s[1] === "test-runs" && s[3] === "items" && s[5] === "attachments") return { type: "EXECUTION_ITEM", id: Number(s[4]) };
  return null;
}

// 브라우저 파일 선택 다이얼로그 (취소 감지 포함)
function pickFile() {
  return new Promise((resolve) => {
    const input = document.createElement("input");
    input.type = "file";
    input.style.display = "none";
    let settled = false;
    const finish = (v) => {
      if (settled) return;
      settled = true;
      window.removeEventListener("focus", onFocus);
      input.remove();
      resolve(v);
    };
    // 다이얼로그가 닫혀 창에 포커스가 돌아왔는데 파일이 없으면 취소로 간주.
    const onFocus = () => setTimeout(() => {
      if (!settled && (!input.files || input.files.length === 0)) finish(null);
    }, 400);
    input.addEventListener("change", () => finish(input.files && input.files[0] ? input.files[0] : null));
    window.addEventListener("focus", onFocus);
    document.body.appendChild(input);
    input.click();
  });
}

const uuid = () => (crypto.randomUUID ? crypto.randomUUID() : String(Date.now()) + Math.random().toString(16).slice(2));

export async function uploadAttachmentImpl(options) {
  try {
    const target = parseUploadTarget(new URL(options.url).pathname);
    if (!target) return { ok: false, status: 400, data: { message: "업로드 대상을 알 수 없습니다." } };
    const file = await pickFile();
    if (!file) return { ok: false, canceled: true, status: 0, data: null };
    if (file.size > MAX_BYTES) return { ok: false, status: 413, data: { message: "첨부파일은 50MB 이하만 업로드할 수 있습니다." } };

    const safe = file.name.replace(/[^\w.\-]+/g, "_");
    const stored = `${target.type}/${target.id}/${uuid()}_${safe}`;
    const contentType = file.type || "application/octet-stream";
    const up = await supabase.storage.from(BUCKET).upload(stored, file, { contentType, upsert: false });
    if (up.error) return { ok: false, status: 500, data: { message: "스토리지 업로드 실패: " + up.error.message + " (버킷/정책 0005 적용 확인)" } };

    const ins = await supabase.from("attachments").insert({
      entity_type: target.type, entity_id: target.id,
      original_filename: file.name, stored_filename: stored,
      content_type: contentType, file_size: file.size,
    }).select("*").single();
    if (ins.error) {
      await supabase.storage.from(BUCKET).remove([stored]); // 메타 저장 실패 시 스토리지 롤백
      return { ok: false, status: 500, data: { message: ins.error.message } };
    }
    return { ok: true, status: 201, data: attToResponse(ins.data) };
  } catch (e) {
    return { ok: false, status: 500, data: { message: e.message } };
  }
}

export async function downloadAttachmentImpl(options) {
  try {
    const s = new URL(options.url).pathname.split("/").filter(Boolean); // api attachments :id download
    // 이 셰임은 첨부 다운로드 전용. 엑셀/CSV 내보내기(/api/export/...)도 같은 셰임을 타므로 구분해 안내한다.
    if (s[1] === "export") {
      return { ok: false, status: 501, data: { message: "엑셀/CSV 내보내기는 아직 웹 버전에서 지원되지 않습니다(후속)." } };
    }
    if (s[1] !== "attachments") {
      return { ok: false, status: 400, data: { message: "지원하지 않는 다운로드 요청입니다." } };
    }
    const id = Number(s[2]);
    const meta = ok(await supabase.from("attachments").select("*").eq("id", id).maybeSingle());
    if (!meta) return { ok: false, status: 404, data: { message: "첨부파일을 찾을 수 없습니다." } };
    const dl = await supabase.storage.from(BUCKET).download(meta.stored_filename);
    if (dl.error) return { ok: false, status: 500, data: { message: "다운로드 실패: " + dl.error.message } };

    const blobUrl = URL.createObjectURL(dl.data);
    const a = document.createElement("a");
    a.href = blobUrl;
    a.download = options.suggestedName || meta.original_filename;
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(blobUrl), 5000);
    return { ok: true, status: 200, savedPath: a.download, data: null };
  } catch (e) {
    return { ok: false, status: 500, data: { message: e.message } };
  }
}

export function registerAttachmentRoutes() {
  on("GET", "/api/testcases/:id/attachments", ({ params }) => listAttachments("TEST_CASE", params.id));
  on("GET", "/api/defects/:id/attachments", ({ params }) => listAttachments("DEFECT", params.id));
  on("GET", "/api/testcases/:tcId/runs/:runId/attachments", ({ params }) => listAttachments("TEST_RUN", params.runId));
  on("GET", "/api/test-runs/:runId/items/:itemId/attachments", ({ params }) => listAttachments("EXECUTION_ITEM", params.itemId));
  on("DELETE", "/api/attachments/:id", async ({ params }) => {
    const meta = ok(await supabase.from("attachments").select("stored_filename").eq("id", params.id).maybeSingle());
    if (meta && meta.stored_filename) await supabase.storage.from(BUCKET).remove([meta.stored_filename]);
    ok(await supabase.from("attachments").delete().eq("id", params.id));
    return null;
  });
}
