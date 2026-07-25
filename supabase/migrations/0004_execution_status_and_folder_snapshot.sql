-- 0004 — 실행(테스트런) 도메인 스키마 드리프트 보정
-- 테스트런 "생성/결과기록"에 필요. (조회는 이 패치 없이도 동작)
-- ⚠️ DB 소유자만 적용 가능: Supabase SQL Editor에 붙여넣고 실행. idempotent.
--
-- 배경:
--   ExecutionStatus  = READY, IN_PROGRESS, COMPLETED   (0001엔 READY 누락)
--   ResultStatus     = UNTESTED, NOT_EXECUTED, IN_PROGRESS, PASSED, FAILED,
--                      BLOCKED, RETEST, NOT_IMPLEMENTED, SKIPPED  (0001엔 뒤 2개 누락)
--   ExecutionItem    = 출처 폴더 스냅샷(source_folder_*) 보관  (0001엔 컬럼 없음)

-- 1) executions.status CHECK 확장 (READY 추가)
alter table executions drop constraint if exists executions_status_check;
alter table executions add constraint executions_status_check
    check (status in ('READY','IN_PROGRESS','COMPLETED'));

-- 2) execution_items.status CHECK 확장 (NOT_IMPLEMENTED, SKIPPED 추가)
alter table execution_items drop constraint if exists execution_items_status_check;
alter table execution_items add constraint execution_items_status_check
    check (status in ('UNTESTED','NOT_EXECUTED','IN_PROGRESS','PASSED','FAILED',
                      'BLOCKED','RETEST','NOT_IMPLEMENTED','SKIPPED'));

-- 3) execution_items 출처 폴더 스냅샷 컬럼
alter table execution_items add column if not exists source_folder_id   bigint;
alter table execution_items add column if not exists source_folder_name text;
alter table execution_items add column if not exists source_folder_code text;
