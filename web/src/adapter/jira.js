// Jira 연동.
// - 설정 저장/조회(jira_settings 싱글턴) : 순수 DB → 웹에서 바로 동작.
// - 연결 테스트 / push / pull / link / sync-all : 외부 Jira REST API 호출 필요.
//   브라우저에서 직접 호출하면 CORS·토큰 노출 문제가 있어, Supabase Edge Function(서버측)
//   배포가 필요하다. 현재는 명확한 안내 메시지를 반환한다.

import { supabase } from "../supabaseClient.js";
import { on, ok, HttpError } from "./index.js";

function toView(row) {
  const hasToken = !!(row && row.api_token && String(row.api_token).trim());
  const enabled = row ? row.enabled : true;
  const baseUrl = row ? row.base_url : null;
  const email = row ? row.email : null;
  return {
    baseUrl: baseUrl ?? null,
    email: email ?? null,
    projectKey: row ? row.project_key : null,
    webBaseUrl: row ? row.web_base_url : null,
    hasToken,
    configured: !!(enabled && baseUrl && email && hasToken),
    enabled,
  };
}
async function getSettingsRow() {
  return ok(await supabase.from("jira_settings").select("*").order("id", { ascending: true }).limit(1).maybeSingle());
}

export function registerJiraRoutes() {
  on("GET", "/api/jira/settings", async () => toView(await getSettingsRow()));

  on("PUT", "/api/jira/settings", async ({ body }) => {
    const existing = await getSettingsRow();
    // apiToken 이 비어 있으면 기존 토큰 유지 (마스킹된 값 재저장 방지)
    const token = (body.apiToken && body.apiToken.trim()) ? body.apiToken.trim() : (existing ? existing.api_token : null);
    const row = {
      base_url: (body.baseUrl || "").trim() || null,
      email: (body.email || "").trim() || null,
      api_token: token,
      project_key: (body.projectKey || "").trim() || null,
      web_base_url: (body.webBaseUrl || "").trim() || null,
      enabled: body.enabled == null ? true : !!body.enabled,
    };
    if (existing) ok(await supabase.from("jira_settings").update(row).eq("id", existing.id));
    else ok(await supabase.from("jira_settings").insert(row));
    return toView(await getSettingsRow());
  });

  // 연결 테스트 — 결과 객체(success=false)로 안내 (renderer가 message를 표시)
  on("POST", "/api/jira/settings/test", async () => ({
    success: false, accountDisplayName: null, accountEmail: null, projectName: null,
    message: "웹 버전에서는 Jira 연결 테스트가 Supabase Edge Function(서버측) 배포 후 지원됩니다. 설정 저장/조회는 정상 동작합니다.",
  }));

  // 외부 Jira API 직접 호출이 필요한 작업 — Edge Function 배포 전까지 안내 메시지
  const needsEdge = (label) => () => {
    throw new HttpError(501, `${label}은(는) Jira API 직접 호출이 필요해, 웹 버전에서는 Supabase Edge Function(서버측) 배포 후 지원됩니다.`);
  };
  on("POST", "/api/jira/sync-all", needsEdge("Jira 전체 동기화"));
  on("POST", "/api/defects/:id/jira/push", needsEdge("Jira 이슈 생성(push)"));
  on("POST", "/api/defects/:id/jira/pull", needsEdge("Jira 상태 동기화(pull)"));
  on("POST", "/api/defects/:id/jira/link", needsEdge("Jira 이슈 연결(link)"));
}
