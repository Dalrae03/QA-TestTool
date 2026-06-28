# TMS (QA-TestTool)

Spring Boot 백엔드 + Electron 데스크톱 셸로 구성된 테스트 관리 시스템(Test Management System)입니다.
테스트 케이스 작성·버전 관리부터 테스트 플랜/스위트 구성, 테스트 런 실행과 결과 추적, 결함 관리, 대시보드 집계까지를 다룹니다.

## 기술 스택

- Java 21
- Spring Boot 3.5.0 (Web, Data JPA, Validation)
- MySQL (운영/개발), H2 (테스트)
- Flyway (DB 마이그레이션)
- spring-dotenv (`.env` 로딩)
- Apache POI (엑셀 임포트)
- Maven
- Electron / electron-builder (데스크톱 셸)

## 현재 구현 범위

| 도메인 | 상태 | 주요 기능 |
|---|---|---|
| `testcase` | 구현 | CRUD, 상태 변경, 폴더 분류, 영역 태그, 버전 관리/복원, 결함 연결 |
| `testplan` | 구현 | 플랜 CRUD, 상태/기간/리스크·범위 등 메타 필드 |
| `testsuite` | 구현 | 플랜별 스위트 + 독립(standalone) 스위트, 케이스 배정 |
| `testrun` | 구현 | 테스트 케이스별 실행 런 CRUD |
| `execution` | 구현 | 테스트 런(실행 사이클), 항목별 결과/상태 기록, 플랜 매칭 |
| `environment` | 구현 | 서버 환경 CRUD 및 케이스 연동 |
| `configuration` | 구현 | OS/브라우저/디바이스 등 재사용 configuration |
| `project` | 구현 | 멀티 프로젝트 및 데이터 격리 |
| `user` | 구현 | 사용자 CRUD, 역할(`UserRole`) |
| `dashboard` | 구현 | 서버 측 통계 집계 |
| `defect` | 구현 | 결함 CRUD, Jira 연동 |
| `attachment` | 구현 | 테스트 케이스/결함/런 첨부 업로드·다운로드 |
| `excel` | 구현 | 엑셀(.xlsx/.xls) 테스트 케이스 임포트 |
| `jira` | 구현 | 결함 ↔ Jira 이슈 push/pull/link/sync |
| `audit` | 구현 | 테스트 케이스 변경 이력 조회 |
| `testresult` | 미구현 | 패키지 뼈대만 존재 (`execution`이 결과를 담당) |
| `auth` | 미구현 | 패키지 뼈대만 존재 (로컬 하드닝은 "보안" 참고) |

## 프로젝트 구조

```text
com.tms
├── global            # 공통 인프라
│   ├── config
│   ├── security      # ApiTokenFilter, SecurityHeadersFilter 등
│   ├── exception
│   ├── response
│   ├── audit
│   ├── init
│   └── util
├── auth              # (뼈대)
├── user
├── project
├── testcase          # 케이스, 폴더, 영역 태그, 버전
├── testplan
├── testsuite
├── testrun
├── execution         # 테스트 런(실행 사이클), 결과
├── testresult        # (뼈대)
├── environment
├── configuration
├── defect
├── attachment
├── excel
├── jira
├── audit
└── dashboard

src/main/java/db/migration   # Flyway 마이그레이션 (V1~V3)
```

## 데이터 모델

### TestCase 주요 필드

- `id`: `Long`, PK
- `type`: `TestCaseType` — `FUNCTIONAL`, `NON_FUNCTIONAL` (필수)
- `priority`: `TestCasePriority` — `HIGH`, `MEDIUM`, `LOW` (필수)
- `status`: `TestCaseStatus` — `DRAFT`, `REVIEW_NEEDED`, `READY`, `COMPLETED`
- `title`: 최대 200자 (필수)
- `description`, `precondition`, `steps`: 필수
- `version`: 최대 50자 (선택)
- `notes`, `assignee`: 선택
- `os` / `browser` / `device`: 실행 환경 enum (선택)
- `folder`, `serverEnvironment`, `testConfiguration`: 연관 엔티티(FK)
- `areaTags`, `defects`: 다대다 연관
- `projectId`: 소속 프로젝트
- `createdAt`, `updatedAt`: 감사 시각

