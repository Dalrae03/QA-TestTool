-- TC 번호 재정렬용 — public 스키마의 모든 테이블을 비우고 ID 시퀀스를 1부터 리셋한다.
-- 실행 후 이관 스크립트를 다시 돌리면 test_cases 등이 1번부터 채번된다.
-- ⚠️ 현재 Supabase에 들어있는 데이터를 모두 지운다(원본 MySQL은 그대로이므로 재적재로 복구됨).
do $$
declare t text;
begin
  for t in select tablename from pg_tables where schemaname = 'public'
  loop
    execute format('truncate table public.%I restart identity cascade', t);
  end loop;
end $$;
