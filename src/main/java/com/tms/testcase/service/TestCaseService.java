package com.tms.testcase.service;

import com.tms.audit.entity.AuditAction;
import com.tms.audit.service.AuditLogService;
import com.tms.attachment.entity.AttachmentEntityType;
import com.tms.attachment.service.AttachmentService;
import com.tms.configuration.entity.TestConfiguration;
import com.tms.configuration.repository.TestConfigurationRepository;
import com.tms.defect.entity.Defect;
import com.tms.defect.service.DefectService;
import com.tms.environment.entity.ServerEnvironment;
import com.tms.environment.repository.ServerEnvironmentRepository;
import com.tms.global.exception.InvalidRequestException;
import com.tms.global.util.ProjectScope;
import com.tms.testcase.dto.CreateTestCaseRequest;
import com.tms.testcase.dto.TestCaseResponse;
import com.tms.testcase.dto.TestCaseVersionResponse;
import com.tms.testcase.dto.UpdateTestCaseRequest;
import com.tms.testcase.dto.UpdateTestCaseStatusRequest;
import com.tms.testcase.entity.AreaTag;
import com.tms.testcase.entity.TestCase;
import com.tms.testcase.entity.TestCaseBrowser;
import com.tms.testcase.entity.TestCaseDevice;
import com.tms.testcase.entity.TestCaseOs;
import com.tms.testcase.entity.TestCasePriority;
import com.tms.testcase.entity.TestCaseStatus;
import com.tms.testcase.entity.TestCaseType;
import com.tms.testcase.entity.TestCaseVersion;
import com.tms.testcase.entity.TestFolder;
import com.tms.testcase.repository.AreaTagRepository;
import com.tms.testcase.repository.TestCaseRepository;
import com.tms.testcase.repository.TestCaseSpecification;
import com.tms.testcase.repository.TestCaseVersionRepository;
import com.tms.testcase.repository.TestFolderRepository;
import com.tms.testrun.repository.TestRunRepository;
import com.tms.testsuite.service.TestSuiteService;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TestCaseService {

    private final TestCaseRepository testCaseRepository;
    private final AreaTagRepository areaTagRepository;
    private final TestSuiteService testSuiteService;
    private final ServerEnvironmentRepository serverEnvironmentRepository;
    private final TestConfigurationRepository testConfigurationRepository;
    private final DefectService defectService;
    private final AttachmentService attachmentService;
    private final TestFolderRepository testFolderRepository;
    private final TestRunRepository testRunRepository;
    private final AuditLogService auditLogService;
    private final TestCaseVersionRepository testCaseVersionRepository;

    public TestCaseService(
            TestCaseRepository testCaseRepository,
            AreaTagRepository areaTagRepository,
            TestSuiteService testSuiteService,
            ServerEnvironmentRepository serverEnvironmentRepository,
            TestConfigurationRepository testConfigurationRepository,
            DefectService defectService,
            AttachmentService attachmentService,
            TestFolderRepository testFolderRepository,
            TestRunRepository testRunRepository,
            AuditLogService auditLogService,
            TestCaseVersionRepository testCaseVersionRepository
    ) {
        this.testCaseRepository = testCaseRepository;
        this.areaTagRepository = areaTagRepository;
        this.testSuiteService = testSuiteService;
        this.serverEnvironmentRepository = serverEnvironmentRepository;
        this.testConfigurationRepository = testConfigurationRepository;
        this.defectService = defectService;
        this.attachmentService = attachmentService;
        this.testFolderRepository = testFolderRepository;
        this.testRunRepository = testRunRepository;
        this.auditLogService = auditLogService;
        this.testCaseVersionRepository = testCaseVersionRepository;
    }

    public List<TestCaseResponse> getAllTestCases(
            TestCaseType type,
            TestCasePriority priority,
            TestCaseStatus status,
            TestCaseOs os,
            TestCaseBrowser browser,
            TestCaseDevice device,
            Long areaTagId,
            String keyword,
            Long folderId,
            Long projectId
    ) {
        Specification<TestCase> spec = (root, query, cb) -> cb.conjunction();
        if (projectId != null) spec = spec.and(TestCaseSpecification.hasProjectId(projectId));
        if (type != null) spec = spec.and(TestCaseSpecification.hasType(type));
        if (priority != null) spec = spec.and(TestCaseSpecification.hasPriority(priority));
        if (status != null) spec = spec.and(TestCaseSpecification.hasStatus(status));
        if (os != null) spec = spec.and(TestCaseSpecification.hasOs(os));
        if (browser != null) spec = spec.and(TestCaseSpecification.hasBrowser(browser));
        if (device != null) spec = spec.and(TestCaseSpecification.hasDevice(device));
        if (areaTagId != null) spec = spec.and(TestCaseSpecification.hasAreaTag(areaTagId));
        if (keyword != null && !keyword.isBlank()) spec = spec.and(TestCaseSpecification.containsKeyword(keyword));
        if (folderId != null) spec = spec.and(TestCaseSpecification.hasFolder(folderId));

        return testCaseRepository.findAll(spec)
                .stream()
                .map(TestCaseResponse::from)
                .toList();
    }

    public TestCaseResponse getTestCase(Long id) {
        return TestCaseResponse.from(findById(id));
    }

    @Transactional
    public TestCaseResponse createTestCase(CreateTestCaseRequest request) {
        List<AreaTag> areaTags = loadAreaTags(request.areaTagIds());
        ServerEnvironment serverEnvironment = loadServerEnvironment(request.serverEnvironmentId());
        TestConfiguration configuration = loadTestConfiguration(request.testConfigurationId());
        TestFolder folder = loadFolder(request.folderId());
        validateProjectScope(request.projectId(), folder, areaTags);
        TestCase testCase = new TestCase(
                request.type(),
                request.priority(),
                request.status(),
                request.title(),
                request.description(),
                request.precondition(),
                request.steps(),
                request.notes(),
                request.os(),
                request.browser(),
                request.device(),
                areaTags,
                serverEnvironment,
                configuration,
                request.assignee(),
                request.version()
        );
        testCase.moveToFolder(folder);
        testCase.setProjectId(request.projectId());
        TestCase saved = testCaseRepository.save(testCase);
        auditLogService.record(AuditLogService.TEST_CASE, saved.getId(), AuditAction.CREATED, "testCase", null, saved.getTitle());
        createVersionSnapshot(saved, "최초 생성");
        return TestCaseResponse.from(saved);
    }

    @Transactional
    public TestCaseResponse updateTestCase(Long id, UpdateTestCaseRequest request) {
        TestCase testCase = findById(id);
        TestCaseAuditSnapshot before = TestCaseAuditSnapshot.from(testCase);
        List<AreaTag> areaTags = loadAreaTags(request.areaTagIds());
        ServerEnvironment serverEnvironment = loadServerEnvironment(request.serverEnvironmentId());
        TestConfiguration configuration = loadTestConfiguration(request.testConfigurationId());
        TestFolder folder = loadFolder(request.folderId());
        Long effectiveProjectId = request.projectId() != null ? request.projectId() : testCase.getProjectId();
        validateProjectScope(effectiveProjectId, folder, areaTags);
        testCase.update(
                request.type(),
                request.priority(),
                request.status(),
                request.title(),
                request.description(),
                request.precondition(),
                request.steps(),
                request.notes(),
                request.os(),
                request.browser(),
                request.device(),
                areaTags,
                serverEnvironment,
                configuration,
                request.assignee(),
                request.version()
        );
        testCase.moveToFolder(folder);
        if (request.projectId() != null) testCase.setProjectId(request.projectId());
        TestCaseAuditSnapshot after = TestCaseAuditSnapshot.from(testCase);
        recordTestCaseUpdates(id, before, after);
        // 실제 변경이 있을 때만 버전 스냅샷을 남긴다 — 수정 없이 저장해도 버전이 오르던 문제 방지.
        if (!before.equals(after)) {
            createVersionSnapshot(testCase, "테스트케이스 수정");
        }
        return TestCaseResponse.from(testCase);
    }

    @Transactional
    public TestCaseResponse moveToFolder(Long testCaseId, Long folderId) {
        TestCase testCase = findById(testCaseId);
        String before = folderName(testCase.getFolder());
        TestFolder folder = loadFolder(folderId);
        validateProjectScope(testCase.getProjectId(), folder, List.of());
        String after = folderName(folder);
        if (Objects.equals(before, after)) {
            return TestCaseResponse.from(testCase); // 폴더 변경 없음 — 버전/로그 생성하지 않음
        }
        testCase.moveToFolder(folder);
        auditLogService.recordIfChanged(
                AuditLogService.TEST_CASE,
                testCaseId,
                AuditAction.MOVED,
                "folder",
                before,
                after
        );
        createVersionSnapshot(testCase, "폴더 변경");
        return TestCaseResponse.from(testCase);
    }

    @Transactional
    public TestCaseResponse updateTestCaseStatus(Long id, UpdateTestCaseStatusRequest request) {
        TestCase testCase = findById(id);
        TestCaseStatus before = testCase.getStatus();
        if (Objects.equals(before, request.status())) {
            return TestCaseResponse.from(testCase); // 상태 변경 없음 — 버전/로그 생성하지 않음
        }
        testCase.updateStatus(request.status());
        auditLogService.recordIfChanged(AuditLogService.TEST_CASE, id, AuditAction.STATUS_CHANGED, "status", before, request.status());
        createVersionSnapshot(testCase, "상태 변경");
        return TestCaseResponse.from(testCase);
    }

    public List<TestCaseVersionResponse> getVersions(Long testCaseId) {
        findById(testCaseId);
        return testCaseVersionRepository.findByTestCaseIdOrderByVersionNumberDesc(testCaseId)
                .stream()
                .map(TestCaseVersionResponse::from)
                .toList();
    }

    @Transactional
    public TestCaseResponse restoreVersion(Long testCaseId, Long versionId) {
        TestCase testCase = findById(testCaseId);
        TestCaseVersion version = testCaseVersionRepository.findByIdAndTestCaseId(versionId, testCaseId)
                .orElseThrow(() -> new EntityNotFoundException("TestCaseVersion not found. id=" + versionId));
        TestCaseAuditSnapshot before = TestCaseAuditSnapshot.from(testCase);
        List<AreaTag> areaTags = loadAreaTags(parseIds(version.getAreaTagIds()));
        ServerEnvironment serverEnvironment = loadServerEnvironment(version.getServerEnvironmentId());
        TestConfiguration configuration = loadTestConfiguration(version.getTestConfigurationId());
        TestFolder folder = loadFolder(version.getFolderId());

        testCase.update(
                version.getType(),
                version.getPriority(),
                version.getStatus(),
                version.getTitle(),
                version.getDescription(),
                version.getPrecondition(),
                version.getSteps(),
                version.getNotes(),
                version.getOs(),
                version.getBrowser(),
                version.getDevice(),
                areaTags,
                serverEnvironment,
                configuration,
                version.getAssignee(),
                version.getVersion()
        );
        testCase.moveToFolder(folder);
        recordTestCaseUpdates(testCaseId, before, TestCaseAuditSnapshot.from(testCase));
        auditLogService.record(
                AuditLogService.TEST_CASE,
                testCaseId,
                AuditAction.VERSION_RESTORED,
                "version",
                null,
                version.getLabel()
        );
        createVersionSnapshot(testCase, "버전 복원: " + version.getLabel());
        return TestCaseResponse.from(testCase);
    }

    @Transactional
    public TestCaseResponse linkDefect(Long testCaseId, Long defectId) {
        TestCase testCase = findById(testCaseId);
        Defect defect = defectService.findById(defectId);
        boolean alreadyLinked = testCase.getDefects().stream().anyMatch(item -> Objects.equals(item.getId(), defectId));
        testCase.linkDefect(defect);
        if (!alreadyLinked) {
            auditLogService.record(AuditLogService.TEST_CASE, testCaseId, AuditAction.DEFECT_LINKED, "defects", null, defectLabel(defect));
        }
        return TestCaseResponse.from(testCase);
    }

    @Transactional
    public TestCaseResponse unlinkDefect(Long testCaseId, Long defectId) {
        TestCase testCase = findById(testCaseId);
        Defect defect = defectService.findById(defectId);
        boolean wasLinked = testCase.getDefects().stream().anyMatch(item -> Objects.equals(item.getId(), defectId));
        testCase.unlinkDefect(defect);
        if (wasLinked) {
            auditLogService.record(AuditLogService.TEST_CASE, testCaseId, AuditAction.DEFECT_UNLINKED, "defects", defectLabel(defect), null);
        }
        return TestCaseResponse.from(testCase);
    }

    @Transactional
    public void deleteTestCase(Long id) {
        TestCase testCase = findById(id);
        auditLogService.record(AuditLogService.TEST_CASE, id, AuditAction.DELETED, "testCase", testCase.getTitle(), null);
        testCaseVersionRepository.deleteAllByTestCaseId(id);
        testRunRepository.deleteAllByTestCaseId(id);
        testSuiteService.removeTestCaseFromAllSuites(id);
        attachmentService.deleteAllByEntity(AttachmentEntityType.TEST_CASE, id);
        testCaseRepository.delete(testCase);
    }

    private TestCase findById(Long id) {
        return testCaseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("TestCase not found. id=" + id));
    }

    private ServerEnvironment loadServerEnvironment(Long id) {
        if (id == null) return null;
        return serverEnvironmentRepository.findById(id)
                .orElseThrow(() -> new InvalidRequestException("존재하지 않는 서버 환경입니다. id=" + id));
    }

    private TestConfiguration loadTestConfiguration(Long id) {
        if (id == null) return null;
        return testConfigurationRepository.findById(id)
                .orElseThrow(() -> new InvalidRequestException("존재하지 않는 configuration입니다. id=" + id));
    }

    private TestFolder loadFolder(Long id) {
        if (id == null) return null;
        return testFolderRepository.findById(id)
                .orElseThrow(() -> new InvalidRequestException("존재하지 않는 폴더입니다. id=" + id));
    }

    /** 폴더·태그가 테스트케이스와 같은 프로젝트에 속하는지 검증한다(데이터 격리 무결성). */
    private void validateProjectScope(Long projectId, TestFolder folder, List<AreaTag> areaTags) {
        if (folder != null && !ProjectScope.compatible(projectId, folder.getProjectId())) {
            throw new InvalidRequestException("다른 프로젝트의 폴더로는 이동할 수 없습니다. folderId=" + folder.getId());
        }
        List<Long> mismatched = areaTags.stream()
                .filter(tag -> !ProjectScope.compatible(projectId, tag.getProjectId()))
                .map(AreaTag::getId).toList();
        if (!mismatched.isEmpty()) {
            throw new InvalidRequestException("다른 프로젝트의 태그는 사용할 수 없습니다: " + mismatched);
        }
    }

    private List<AreaTag> loadAreaTags(List<Long> areaTagIds) {
        if (areaTagIds == null || areaTagIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> uniqueTagIds = new ArrayList<>(new LinkedHashSet<>(areaTagIds));
        List<AreaTag> areaTags = areaTagRepository.findAllByIdIn(uniqueTagIds);
        Map<Long, AreaTag> areaTagsById = areaTags.stream()
                .collect(Collectors.toMap(AreaTag::getId, Function.identity()));

        if (areaTagsById.size() != uniqueTagIds.size()) {
            List<Long> missingTagIds = uniqueTagIds.stream()
                    .filter(id -> !areaTagsById.containsKey(id))
                    .toList();
            throw new InvalidRequestException("존재하지 않는 태그 ID가 포함되어 있습니다: " + missingTagIds);
        }

        return uniqueTagIds.stream()
                .map(areaTagsById::get)
                .toList();
    }

    private void recordTestCaseUpdates(Long id, TestCaseAuditSnapshot before, TestCaseAuditSnapshot after) {
        auditLogService.recordIfChanged(AuditLogService.TEST_CASE, id, AuditAction.UPDATED, "type", before.type(), after.type());
        auditLogService.recordIfChanged(AuditLogService.TEST_CASE, id, AuditAction.UPDATED, "priority", before.priority(), after.priority());
        auditLogService.recordIfChanged(AuditLogService.TEST_CASE, id, AuditAction.STATUS_CHANGED, "status", before.status(), after.status());
        auditLogService.recordIfChanged(AuditLogService.TEST_CASE, id, AuditAction.UPDATED, "title", before.title(), after.title());
        auditLogService.recordIfChanged(AuditLogService.TEST_CASE, id, AuditAction.UPDATED, "description", before.description(), after.description());
        auditLogService.recordIfChanged(AuditLogService.TEST_CASE, id, AuditAction.UPDATED, "precondition", before.precondition(), after.precondition());
        auditLogService.recordIfChanged(AuditLogService.TEST_CASE, id, AuditAction.UPDATED, "steps", before.steps(), after.steps());
        auditLogService.recordIfChanged(AuditLogService.TEST_CASE, id, AuditAction.UPDATED, "notes", before.notes(), after.notes());
        auditLogService.recordIfChanged(AuditLogService.TEST_CASE, id, AuditAction.UPDATED, "os", before.os(), after.os());
        auditLogService.recordIfChanged(AuditLogService.TEST_CASE, id, AuditAction.UPDATED, "browser", before.browser(), after.browser());
        auditLogService.recordIfChanged(AuditLogService.TEST_CASE, id, AuditAction.UPDATED, "device", before.device(), after.device());
        auditLogService.recordIfChanged(AuditLogService.TEST_CASE, id, AuditAction.UPDATED, "assignee", before.assignee(), after.assignee());
        auditLogService.recordIfChanged(AuditLogService.TEST_CASE, id, AuditAction.UPDATED, "version", before.version(), after.version());
        auditLogService.recordIfChanged(AuditLogService.TEST_CASE, id, AuditAction.MOVED, "folder", before.folder(), after.folder());
        auditLogService.recordIfChanged(AuditLogService.TEST_CASE, id, AuditAction.UPDATED, "serverEnvironment", before.serverEnvironment(), after.serverEnvironment());
        auditLogService.recordIfChanged(AuditLogService.TEST_CASE, id, AuditAction.UPDATED, "testConfiguration", before.testConfiguration(), after.testConfiguration());
        auditLogService.recordIfChanged(AuditLogService.TEST_CASE, id, AuditAction.UPDATED, "areaTags", before.areaTags(), after.areaTags());
    }

    private void createVersionSnapshot(TestCase testCase, String changeSummary) {
        Integer nextVersionNumber = testCaseVersionRepository.findTopByTestCaseIdOrderByVersionNumberDesc(testCase.getId())
                .map(version -> version.getVersionNumber() + 1)
                .orElse(1);
        String label = (testCase.getVersion() == null || testCase.getVersion().isBlank())
                ? "v" + nextVersionNumber
                : testCase.getVersion();
        TestCaseVersion snapshot = testCaseVersionRepository.save(new TestCaseVersion(
                testCase.getId(),
                nextVersionNumber,
                label,
                changeSummary,
                testCase.getType(),
                testCase.getPriority(),
                testCase.getStatus(),
                testCase.getTitle(),
                testCase.getVersion(),
                testCase.getDescription(),
                testCase.getPrecondition(),
                testCase.getSteps(),
                testCase.getNotes(),
                testCase.getOs(),
                testCase.getBrowser(),
                testCase.getDevice(),
                testCase.getAssignee(),
                testCase.getFolder() == null ? null : testCase.getFolder().getId(),
                testCase.getFolder() == null ? null : testCase.getFolder().getName(),
                testCase.getServerEnvironment() == null ? null : testCase.getServerEnvironment().getId(),
                testCase.getServerEnvironment() == null ? null : testCase.getServerEnvironment().getName(),
                testCase.getTestConfiguration() == null ? null : testCase.getTestConfiguration().getId(),
                testCase.getTestConfiguration() == null ? null : testCase.getTestConfiguration().getName(),
                testCase.getAreaTags().stream().map(AreaTag::getId).map(String::valueOf).collect(Collectors.joining(",")),
                testCase.getAreaTags().stream().map(AreaTag::getName).collect(Collectors.joining(", "))
        ));
        auditLogService.record(AuditLogService.TEST_CASE, testCase.getId(), AuditAction.VERSION_CREATED, "version", null, snapshot.getLabel());
    }

    private List<Long> parseIds(String ids) {
        if (ids == null || ids.isBlank()) {
            return Collections.emptyList();
        }
        return List.of(ids.split(","))
                .stream()
                .filter(value -> !value.isBlank())
                .map(Long::valueOf)
                .toList();
    }

    private static String folderName(TestFolder folder) {
        return folder == null ? "미분류" : folder.getName();
    }

    private static String defectLabel(Defect defect) {
        return "#" + defect.getId() + " " + defect.getTitle();
    }

    private record TestCaseAuditSnapshot(
            TestCaseType type,
            TestCasePriority priority,
            TestCaseStatus status,
            String title,
            String description,
            String precondition,
            String steps,
            String notes,
            TestCaseOs os,
            TestCaseBrowser browser,
            TestCaseDevice device,
            String assignee,
            String version,
            String folder,
            String serverEnvironment,
            String testConfiguration,
            String areaTags
    ) {
        static TestCaseAuditSnapshot from(TestCase testCase) {
            return new TestCaseAuditSnapshot(
                    testCase.getType(),
                    testCase.getPriority(),
                    testCase.getStatus(),
                    testCase.getTitle(),
                    testCase.getDescription(),
                    testCase.getPrecondition(),
                    testCase.getSteps(),
                    testCase.getNotes(),
                    testCase.getOs(),
                    testCase.getBrowser(),
                    testCase.getDevice(),
                    testCase.getAssignee(),
                    testCase.getVersion(),
                    folderName(testCase.getFolder()),
                    testCase.getServerEnvironment() == null ? null : testCase.getServerEnvironment().getName(),
                    testCase.getTestConfiguration() == null ? null : testCase.getTestConfiguration().getName(),
                    testCase.getAreaTags().stream().map(AreaTag::getName).collect(Collectors.joining(", "))
            );
        }
    }
}
