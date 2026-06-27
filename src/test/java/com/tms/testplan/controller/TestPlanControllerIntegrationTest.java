package com.tms.testplan.controller;

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
import com.tms.testplan.dto.TestPlanRequest;
import com.tms.testplan.entity.TestPlanStatus;
import com.tms.testplan.repository.TestPlanRepository;
import com.tms.testsuite.dto.TestSuiteRequest;
import com.tms.testsuite.repository.TestSuiteRepository;
import java.time.LocalDate;
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
class TestPlanControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired TestPlanRepository testPlanRepository;
    @Autowired TestSuiteRepository testSuiteRepository;
    @Autowired TestCaseRepository testCaseRepository;

    @AfterEach
    void tearDown() {
        testSuiteRepository.deleteAll();
        testPlanRepository.deleteAll();
        testCaseRepository.deleteAll();
    }

    @Test
    void shouldManagePlanAndSuitesWithOrderedTestCases() throws Exception {
        TestCase first = saveCase("Login success");
        TestCase second = saveCase("Login failure");

        MvcResult planResult = mockMvc.perform(post("/api/test-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestPlanRequest(
                                "Release 1.0", "Regression plan", TestPlanStatus.ACTIVE,
                                null, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                                null, null, null, null, null, null, null, null
                        ))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/test-plans/\\d+")))
                .andExpect(jsonPath("$.name").value("Release 1.0"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.suiteCount").value(0))
                .andReturn();
        long planId = objectMapper.readTree(planResult.getResponse().getContentAsString()).get("id").asLong();

        MvcResult suiteResult = mockMvc.perform(post("/api/test-plans/{planId}/suites", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestSuiteRequest(
                                "Authentication", "Login scenarios", List.of(second.getId(), first.getId(), second.getId()), null
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.testPlanId").value(planId))
                .andExpect(jsonPath("$.testCases", hasSize(2)))
                .andExpect(jsonPath("$.testCases[0].id").value(second.getId()))
                .andExpect(jsonPath("$.testCases[1].id").value(first.getId()))
                .andReturn();
        long suiteId = objectMapper.readTree(suiteResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/test-plans/{planId}", planId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suiteCount").value(1))
                .andExpect(jsonPath("$.testCaseCount").value(2));

        mockMvc.perform(put("/api/test-plans/{planId}/suites/{suiteId}", planId, suiteId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestSuiteRequest(
                                "Authentication smoke", null, List.of(first.getId()), null
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Authentication smoke"))
                .andExpect(jsonPath("$.testCases", hasSize(1)));

        mockMvc.perform(delete("/api/test-plans/{planId}", planId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/test-plans/{planId}", planId))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/test-plans/{planId}/suites/{suiteId}", planId, suiteId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectInvalidDateRangeAndUnknownTestCase() throws Exception {
        mockMvc.perform(post("/api/test-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestPlanRequest(
                                "Invalid plan", null, TestPlanStatus.DRAFT,
                                null, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 6, 1),
                                null, null, null, null, null, null, null, null
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("종료일은 시작일보다 빠를 수 없습니다."));

        MvcResult planResult = mockMvc.perform(post("/api/test-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestPlanRequest(
                                "Valid plan", null, TestPlanStatus.DRAFT, null, null, null, null, null, null, null, null, null, null, null
                        ))))
                .andExpect(status().isCreated()).andReturn();
        long planId = objectMapper.readTree(planResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/test-plans/{planId}/suites", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestSuiteRequest(
                                "Broken suite", null, List.of(9999L), null
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("존재하지 않는 테스트케이스 ID가 포함되어 있습니다: [9999]"));
    }

    @Test
    void deletingTestCaseShouldRemoveItFromSuites() throws Exception {
        TestCase testCase = saveCase("Removable case");
        MvcResult planResult = mockMvc.perform(post("/api/test-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestPlanRequest(
                                "Cleanup plan", null, TestPlanStatus.DRAFT, null, null, null, null, null, null, null, null, null, null, null
                        ))))
                .andExpect(status().isCreated()).andReturn();
        long planId = objectMapper.readTree(planResult.getResponse().getContentAsString()).get("id").asLong();
        MvcResult suiteResult = mockMvc.perform(post("/api/test-plans/{planId}/suites", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestSuiteRequest(
                                "Cleanup suite", null, List.of(testCase.getId()), null
                        ))))
                .andExpect(status().isCreated()).andReturn();
        long suiteId = objectMapper.readTree(suiteResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/testcases/{id}", testCase.getId()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/test-plans/{planId}/suites/{suiteId}", planId, suiteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.testCases", hasSize(0)));
    }

    private TestCase saveCase(String title) {
        return testCaseRepository.save(new TestCase(
                TestCaseType.FUNCTIONAL, TestCasePriority.HIGH, TestCaseStatus.READY,
                title, "Description", "Precondition", "Steps", null,
                null, null, null, List.of()
        ));
    }
}
