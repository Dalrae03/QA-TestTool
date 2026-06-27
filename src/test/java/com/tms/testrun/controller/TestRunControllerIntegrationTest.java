package com.tms.testrun.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.testcase.entity.TestCase;
import com.tms.testcase.entity.TestCasePriority;
import com.tms.testcase.entity.TestCaseStatus;
import com.tms.testcase.entity.TestCaseType;
import com.tms.testcase.repository.TestCaseRepository;
import com.tms.execution.entity.ResultStatus;
import com.tms.testrun.dto.CreateTestRunRequest;
import com.tms.testrun.repository.TestRunRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class TestRunControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private TestRunRepository testRunRepository;

    @AfterEach
    void tearDown() {
        testRunRepository.deleteAll();
        testCaseRepository.deleteAll();
    }

    @Test
    void shouldCreateAndListTestRuns() throws Exception {
        TestCase testCase = testCaseRepository.save(new TestCase(
                TestCaseType.FUNCTIONAL,
                TestCasePriority.HIGH,
                TestCaseStatus.DRAFT,
                "Login test",
                "Verify login",
                "User exists",
                "Open page\nSubmit form",
                null,
                null,
                null,
                null,
                List.of()
        ));

        CreateTestRunRequest request = new CreateTestRunRequest(
                ResultStatus.PASSED,
                "로그인이 정상 동작했습니다.",
                "Chrome 최신 버전에서 확인",
                null, null
        );

        MvcResult createResult = mockMvc.perform(post("/api/testcases/{testCaseId}/runs", testCase.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.matchesPattern("/api/testcases/" + testCase.getId() + "/runs/\\d+")))
                .andExpect(jsonPath("$.testCaseId").value(testCase.getId()))
                .andExpect(jsonPath("$.status").value("PASSED"))
                .andExpect(jsonPath("$.actualResult").value("로그인이 정상 동작했습니다."))
                .andExpect(jsonPath("$.notes").value("Chrome 최신 버전에서 확인"))
                .andReturn();

        Long runId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/testcases/{testCaseId}/runs", testCase.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("PASSED"))
                .andExpect(jsonPath("$[0].actualResult").value("로그인이 정상 동작했습니다."));

        mockMvc.perform(delete("/api/testcases/{testCaseId}/runs/{runId}", testCase.getId(), runId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/testcases/{testCaseId}/runs", testCase.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldRejectInvalidTestRunRequest() throws Exception {
        TestCase testCase = testCaseRepository.save(new TestCase(
                TestCaseType.FUNCTIONAL,
                TestCasePriority.MEDIUM,
                TestCaseStatus.DRAFT,
                "Profile test",
                "Verify profile save",
                "User logged in",
                "Open profile",
                null,
                null,
                null,
                null,
                List.of()
        ));

        CreateTestRunRequest invalidRequest = new CreateTestRunRequest(
                null,
                " ",
                null,
                null, null
        );

        mockMvc.perform(post("/api/testcases/{testCaseId}/runs", testCase.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.status").exists())
                .andExpect(jsonPath("$.errors.actualResult").exists());
    }

    @Test
    void shouldOnlyAcceptPassFailAndBlockedResults() throws Exception {
        TestCase testCase = testCaseRepository.save(new TestCase(
                TestCaseType.FUNCTIONAL,
                TestCasePriority.HIGH,
                TestCaseStatus.READY,
                "Result status test",
                "Verify result statuses",
                "Test case exists",
                "Run test",
                null,
                null,
                null,
                null,
                List.of()
        ));

        for (ResultStatus status : List.of(ResultStatus.PASSED, ResultStatus.FAILED, ResultStatus.BLOCKED)) {
            mockMvc.perform(post("/api/testcases/{testCaseId}/runs", testCase.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CreateTestRunRequest(
                                    status, status + " result", null, null, null
                            ))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value(status.name()));
        }

        mockMvc.perform(post("/api/testcases/{testCaseId}/runs", testCase.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateTestRunRequest(
                                ResultStatus.SKIPPED, "Skipped result", null, null, null
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.supportedStatus")
                        .value("실행 결과는 PASSED, FAILED, BLOCKED 중 하나여야 합니다."));

        mockMvc.perform(get("/api/testcases/{testCaseId}/runs", testCase.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void shouldUpdateTestRun() throws Exception {
        TestCase testCase = testCaseRepository.save(new TestCase(
                TestCaseType.FUNCTIONAL, TestCasePriority.HIGH, TestCaseStatus.READY,
                "Update test", "Description", "Precondition", "Steps", null,
                null, null, null, List.of()
        ));

        MvcResult createResult = mockMvc.perform(post("/api/testcases/{testCaseId}/runs", testCase.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateTestRunRequest(
                                ResultStatus.FAILED, "초기 결과", null, null, null
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        long runId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/testcases/{testCaseId}/runs/{runId}", testCase.getId(), runId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateTestRunRequest(
                                ResultStatus.PASSED, "수정된 결과", "추가 메모", null, null
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PASSED"))
                .andExpect(jsonPath("$.actualResult").value("수정된 결과"))
                .andExpect(jsonPath("$.notes").value("추가 메모"));
    }

    @Test
    void shouldSaveAndUpdateAssignee() throws Exception {
        TestCase testCase = testCaseRepository.save(new TestCase(
                TestCaseType.FUNCTIONAL, TestCasePriority.MEDIUM, TestCaseStatus.READY,
                "Assignee test", "Description", "Precondition", "Steps", null,
                null, null, null, List.of()
        ));

        MvcResult createResult = mockMvc.perform(post("/api/testcases/{testCaseId}/runs", testCase.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateTestRunRequest(
                                ResultStatus.PASSED, "정상 동작", null, "홍길동", null
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assignee").value("홍길동"))
                .andReturn();
        long runId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/testcases/{testCaseId}/runs/{runId}", testCase.getId(), runId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateTestRunRequest(
                                ResultStatus.PASSED, "정상 동작", null, "김철수", null
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignee").value("김철수"));

        mockMvc.perform(put("/api/testcases/{testCaseId}/runs/{runId}", testCase.getId(), runId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateTestRunRequest(
                                ResultStatus.PASSED, "정상 동작", null, null, null
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignee").doesNotExist());
    }

    @Test
    void shouldReturn404ForUnknownTestCase() throws Exception {
        mockMvc.perform(get("/api/testcases/9999/runs"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/testcases/9999/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateTestRunRequest(
                                ResultStatus.PASSED, "결과", null, null, null
                        ))))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenRunDoesNotBelongToTestCase() throws Exception {
        TestCase caseA = testCaseRepository.save(new TestCase(
                TestCaseType.FUNCTIONAL, TestCasePriority.HIGH, TestCaseStatus.READY,
                "Case A", "Description", "Precondition", "Steps", null,
                null, null, null, List.of()
        ));
        TestCase caseB = testCaseRepository.save(new TestCase(
                TestCaseType.FUNCTIONAL, TestCasePriority.HIGH, TestCaseStatus.READY,
                "Case B", "Description", "Precondition", "Steps", null,
                null, null, null, List.of()
        ));

        MvcResult createResult = mockMvc.perform(post("/api/testcases/{testCaseId}/runs", caseA.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateTestRunRequest(
                                ResultStatus.PASSED, "결과", null, null, null
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        Long runId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/testcases/{testCaseId}/runs/{runId}", caseB.getId(), runId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateTestRunRequest(
                                ResultStatus.FAILED, "다른 케이스로 수정 시도", null, null, null
                        ))))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/testcases/{testCaseId}/runs/{runId}", caseB.getId(), runId))
                .andExpect(status().isNotFound());
    }
}
