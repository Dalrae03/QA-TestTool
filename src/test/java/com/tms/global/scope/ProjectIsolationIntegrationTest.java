package com.tms.global.scope;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.execution.dto.CreateExecutionRequest;
import com.tms.execution.repository.ExecutionRepository;
import com.tms.testcase.entity.TestCase;
import com.tms.testcase.entity.TestCasePriority;
import com.tms.testcase.entity.TestCaseStatus;
import com.tms.testcase.entity.TestCaseType;
import com.tms.testcase.repository.TestCaseRepository;
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

/**
 * 프로젝트 간 데이터 격리 무결성 검증.
 * 서로 다른 프로젝트의 테스트케이스가 한 스위트/테스트런에 섞이는 것을 막는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProjectIsolationIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired TestCaseRepository testCaseRepository;
    @Autowired TestSuiteRepository testSuiteRepository;
    @Autowired ExecutionRepository executionRepository;

    @AfterEach
    void tearDown() {
        executionRepository.deleteAll();
        testSuiteRepository.deleteAll();
        testCaseRepository.deleteAll();
    }

    @Test
    void rejectsStandaloneSuiteWithCaseFromAnotherProject() throws Exception {
        TestCase otherProjectCase = saveCase("프로젝트2 케이스", 2L);

        mockMvc.perform(post("/api/suites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TestSuiteRequest("프로젝트1 스위트", null, List.of(otherProjectCase.getId()), 1L))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void allowsStandaloneSuiteWithinSameProject() throws Exception {
        TestCase sameProjectCase = saveCase("프로젝트1 케이스", 1L);

        mockMvc.perform(post("/api/suites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TestSuiteRequest("프로젝트1 스위트", null, List.of(sameProjectCase.getId()), 1L))))
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsRunBuiltFromCasesAcrossProjects() throws Exception {
        TestCase projectOneCase = saveCase("프로젝트1 케이스", 1L);
        TestCase projectTwoCase = saveCase("프로젝트2 케이스", 2L);

        mockMvc.perform(post("/api/test-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateExecutionRequest(
                                null,
                                List.of(projectOneCase.getId(), projectTwoCase.getId()),
                                null,
                                1L,
                                "교차 프로젝트 런",
                                null,
                                null))))
                .andExpect(status().isBadRequest());
    }

    private TestCase saveCase(String title, Long projectId) {
        TestCase testCase = new TestCase(
                TestCaseType.FUNCTIONAL, TestCasePriority.HIGH, TestCaseStatus.READY,
                title, "설명", "선행조건", "절차", null,
                null, null, null, List.of()
        );
        testCase.setProjectId(projectId);
        return testCaseRepository.save(testCase);
    }
}
