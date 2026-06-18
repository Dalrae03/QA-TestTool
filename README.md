# TMS

Test Management System 백엔드 뼈대 프로젝트입니다.  
현재는 Spring Boot 기반의 기본 구조와 `TestCase` 도메인 CRUD가 구현되어 있습니다.

## 기술 스택

- Java 21
- Spring Boot 3.5.0
- Spring Web
- Spring Data JPA
- Spring Validation
- MySQL
- H2
- Maven
- Electron
- electron-builder

## 현재 구현 범위

현재 `testcase`, `testrun`, `testplan`, `testsuite`, `environment`, `configuration` 도메인이 동작합니다.

구현 완료:
- `TestCaseType` enum
- `TestCase` 엔티티
- `CreateTestCaseRequest`
- `UpdateTestCaseRequest`
- `TestCaseResponse`
- `TestCaseRepository`
- `TestCaseService`
- `TestCaseController`
- Validation 적용
- 전역 예외 처리
- `EntityNotFoundException` 기반 404 처리
- CRUD 통합 테스트
- 테스트 플랜 CRUD 및 상태/기간 관리
- 플랜별 테스트 스위트 CRUD
- 스위트별 테스트케이스 배정 및 순서 관리
- 서버 환경 CRUD 및 테스트케이스 연동
- 재사용 가능한 테스트 configuration CRUD 및 테스트케이스 연동

패키지 뼈대만 생성됨:
- `auth`
- `user`
- `project`
- `testresult`
- `dashboard`
- `global`

## 프로젝트 구조

```text
com.tms
├── global
│   ├── config
│   ├── security
│   ├── exception
│   ├── response
│   ├── util
│   └── audit
├── auth
├── user
├── project
├── testsuite
├── testcase
├── testrun
├── testresult
└── dashboard
```

## TestCase 도메인

현재 `TestCase`는 아래 필드를 가집니다.

- `id`: `Long`, PK, Auto Increment
- `type`: `FUNCTIONAL`, `NON_FUNCTIONAL`
- `title`: 최대 200자, 필수
- `description`: 필수
- `precondition`: 필수
- `steps`: 필수
- `notes`: 선택

테이블명:
- `test_cases`

## 실행 환경

기본 실행은 MySQL 기준입니다.

[application.yml](/Users/gimjun-won/Desktop/TMS/src/main/resources/application.yml:1)

기본 환경 변수:
- `DB_URL=jdbc:mysql://localhost:3306/tms?serverTimezone=Asia/Seoul&characterEncoding=UTF-8`
- `DB_USERNAME=root`
- `DB_PASSWORD=1234`
- `SERVER_PORT=8080`

예시 DB 생성:

```sql
CREATE DATABASE tms CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

## 실행 방법

```bash
mvn spring-boot:run
```

## Electron 데스크톱 앱

Electron 앱은 현재 Spring Boot 백엔드를 호출하는 데스크톱 셸로 구성되어 있습니다.

구성 파일:
- [package.json](/Users/gimjun-won/Desktop/TMS/package.json:1)
- [electron/main.js](/Users/gimjun-won/Desktop/TMS/electron/main.js:1)
- [electron/preload.js](/Users/gimjun-won/Desktop/TMS/electron/preload.js:1)
- [desktop/index.html](/Users/gimjun-won/Desktop/TMS/desktop/index.html:1)
- [desktop/styles.css](/Users/gimjun-won/Desktop/TMS/desktop/styles.css:1)
- [desktop/renderer.js](/Users/gimjun-won/Desktop/TMS/desktop/renderer.js:1)

현재 Electron 앱에서 가능한 것:
- 백엔드 URL 지정
- 테스트케이스 목록 조회
- 테스트케이스 생성
- 테스트케이스 수정
- 테스트케이스 삭제
- 테스트 플랜 생성/수정/삭제
- 테스트 스위트 생성/수정/삭제 및 테스트케이스 배정
- 서버 환경 등록 및 테스트케이스 배정
- 서버·OS·브라우저·디바이스 configuration 관리 및 자동 적용

주의:
- Electron 앱은 현재 백엔드를 내장 실행하지 않습니다.
- 먼저 Spring Boot 서버가 떠 있어야 합니다.
- 기본 연결 주소는 `http://localhost:8080` 입니다.

### Electron 개발 실행

```bash
npm install
npm run desktop:dev
```

### Electron 패키징

macOS:

```bash
npm run desktop:dist:mac
```

Windows:

```bash
npm run desktop:dist:win
```

압축 없이 실행 가능한 앱 디렉터리만 확인:

```bash
npm run desktop:pack
```

배포 관련 참고:
- `desktop:dist` 계열 스크립트는 `--publish never`로 설정되어 있어 빌드만 수행합니다.
- GitHub Actions는 `desktop-v*` 태그 푸시 시 macOS/Windows 산출물을 생성하고 artifact로 업로드합니다.

출력 경로:
- `release/`

플랫폼 주의사항:
- macOS 배포 파일은 macOS에서 빌드하는 것이 가장 안전합니다.
- Windows 설치 파일은 Windows 러너 또는 CI에서 빌드하는 것이 안정적입니다.
- 코드 서명과 notarization은 아직 설정하지 않았습니다.
- CI에서 별도 `GH_TOKEN` 없이도 패키징되도록 설정되어 있습니다.

