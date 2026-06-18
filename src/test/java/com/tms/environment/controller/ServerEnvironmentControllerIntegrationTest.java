package com.tms.environment.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.environment.dto.ServerEnvironmentRequest;
import com.tms.environment.entity.ServerEnvironmentType;
import com.tms.environment.repository.ServerEnvironmentRepository;
import com.tms.testcase.dto.CreateTestCaseRequest;
import com.tms.testcase.entity.TestCasePriority;
import com.tms.testcase.entity.TestCaseStatus;
import com.tms.testcase.entity.TestCaseType;
import com.tms.testcase.repository.TestCaseRepository;
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
class ServerEnvironmentControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ServerEnvironmentRepository serverEnvironmentRepository;
    @Autowired TestCaseRepository testCaseRepository;
    @Autowired TestRunRepository testRunRepository;

    @AfterEach
    void tearDown() {
        testRunRepository.deleteAll();
        testCaseRepository.deleteAll();
        serverEnvironmentRepository.deleteAll();
    }

    @Test
    void shouldManageServerEnvironmentAndAssignItToTestCase() throws Exception {
        MvcResult environmentResult = mockMvc.perform(post("/api/server-environments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ServerEnvironmentRequest(
                                "Staging API", ServerEnvironmentType.STAGING,
                                "https://staging.example.com", "Release verification", true
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Staging API"))
                .andExpect(jsonPath("$.type").value("STAGING"))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();
        long environmentId = objectMapper.readTree(environmentResult.getResponse().getContentAsString()).get("id").asLong();

        MvcResult testCaseResult = mockMvc.perform(post("/api/testcases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateTestCaseRequest(
                                TestCaseType.FUNCTIONAL, TestCasePriority.HIGH, TestCaseStatus.READY,
                                "Staging login", "Verify login", "User exists", "Open login", null,
                                null, null, null, List.of(), environmentId
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serverEnvironment.id").value(environmentId))
                .andExpect(jsonPath("$.serverEnvironment.baseUrl").value("https://staging.example.com"))
                .andReturn();
        long testCaseId = objectMapper.readTree(testCaseResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/server-environments/{id}", environmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ServerEnvironmentRequest(
                                "Staging API", ServerEnvironmentType.STAGING,
                                "https://staging-v2.example.com", null, false
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseUrl").value("https://staging-v2.example.com"))
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(delete("/api/server-environments/{id}", environmentId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/testcases/{id}", testCaseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serverEnvironment").doesNotExist());
    }

    @Test
    void shouldRejectDuplicateNameAndUnknownAssignment() throws Exception {
        ServerEnvironmentRequest request = new ServerEnvironmentRequest(
                "Production", ServerEnvironmentType.PRODUCTION, "https://api.example.com", null, true
        );
        mockMvc.perform(post("/api/server-environments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/server-environments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("이미 존재하는 서버 환경입니다: Production"));

        mockMvc.perform(post("/api/testcases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateTestCaseRequest(
                                TestCaseType.FUNCTIONAL, TestCasePriority.MEDIUM, TestCaseStatus.DRAFT,
                                "Unknown environment", "Description", "Precondition", "Steps", null,
                                null, null, null, List.of(), 9999L
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("존재하지 않는 서버 환경입니다. id=9999"));

        mockMvc.perform(get("/api/server-environments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }
}
