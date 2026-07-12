-- ⚠️ 개발 전용 RLS 정책 — public 스키마의 모든 테이블에 anon/authenticated 전체 접근을 허용한다.
-- Supabase는 테이블에 RLS를 켜 두므로, 정책이 없으면 읽기는 0행·쓰기는 차단된다.
-- 프로토타입/개발 단계에서 앱이 동작하도록 열어 두는 것이며,
-- 운영 배포 전에는 반드시 프로젝트 격리·인증 기반의 실제 정책으로 교체할 것.
do $$
declare t text;
begin
  for t in
    select tablename from pg_tables where schemaname = 'public'
  loop
    execute format('alter table public.%I enable row level security', t);
    execute format('drop policy if exists dev_all_access on public.%I', t);
    execute format(
      'create policy dev_all_access on public.%I for all to anon, authenticated using (true) with check (true)', t);
  end loop;
end $$;
