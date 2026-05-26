package com.tms.testcase.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.testcase.dto.CreateAreaTagRequest;
import com.tms.testcase.entity.AreaTag;
import com.tms.testcase.entity.TestCase;
import com.tms.testcase.entity.TestCasePriority;
import com.tms.testcase.entity.TestCaseStatus;
import com.tms.testcase.entity.TestCaseType;
import com.tms.testcase.repository.AreaTagRepository;
import com.tms.testcase.repository.TestCaseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AreaTagControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AreaTagRepository areaTagRepository;

    @Autowired
    private TestCaseRepository testCaseRepository;

    @AfterEach
    void tearDown() {
        testCaseRepository.deleteAll();
        areaTagRepository.deleteAll();
    }

    @Test
    void shouldRejectDuplicateTagNamesIgnoringCaseAndWhitespace() throws Exception {
        areaTagRepository.save(new AreaTag("Smoke"));

        mockMvc.perform(post("/api/area-tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateAreaTagRequest(" smoke "))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("이미 존재하는 태그입니다: smoke"));
    }

    @Test
    void shouldDeleteTagEvenWhenLinkedToTestCase() throws Exception {
        AreaTag tag = areaTagRepository.save(new AreaTag("Regression"));
        TestCase testCase = testCaseRepository.save(new TestCase(
                TestCaseType.FUNCTIONAL,
                TestCasePriority.HIGH,
                TestCaseStatus.DRAFT,
                "Linked tag testcase",
                "desc",
                "precondition",
                "step",
                "expected",
                null,
                null,
                null,
                null,
                java.util.List.of(tag)
        ));

        mockMvc.perform(delete("/api/area-tags/{id}", tag.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/testcases/{id}", testCase.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.areaTags").isArray())
                .andExpect(jsonPath("$.areaTags").isEmpty());
    }
}
