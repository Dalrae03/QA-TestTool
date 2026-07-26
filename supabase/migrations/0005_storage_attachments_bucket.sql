-- 0005 — 첨부파일용 Supabase Storage 버킷 + 개발용 정책
-- 웹 첨부파일 업로드/다운로드에 필요. ⚠️ DB 소유자만 적용 가능(Storage 스키마).
-- Supabase SQL Editor에 붙여넣고 실행. idempotent.
--
-- 기존: 로컬 uploads/ 폴더 + attachments 테이블(메타데이터)
-- 전환: 실제 파일은 Storage 'attachments' 버킷, 메타데이터는 기존 attachments 테이블 유지.

-- 'attachments' 버킷 생성 (public: 개발 편의. 운영 시 private + 서명 URL 권장)
insert into storage.buckets (id, name, public)
values ('attachments', 'attachments', true)
on conflict (id) do nothing;

-- 개발용 정책: anon/authenticated 가 이 버킷에 전체 접근(업로드·조회·삭제).
-- ⚠️ 운영 배포 전에는 프로젝트/인증 기반 정책으로 반드시 교체할 것.
drop policy if exists dev_attachments_all on storage.objects;
create policy dev_attachments_all on storage.objects
  for all to anon, authenticated
  using (bucket_id = 'attachments')
  with check (bucket_id = 'attachments');
