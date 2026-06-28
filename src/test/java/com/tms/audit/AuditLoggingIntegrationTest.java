package com.tms.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.tms.audit.entity.AuditAction;
import com.tms.audit.entity.AuditLog;
import com.tms.audit.repository.AuditLogRepository;
import com.tms.defect.dto.DefectRequest;
import com.tms.defect.entity.DefectSeverity;
import com.tms.defect.entity.DefectStatus;
import com.tms.defect.service.DefectService;
import com.tms.environment.dto.ServerEnvironmentRequest;
import com.tms.environment.entity.ServerEnvironmentType;
import com.tms.environment.service.ServerEnvironmentService;
import com.tms.execution.dto.CreateExecutionRequest;
import com.tms.execution.dto.RecordResultRequest;
import com.tms.execution.dto.UpdateExecutionRequest;
import com.tms.execution.dto.ExecutionResponse;
import com.tms.execution.entity.ExecutionStatus;
import com.tms.execution.entity.ResultStatus;
import com.tms.execution.service.ExecutionService;
import com.tms.testcase.entity.TestCase;
import com.tms.testcase.entity.TestCasePriority;
import com.tms.testcase.entity.TestCaseStatus;
import com.tms.testcase.entity.TestCaseType;
import com.tms.testcase.repository.TestCaseRepository;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 요청한 액션들(테스트런 결과/완료/재오픈, 결함 생성/상태변경, 설정변경 등)이 감사 로그로 남는지 검증한다.
 */
@SpringBootTest
class AuditLoggingIntegrationTest {

    @Autowired AuditLogRepository auditLogRepository;
    @Autowired ExecutionService executionService;
    @Autowired DefectService defectService;
    @Autowired ServerEnvironmentService serverEnvironmentService;
    @Autowired TestCaseRepository testCaseRepository;
    @Autowired com.tms.execution.repository.ExecutionRepository executionRepository;
    @Autowired com.tms.defect.repository.DefectRepository defectRepository;
    @Autowired com.tms.environment.repository.ServerEnvironmentRepository serverEnvironmentRepository;

    @AfterEach
    void tearDown() {
        // 공유 인메모리 DB 를 다른 테스트에 누수시키지 않도록 생성한 데이터를 모두 정리한다.
        executionRepository.deleteAll();
        defectRepository.deleteAll();
        serverEnvironmentRepository.deleteAll();
        auditLogRepository.deleteAll();
        testCaseRepository.deleteAll();
    }

    private List<AuditLog> logs(String entityType, AuditAction action) {
        return auditLogRepository.findAll().stream()
                .filter(l -> entityType.equals(l.getEntityType()) && l.getAction() == action)
                .toList();
    }

    @Test
    void testRunResultCompleteReopenAreLogged() {
        TestCase tc = testCaseRepository.save(new TestCase(TestCaseType.FUNCTIONAL, TestCasePriority.HIGH,
                TestCaseStatus.READY, "로그인", "설명", "사전", "스텝", "비고",
                null, null, null, new ArrayList<>(), null, null, "tester", "v1"));

        // CreateExecutionRequest(suiteId, testCaseIds, testPlanId, projectId, testConfigurationId, name, description, assignee)
        CreateExecutionRequest createReq = new CreateExecutionRequest(
                null, List.of(tc.getId()), null, 7L, null, "회귀런", null, null);
        ExecutionResponse run = executionService.createExecution(createReq);
        assertThat(logs(AuditLogService_TEST_RUN, AuditAction.CREATED)).isNotEmpty();

        Long itemId = run.items().get(0).id();
        executionService.recordResult(run.id(), itemId, new RecordResultRequest(ResultStatus.PASSED, "ok", null));
        assertThat(logs(AuditLogService_TEST_RUN, AuditAction.RESULT_RECORDED)).isNotEmpty();

        // 완료 처리
        executionService.updateExecution(run.id(),
                new UpdateExecutionRequest("회귀런", null, ExecutionStatus.COMPLETED, null));
        assertThat(logs(AuditLogService_TEST_RUN, AuditAction.COMPLETED)).isNotEmpty();

        // 재오픈
        executionService.updateExecution(run.id(),
                new UpdateExecutionRequest("회귀런", null, ExecutionStatus.IN_PROGRESS, null));
        assertThat(logs(AuditLogService_TEST_RUN, AuditAction.REOPENED)).isNotEmpty();
    }

    @Test
    void defectCreateAndStatusChangeAreLogged() {
        var created = defectService.create(new DefectRequest(
                "결제 실패", "오류", DefectSeverity.CRITICAL, DefectStatus.OPEN, null));
        assertThat(logs("DEFECT", AuditAction.CREATED)).isNotEmpty();

        defectService.update(created.id(), new DefectRequest(
                "결제 실패", "오류", DefectSeverity.CRITICAL, DefectStatus.IN_PROGRESS, null));
        assertThat(logs("DEFECT", AuditAction.STATUS_CHANGED)).isNotEmpty();
    }

    @Test
    void settingChangeIsLogged() {
        var env = serverEnvironmentService.create(new ServerEnvironmentRequest(
                "스테이징", ServerEnvironmentType.STAGING, "https://staging.example.com", "설명", true));
        assertThat(logs("SERVER_ENVIRONMENT", AuditAction.CREATED)).isNotEmpty();

        serverEnvironmentService.delete(env.id());
        assertThat(logs("SERVER_ENVIRONMENT", AuditAction.DELETED)).isNotEmpty();
    }

    private static final String AuditLogService_TEST_RUN = "TEST_RUN";
}
