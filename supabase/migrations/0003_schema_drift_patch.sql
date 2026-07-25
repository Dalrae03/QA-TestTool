-- 0003 — 스키마 드리프트 보정
-- 최신 Electron/Spring 모델에는 있으나 0001 초기 스키마에 빠져 있던 컬럼을 보충한다.
-- Supabase SQL Editor에 붙여넣고 실행. 이미 있으면 무시(idempotent).

-- 폴더 코드(표시 ID 접두사) — TestFolder.code / effectiveCode(상위 상속) 지원.
alter table test_folders add column if not exists code text;
