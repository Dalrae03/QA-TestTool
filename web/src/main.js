// 웹 진입점.
// 1) Supabase 어댑터로 window.desktopApi 를 설치한다 (renderer.js가 기대하는 인터페이스).
// 2) 그런 다음 데스크톱 renderer.js(무수정 복사본)를 클래식 스크립트로 주입해 실행한다.
//    renderer.js는 로드 끝에서 bootstrap()을 호출하므로, 그 전에 window.desktopApi가 준비돼 있어야 한다.
//    클래식 스크립트로 주입해야 renderer.js의 전역 함수들이 window에 노출되어
//    index.html의 인라인 onclick 핸들러(65개)가 정상 동작한다.

import { isSupabaseConfigured } from "./supabaseClient.js";
import { installDesktopApi } from "./adapter/index.js";

function fatal(msg) {
  document.body.innerHTML =
    `<div style="max-width:640px;margin:60px auto;font-family:system-ui;color:#b91c1c;padding:0 20px">
       <h2>초기화 실패</h2><p>${msg}</p>
       <p style="color:#374151">web/.env.local 의 VITE_SUPABASE_URL / VITE_SUPABASE_ANON_KEY 를 확인하세요.</p>
     </div>`;
}

if (!isSupabaseConfigured) {
  fatal("Supabase 환경변수가 설정되지 않았습니다.");
} else {
  installDesktopApi();
  const s = document.createElement("script");
  s.src = "/renderer.js"; // web/public/renderer.js (데스크톱 원본과 동일)
  s.onerror = () => fatal("renderer.js 로드에 실패했습니다.");
  document.body.appendChild(s);
}
