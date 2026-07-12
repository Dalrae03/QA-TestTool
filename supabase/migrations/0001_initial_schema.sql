-- TMS 초기 스키마 — 기존 Spring Boot(JPA/MySQL) 데이터 모델을 Supabase(PostgreSQL)로 이관.
-- 열거형은 text + CHECK 로 표현한다. 기존 데이터 이관 중 CHECK 위반이 나면 해당 값 세트를 보완할 것.
-- Supabase SQL Editor 또는 `supabase db push` 로 적용.

-- ── updated_at 자동 갱신 트리거 ───────────────────────────────────
create or replace function set_updated_at()
returns trigger as $$
begin
  new.updated_at = now();
  return new;
end;
$$ language plpgsql;

-- ── 프로젝트 ───────────────────────────────────────────────────────
create table projects (
    id          bigint generated always as identity primary key,
    name        text not null,
    description text,
    owner       text,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now()
);
create trigger trg_projects_updated before update on projects for each row execute function set_updated_at();

-- ── 폴더(자기참조 트리) ───────────────────────────────────────────
create table test_folders (
    id         bigint generated always as identity primary key,
    name       text not null,
    parent_id  bigint references test_folders(id) on delete cascade,
    project_id bigint references projects(id) on delete cascade,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index idx_test_folders_parent on test_folders(parent_id);
create trigger trg_test_folders_updated before update on test_folders for each row execute function set_updated_at();

-- ── 영역 태그 ──────────────────────────────────────────────────────
create table area_tags (
    id         bigint generated always as identity primary key,
    name       text not null,
    project_id bigint references projects(id) on delete cascade
);

-- ── 서버 환경 ──────────────────────────────────────────────────────
create table server_environments (
    id          bigint generated always as identity primary key,
    name        text not null unique,
    type        text not null check (type in ('LOCAL','DEVELOPMENT','STAGING','PRODUCTION')),
    base_url    text not null,
    description text,
    active      boolean not null default true,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now()
);
create trigger trg_server_environments_updated before update on server_environments for each row execute function set_updated_at();

-- ── 테스트 컨피그(환경 조합) ──────────────────────────────────────
create table test_configurations (
    id                     bigint generated always as identity primary key,
    name                   text not null unique,
    server_environment_id  bigint references server_environments(id) on delete set null,
    os                     text check (os in ('MAC','WINDOWS','LINUX','IOS','ANDROID')),
    os_version             text,
    browser                text check (browser in ('CHROME','FIREFOX','SAFARI','EDGE','SAMSUNG_INTERNET','NONE')),
    browser_version        text,
    device                 text check (device in ('DESKTOP','MOBILE','TABLET')),
    runtime_version        text,
    db_version             text,
    active                 boolean not null default true,
    created_at             timestamptz not null default now(),
    updated_at             timestamptz not null default now()
);
create trigger trg_test_configurations_updated before update on test_configurations for each row execute function set_updated_at();

-- ── 결함 ───────────────────────────────────────────────────────────
create table defects (
    id           bigint generated always as identity primary key,
    title        text not null,
    description  text,
    severity     text not null check (severity in ('CRITICAL','MAJOR','MINOR','TRIVIAL')),
    status       text not null check (status in ('OPEN','IN_PROGRESS','RESOLVED','CLOSED')),
    external_url text,
    jira_key     text,
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now()
);
create trigger trg_defects_updated before update on defects for each row execute function set_updated_at();

-- ── 테스트케이스 ───────────────────────────────────────────────────
create table test_cases (
    id                     bigint generated always as identity primary key,
    type                   text not null check (type in ('FUNCTIONAL','NON_FUNCTIONAL')),
    priority               text not null check (priority in ('HIGH','MEDIUM','LOW')),
    status                 text not null check (status in ('DRAFT','REVIEW_NEEDED','READY','COMPLETED')),
    title                  text not null,
    version                text,
    description            text not null default '',
    precondition           text not null default '',
    steps                  text not null default '',
    expected               text not null default '',
    notes                  text,
    os                     text check (os in ('MAC','WINDOWS','LINUX','IOS','ANDROID')),
    browser                text check (browser in ('CHROME','FIREFOX','SAFARI','EDGE','SAMSUNG_INTERNET','NONE')),
    device                 text check (device in ('DESKTOP','MOBILE','TABLET')),
    assignee               text,
    folder_id              bigint references test_folders(id) on delete set null,
    server_environment_id  bigint references server_environments(id) on delete set null,
    test_configuration_id  bigint references test_configurations(id) on delete set null,
    project_id             bigint references projects(id) on delete cascade,
    created_at             timestamptz not null default now(),
    updated_at             timestamptz not null default now()
);
create index idx_test_cases_folder on test_cases(folder_id);
create index idx_test_cases_project on test_cases(project_id);
create trigger trg_test_cases_updated before update on test_cases for each row execute function set_updated_at();

-- 테스트케이스 ↔ 영역 태그 (M:N)
create table test_case_area_tags (
    test_case_id bigint not null references test_cases(id) on delete cascade,
    area_tag_id  bigint not null references area_tags(id) on delete cascade,
    primary key (test_case_id, area_tag_id)
);

-- 테스트케이스 ↔ 결함 (M:N)
create table test_case_defects (
    test_case_id bigint not null references test_cases(id) on delete cascade,
    defect_id    bigint not null references defects(id) on delete cascade,
    primary key (test_case_id, defect_id)
);

-- 테스트케이스 → Jira 요구사항 key (요소 컬렉션)
create table test_case_jira_requirements (
    test_case_id bigint not null references test_cases(id) on delete cascade,
    jira_key     text not null,
    primary key (test_case_id, jira_key)
);

-- ── 테스트케이스 버전 스냅샷 ──────────────────────────────────────
create table test_case_versions (
    id             bigint generated always as identity primary key,
    test_case_id   bigint not null references test_cases(id) on delete cascade,
    version_number integer not null,
    label          text not null,
    change_summary text,
    type           text not null,
    priority       text not null,
    status         text not null,
    title          text not null,
    version        text,
    description    text not null default '',
    precondition   text not null default '',
    steps          text not null default '',
    expected_result text,
    notes          text,
    os             text,
    browser        text,
    device         text,
    assignee       text,
    folder_id      bigint,
    folder_name    text,
    created_at     timestamptz not null default now()
);
create index idx_tc_versions_case on test_case_versions(test_case_id);

-- ── 테스트 플랜 ────────────────────────────────────────────────────
create table test_plans (
    id                      bigint generated always as identity primary key,
    name                    text not null,
    status                  text not null check (status in ('DRAFT','IN_REVIEW','APPROVED','IN_PROGRESS','COMPLETED','ON_HOLD')),
    assignee                text,
    start_date              date,
    end_date                date,
    target_system           text,
    target_version          text,
    test_goal               text,
    test_target             text,
    impact_scope            text,
    common_scope            text,
    priority_targets        text,
    risk_analysis           text,
    test_approach           text,
    test_perspective        text,
    entry_criteria          text,
    exit_criteria           text,
    server_environment_note text,
    device_matrix           text,
    test_data               text,
    schedule                text,
    deliverables            text,
    project_id              bigint references projects(id) on delete cascade,
    created_at              timestamptz not null default now(),
    updated_at              timestamptz not null default now()
);
create trigger trg_test_plans_updated before update on test_plans for each row execute function set_updated_at();

-- 플랜 ↔ 핵심 테스트케이스 (M:N)
create table test_plan_core_test_cases (
    test_plan_id bigint not null references test_plans(id) on delete cascade,
    test_case_id bigint not null references test_cases(id) on delete cascade,
    primary key (test_plan_id, test_case_id)
);

-- ── 테스트 스위트 ──────────────────────────────────────────────────
create table test_suites (
    id           bigint generated always as identity primary key,
    test_plan_id bigint references test_plans(id) on delete set null,
    name         text not null,
    description  text,
    project_id   bigint references projects(id) on delete cascade,
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now()
);
create trigger trg_test_suites_updated before update on test_suites for each row execute function set_updated_at();

-- 스위트 ↔ 테스트케이스 (M:N, 순서 보존)
create table test_suite_test_cases (
    test_suite_id bigint not null references test_suites(id) on delete cascade,
    test_case_id  bigint not null references test_cases(id) on delete cascade,
    position      integer,
    primary key (test_suite_id, test_case_id)
);

-- ── 단건 실행 기록(TestRun, 레거시/케이스별 이력) ────────────────
create table test_runs (
    id             bigint generated always as identity primary key,
    test_case_id   bigint not null references test_cases(id) on delete cascade,
    status         text not null check (status in ('UNTESTED','NOT_EXECUTED','IN_PROGRESS','PASSED','FAILED','BLOCKED','RETEST')),
    actual_result  text not null default '',
    notes          text,
    assignee       text,
    failure_reason text,
    executed_at    timestamptz not null default now()
);
create index idx_test_runs_case on test_runs(test_case_id);

-- ── 실행 사이클(Execution) ────────────────────────────────────────
create table executions (
    id                     bigint generated always as identity primary key,
    name                   text not null,
    version                text,
    description            text,
    project_id             bigint references projects(id) on delete cascade,
    test_plan_id           bigint,   -- 출처 스냅샷(FK 미사용): 플랜 삭제돼도 런 보존
    plan_name              text,
    test_suite_id          bigint,
    suite_name             text,
    test_configuration_id  bigint,
    configuration_name     text,
    environment_detail     text,
    status                 text not null check (status in ('IN_PROGRESS','COMPLETED')),
    assignee               text,
    created_at             timestamptz not null default now(),
    updated_at             timestamptz not null default now(),
    completed_at           timestamptz
);
create trigger trg_executions_updated before update on executions for each row execute function set_updated_at();

-- 실행 항목
create table execution_items (
    id                bigint generated always as identity primary key,
    execution_id      bigint not null references executions(id) on delete cascade,
    item_order        integer,
    test_case_id      bigint not null,
    case_title        text not null,
    version_number    integer,
    version_label     text,
    source_suite_id   bigint,
    source_suite_name text,
    status            text not null check (status in ('UNTESTED','NOT_EXECUTED','IN_PROGRESS','PASSED','FAILED','BLOCKED','RETEST')),
    comment           text,
    failure_reason    text,
    executed_at       timestamptz
);
create index idx_execution_items_exec on execution_items(execution_id);

-- 실행 항목 이력(재시도 흐름)
create table execution_item_history (
    id                bigint generated always as identity primary key,
    execution_item_id bigint not null references execution_items(id) on delete cascade,
    status            text not null,
    comment           text,
    failure_reason    text,
    recorded_at       timestamptz not null default now()
);
create index idx_exec_item_history_item on execution_item_history(execution_item_id);

-- ── 첨부파일(메타데이터; 실제 파일은 Supabase Storage) ────────────
create table attachments (
    id                bigint generated always as identity primary key,
    entity_type       text not null check (entity_type in ('TEST_CASE','DEFECT','TEST_RUN','EXECUTION_ITEM')),
    entity_id         bigint not null,
    original_filename text not null,
    stored_filename   text not null,
    content_type      text not null,
    file_size         bigint not null,
    created_at        timestamptz not null default now()
);
create index idx_attachments_entity on attachments(entity_type, entity_id);

-- ── 감사 로그 ──────────────────────────────────────────────────────
create table audit_logs (
    id          bigint generated always as identity primary key,
    entity_type text not null,
    entity_id   bigint not null,
    action      text not null,
    field_name  text not null,
    old_value   text,
    new_value   text,
    summary     text not null,
    actor       text,
    created_at  timestamptz not null default now()
);
create index idx_audit_logs_entity on audit_logs(entity_type, entity_id);

-- ── 사용자(앱 내 담당자/역할; Supabase Auth와는 별개) ─────────────
create table users (
    id         bigint generated always as identity primary key,
    name       text not null,
    email      text unique,
    role       text not null check (role in ('ADMIN','QA_LEAD','QA','DEVELOPER','VIEWER')),
    active     boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create trigger trg_users_updated before update on users for each row execute function set_updated_at();

-- ── Jira 연동 설정(싱글턴) ────────────────────────────────────────
create table jira_settings (
    id           bigint generated always as identity primary key,
    base_url     text,
    email        text,
    api_token    text,
    project_key  text,
    web_base_url text,
    enabled      boolean not null default true,
    updated_at   timestamptz not null default now()
);
create trigger trg_jira_settings_updated before update on jira_settings for each row execute function set_updated_at();
