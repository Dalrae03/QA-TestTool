package com.tms.testrun.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.testcase.entity.TestCase;
import com.tms.testcase.entity.TestCasePriority;
import com.tms.testcase.entity.TestCaseType;
import com.tms.testcase.repository.TestCaseRepository;
import com.tms.testrun.dto.CreateTestRunRequest;
import com.tms.testrun.entity.TestRunStatus;
import com.tms.testrun.repository.TestRunRepository;
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
                "Login test",
                "Verify login",
                "User exists",
                "Open page\nSubmit form",
                "Page opens\nLogin succeeds",
                null
        ));

        CreateTestRunRequest request = new CreateTestRunRequest(
                TestRunStatus.PASSED,
                "로그인이 정상 동작했습니다.",
                "Chrome 최신 버전에서 확인"
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
                "Profile test",
                "Verify profile save",
                "User logged in",
                "Open profile",
                "Profile opens",
                null
        ));

        CreateTestRunRequest invalidRequest = new CreateTestRunRequest(
                null,
                " ",
                null
        );

        mockMvc.perform(post("/api/testcases/{testCaseId}/runs", testCase.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.status").exists())
                .andExpect(jsonPath("$.errors.actualResult").exists());
    }
}
