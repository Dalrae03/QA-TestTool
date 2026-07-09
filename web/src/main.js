import { supabase, isSupabaseConfigured } from "./supabaseClient.js";

// 스캐폴드 단계 — Supabase 연결이 살아있는지 확인하는 최소 화면.
// 이후 단계에서 데스크톱 앱(renderer.js)의 화면/기능을 이 위에 이식한다.
const app = document.getElementById("app");

function line(text, ok) {
  const el = document.createElement("div");
  el.className = "status " + (ok === true ? "ok" : ok === false ? "err" : "");
  el.textContent = text;
  app.appendChild(el);
}

async function main() {
  app.innerHTML = "<h1>TMS Web — 연결 확인</h1>";

  if (!isSupabaseConfigured) {
    line("✗ Supabase 환경변수가 설정되지 않았습니다. web/.env.local 을 채우세요.", false);
    return;
  }
  line("Supabase URL 설정됨 ✓", true);

  // projects 테이블에 가벼운 쿼리를 날려 연결·스키마 적용 여부를 확인한다.
  const { data, error, count } = await supabase
    .from("projects")
    .select("*", { count: "exact", head: false })
    .limit(1);

  if (error) {
    line("✗ 쿼리 실패: " + error.message, false);
    line("스키마(supabase/migrations/0001_initial_schema.sql)를 적용했는지, RLS 정책이 있는지 확인하세요.");
    return;
  }
  line(`✓ Supabase 연결 성공 — projects 테이블 접근 OK (행 수: ${count ?? data.length})`, true);
}

main().catch((e) => line("✗ 예외: " + e.message, false));
