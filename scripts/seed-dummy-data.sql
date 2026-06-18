-- TMS 더미 데이터 시드 스크립트
-- 실행: mysql -u root -p1234 tms < scripts/seed-dummy-data.sql

SET NAMES utf8mb4;

-- =====================
-- 1. 서버 환경 (ServerEnvironment)
-- =====================
INSERT INTO server_environments (name, type, base_url, description, active, created_at, updated_at) VALUES
('로컬 개발 서버', 'LOCAL',       'http://localhost:8080',           '개발자 로컬 환경', true,  NOW(), NOW()),
('개발 서버 (DEV)', 'DEVELOPMENT', 'https://dev-api.example.com',    '팀 공용 개발 환경', true,  NOW(), NOW()),
('스테이징 서버',   'STAGING',     'https://staging-api.example.com', '출시 전 검증 환경', true,  NOW(), NOW()),
('프로덕션 서버',   'PRODUCTION',  'https://api.example.com',         '실서비스 환경',    false, NOW(), NOW());

-- =====================
-- 2. 테스트 구성 (TestConfiguration)
-- =====================
INSERT INTO test_configurations (name, server_environment_id, os, os_version, browser, browser_version, device, runtime_version, db_version, active, created_at, updated_at) VALUES
('Chrome / Windows / DEV',  2, 'WINDOWS', '11',    'CHROME',  '123.0', 'DESKTOP', 'Java 21', 'MySQL 8.0', true,  NOW(), NOW()),
('Safari / macOS / LOCAL',  1, 'MAC',     '14',    'SAFARI',  '17.0',  'DESKTOP', 'Java 21', 'MySQL 8.0', true,  NOW(), NOW()),
('Chrome / Android / STG',  3, 'ANDROID', '14',    'CHROME',  '123.0', 'MOBILE',  'Java 21', 'MySQL 8.0', true,  NOW(), NOW()),
('Firefox / Linux / DEV',   2, 'LINUX',   'Ubuntu 22.04', 'FIREFOX', '124.0', 'DESKTOP', 'Java 21', 'MySQL 8.0', false, NOW(), NOW());

-- =====================
-- 3. 영역 태그 (AreaTag)
-- =====================
INSERT INTO area_tags (name) VALUES
('로그인/인증'),
('회원가입'),
('결제'),
('장바구니'),
('상품 목록'),
('검색'),
('마이페이지'),
('관리자');

-- =====================
-- 4. 테스트 케이스 (TestCase)
-- =====================
INSERT INTO test_cases (type, priority, status, title, version, description, precondition, steps, expected, notes, os, browser, device, assignee, server_environment_id, test_configuration_id, created_at, updated_at) VALUES
('FUNCTIONAL',     'HIGH',   'READY',         '이메일/비밀번호 로그인 성공',        'v1.0',
 '유효한 이메일과 비밀번호로 로그인 시 홈 화면으로 이동한다.',
 '가입된 계정(test@example.com / Pass1234!) 존재',
 '1. 로그인 페이지 접속\n2. 이메일 입력: test@example.com\n3. 비밀번호 입력: Pass1234!\n4. 로그인 버튼 클릭',
 '홈 화면으로 리다이렉트, 사용자 이름 표시',
 'SSO 환경에서는 별도 검토 필요',
 'WINDOWS', 'CHROME', 'DESKTOP', '김준원', 2, 1, NOW(), NOW()),

('FUNCTIONAL',     'HIGH',   'READY',         '잘못된 비밀번호 로그인 실패',         'v1.0',
 '잘못된 비밀번호 입력 시 오류 메시지가 표시된다.',
 '가입된 계정 존재',
 '1. 로그인 페이지 접속\n2. 이메일 입력: test@example.com\n3. 비밀번호 입력: WrongPass!\n4. 로그인 버튼 클릭',
 '"이메일 또는 비밀번호가 올바르지 않습니다." 메시지 표시, 페이지 유지',
 NULL,
 'WINDOWS', 'CHROME', 'DESKTOP', '김준원', 2, 1, NOW(), NOW()),