## 테스트 방법

테스트는 H2 인메모리 DB를 사용합니다.  
즉, 로컬 MySQL이 없어도 `mvn test`는 실행됩니다.

테스트 설정 파일:
- [src/test/resources/application.yml](/Users/gimjun-won/Desktop/TMS/src/test/resources/application.yml:1)

실행:

```bash
mvn test
```

현재 확인된 테스트:
- 애플리케이션 컨텍스트 로딩 테스트
- `TestCase` CRUD 전체 통합 테스트
- `TestPlan`/`TestSuite` CRUD 및 관계 통합 테스트
- Electron 테스트 플랜/스위트 사용자 흐름 테스트

최근 확인 결과:
- `Tests run: 17`
- `Failures: 0`
- `Errors: 0`
- `BUILD SUCCESS`

## GitHub Actions

Electron 패키징용 GitHub Actions 워크플로를 추가했습니다.

파일:
- [.github/workflows/electron-build.yml](/Users/gimjun-won/Desktop/TMS/.github/workflows/electron-build.yml:1)

동작:
- 수동 실행 `workflow_dispatch`
- `desktop-v*` 태그 푸시 시 실행
- macOS 빌드
- Windows 빌드
- 결과물을 GitHub Actions artifact로 업로드

## API

기준 경로:
- `/api/testcases`

지원 API:
- `GET /api/testcases`
- `GET /api/testcases/{id}`
- `POST /api/testcases`
- `PUT /api/testcases/{id}`
- `DELETE /api/testcases/{id}`
- `GET /api/test-plans`
- `GET /api/test-plans/{id}`
- `POST /api/test-plans`
- `PUT /api/test-plans/{id}`
- `DELETE /api/test-plans/{id}`
- `GET /api/test-plans/{planId}/suites`
- `GET /api/test-plans/{planId}/suites/{suiteId}`
- `POST /api/test-plans/{planId}/suites`
- `PUT /api/test-plans/{planId}/suites/{suiteId}`
- `DELETE /api/test-plans/{planId}/suites/{suiteId}`
- `GET /api/server-environments`
- `GET /api/server-environments/{id}`
- `POST /api/server-environments`
- `PUT /api/server-environments/{id}`
- `DELETE /api/server-environments/{id}`
- `GET /api/test-configurations`
- `GET /api/test-configurations/{id}`
- `POST /api/test-configurations`
- `PUT /api/test-configurations/{id}`
- `DELETE /api/test-configurations/{id}`

컨트롤러:
- [TestCaseController.java](/Users/gimjun-won/Desktop/TMS/src/main/java/com/tms/testcase/controller/TestCaseController.java:1)

## 요청 예시

### 테스트케이스 생성

```json
{
  "type": "FUNCTIONAL",
  "title": "로그인 성공 테스트",
  "description": "정상 계정으로 로그인 가능해야 한다.",
  "precondition": "가입된 사용자가 존재한다.",
  "steps": "1. 로그인 페이지 접속\n2. 이메일/비밀번호 입력\n3. 로그인 버튼 클릭",
  "notes": "스모크 테스트"
}
```

### 테스트케이스 수정

```json
{
  "type": "NON_FUNCTIONAL",
  "title": "로그인 성능 테스트",
  "description": "부하 상황에서 로그인 응답 시간을 확인한다.",
  "precondition": "성능 테스트 환경이 준비되어 있다.",
  "steps": "1. 동시 로그인 요청 전송\n2. 응답 시간 측정",
  "notes": "성능 기준 재확인 필요"
}
```

## 응답 형식

성공 시 `TestCaseResponse` DTO를 반환합니다.

예시:

```json
{
  "id": 1,
  "type": "FUNCTIONAL",
  "title": "로그인 성공 테스트",
  "description": "정상 계정으로 로그인 가능해야 한다.",
  "precondition": "가입된 사용자가 존재한다.",
  "steps": "1. 로그인 페이지 접속\n2. 이메일/비밀번호 입력\n3. 로그인 버튼 클릭",
  "notes": "스모크 테스트"
}
```

## 예외 처리

전역 예외 처리:
- [GlobalExceptionHandler.java](/Users/gimjun-won/Desktop/TMS/src/main/java/com/tms/global/exception/GlobalExceptionHandler.java:1)

에러 응답 DTO:
- [ErrorResponse.java](/Users/gimjun-won/Desktop/TMS/src/main/java/com/tms/global/response/ErrorResponse.java:1)

현재 처리하는 주요 예외:
- `EntityNotFoundException` -> `404 NOT_FOUND`
- `MethodArgumentNotValidException` -> `400 BAD_REQUEST`

Validation 실패 시 예시:

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

- 프론트엔드 UI는 아직 없음
- Swagger/OpenAPI 미적용
- `project`, `testsuite`, `testrun`, `testresult`는 아직 뼈대만 존재
- 인증/권한 미구현
- 페이징/검색 미구현
- 운영용 DB 마이그레이션 도구 미적용

## 다음 추천 작업

우선순위 추천:
1. `Project` 도메인 구현
2. `TestSuite` 도메인 구현
3. `TestRun` 도메인 구현
4. `TestResult` 도메인 구현
5. 공통 `BaseEntity` 추가
6. Swagger/OpenAPI 추가
7. 검색/필터/페이징 추가
8. 인증/권한 추가
