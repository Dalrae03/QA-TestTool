package com.tms.execution.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.execution.dto.CreateExecutionRequest;
import com.tms.execution.dto.RecordResultRequest;
import com.tms.execution.dto.UpdateExecutionRequest;
import com.tms.execution.entity.ExecutionStatus;
import com.tms.execution.entity.ResultStatus;
import com.tms.execution.repository.ExecutionRepository;
import com.tms.testcase.entity.TestCase;
import com.tms.testcase.entity.TestCasePriority;
import com.tms.testcase.entity.TestCaseStatus;
import com.tms.testcase.entity.TestCaseType;
import com.tms.testcase.repository.TestCaseRepository;
import com.tms.testplan.dto.TestPlanRequest;
import com.tms.testplan.entity.TestPlanStatus;
import com.tms.testplan.repository.TestPlanRepository;
import com.tms.testsuite.dto.TestSuiteRequest;
import com.tms.testsuite.repository.TestSuiteRepository;
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
class ExecutionControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ExecutionRepository executionRepository;
    @Autowired TestPlanRepository testPlanRepository;
    @Autowired TestSuiteRepository testSuiteRepository;
    @Autowired TestCaseRepository testCaseRepository;

    @AfterEach
    void tearDown() {
        executionRepository.deleteAll();
        testSuiteRepository.deleteAll();
        testPlanRepository.deleteAll();
        testCaseRepository.deleteAll();
    }

    @Test
    void shouldCreateRunFromSuiteRecordResultAndComplete() throws Exception {
        TestCase first = saveCase("Login success");
        TestCase second = saveCase("Login failure");
        long suiteId = createSuite("Authentication", List.of(first.getId(), second.getId()));

        MvcResult created = mockMvc.perform(post("/api/test-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateExecutionRequest(
                                suiteId, null, "Sprint regression", "jun"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/test-runs/\\d+")))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.suiteName").value("Authentication"))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.untested").value(2))
                .andExpect(jsonPath("$.progressPct").value(0))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andReturn();
        long runId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        long itemId = objectMapper.readTree(created.getResponse().getContentAsString()).get("items").get(0).get("id").asLong();

        mockMvc.perform(patch("/api/test-runs/{id}/items/{itemId}", runId, itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RecordResultRequest(ResultStatus.PASSED, "확인 완료"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passed").value(1))
                .andExpect(jsonPath("$.untested").value(1))
                .andExpect(jsonPath("$.progressPct").value(50));

        mockMvc.perform(put("/api/test-runs/{id}", runId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateExecutionRequest(
                                "Sprint regression", null, ExecutionStatus.COMPLETED, "jun"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());

        mockMvc.perform(patch("/api/test-runs/{id}/items/{itemId}", runId, itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RecordResultRequest(ResultStatus.FAILED, "완료 후 수정"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("완료된 테스트런은 다시 열기 전까지 수정할 수 없습니다."));

        mockMvc.perform(put("/api/test-runs/{id}", runId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateExecutionRequest(
                                "Sprint regression reopened", null, ExecutionStatus.IN_PROGRESS, " jun "
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.assignee").value("jun"))
                .andExpect(jsonPath("$.completedAt").value(org.hamcrest.Matchers.nullValue()));

        mockMvc.perform(patch("/api/test-runs/{id}/items/{itemId}", runId, itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RecordResultRequest(ResultStatus.FAILED, " 재확인 필요 "))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.failed").value(1))
                .andExpect(jsonPath("$.items[0].comment").value("재확인 필요"));

        mockMvc.perform(get("/api/test-runs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(delete("/api/test-runs/{id}", runId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/test-runs/{id}", runId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectRunFromEmptySuite() throws Exception {
        long suiteId = createSuite("Empty suite", List.of());

        mockMvc.perform(post("/api/test-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateExecutionRequest(suiteId, null, null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("테스트케이스가 없는 스위트로는 테스트런을 만들 수 없습니다."));
    }

    @Test
    void shouldReturnNotFoundForUnknownSuite() throws Exception {
        mockMvc.perform(post("/api/test-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateExecutionRequest(9999L, null, null, null))))
                .andExpect(status().isNotFound());
    }

    private long createSuite(String name, List<Long> caseIds) throws Exception {
        MvcResult planResult = mockMvc.perform(post("/api/test-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestPlanRequest(
                                "Plan " + name, null, TestPlanStatus.ACTIVE, null, null, null
                        ))))
                .andExpect(status().isCreated()).andReturn();
        long planId = objectMapper.readTree(planResult.getResponse().getContentAsString()).get("id").asLong();

        MvcResult suiteResult = mockMvc.perform(post("/api/test-plans/{planId}/suites", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestSuiteRequest(name, null, caseIds))))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(suiteResult.getResponse().getContentAsString()).get("id").asLong();
    }

    private TestCase saveCase(String title) {
        return testCaseRepository.save(new TestCase(
                TestCaseType.FUNCTIONAL, TestCasePriority.HIGH, TestCaseStatus.READY,
                title, "Description", "Precondition", "Steps", null,
                null, null, null, List.of()
        ));
    }
}
