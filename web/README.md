# TMS Web — Vercel + Supabase 전환

기존 **Electron 데스크톱 앱 + Spring Boot + MySQL** 을 **웹앱(Vercel) + Supabase(PostgreSQL/Auth/Storage)** 로 옮기기 위한 작업 폴더입니다.

## 아키텍처 매핑

| 기존 | 전환 후 |
|------|---------|
| Electron 셸 + `desktop/renderer.js` | Vercel에 배포되는 웹 프론트(`web/`) |
| Spring Boot REST API | Supabase 자동 REST(PostgREST) + 필요한 곳만 Edge Functions |
| MySQL | Supabase PostgreSQL (`supabase/migrations/0001_initial_schema.sql`) |
| 로컬 `uploads/` 첨부파일 | Supabase Storage |
| `TMS_API_TOKEN` 필터 | Supabase Auth + Row Level Security(RLS) |
| `.env` (spring-dotenv) | Vercel 환경변수 + `web/.env.local` |

## 준비물 (계정/프로젝트 — 먼저 만들어야 함)

### 1) Supabase 프로젝트 생성
1. https://supabase.com 가입 → **New project** 생성 (DB 비밀번호 기록해 두기)
2. 좌측 **SQL Editor** → `supabase/migrations/0001_initial_schema.sql` 내용 붙여넣고 실행 → 테이블 생성
3. **Project Settings → API** 에서 두 값 복사:
   - `Project URL` → `VITE_SUPABASE_URL`
   - `anon public` 키 → `VITE_SUPABASE_ANON_KEY`

### 2) 로컬에서 연결 확인
```bash
cd web
cp .env.example .env.local   # 위에서 복사한 값 채우기
npm install
npm run dev                  # http://localhost:5173 에서 "연결 성공" 확인
```
> 처음엔 RLS가 켜져 있어 조회가 막힐 수 있습니다. 개발 초기에는 Supabase Table editor에서 각 테이블 RLS를 잠시 끄거나, "anon read" 정책을 추가하세요. (운영 전 반드시 RLS 정책 정비)

### 3) Vercel 배포
1. https://vercel.com 가입 → **Add New → Project** → 이 저장소 import
2. **Root Directory** 를 `web` 으로 지정
3. **Environment Variables** 에 `VITE_SUPABASE_URL`, `VITE_SUPABASE_ANON_KEY` 추가
4. Deploy

## 남은 이관 작업(점진적)
- [ ] `desktop/renderer.js`의 API 호출(`request(...)`)을 Supabase 쿼리로 치환 — 화면 단위로 이식
- [ ] 프로젝트 격리/담당자 권한 → RLS 정책으로 재구현
- [ ] 첨부파일 업로드 → Supabase Storage 버킷
- [ ] 엑셀 import/export, 백업/복구 → Edge Function 또는 클라이언트 처리
- [ ] Jira 연동(현재 Spring `JiraClient`) → Edge Function(토큰은 서버측 보관, 브라우저 노출 금지)
- [ ] 기존 MySQL 데이터 → Supabase 이관 스크립트

## 현재 상태
- ✅ Postgres 스키마 초안 (`supabase/migrations/0001_initial_schema.sql`)
- ✅ 웹앱 스캐폴드 + Supabase 클라이언트 + 연결 확인 화면
- ⬜ 계정/자격증명 (사용자 준비 필요) → 이후 실제 연결·배포
