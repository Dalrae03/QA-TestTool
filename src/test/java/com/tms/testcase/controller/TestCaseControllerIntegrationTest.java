package com.tms.testcase.controller;

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
import com.tms.testcase.dto.CreateTestCaseRequest;
import com.tms.testcase.dto.UpdateTestCaseRequest;
import com.tms.testcase.dto.UpdateTestCaseStatusRequest;
import com.tms.testcase.entity.AreaTag;
import com.tms.testcase.entity.TestCaseBrowser;
import com.tms.testcase.entity.TestCaseDevice;
import com.tms.testcase.entity.TestCaseOs;
import com.tms.testcase.entity.TestCasePriority;
import com.tms.testcase.entity.TestCaseStatus;
import com.tms.testcase.entity.TestCaseType;
import com.tms.testcase.repository.AreaTagRepository;
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
class TestCaseControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private AreaTagRepository areaTagRepository;

    @AfterEach
    void tearDown() {
        testCaseRepository.deleteAll();
        areaTagRepository.deleteAll();
    }

    @Test
    void shouldSupportFullCrudFlow() throws Exception {
        CreateTestCaseRequest createRequest = new CreateTestCaseRequest(
                TestCaseType.FUNCTIONAL,
                TestCasePriority.HIGH,
                TestCaseStatus.DRAFT,
                "Login success test",
                "Verify a user can log in with valid credentials.",
                "A registered user exists.",
                "1. Open login page\n2. Enter valid credentials\n3. Submit",
                "Smoke test",
                TestCaseOs.MAC,
                TestCaseBrowser.CHROME,
                TestCaseDevice.DESKTOP,
                List.of()
        );

        MvcResult createResult = mockMvc.perform(post("/api/testcases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/testcases/\\d+")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.type").value("FUNCTIONAL"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.title").value("Login success test"))
                .andExpect(jsonPath("$.expected").doesNotExist())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andReturn();

        Long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/testcases/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.description").value("Verify a user can log in with valid credentials."));

        mockMvc.perform(get("/api/testcases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].status").value("DRAFT"))
                .andExpect(jsonPath("$[0].os").value("MAC"));

        UpdateTestCaseRequest updateRequest = new UpdateTestCaseRequest(
                TestCaseType.NON_FUNCTIONAL,
                TestCasePriority.LOW,
                TestCaseStatus.REVIEW_NEEDED,
                "Login performance test",
                "Verify login response time under load.",
                "Performance environment is available.",
                "1. Send concurrent login requests\n2. Measure response times",
                "Updated note",
                TestCaseOs.WINDOWS,
                TestCaseBrowser.EDGE,
                TestCaseDevice.DESKTOP,
                List.of()
        );

        mockMvc.perform(put("/api/testcases/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.type").value("NON_FUNCTIONAL"))
                .andExpect(jsonPath("$.priority").value("LOW"))
                .andExpect(jsonPath("$.status").value("REVIEW_NEEDED"))
                .andExpect(jsonPath("$.browser").value("EDGE"))
                .andExpect(jsonPath("$.title").value("Login performance test"))
                .andExpect(jsonPath("$.notes").value("Updated note"))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        mockMvc.perform(patch("/api/testcases/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateTestCaseStatusRequest(TestCaseStatus.COMPLETED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(delete("/api/testcases/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/testcases/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("TestCase not found. id=" + id));
    }

    @Test
    void shouldRejectCreateWhenRequiredFieldsAreBlank() throws Exception {
        CreateTestCaseRequest invalidRequest = new CreateTestCaseRequest(
                TestCaseType.FUNCTIONAL,
                TestCasePriority.MEDIUM,
                TestCaseStatus.DRAFT,
                "   ",
                "",
                " ",
                "",
                null,
                null,
                null,
                null,
                List.of()
        );

        mockMvc.perform(post("/api/testcases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.description").exists())
                .andExpect(jsonPath("$.errors.precondition").exists())
                .andExpect(jsonPath("$.errors.steps").exists());
    }

    @Test
    void shouldFilterByStatusOsAndAreaTag() throws Exception {
        AreaTag smoke = areaTagRepository.save(new AreaTag("Smoke"));
        AreaTag regression = areaTagRepository.save(new AreaTag("Regression"));

        testCaseRepository.save(new com.tms.testcase.entity.TestCase(
                TestCaseType.FUNCTIONAL,
                TestCasePriority.HIGH,
                TestCaseStatus.READY,
                "Mac smoke login",
                "Mac login flow",
                "User exists",
                "Open page",
                null,
                TestCaseOs.MAC,
                TestCaseBrowser.SAFARI,
                TestCaseDevice.DESKTOP,
                List.of(smoke)
        ));

        testCaseRepository.save(new com.tms.testcase.entity.TestCase(
                TestCaseType.NON_FUNCTIONAL,
                TestCasePriority.LOW,
                TestCaseStatus.DRAFT,
                "Windows regression login",
                "Windows login perf",
                "Perf env exists",
                "Run load",
                null,
                TestCaseOs.WINDOWS,
                TestCaseBrowser.EDGE,
                TestCaseDevice.DESKTOP,
                List.of(regression)
        ));

        mockMvc.perform(get("/api/testcases")
                        .param("status", "READY")
                        .param("os", "MAC")
                        .param("areaTagId", String.valueOf(smoke.getId()))
                        .param("keyword", "login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Mac smoke login"))
                .andExpect(jsonPath("$[0].areaTags[0].name").value("Smoke"));
    }

    @Test
    void shouldRejectUnknownAreaTagIds() throws Exception {
        CreateTestCaseRequest request = new CreateTestCaseRequest(
                TestCaseType.FUNCTIONAL,
                TestCasePriority.HIGH,
                TestCaseStatus.DRAFT,
                "Unknown tag test",
                "Verify invalid tag ids fail",
                "None",
                "Run",
                null,
                null,
                null,
                null,
                List.of(9999L)
        );

        mockMvc.perform(post("/api/testcases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("존재하지 않는 태그 ID가 포함되어 있습니다: [9999]"));
    }
}