('FUNCTIONAL',     'MEDIUM', 'REVIEW_NEEDED', '비밀번호 5회 오입력 시 계정 잠금',    'v1.1',
 '비밀번호를 5회 연속 틀리면 계정이 30분 잠긴다.',
 '가입된 계정 존재, 이전 잠금 이력 없음',
 '1. 잘못된 비밀번호로 5회 로그인 시도\n2. 6번째 로그인 시도',
 '"계정이 잠겼습니다. 30분 후 다시 시도하세요." 메시지 표시',
 '잠금 해제 API 정책 확인 필요',
 'WINDOWS', 'CHROME', 'DESKTOP', '이예진', 2, 1, NOW(), NOW()),

('FUNCTIONAL',     'HIGH',   'READY',         '신규 회원가입 성공',                  'v1.0',
 '유효한 정보로 회원가입 완료 후 이메일 인증 메일 발송.',
 '가입 이력 없는 이메일 준비',
 '1. 회원가입 페이지 접속\n2. 이름, 이메일, 비밀번호 입력\n3. 가입하기 버튼 클릭',
 '"인증 메일을 전송했습니다." 안내, DB에 PENDING 상태로 저장',
 NULL,
 'MAC', 'SAFARI', 'DESKTOP', '박지민', 1, 2, NOW(), NOW()),

('FUNCTIONAL',     'HIGH',   'READY',         '중복 이메일 회원가입 시도',            'v1.0',
 '이미 존재하는 이메일로 가입 시 오류 반환.',
 '해당 이메일 계정 이미 가입됨',
 '1. 회원가입 페이지 접속\n2. 기존 이메일 입력\n3. 가입하기 버튼 클릭',
 '"이미 사용 중인 이메일입니다." 오류 메시지, HTTP 409',
 NULL,
 'MAC', 'SAFARI', 'DESKTOP', '박지민', 1, 2, NOW(), NOW()),

('FUNCTIONAL',     'HIGH',   'READY',         '장바구니 상품 추가',                  'v2.0',
 '상품 상세 페이지에서 장바구니 추가 버튼 클릭 시 장바구니에 반영.',
 '로그인 상태, 재고 있는 상품',
 '1. 상품 목록 페이지 접속\n2. 상품 클릭\n3. 수량 선택 (2개)\n4. 장바구니 담기 클릭',
 '장바구니 아이콘 배지 +2, 장바구니 목록에 상품 표시',
 NULL,
 'WINDOWS', 'CHROME', 'DESKTOP', '최민수', 2, 1, NOW(), NOW()),

('FUNCTIONAL',     'HIGH',   'READY',         '신용카드 결제 성공',                  'v2.0',
 '유효한 신용카드로 결제 완료 시 주문 확인 페이지 이동.',
 '장바구니에 상품 있음, 로그인 상태',
 '1. 장바구니 → 결제하기\n2. 배송지 입력\n3. 카드 정보 입력 (테스트카드)\n4. 결제 버튼 클릭',
 '주문 완료 화면, 주문번호 표시, 결제 완료 이메일 발송',
 '테스트 카드: 4111-1111-1111-1111, 만료 12/26, CVC 123',
 'WINDOWS', 'CHROME', 'DESKTOP', '최민수', 3, 1, NOW(), NOW()),

('FUNCTIONAL',     'MEDIUM', 'DRAFT',         '모바일 장바구니 담기',                'v2.0',
 '모바일 웹에서 장바구니 담기 기능 정상 동작 확인.',
 '모바일 Chrome, 로그인 상태',
 '1. 모바일 브라우저로 상품 페이지 접속\n2. 장바구니 담기 버튼 탭',
 '장바구니 추가 성공 토스트 메시지, 배지 증가',
 NULL,
 'ANDROID', 'CHROME', 'MOBILE', '이예진', 3, 3, NOW(), NOW()),

('FUNCTIONAL',     'LOW',    'READY',         '상품 키워드 검색',                    'v1.5',
 '검색창에 키워드 입력 시 관련 상품 목록 반환.',
 '상품 데이터 존재',
 '1. 검색창 클릭\n2. "무선 이어폰" 입력\n3. 엔터 또는 검색 버튼 클릭',
 '키워드 포함 상품 목록 표시, 결과 없을 경우 안내 메시지',
 NULL,
 'WINDOWS', 'FIREFOX', 'DESKTOP', '김준원', 2, 4, NOW(), NOW()),

