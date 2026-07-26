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
- ✅ Postgres 스키마 (`supabase/migrations/0001`, 개발 RLS `0002`, 드리프트 보정 `0003`)
- ✅ 웹앱 스캐폴드 + Supabase 클라이언트 + 연결 확인
- ✅ **전송 어댑터** — 데스크톱 `renderer.js`를 무수정으로 재사용하고,
  `window.desktopApi.request(...)`를 Supabase(PostgREST) 쿼리로 라우팅 (`src/adapter/`)
- ✅ **테스트케이스 관리 화면 전체** 동작 (부팅 로드 + TC/폴더/태그/환경/컨피그/사용자/결함 CRUD)
- ✅ **테스트 플랜 · 스위트(플랜소속/독립)** 전체 CRUD (`src/adapter/runs.js`)
- ✅ **테스트런(실행) · 레거시 케이스별 런** — 조회는 즉시 동작.
  생성/결과기록은 **마이그레이션 `0004` 적용 필요**(executions 'READY' 상태·항목 상태 확장·폴더 스냅샷 컬럼)
- ✅ **대시보드 통계**(`/api/dashboard/stats`) — TC/실행/결함 집계, 히트맵, 잔존이슈, 최근 감사로그
  (결함은 스키마에 project_id 없어 전역 집계 — 후속 스키마 패치로 보완 가능)
- ✅ **첨부파일** → Supabase Storage (`src/adapter/attachments.js`, 마이그레이션 `0005` 버킷 필요)
- ✅ **엑셀 임포트** → 브라우저 xlsx 파싱 후 폴더/케이스 생성 (`src/adapter/excel.js`, `xlsx` 의존성)
- ✅ **Jira 설정 저장/조회** (`src/adapter/jira.js`) — 순수 DB
- ✅ **백업/복구** → 전체 데이터 JSON 스냅샷 내보내기/복구 (`src/adapter/backup.js`, id 재매핑)
- ⬜ **Jira 연결테스트·push·pull·sync** — 외부 Jira API 호출이라 Supabase Edge Function(서버측) 필요
- ⬜ **엑셀/CSV 내보내기**(`/api/export/*`) — 후속 (현재 안내 메시지)
- ⬜ 운영용 RLS 정책 (현재 `0002` 개발 전면 허용)

### 스키마/인프라 적용 필요(DB 소유자)
| 마이그레이션 | 용도 | 없으면 |
|---|---|---|
| `0004` | 실행 상태 확장·폴더 스냅샷 | 테스트런 생성/기록 실패 |
| `0005` | Storage `attachments` 버킷·정책 | 첨부 업로드/다운로드 실패 |
| `0003` (선택) | `test_folders.code` | 폴더코드 접두사 표시(현재 미사용) |

### 아키텍처 (어댑터 방식)
```
index.html ─▶ src/main.js ─▶ installDesktopApi()  (window.desktopApi = Supabase 어댑터)
                         └─▶ /renderer.js 주입      (데스크톱 원본, 무수정)
renderer.js: request("/api/testcases") ─▶ 어댑터가 경로 파싱 ─▶ supabase.from("test_cases")...
```
`src/adapter/index.js`에 라우트를 추가하는 방식으로 나머지 도메인을 점진 이식한다.