버전 이력은 `TestCaseVersion`으로 관리되며 `/api/testcases/{id}/versions`로 조회·복원합니다.

### 기타 enum

- `ResultStatus`(실행 결과): `UNTESTED`, `NOT_EXECUTED`, `IN_PROGRESS`, `PASSED`, `FAILED`, `BLOCKED`, `RETEST`, `SKIPPED`
- `UserRole`: `ADMIN`, `QA_LEAD`, `QA`, `DEVELOPER`, `VIEWER`

## 실행 환경

기본 실행은 MySQL 기준입니다. 설정: [application.yml](src/main/resources/application.yml)

주요 환경 변수(기본값):

- `DB_URL=jdbc:mysql://localhost:3306/tms?serverTimezone=Asia/Seoul&characterEncoding=UTF-8`
- `DB_USERNAME=root`
- `DB_PASSWORD=1234` (운영에서는 반드시 변경)
- `SERVER_PORT=8080`
- `SERVER_ADDRESS=127.0.0.1` (루프백 바인딩, "보안" 참고)
- `TMS_API_TOKEN=` (비어 있으면 토큰 검증 비활성화)
- `UPLOAD_DIR=./uploads`

예시 DB 생성:

```sql
CREATE DATABASE tms CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### DB 마이그레이션 (Flyway)

스키마 변경은 Flyway로 버전 관리됩니다([src/main/java/db/migration](src/main/java/db/migration)).
기존에 Hibernate `ddl-auto`로 생성한 DB가 이미 있는 경우, Flyway 이력 테이블이 없어 기동이 실패할 수 있습니다.
이를 위해 `spring.flyway.baseline-on-migrate: true`가 설정되어 있어, 기존 스키마를 baseline 처리한 뒤 마이그레이션을 적용합니다.

## 실행 방법

### 백엔드 + Electron 동시 실행 (권장)

```bash
npm install
npm run dev:start
```

Spring Boot를 먼저 기동하고 8080 포트가 열리면 Electron을 실행합니다.
실행 시 임의 API 토큰을 생성해 백엔드와 Electron에 함께 주입하므로 별도 설정 없이 보호됩니다.

### 백엔드만 실행

```bash
mvn spring-boot:run
```

## 보안

데스크톱 단일 사용자 환경을 전제로 한 로컬 하드닝이 적용되어 있습니다.

- **로컬 바인딩**: 백엔드는 기본적으로 `127.0.0.1`에만 바인딩되어 외부 네트워크에 노출되지 않습니다(`server.address`, 기본값 `127.0.0.1`). 필요 시 `SERVER_ADDRESS` 환경변수로 변경할 수 있습니다.
- **공유 API 토큰**: `tms.security.api-token`(환경변수 `TMS_API_TOKEN`)이 설정되면 `/api/**` 요청은 `X-TMS-Token` 헤더가 일치해야 합니다(상수 시간 비교). 브라우저발 CSRF·동일 머신의 다른 프로세스로부터 API를 보호합니다.
  - `npm run dev:start`는 실행 시 임의 토큰을 생성해 백엔드와 Electron 셸에 함께 주입하므로 별도 설정이 필요 없습니다.
  - 토큰이 비어 있으면 검증은 비활성화됩니다(예: `mvn test`, 단독 `mvn spring-boot:run`).
- **보안 응답 헤더**: 모든 응답에 `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`, `Content-Security-Policy: default-src 'none'`가 적용됩니다.
- **파일 업로드**: 첨부 파일은 확장자 화이트리스트로 제한되고, UUID 파일명으로 저장되며, 저장 경로가 업로드 루트를 벗어나지 못하도록 검증합니다(경로 조작 방지). 다운로드는 항상 `Content-Disposition: attachment`로 처리됩니다.
- **Electron**: `contextIsolation: true`, `nodeIntegration: false`, `sandbox: true`로 렌더러를 격리합니다.

> 다중 사용자 로그인(사용자별 비밀번호/세션) 인증은 아직 도입하지 않았습니다. 다중 사용자/네트워크 배포 시 별도 인증 도입이 필요합니다.

관련 파일:
- [ApiTokenFilter.java](src/main/java/com/tms/global/security/ApiTokenFilter.java)
- [SecurityHeadersFilter.java](src/main/java/com/tms/global/security/SecurityHeadersFilter.java)
- [AttachmentService.java](src/main/java/com/tms/attachment/service/AttachmentService.java)

## Electron 데스크톱 앱

Electron 앱은 Spring Boot 백엔드를 호출하는 데스크톱 셸입니다.
렌더러는 직접 네트워크 요청을 하지 않고, 메인 프로세스의 IPC 핸들러를 통해 백엔드와 통신합니다(토큰 헤더 주입 포함).

구성 파일:
- [package.json](package.json)
- [electron/main.js](electron/main.js)
- [electron/preload.js](electron/preload.js)
- [desktop/index.html](desktop/index.html)
- [desktop/renderer.js](desktop/renderer.js)

주의:
- Electron 앱은 백엔드를 내장 실행하지 않습니다. 먼저 Spring Boot 서버가 떠 있어야 합니다(또는 `npm run dev:start` 사용).
- 기본 연결 주소는 `http://localhost:8080` 입니다.

### Electron 개발 실행

```bash
npm install
npm run desktop:dev
```

### Electron 패키징

```bash
npm run desktop:dist:mac    # macOS
npm run desktop:dist:win    # Windows
npm run desktop:pack        # 압축 없이 앱 디렉터리만
```

- `desktop:dist` 계열은 `--publish never`로 빌드만 수행합니다.
- 출력 경로: `release/`
- 코드 서명/notarization은 아직 설정하지 않았습니다.

## 테스트 방법

테스트는 H2 인메모리 DB를 사용하므로 로컬 MySQL 없이 실행됩니다. Flyway는 테스트에서 비활성화됩니다.
테스트 설정: [src/test/resources/application.yml](src/test/resources/application.yml)

```bash
mvn test
```

현재 통합 테스트 위주로 14개 클래스 / 52개 테스트가 있으며, 도메인별 CRUD·관계, 보안 필터(토큰/헤더) 등을 검증합니다.

> 알려진 이슈: `TestPlanControllerIntegrationTest` 1건이 실패합니다(H2에서 미지원되는 `UPDATE ... JOIN` 구문 + 에러 메시지 문자열 불일치). 별도로 수정 예정입니다.

## GitHub Actions

Electron 패키징 워크플로: [.github/workflows/electron-build.yml](.github/workflows/electron-build.yml)

- 수동 실행(`workflow_dispatch`) 및 `desktop-v*` 태그 푸시 시 실행
- macOS/Windows 빌드 결과물을 artifact로 업로드

## API

모든 엔드포인트는 `/api` 하위에 있습니다. 토큰이 설정된 경우 `X-TMS-Token` 헤더가 필요합니다.

### 테스트 케이스
- `GET/POST /api/testcases`, `GET/PUT/DELETE /api/testcases/{id}`
- `PATCH /api/testcases/{id}/status` — 상태 변경
- `PATCH /api/testcases/{id}/folder` — 폴더 이동
- `GET /api/testcases/{id}/versions`, `POST /api/testcases/{id}/versions/{versionId}/restore`
- `POST/DELETE /api/testcases/{id}/defects/{defectId}` — 결함 연결
- `GET /api/testcases/{testCaseId}/audit-logs` — 변경 이력

### 폴더 / 영역 태그
- `GET/POST /api/folders`, `GET/PUT/DELETE /api/folders/{id}`
- `GET/POST /api/area-tags`, `DELETE /api/area-tags/{id}`

### 테스트 플랜 / 스위트
- `GET/POST /api/test-plans`, `GET/PUT/DELETE /api/test-plans/{id}`
- `GET/POST /api/test-plans/{planId}/suites`, `GET/PUT/DELETE /api/test-plans/{planId}/suites/{suiteId}`
- `GET/POST /api/suites`, `GET /api/suites/standalone`, `GET/PUT/DELETE /api/suites/{suiteId}` — 독립 스위트

### 테스트 런 / 실행
- `GET/POST /api/testcases/{testCaseId}/runs`, `PUT/DELETE /api/testcases/{testCaseId}/runs/{runId}`
- `GET/POST /api/test-runs`, `GET/PUT/DELETE /api/test-runs/{id}` — 실행 사이클
- `PATCH /api/test-runs/{id}/plan`, `PATCH /api/test-runs/{id}/items/{itemId}`
- `GET /api/test-runs/items/by-test-case/{testCaseId}`

### 환경 / configuration / 프로젝트 / 사용자
- `GET/POST /api/server-environments`, `GET/PUT/DELETE /api/server-environments/{id}`
- `GET/POST /api/test-configurations`, `GET/PUT/DELETE /api/test-configurations/{id}`
- `GET/POST /api/projects`, `GET/PUT/DELETE /api/projects/{id}`
- `GET/POST /api/users`, `GET/PUT/DELETE /api/users/{id}`

### 결함 / Jira / 첨부 / 임포트 / 대시보드
- `GET/POST /api/defects`, `GET/PUT/DELETE /api/defects/{id}`
- `POST /api/defects/{id}/jira/push|pull|link`, `POST /api/jira/sync-all`
- `GET/POST /api/testcases/{id}/attachments`, `GET/POST /api/defects/{id}/attachments`, `GET/POST /api/testcases/{testCaseId}/runs/{runId}/attachments`
- `GET /api/attachments/{id}/download`, `DELETE /api/attachments/{id}`
- `POST /api/import/excel`
- `GET /api/dashboard/stats`

## 요청 / 응답 예시

### 테스트 케이스 생성 (`POST /api/testcases`)

```json
{
  "type": "FUNCTIONAL",
  "priority": "HIGH",
  "status": "DRAFT",
  "title": "로그인 성공 테스트",
  "description": "정상 계정으로 로그인 가능해야 한다.",
  "precondition": "가입된 사용자가 존재한다.",
  "steps": "1. 로그인 페이지 접속\n2. 이메일/비밀번호 입력\n3. 로그인 버튼 클릭",
  "notes": "스모크 테스트",
  "areaTagIds": [1, 2],
  "projectId": 1
}
```

`type`, `priority`, `title`, `description`, `precondition`, `steps`는 필수입니다.

## 예외 처리

전역 예외 처리: [GlobalExceptionHandler.java](src/main/java/com/tms/global/exception/GlobalExceptionHandler.java)
에러 응답 DTO: [ErrorResponse.java](src/main/java/com/tms/global/response/ErrorResponse.java)

주요 매핑:
- `EntityNotFoundException` → `404 NOT_FOUND`
- `InvalidRequestException` → `400 BAD_REQUEST`
- `MethodArgumentNotValidException` → `400 BAD_REQUEST` (필드별 메시지)
- `IllegalArgumentException` / `DataIntegrityViolationException` → `409 CONFLICT`

Validation 실패 응답 예시:

```json
{
  "message": "Validation failed",
  "errors": {
    "title": "must not be blank",
    "type": "must not be null"
  }
}
```

## 현재 한계

- 프론트엔드는 Electron 데스크톱 셸만 제공(웹 UI 없음)
- Swagger/OpenAPI 미적용
- `testresult`, `auth`는 패키지 뼈대만 존재
- 다중 사용자 로그인 인증 미구현 (로컬 하드닝만 적용 — "보안" 참고)
- 페이징/검색 미적용

## 다음 추천 작업

1. 다중 사용자 인증/권한 (필요 시)
2. Swagger/OpenAPI 문서화
3. 목록 API 페이징·검색·필터
4. 결함 히트맵 등 대시보드 시각화 보강
5. 알려진 실패 테스트(H2 SQL 호환성) 수정