('NON_FUNCTIONAL', 'MEDIUM', 'REVIEW_NEEDED', '로그인 API 응답 시간 ≤ 200ms',       'v1.0',
 '정상 로그인 요청의 P95 응답 시간이 200ms 이하여야 한다.',
 '부하 테스트 환경 구성 완료',
 '1. 동시 100 VU로 로그인 요청 1분 지속\n2. P95 응답 시간 측정',
 'P95 ≤ 200ms, 에러율 0%',
 'k6 스크립트 준비 필요',
 'LINUX', NULL, 'DESKTOP', '박지민', 3, NULL, NOW(), NOW());

-- =====================
-- 5. 영역 태그 연결
-- =====================
INSERT INTO test_case_area_tags (test_case_id, area_tag_id) VALUES
(1, 1), -- 로그인 성공 → 로그인/인증
(2, 1), -- 로그인 실패 → 로그인/인증
(3, 1), -- 계정 잠금 → 로그인/인증
(4, 2), -- 회원가입 성공 → 회원가입
(5, 2), -- 중복 이메일 → 회원가입
(6, 4), -- 장바구니 추가 → 장바구니
(6, 5), -- 장바구니 추가 → 상품 목록
(7, 3), -- 신용카드 결제 → 결제
(7, 4), -- 신용카드 결제 → 장바구니
(8, 4), -- 모바일 장바구니 → 장바구니
(9, 6), -- 검색 → 검색
(10,1); -- 응답시간 → 로그인/인증

-- =====================
-- 6. 결함 (Defect)
-- =====================
INSERT INTO defects (title, description, severity, status, external_url, jira_key, created_at, updated_at) VALUES
('로그인 후 리다이렉트 URL 손실',        '로그인 성공 후 원래 접근하려던 URL로 이동하지 않고 항상 홈으로 이동함.',             'MAJOR',    'OPEN',        NULL,                               NULL,     NOW(), NOW()),
('모바일 장바구니 버튼 미반응 (iOS)',     'iPhone 15 / Safari에서 장바구니 담기 버튼 탭 후 아무 반응 없음.',                   'CRITICAL', 'IN_PROGRESS', NULL,                               NULL,     NOW(), NOW()),
('결제 완료 이메일 발송 지연 (5분 이상)', '결제 완료 후 확인 이메일이 5분 이상 지연 도착하거나 미도착.',                       'MAJOR',    'IN_PROGRESS', NULL,                               NULL,     NOW(), NOW()),
('검색 결과 특수문자 XSS 취약점',        '검색어에 <script>alert(1)</script> 입력 시 스크립트 실행됨.',                        'CRITICAL', 'OPEN',        NULL,                               NULL,     NOW(), NOW()),
('마이페이지 주문 목록 페이지네이션 오류', '2페이지 이상 이동 시 동일한 1페이지 데이터 반복 표시.',                             'MINOR',    'RESOLVED',    NULL,                               NULL,     NOW(), NOW()),
('회원가입 비밀번호 강도 표시 미작동',    '비밀번호 입력 시 강도 미터가 항상 "약함"으로 표시됨.',                              'TRIVIAL',  'CLOSED',      NULL,                               NULL,     NOW(), NOW());

-- 테스트 케이스 ↔ 결함 연결
INSERT INTO test_case_defects (test_case_id, defect_id) VALUES
(1, 1), -- 로그인 성공 TC ↔ 리다이렉트 버그
(2, 1), -- 로그인 실패 TC ↔ 리다이렉트 버그
(6, 2), -- 장바구니 TC ↔ 모바일 버튼 버그
(7, 3), -- 결제 TC ↔ 이메일 지연 버그
(9, 4); -- 검색 TC ↔ XSS 버그

