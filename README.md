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

## 현재 구현 범위

현재 실제로 동작하는 도메인은 `testcase` 입니다.

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

패키지 뼈대만 생성됨:
- `auth`
- `user`
- `project`
- `testsuite`
- `testrun`
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
- `expected`: 필수
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

최근 확인 결과:
- `Tests run: 2`
- `Failures: 0`
- `Errors: 0`
- `BUILD SUCCESS`

## API

기준 경로:
- `/api/testcases`

지원 API:
- `GET /api/testcases`
- `GET /api/testcases/{id}`
- `POST /api/testcases`
- `PUT /api/testcases/{id}`
- `DELETE /api/testcases/{id}`

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
  "expected": "대시보드로 이동한다.",
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
  "expected": "평균 응답 시간이 기준 이내여야 한다.",
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
  "expected": "대시보드로 이동한다.",
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
