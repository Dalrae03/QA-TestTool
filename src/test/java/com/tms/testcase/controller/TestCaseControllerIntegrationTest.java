package com.tms.testcase.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.testcase.dto.CreateTestCaseRequest;
import com.tms.testcase.dto.UpdateTestCaseRequest;
import com.tms.testcase.entity.TestCaseType;
import com.tms.testcase.repository.TestCaseRepository;
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
class TestCaseControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestCaseRepository testCaseRepository;

    @AfterEach
    void tearDown() {
        testCaseRepository.deleteAll();
    }

    @Test
    void shouldSupportFullCrudFlow() throws Exception {
        CreateTestCaseRequest createRequest = new CreateTestCaseRequest(
                TestCaseType.FUNCTIONAL,
                "Login success test",
                "Verify a user can log in with valid credentials.",
                "A registered user exists.",
                "1. Open login page\n2. Enter valid credentials\n3. Submit",
                "User is redirected to the dashboard.",
                "Smoke test"
        );

        MvcResult createResult = mockMvc.perform(post("/api/testcases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/testcases/\\d+")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.type").value("FUNCTIONAL"))
                .andExpect(jsonPath("$.title").value("Login success test"))
                .andReturn();

        Long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/testcases/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.description").value("Verify a user can log in with valid credentials."));

        mockMvc.perform(get("/api/testcases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(id));

        UpdateTestCaseRequest updateRequest = new UpdateTestCaseRequest(
                TestCaseType.NON_FUNCTIONAL,
                "Login performance test",
                "Verify login response time under load.",
                "Performance environment is available.",
                "1. Send concurrent login requests\n2. Measure response times",
                "Average response time stays within the threshold.",
                "Updated note"
        );

        mockMvc.perform(put("/api/testcases/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.type").value("NON_FUNCTIONAL"))
                .andExpect(jsonPath("$.title").value("Login performance test"))
                .andExpect(jsonPath("$.notes").value("Updated note"));

        mockMvc.perform(delete("/api/testcases/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/testcases/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("TestCase not found. id=" + id));
    }
}
