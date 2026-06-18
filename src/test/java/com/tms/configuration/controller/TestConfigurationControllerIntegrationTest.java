package com.tms.configuration.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.configuration.dto.TestConfigurationRequest;
import com.tms.configuration.repository.TestConfigurationRepository;
import com.tms.environment.entity.ServerEnvironment;
import com.tms.environment.entity.ServerEnvironmentType;
import com.tms.environment.repository.ServerEnvironmentRepository;
import com.tms.testcase.dto.CreateTestCaseRequest;
import com.tms.testcase.entity.TestCaseBrowser;
import com.tms.testcase.entity.TestCaseDevice;
import com.tms.testcase.entity.TestCaseOs;
import com.tms.testcase.entity.TestCasePriority;
import com.tms.testcase.entity.TestCaseStatus;
import com.tms.testcase.entity.TestCaseType;
import com.tms.testcase.repository.TestCaseRepository;
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
class TestConfigurationControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired TestConfigurationRepository configurationRepository;
    @Autowired ServerEnvironmentRepository serverEnvironmentRepository;
    @Autowired TestCaseRepository testCaseRepository;

    @AfterEach
    void tearDown() {
        testCaseRepository.deleteAll();
        configurationRepository.deleteAll();
        serverEnvironmentRepository.deleteAll();
    }

    @Test
    void shouldManageConfigurationAndAssignItToTestCase() throws Exception {
        ServerEnvironment environment = serverEnvironmentRepository.save(new ServerEnvironment(
                "Staging", ServerEnvironmentType.STAGING, "https://staging.example.com", null, true
        ));

        MvcResult configurationResult = mockMvc.perform(post("/api/test-configurations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestConfigurationRequest(
                                "Staging Chrome Desktop", environment.getId(), TestCaseOs.MAC, "14 Sonoma",
                                TestCaseBrowser.CHROME, "120", TestCaseDevice.DESKTOP, "Java 21", "MySQL 8.0", true
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Staging Chrome Desktop"))
                .andExpect(jsonPath("$.serverEnvironment.id").value(environment.getId()))
                .andExpect(jsonPath("$.os").value("MAC"))
                .andExpect(jsonPath("$.osVersion").value("14 Sonoma"))
                .andExpect(jsonPath("$.browser").value("CHROME"))
                .andExpect(jsonPath("$.browserVersion").value("120"))
                .andExpect(jsonPath("$.runtimeVersion").value("Java 21"))
                .andExpect(jsonPath("$.dbVersion").value("MySQL 8.0"))
                .andReturn();
        long configurationId = objectMapper.readTree(configurationResult.getResponse().getContentAsString())
                .get("id").asLong();

        MvcResult testCaseResult = mockMvc.perform(post("/api/testcases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateTestCaseRequest(
                                TestCaseType.FUNCTIONAL, TestCasePriority.HIGH, TestCaseStatus.READY,
                                "Configured login", "Description", "Precondition", "Steps", null,
                                TestCaseOs.MAC, TestCaseBrowser.CHROME, TestCaseDevice.DESKTOP,
                                List.of(), environment.getId(), configurationId
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.testConfiguration.id").value(configurationId))
                .andReturn();
        long testCaseId = objectMapper.readTree(testCaseResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/test-configurations/{id}", configurationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestConfigurationRequest(
                                "Staging Safari Desktop", environment.getId(), TestCaseOs.MAC, "14 Sonoma",
                                TestCaseBrowser.SAFARI, "17", TestCaseDevice.DESKTOP, "Java 21", "MySQL 8.0", false
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.browser").value("SAFARI"))
                .andExpect(jsonPath("$.browserVersion").value("17"))
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(delete("/api/test-configurations/{id}", configurationId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/testcases/{id}", testCaseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.testConfiguration").doesNotExist());
    }

    @Test
    void shouldRejectDuplicateNameAndUnknownReferences() throws Exception {
        TestConfigurationRequest request = new TestConfigurationRequest(
                "Local Chrome", null, TestCaseOs.WINDOWS, "11",
                TestCaseBrowser.CHROME, "120", TestCaseDevice.DESKTOP, "Java 21", "MySQL 8.0", true
        );
        mockMvc.perform(post("/api/test-configurations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/test-configurations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("이미 존재하는 configuration입니다: Local Chrome"));

        mockMvc.perform(post("/api/test-configurations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestConfigurationRequest(
                                "Unknown server", 9999L, null, null, null, null, null, null, null, true
                        ))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/testcases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateTestCaseRequest(
                                TestCaseType.FUNCTIONAL, TestCasePriority.MEDIUM, TestCaseStatus.DRAFT,
                                "Unknown config", "Description", "Precondition", "Steps", null,
                                null, null, null, List.of(), null, 9999L
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("존재하지 않는 configuration입니다. id=9999"));

        mockMvc.perform(get("/api/test-configurations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }
}