-- =====================
-- 7. 테스트 플랜 (TestPlan)
-- =====================
INSERT INTO test_plans (name, description, status, assignee, start_date, end_date, created_at, updated_at) VALUES
('v1.0 로그인/회원가입 검증',   '로그인, 회원가입, 계정 잠금 기능 전체 검증 플랜.',                   'ACTIVE',    '김준원', '2026-06-01', '2026-06-20', NOW(), NOW()),
('v2.0 커머스 기능 검증',       '장바구니, 결제, 검색 등 커머스 핵심 기능 검증.',                     'DRAFT',     '이예진', '2026-06-15', '2026-07-10', NOW(), NOW()),
('v1.5 성능 테스트',            '주요 API 응답 시간 및 동시 접속 부하 테스트.',                       'DRAFT',     '박지민', '2026-07-01', '2026-07-15', NOW(), NOW()),
('Q2 회귀 테스트',              '분기별 전체 기능 회귀 테스트.',                                       'COMPLETED', '최민수', '2026-04-01', '2026-06-10', NOW(), NOW());

-- =====================
-- 8. 테스트 스위트 (TestSuite)
-- =====================
INSERT INTO test_suites (test_plan_id, name, description, created_at, updated_at) VALUES
(1, '로그인 기능 스위트',     '이메일/비밀번호 로그인, 실패 케이스, 계정 잠금 포함.', NOW(), NOW()),
(1, '회원가입 기능 스위트',   '신규 가입, 중복 이메일 검증 포함.',                   NOW(), NOW()),
(2, '장바구니 스위트',        '데스크탑 및 모바일 장바구니 시나리오.',                NOW(), NOW()),
(2, '결제 스위트',            '신용카드 결제 성공/실패 시나리오.',                    NOW(), NOW()),
(3, '성능/부하 스위트',       '응답 시간, 동시 접속 테스트 케이스 모음.',             NOW(), NOW());

-- 스위트 ↔ 테스트 케이스 연결 (case_order 포함)
INSERT INTO test_suite_cases (test_suite_id, test_case_id, case_order) VALUES
(1, 1, 0), -- 로그인 스위트 → 로그인 성공
(1, 2, 1), -- 로그인 스위트 → 로그인 실패
(1, 3, 2), -- 로그인 스위트 → 계정 잠금
(2, 4, 0), -- 회원가입 스위트 → 가입 성공
(2, 5, 1), -- 회원가입 스위트 → 중복 이메일
(3, 6, 0), -- 장바구니 스위트 → 데스크탑
(3, 8, 1), -- 장바구니 스위트 → 모바일
(4, 7, 0), -- 결제 스위트 → 신용카드 결제
(5,10, 0); -- 성능 스위트 → 로그인 API 응답

-- =====================
-- 9. 테스트 실행 (TestRun)
-- =====================
INSERT INTO test_runs (test_case_id, status, actual_result, notes, assignee, executed_at) VALUES
(1, 'PASSED',  '정상적으로 홈 화면으로 이동함.',                        NULL,                       '김준원', '2026-06-10 10:00:00'),
(2, 'PASSED',  '오류 메시지 정상 표시.',                               NULL,                       '김준원', '2026-06-10 10:15:00'),
(3, 'FAILED',  '5회 오입력 후에도 로그인 가능함. 잠금 미동작.',         '개발팀에 버그 리포트 전달', '이예진', '2026-06-10 11:00:00'),
(4, 'PASSED',  '인증 메일 정상 발송.',                                  NULL,                       '박지민', '2026-06-11 09:00:00'),
(5, 'PASSED',  '중복 이메일 오류 메시지 정상.',                         NULL,                       '박지민', '2026-06-11 09:20:00'),
(6, 'PASSED',  '장바구니 배지 정상 증가.',                              NULL,                       '최민수', '2026-06-12 14:00:00'),
(7, 'PASSED',  '결제 완료 페이지 정상, 이메일은 7분 지연 도착.',         '이메일 지연 결함 확인됨',  '최민수', '2026-06-12 14:30:00'),
(8, 'BLOCKED', '테스트 기기 미준비로 실행 불가.',                        '기기 확보 후 재실행 필요', '이예진', '2026-06-13 10:00:00'),
(9, 'PASSED',  '검색 결과 정상 반환, XSS는 별도 보안 테스트 케이스로.',  NULL,                       '김준원', '2026-06-13 11:00:00'),
(1, 'PASSED',  '재실행 - 동일 결과.',                                   NULL,                       '김준원', '2026-06-14 09:00:00');
