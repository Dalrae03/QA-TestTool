package com.tms.execution.service;

import com.tms.execution.dto.AddSuitesToExecutionRequest;
import com.tms.execution.dto.BulkRecordResultRequest;
import com.tms.execution.dto.CreateExecutionRequest;
import com.tms.execution.dto.ExecutionResponse;
import com.tms.execution.dto.RecordResultRequest;
import com.tms.execution.dto.TestCaseExecutionHistoryResponse;
import com.tms.execution.dto.UpdateExecutionEnvironmentRequest;
import com.tms.execution.dto.UpdateExecutionPlanRequest;
import com.tms.execution.dto.UpdateExecutionRequest;
import com.tms.attachment.entity.AttachmentEntityType;
import com.tms.attachment.service.AttachmentService;
import com.tms.audit.entity.AuditAction;
import com.tms.audit.service.AuditLogService;
import com.tms.execution.entity.Execution;
import com.tms.execution.entity.ExecutionItem;
import com.tms.execution.entity.ResultStatus;
import com.tms.execution.repository.ExecutionItemRepository;
import com.tms.execution.repository.ExecutionRepository;
import com.tms.configuration.entity.TestConfiguration;
import com.tms.configuration.repository.TestConfigurationRepository;
import com.tms.global.exception.InvalidRequestException;
import com.tms.global.util.ProjectScope;
import com.tms.testcase.entity.TestCase;
import com.tms.testcase.entity.TestCaseVersion;
import com.tms.testcase.entity.TestFolder;
import com.tms.testcase.repository.TestCaseRepository;
import com.tms.testcase.repository.TestCaseVersionRepository;
import com.tms.testplan.entity.TestPlan;
import com.tms.testplan.repository.TestPlanRepository;
import com.tms.testsuite.entity.TestSuite;
import com.tms.testsuite.repository.TestSuiteRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ExecutionService {

    private final ExecutionRepository executionRepository;
    private final ExecutionItemRepository executionItemRepository;
    private final TestSuiteRepository testSuiteRepository;
    private final TestPlanRepository testPlanRepository;
    private final TestCaseRepository testCaseRepository;
    private final TestCaseVersionRepository testCaseVersionRepository;
    private final TestConfigurationRepository testConfigurationRepository;
    private final AttachmentService attachmentService;
    private final AuditLogService auditLogService;

    public ExecutionService(ExecutionRepository executionRepository, ExecutionItemRepository executionItemRepository,
                             TestSuiteRepository testSuiteRepository,
                             TestPlanRepository testPlanRepository, TestCaseRepository testCaseRepository,
                             TestCaseVersionRepository testCaseVersionRepository,
                             TestConfigurationRepository testConfigurationRepository,
                             AttachmentService attachmentService,
                             AuditLogService auditLogService) {
        this.executionRepository = executionRepository;
        this.executionItemRepository = executionItemRepository;
        this.testSuiteRepository = testSuiteRepository;
        this.testPlanRepository = testPlanRepository;
        this.testCaseRepository = testCaseRepository;
        this.testCaseVersionRepository = testCaseVersionRepository;
        this.testConfigurationRepository = testConfigurationRepository;
        this.auditLogService = auditLogService;
        this.attachmentService = attachmentService;
    }

    /** 테스트케이스 상세 "실행 기록" 탭 — 이 케이스가 테스트런을 통해 실행된 모든 이력(버전 스냅샷 포함). */
    public List<TestCaseExecutionHistoryResponse> getExecutionHistory(Long testCaseId) {
        return executionItemRepository.findAllByTestCaseIdOrderByIdDesc(testCaseId).stream()
                .map(item -> TestCaseExecutionHistoryResponse.from(item, resolveVersion(item)))
                .toList();
    }

    /**
     * 버전 스냅샷 기능 도입 전에 만들어진 런은 ExecutionItem에 버전이 비어 있다 —
     * 그 런이 생성된 시각 기준으로 "당시 현재 버전"이었던 스냅샷을 역추적해 보여준다.
     */
    private TestCaseVersion resolveVersion(ExecutionItem item) {
        if (item.getVersionNumber() != null) return null; // 이미 스냅샷이 있으면 그대로 사용 (item 필드 우선)
        return testCaseVersionRepository
                .findFirstByTestCaseIdAndCreatedAtLessThanEqualOrderByCreatedAtDesc(
                        item.getTestCaseId(), item.getExecution().getCreatedAt())
                .orElse(null);
    }

    /** 런 생성 시점 기준 테스트케이스의 최신 버전 스냅샷 — 없으면(버전 이력이 없는 구버전 케이스) null. */
    private void addItemWithCurrentVersion(Execution execution, TestCase testCase) {
        addItemWithCurrentVersion(execution, testCase, null);
    }

    /** 출처 스위트를 함께 기록하는 버전 — 여러 스위트를 병합한 런에서 항목별로 원래 스위트를 구분하기 위함. */
    private void addItemWithCurrentVersion(Execution execution, TestCase testCase, TestSuite sourceSuite) {
        addItemWithCurrentVersion(execution, testCase,
                sourceSuite != null ? sourceSuite.getId() : null,
                sourceSuite != null ? sourceSuite.getName() : null);
    }

    private void addItemWithCurrentVersion(Execution execution, TestCase testCase, Long sourceSuiteId, String sourceSuiteName) {
        TestCaseVersion latest = testCaseVersionRepository
                .findTopByTestCaseIdOrderByVersionNumberDesc(testCase.getId())
                .orElse(null);
        Integer versionNumber = latest != null ? latest.getVersionNumber() : null;
        String versionLabel = latest != null ? latest.getLabel() : null;
        // 케이스가 현재 속한 폴더(섹션)를 함께 스냅샷 — 런 안에서 폴더별로 묶고, 표시 ID 접두사에 쓰기 위함.
        TestFolder folder = testCase.getFolder();
        Long sourceFolderId = folder != null ? folder.getId() : null;
        String sourceFolderName = folder != null ? folder.getName() : null;
        String sourceFolderCode = folder != null ? folder.effectiveCode() : null;
        execution.addItem(testCase.getId(), testCase.getTitle(), versionNumber, versionLabel,
                sourceSuiteId, sourceSuiteName, sourceFolderId, sourceFolderName, sourceFolderCode);
    }

    public List<ExecutionResponse> getExecutions(Long projectId, String assignee) {
        String assigneeFilter = normalizeOptional(assignee);
        List<Execution> executions;
        if (projectId != null && assigneeFilter != null) {
            executions = executionRepository.findAllByProjectIdAndAssigneeOrderByCreatedAtDesc(projectId, assigneeFilter);
        } else if (projectId != null) {
            executions = executionRepository.findAllByProjectIdOrderByCreatedAtDesc(projectId);
        } else if (assigneeFilter != null) {
            executions = executionRepository.findAllByAssigneeOrderByCreatedAtDesc(assigneeFilter);
        } else {
            executions = executionRepository.findAllByOrderByCreatedAtDesc();
        }
        return executions.stream().map(ExecutionResponse::summary).toList();
    }

    public ExecutionResponse getExecution(Long id) {
        return detailWithDefects(findById(id));
    }

    /**
     * 실행 상세 응답을 만들되, 각 아이템 케이스에 연결된 결함 수를 함께 실어 준다.
     * 프론트에서 실행 그리드 행에 결함 배지를 바로 노출하기 위한 것.
     */
    private ExecutionResponse detailWithDefects(Execution execution) {
        List<Long> caseIds = execution.getItems().stream()
                .map(ExecutionItem::getTestCaseId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Integer> defectCounts = new java.util.HashMap<>();
        if (!caseIds.isEmpty()) {
            for (Object[] row : testCaseRepository.countDefectsByTestCaseIds(caseIds)) {
                defectCounts.put(((Number) row[0]).longValue(), ((Number) row[1]).intValue());
            }
        }
        return ExecutionResponse.detail(execution, defectCounts);
    }

    @Transactional
    public ExecutionResponse createExecution(CreateExecutionRequest request) {
        List<Long> suiteIds = new LinkedHashSet<>(
                Stream.concat(
                        Stream.ofNullable(request.suiteId()),
                        request.suiteIds() == null ? Stream.<Long>empty() : request.suiteIds().stream()
                ).filter(java.util.Objects::nonNull).toList()
        ).stream().toList();
        if (!suiteIds.isEmpty()) {
            return createFromSuites(suiteIds, request);
        }
        return createFromTestCases(request);
    }

    /** 스위트 1개 이상을 선택해 테스트런을 생성 — 여러 개를 선택하면 테스트케이스를 병합(중복 제거)한 테스트런 1개를 만든다. */
    private ExecutionResponse createFromSuites(List<Long> suiteIds, CreateExecutionRequest request) {
        List<TestSuite> suites = testSuiteRepository.findAllById(suiteIds);
        List<Long> foundIds = suites.stream().map(TestSuite::getId).toList();
        List<Long> missingIds = suiteIds.stream().filter(id -> !foundIds.contains(id)).toList();
        if (!missingIds.isEmpty()) {
            throw new EntityNotFoundException("TestSuite not found. id=" + missingIds);
        }
        // 실행의 projectId는 스위트에서 도출한다: request.projectId()가 있으면 그것을,
        // 없으면 선택된 스위트의 프로젝트를 쓴다(프로젝트 미선택 상태로 만든 런이 projectId=null로
        // 저장돼 프로젝트별 목록에서 사라지는 것을 막는다).
        Long effectiveProjectId = request.projectId();
        if (effectiveProjectId == null) {
            effectiveProjectId = suites.stream()
                    .map(TestSuite::getProjectId)
                    .filter(java.util.Objects::nonNull)
                    .findFirst().orElse(null);
        }
        Long scopeId = effectiveProjectId;
        List<Long> mismatched = suites.stream()
                .filter(s -> !ProjectScope.compatible(scopeId, s.getProjectId()))
                .map(TestSuite::getId).toList();
        if (!mismatched.isEmpty()) {
            throw new InvalidRequestException("다른 프로젝트의 스위트로는 테스트런을 만들 수 없습니다: " + mismatched);
        }

        // 같은 케이스가 여러 스위트에 걸쳐 있으면 처음 발견된 스위트를 "출처"로 기록한다.
        Map<Long, TestCase> mergedCasesById = new java.util.LinkedHashMap<>();
        Map<Long, TestSuite> sourceSuiteByCaseId = new java.util.LinkedHashMap<>();
        suites.forEach(s -> s.getTestCases().forEach(tc -> {
            mergedCasesById.putIfAbsent(tc.getId(), tc);
            sourceSuiteByCaseId.putIfAbsent(tc.getId(), s);
        }));
        if (mergedCasesById.isEmpty()) {
            throw new InvalidRequestException("테스트케이스가 없는 스위트로는 테스트런을 만들 수 없습니다.");
        }

        boolean single = suites.size() == 1;
        TestSuite first = suites.get(0);
        String suiteNames = suites.stream().map(TestSuite::getName).collect(Collectors.joining(", "));
        String defaultName = (single ? first.getName() : suiteNames) + " — " + LocalDate.now();
        String name = request.name() != null && !request.name().isBlank() ? request.name().trim() : defaultName;

        // 사용자가 새 테스트런 모달에서 플랜을 명시적으로 골랐으면 그걸 그대로 쓴다.
        // 명시적으로 고르지 않았을 때만 스위트에서 자동으로 유추한다(B안): 여러 스위트가 서로 다른
        // 플랜에 속하면 플랜을 지정하지 않지만, 선택된 스위트가 모두 같은 플랜에 속하면 그 플랜을 연결한다.
        Long planId = null;
        String planName = null;
        if (request.testPlanId() != null) {
            TestPlan plan = testPlanRepository.findById(request.testPlanId())
                    .orElseThrow(() -> new EntityNotFoundException("TestPlan not found. id=" + request.testPlanId()));
            planId = plan.getId();
            planName = plan.getName();
        } else {
            Set<Long> planIds = suites.stream()
                    .map(TestSuite::getTestPlan)
                    .filter(java.util.Objects::nonNull)
                    .map(TestPlan::getId)
                    .collect(Collectors.toSet());
            if (planIds.size() == 1 && suites.stream().allMatch(s -> s.getTestPlan() != null)) {
                planId = first.getTestPlan().getId();
                planName = first.getTestPlan().getName();
            }
        }

        Execution execution = new Execution(
                truncate(name, 200),
                normalizeOptional(request.description()),
                effectiveProjectId,
                planId,
                planName,
                single ? first.getId() : null,
                truncate(single ? first.getName() : suiteNames, 200),
                normalizeOptional(request.assignee()),
                normalizeOptional(request.version())
        );
        applyConfiguration(execution, request.testConfigurationId());
        mergedCasesById.values().forEach(testCase ->
                addItemWithCurrentVersion(execution, testCase, sourceSuiteByCaseId.get(testCase.getId())));
        return saveAndLog(execution);
    }

    /** 스위트 없이 테스트케이스를 직접 선택해 만드는 테스트런 — 임시 스위트를 만들지 않는다. */
    private ExecutionResponse createFromTestCases(CreateExecutionRequest request) {
        List<Long> ids = request.testCaseIds();
        if (ids == null || ids.isEmpty()) {
            throw new InvalidRequestException("스위트 또는 테스트케이스를 선택해야 합니다.");
        }
        List<Long> uniqueIds = new LinkedHashSet<>(ids).stream().toList();
        Map<Long, TestCase> casesById = testCaseRepository.findAllById(uniqueIds).stream()
                .collect(Collectors.toMap(TestCase::getId, Function.identity()));
        List<Long> missingIds = uniqueIds.stream().filter(id -> !casesById.containsKey(id)).toList();
        if (!missingIds.isEmpty()) {
            throw new InvalidRequestException("존재하지 않는 테스트케이스 ID가 포함되어 있습니다: " + missingIds);
        }
        List<Long> mismatched = uniqueIds.stream()
                .map(casesById::get)
                .filter(tc -> !ProjectScope.compatible(request.projectId(), tc.getProjectId()))
                .map(TestCase::getId).toList();
        if (!mismatched.isEmpty()) {
            throw new InvalidRequestException("다른 프로젝트의 테스트케이스로는 테스트런을 만들 수 없습니다: " + mismatched);
        }

        String name = request.name() != null && !request.name().isBlank()
                ? request.name().trim()
                : "직접 선택 테스트런 — " + LocalDate.now();

        Long planId = null;
        String planName = null;
        if (request.testPlanId() != null) {
            TestPlan plan = testPlanRepository.findById(request.testPlanId())
                    .orElseThrow(() -> new EntityNotFoundException("TestPlan not found. id=" + request.testPlanId()));
            planId = plan.getId();
            planName = plan.getName();
        }

        Execution execution = new Execution(
                name,
                normalizeOptional(request.description()),
                request.projectId(),
                planId,
                planName,
                null,
                null,
                normalizeOptional(request.assignee()),
                normalizeOptional(request.version())
        );
        applyConfiguration(execution, request.testConfigurationId());
        uniqueIds.forEach(id -> addItemWithCurrentVersion(execution, casesById.get(id)));
        return saveAndLog(execution);
    }

    /** 요청의 컨피그 id로 실행환경 스냅샷(id + 이름 + 상세 한 줄)을 채운다. id가 null이면 환경 없음. */
    private void applyConfiguration(Execution execution, Long testConfigurationId) {
        if (testConfigurationId == null) {
            execution.updateEnvironment(null, null, null);
            return;
        }
        TestConfiguration config = testConfigurationRepository.findById(testConfigurationId)
                .orElseThrow(() -> new EntityNotFoundException("TestConfiguration not found. id=" + testConfigurationId));
        execution.updateEnvironment(config.getId(), config.getName(), buildEnvironmentDetail(config));
    }

    /** 컨피그의 서버환경·OS·브라우저·기기·런타임(Java)·DB 버전을 한 줄 요약으로 굳힌다. */
    private String buildEnvironmentDetail(TestConfiguration config) {
        List<String> parts = new java.util.ArrayList<>();
        if (config.getServerEnvironment() != null) parts.add(config.getServerEnvironment().getName());
        if (config.getOs() != null) parts.add(config.getOs() + (config.getOsVersion() != null ? " " + config.getOsVersion() : ""));
        if (config.getBrowser() != null) parts.add(config.getBrowser() + (config.getBrowserVersion() != null ? " " + config.getBrowserVersion() : ""));
        if (config.getDevice() != null) parts.add(config.getDevice().toString());
        if (config.getRuntimeVersion() != null) parts.add(config.getRuntimeVersion());
        if (config.getDbVersion() != null) parts.add(config.getDbVersion());
        return parts.isEmpty() ? null : String.join(" · ", parts);
    }

    /**
     * 기존 테스트런을 복제해 회귀 테스트 사이클을 만든다 — 같은 테스트케이스 목록을 가져오되,
     * 각 케이스는 "현재 최신 버전"으로 다시 스냅샷하고 결과는 모두 미실행으로 초기화한다(결과·코멘트·첨부는 복사하지 않음).
     * 회귀는 그동안 수정된 최신 케이스에 대해 다시 도는 것이므로 원본 런의 옛 버전 스냅샷은 따르지 않는다.
     */
    @Transactional
    public ExecutionResponse cloneExecution(Long id) {
        Execution source = findById(id);

        String name = source.getName() + " (복제)";
        if (name.length() > 200) {
            name = name.substring(0, 200);
        }

        Execution clone = new Execution(
                name,
                source.getDescription(),
                source.getProjectId(),
                source.getTestPlanId(),
                source.getPlanName(),
                source.getTestSuiteId(),
                source.getSuiteName(),
                source.getAssignee(),
                source.getVersion()
        );
        clone.updateEnvironment(source.getTestConfigurationId(), source.getConfigurationName(), source.getEnvironmentDetail());
        // 원본 런의 케이스를 현재 최신 버전으로 재스냅샷 — 그 사이 삭제된 케이스는 건너뛴다.
        List<Long> caseIds = source.getItems().stream().map(ExecutionItem::getTestCaseId).toList();
        Map<Long, TestCase> casesById = testCaseRepository.findAllById(caseIds).stream()
                .collect(Collectors.toMap(TestCase::getId, Function.identity()));
        for (ExecutionItem item : source.getItems()) {
            TestCase testCase = casesById.get(item.getTestCaseId());
            if (testCase != null) {
                addItemWithCurrentVersion(clone, testCase, item.getSourceSuiteId(), item.getSourceSuiteName());
            }
        }
        return saveAndLog(clone);
    }

    /** 새 테스트런 저장 + 생성 로그. */
    private ExecutionResponse saveAndLog(Execution execution) {
        Execution saved = executionRepository.save(execution);
        auditLogService.log(AuditLogService.TEST_RUN, saved.getId(), AuditAction.CREATED,
                "테스트런 '" + saved.getName() + "'이(가) 생성되었습니다. (" + saved.getItems().size() + "건)");
        return detailWithDefects(saved);
    }

    @Transactional
    public ExecutionResponse updateExecution(Long id, UpdateExecutionRequest request) {
        Execution execution = findById(id);
        boolean wasCompleted = execution.getStatus().isCompleted();
        execution.update(
                request.name().trim(),
                normalizeOptional(request.description()),
                request.status(),
                normalizeOptional(request.assignee())
        );
        boolean nowCompleted = execution.getStatus().isCompleted();
        // 완료 ↔ 재오픈 전환을 별도 로그로 남긴다.
        if (!wasCompleted && nowCompleted) {
            auditLogService.log(AuditLogService.TEST_RUN, id, AuditAction.COMPLETED,
                    "테스트런 '" + execution.getName() + "'이(가) 완료 처리되었습니다.");
        } else if (wasCompleted && !nowCompleted) {
            auditLogService.log(AuditLogService.TEST_RUN, id, AuditAction.REOPENED,
                    "테스트런 '" + execution.getName() + "'이(가) 다시 열렸습니다.");
        }
        return detailWithDefects(execution);
    }

    @Transactional
    public ExecutionResponse updateExecutionPlan(Long id, UpdateExecutionPlanRequest request) {
        Execution execution = findById(id);
        if (request.testPlanId() == null) {
            execution.updatePlan(null, null);
        } else {
            TestPlan plan = testPlanRepository.findById(request.testPlanId())
                    .orElseThrow(() -> new EntityNotFoundException("TestPlan not found. id=" + request.testPlanId()));
            execution.updatePlan(plan.getId(), plan.getName());
        }
        return detailWithDefects(execution);
    }

    @Transactional
    public ExecutionResponse updateExecutionEnvironment(Long id, UpdateExecutionEnvironmentRequest request) {
        Execution execution = findById(id);
        applyConfiguration(execution, request.testConfigurationId());
        return detailWithDefects(execution);
    }

    /**
     * 이미 만들어진 테스트런에 스위트를 추가한다 — 선택한 스위트들의 테스트케이스를 붙이되,
     * 이미 런에 들어 있는 케이스는 건너뛴다(중복 방지). 새로 붙는 항목은 결과 미실행으로 시작한다.
     */
    @Transactional
    public ExecutionResponse addSuitesToExecution(Long id, AddSuitesToExecutionRequest request) {
        Execution execution = findById(id);
        if (execution.getStatus().isCompleted()) {
            throw new InvalidRequestException("완료된 테스트런에는 스위트를 추가할 수 없습니다. 먼저 다시 열어 주세요.");
        }
        List<Long> suiteIds = new LinkedHashSet<>(
                request.suiteIds().stream().filter(java.util.Objects::nonNull).toList()).stream().toList();
        if (suiteIds.isEmpty()) {
            throw new InvalidRequestException("추가할 스위트를 선택해야 합니다.");
        }

        List<TestSuite> suites = testSuiteRepository.findAllById(suiteIds);
        List<Long> foundIds = suites.stream().map(TestSuite::getId).toList();
        List<Long> missingIds = suiteIds.stream().filter(sid -> !foundIds.contains(sid)).toList();
        if (!missingIds.isEmpty()) {
            throw new EntityNotFoundException("TestSuite not found. id=" + missingIds);
        }
        List<Long> mismatched = suites.stream()
                .filter(s -> !ProjectScope.compatible(execution.getProjectId(), s.getProjectId()))
                .map(TestSuite::getId).toList();
        if (!mismatched.isEmpty()) {
            throw new InvalidRequestException("다른 프로젝트의 스위트는 테스트런에 추가할 수 없습니다: " + mismatched);
        }

        Set<Long> existingCaseIds = execution.getItems().stream()
                .map(ExecutionItem::getTestCaseId)
                .collect(Collectors.toCollection(java.util.HashSet::new));
        int added = 0;
        for (TestSuite suite : suites) {
            for (TestCase testCase : suite.getTestCases()) {
                if (existingCaseIds.add(testCase.getId())) {
                    addItemWithCurrentVersion(execution, testCase, suite);
                    added++;
                }
            }
        }
        if (added == 0) {
            throw new InvalidRequestException("추가할 새 테스트케이스가 없습니다 — 선택한 스위트의 케이스가 이미 모두 포함되어 있습니다.");
        }
        refreshSuiteSnapshot(execution);
        auditLogService.log(AuditLogService.TEST_RUN, id, AuditAction.UPDATED,
                "테스트런 '" + execution.getName() + "'에 스위트 " + suites.size() + "개(" + added + "건)를 추가했습니다.");
        return detailWithDefects(execution);
    }

    /** 테스트런에서 특정 출처 스위트에서 온 항목을 모두 제거한다 — 마지막 남은 스위트는 제거할 수 없다(런을 비우지 않기 위함). */
    @Transactional
    public ExecutionResponse removeSuiteFromExecution(Long id, Long suiteId) {
        Execution execution = findById(id);
        if (execution.getStatus().isCompleted()) {
            throw new InvalidRequestException("완료된 테스트런에서는 스위트를 제거할 수 없습니다. 먼저 다시 열어 주세요.");
        }
        List<ExecutionItem> toRemove = execution.getItems().stream()
                .filter(it -> java.util.Objects.equals(it.getSourceSuiteId(), suiteId))
                .toList();
        if (toRemove.isEmpty()) {
            throw new InvalidRequestException("이 테스트런에는 해당 스위트에서 온 항목이 없습니다.");
        }
        if (toRemove.size() == execution.getItems().size()) {
            throw new InvalidRequestException("마지막 스위트는 제거할 수 없습니다. 테스트런 자체를 삭제해 주세요.");
        }
        String removedSuiteName = toRemove.get(0).getSourceSuiteName();
        // 제거되는 항목에 달린 첨부(실패 증거 등)를 먼저 정리한다 — 고아 파일 방지.
        for (ExecutionItem item : toRemove) {
            attachmentService.deleteAllByEntity(AttachmentEntityType.EXECUTION_ITEM, item.getId());
        }
        execution.getItems().removeAll(toRemove);
        refreshSuiteSnapshot(execution);
        auditLogService.log(AuditLogService.TEST_RUN, id, AuditAction.UPDATED,
                "테스트런 '" + execution.getName() + "'에서 스위트 '"
                        + (removedSuiteName != null ? removedSuiteName : suiteId) + "'(" + toRemove.size() + "건)를 제거했습니다.");
        return detailWithDefects(execution);
    }

    /**
     * 현재 항목들의 출처 스위트를 훑어 런의 스위트 스냅샷(testSuiteId / suiteName)을 다시 맞춘다.
     * 스위트 1개면 그 id·이름을, 여러 개면 이름을 이어 붙이고 id는 비운다. 출처 스위트가 하나도 남지 않으면
     * (케이스를 직접 선택해 만든 런이거나, 붙였던 스위트를 모두 제거한 경우) 스냅샷을 비운다.
     */
    private void refreshSuiteSnapshot(Execution execution) {
        java.util.LinkedHashMap<Long, String> suiteById = new java.util.LinkedHashMap<>();
        for (ExecutionItem item : execution.getItems()) {
            if (item.getSourceSuiteId() != null) {
                suiteById.putIfAbsent(item.getSourceSuiteId(), item.getSourceSuiteName());
            }
        }
        if (suiteById.isEmpty()) {
            execution.updateSuiteSnapshot(null, null);
        } else if (suiteById.size() == 1) {
            Map.Entry<Long, String> only = suiteById.entrySet().iterator().next();
            execution.updateSuiteSnapshot(only.getKey(), truncate(only.getValue(), 200));
        } else {
            String joined = suiteById.values().stream()
                    .map(n -> n != null ? n : "이름 없는 스위트")
                    .collect(Collectors.joining(", "));
            execution.updateSuiteSnapshot(null, truncate(joined, 200));
        }
    }

    @Transactional
    public void deleteExecution(Long id) {
        Execution execution = findById(id);
        // 각 실행 아이템에 달린 첨부파일(실패 증거 등)을 먼저 정리한다 — 고아 파일 방지.
        for (ExecutionItem item : execution.getItems()) {
            attachmentService.deleteAllByEntity(AttachmentEntityType.EXECUTION_ITEM, item.getId());
        }
        executionRepository.delete(execution);
        auditLogService.log(AuditLogService.TEST_RUN, id, AuditAction.DELETED,
                "테스트런 '" + execution.getName() + "'이(가) 삭제되었습니다.");
    }

    @Transactional
    public ExecutionResponse recordResult(Long executionId, Long itemId, RecordResultRequest request) {
        Execution execution = findById(executionId);
        if (execution.getStatus().isCompleted()) {
            throw new InvalidRequestException("완료된 테스트런은 다시 열기 전까지 수정할 수 없습니다.");
        }
        ExecutionItem item = execution.getItems().stream()
                .filter(it -> it.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("ExecutionItem not found. id=" + itemId));
        item.record(request.status(), normalizeOptional(request.comment()), normalizeOptional(request.failureReason()));
        // 첫 실제 결과가 들어오면 '준비됨' 런을 '진행 중'으로 자동 전환 (미실행 되돌리기는 제외).
        if (request.status() != ResultStatus.UNTESTED) {
            execution.startIfReady();
        }
        auditLogService.log(AuditLogService.TEST_RUN, executionId, AuditAction.RESULT_RECORDED,
                "'" + item.getCaseTitle() + "' 결과: " + request.status()
                        + " (런 '" + execution.getName() + "')");
        return detailWithDefects(execution);
    }

    /** 선택한 여러 실행 아이템에 같은 결과를 한 번에 기록한다. */
    @Transactional
    public ExecutionResponse bulkRecordResult(Long executionId, BulkRecordResultRequest request) {
        Execution execution = findById(executionId);
        if (execution.getStatus().isCompleted()) {
            throw new InvalidRequestException("완료된 테스트런은 다시 열기 전까지 수정할 수 없습니다.");
        }
        Set<Long> targetIds = new LinkedHashSet<>(request.itemIds());
        Map<Long, ExecutionItem> itemsById = execution.getItems().stream()
                .collect(Collectors.toMap(ExecutionItem::getId, Function.identity()));
        List<Long> missing = targetIds.stream().filter(id -> !itemsById.containsKey(id)).toList();
        if (!missing.isEmpty()) {
            throw new InvalidRequestException("이 테스트런에 속하지 않은 항목이 포함되어 있습니다: " + missing);
        }

        String comment = normalizeOptional(request.comment());
        String failureReason = normalizeOptional(request.failureReason());
        for (Long id : targetIds) {
            itemsById.get(id).record(request.status(), comment, failureReason);
        }
        // 첫 실제 결과가 들어오면 '준비됨' 런을 '진행 중'으로 자동 전환 (미실행 되돌리기는 제외).
        if (request.status() != ResultStatus.UNTESTED) {
            execution.startIfReady();
        }
        auditLogService.log(AuditLogService.TEST_RUN, executionId, AuditAction.RESULT_RECORDED,
                "일괄 결과 기록: " + targetIds.size() + "건 → " + request.status()
                        + " (런 '" + execution.getName() + "')");
        return detailWithDefects(execution);
    }

    private Execution findById(Long id) {
        return executionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("TestRun not found. id=" + id));
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    /** 컬럼 길이를 초과하는 문자열을 잘라 INSERT 실패(Data too long)를 방지한다. */
    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, maxLength);
    }
}
