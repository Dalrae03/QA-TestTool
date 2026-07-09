package com.tms.execution.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.execution.dto.CreateExecutionRequest;
import com.tms.execution.dto.UpdateExecutionRequest;
import com.tms.execution.entity.ExecutionStatus;
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

/** 이미 만들어진 테스트런에 스위트를 붙이고 떼는 흐름 — 케이스 병합·중복 처리와 스위트 스냅샷 갱신을 함께 본다. */
@SpringBootTest
@AutoConfigureMockMvc
class ExecutionSuiteMutationIntegrationTest {

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
    void shouldMergeNewCasesSkipDuplicatesAndRefreshSuiteSnapshot() throws Exception {
        TestCase tc1 = saveCase("TC1");
        TestCase tc2 = saveCase("TC2");
        TestCase tc3 = saveCase("TC3");
        long suiteA = createSuite("Suite A", List.of(tc1.getId(), tc2.getId()));
        long suiteB = createSuite("Suite B", List.of(tc2.getId(), tc3.getId())); // tc2가 겹친다
        long runId = createRunFromSuite(suiteA);

        // 스위트 B 추가 → 겹치는 tc2는 건너뛰고 tc3만 붙는다. 스위트가 둘이 되면 id는 비고 이름은 병합된다.
        mockMvc.perform(post("/api/test-runs/{id}/suites", runId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"suiteIds\":[" + suiteB + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.testSuiteId").value(nullValue()))
                .andExpect(jsonPath("$.suiteName").value("Suite A, Suite B"));

        // 같은 스위트를 다시 추가 → 새로 붙일 케이스가 없다.
        mockMvc.perform(post("/api/test-runs/{id}/suites", runId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"suiteIds\":[" + suiteB + "]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "추가할 새 테스트케이스가 없습니다 — 선택한 스위트의 케이스가 이미 모두 포함되어 있습니다."));

        // 스위트 B 제거 → tc3만 빠지고 스냅샷은 단일 스위트 A로 되돌아온다.
        mockMvc.perform(delete("/api/test-runs/{id}/suites/{suiteId}", runId, suiteB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.testSuiteId").value((int) suiteA))
                .andExpect(jsonPath("$.suiteName").value("Suite A"));

        // 하나 남은 스위트를 빼면 런이 비므로 막는다.
        mockMvc.perform(delete("/api/test-runs/{id}/suites/{suiteId}", runId, suiteA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("마지막 스위트는 제거할 수 없습니다. 테스트런 자체를 삭제해 주세요."));
    }

    /**
     * 케이스는 처음 붙인 스위트에 귀속되므로, 두 스위트가 공유하는 케이스는 먼저 붙은 쪽을 제거할 때 함께 빠진다.
     * 생성 시점의 귀속 규칙(sourceSuiteByCaseId)과 같은 동작이며, 의도된 것임을 고정해 둔다.
     */
    @Test
    void shouldDropSharedCaseWhenItsOwningSuiteIsRemoved() throws Exception {
        TestCase tc1 = saveCase("TC1");
        TestCase tc2 = saveCase("TC2");
        TestCase tc3 = saveCase("TC3");
        long suiteA = createSuite("Suite A", List.of(tc1.getId(), tc2.getId()));
        long suiteB = createSuite("Suite B", List.of(tc2.getId(), tc3.getId()));
        long runId = createRunFromSuite(suiteA);

        mockMvc.perform(post("/api/test-runs/{id}/suites", runId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"suiteIds\":[" + suiteB + "]}"))
                .andExpect(status().isOk());

        // A를 빼면 A 출처로 기록된 tc2까지 사라지고, B가 데려온 tc3만 남는다.
        mockMvc.perform(delete("/api/test-runs/{id}/suites/{suiteId}", runId, suiteA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].caseTitle").value("TC3"))
                .andExpect(jsonPath("$.testSuiteId").value((int) suiteB))
                .andExpect(jsonPath("$.suiteName").value("Suite B"));
    }

    /** 케이스를 직접 골라 만든 런에 스위트를 붙였다 떼면, 스냅샷도 원래대로 비워져야 한다. */
    @Test
    void shouldClearSuiteSnapshotWhenLastSourceSuiteLeavesCasePickedRun() throws Exception {
        TestCase picked = saveCase("TC1");
        long suiteA = createSuite("Suite A", List.of(saveCase("TC2").getId()));

        MvcResult created = mockMvc.perform(post("/api/test-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateExecutionRequest(
                                null, null, List.of(picked.getId()), null, null, null, null, "run", null, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.suiteName").value(nullValue()))
                .andReturn();
        long runId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/test-runs/{id}/suites", runId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"suiteIds\":[" + suiteA + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suiteName").value("Suite A"));

        // 직접 고른 케이스가 남아 있어 제거는 허용되고, 출처 스위트가 없어지므로 스냅샷은 비워진다.
        mockMvc.perform(delete("/api/test-runs/{id}/suites/{suiteId}", runId, suiteA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.testSuiteId").value(nullValue()))
                .andExpect(jsonPath("$.suiteName").value(nullValue()));
    }

    @Test
    void shouldRejectInvalidSuiteMutations() throws Exception {
        long suiteA = createSuite("Suite A", List.of(saveCase("TC1").getId()));
        long otherSuite = createSuite("Other", List.of(saveCase("TC9").getId()));
        long runId = createRunFromSuite(suiteA);

        mockMvc.perform(post("/api/test-runs/{id}/suites", runId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"suiteIds\":[]}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/test-runs/{id}/suites", runId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"suiteIds\":[999999]}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/test-runs/{id}/suites/{suiteId}", runId, otherSuite))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이 테스트런에는 해당 스위트에서 온 항목이 없습니다."));

        // 완료된 런은 다시 열기 전까지 스위트 구성을 못 바꾼다.
        mockMvc.perform(put("/api/test-runs/{id}", runId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateExecutionRequest(
                                "run", null, ExecutionStatus.COMPLETED, null))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/test-runs/{id}/suites", runId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"suiteIds\":[" + otherSuite + "]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("완료된 테스트런에는 스위트를 추가할 수 없습니다. 먼저 다시 열어 주세요."));

        mockMvc.perform(delete("/api/test-runs/{id}/suites/{suiteId}", runId, suiteA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("완료된 테스트런에서는 스위트를 제거할 수 없습니다. 먼저 다시 열어 주세요."));
    }

    private long createRunFromSuite(long suiteId) throws Exception {
        MvcResult created = mockMvc.perform(post("/api/test-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateExecutionRequest(
                                suiteId, null, null, null, null, null, null, "run", null, null))))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createSuite(String name, List<Long> caseIds) throws Exception {
        MvcResult planResult = mockMvc.perform(post("/api/test-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestPlanRequest(
                                "Plan " + name, TestPlanStatus.IN_PROGRESS, null, null, null,
                                null, null, null, null, null, null, null, null, null, null, null,
                                null, null, null, null, null, null, null, null))))
                .andExpect(status().isCreated()).andReturn();
        long planId = objectMapper.readTree(planResult.getResponse().getContentAsString()).get("id").asLong();

        MvcResult suiteResult = mockMvc.perform(post("/api/test-plans/{planId}/suites", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestSuiteRequest(name, null, caseIds, null))))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(suiteResult.getResponse().getContentAsString()).get("id").asLong();
    }

    private TestCase saveCase(String title) {
        return testCaseRepository.save(new TestCase(
                TestCaseType.FUNCTIONAL, TestCasePriority.HIGH, TestCaseStatus.READY,
                title, "Description", "Precondition", "Steps", null,
                null, null, null, List.of()));
    }
}
