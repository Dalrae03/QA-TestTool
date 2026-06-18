package com.tms.defect.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.defect.dto.DefectRequest;
import com.tms.defect.entity.DefectSeverity;
import com.tms.defect.entity.DefectStatus;
import com.tms.defect.repository.DefectRepository;
import com.tms.testcase.dto.CreateTestCaseRequest;
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
class DefectControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired DefectRepository defectRepository;
    @Autowired TestCaseRepository testCaseRepository;

    @AfterEach
    void tearDown() {
        testCaseRepository.deleteAll();
        defectRepository.deleteAll();
    }

    @Test
    void shouldManageDefectCrud() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/defects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DefectRequest(
                                "로그인 버튼 클릭 시 500 에러", "재현 조건: 비밀번호 특수문자 포함",
                                DefectSeverity.CRITICAL, DefectStatus.OPEN, "https://jira.example.com/TMS-1"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("로그인 버튼 클릭 시 500 에러"))
                .andExpect(jsonPath("$.severity").value("CRITICAL"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.externalUrl").value("https://jira.example.com/TMS-1"))
                .andReturn();
        long defectId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/defects/{id}", defectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(defectId));

        mockMvc.perform(put("/api/defects/{id}", defectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DefectRequest(
                                "로그인 버튼 클릭 시 500 에러 (수정)", null,
                                DefectSeverity.MAJOR, DefectStatus.IN_PROGRESS, null
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.severity").value("MAJOR"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.externalUrl").doesNotExist());

        mockMvc.perform(get("/api/defects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(delete("/api/defects/{id}", defectId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/defects/{id}", defectId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldLinkAndUnlinkDefectToTestCase() throws Exception {
        MvcResult defectResult = mockMvc.perform(post("/api/defects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DefectRequest(
                                "회원가입 이메일 중복 허용 버그", null,
                                DefectSeverity.MAJOR, DefectStatus.OPEN, null
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        long defectId = objectMapper.readTree(defectResult.getResponse().getContentAsString()).get("id").asLong();

        MvcResult testCaseResult = mockMvc.perform(post("/api/testcases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateTestCaseRequest(
                                TestCaseType.FUNCTIONAL, TestCasePriority.HIGH, TestCaseStatus.READY,
                                "회원가입 이메일 중복 검증", "설명", "사전조건", "단계", null,
                                null, null, null, List.of()
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        long testCaseId = objectMapper.readTree(testCaseResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/testcases/{id}/defects/{defectId}", testCaseId, defectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defects", hasSize(1)))
                .andExpect(jsonPath("$.defects[0].id").value(defectId))
                .andExpect(jsonPath("$.defects[0].severity").value("MAJOR"));

        // 중복 연결 시 하나만 유지
        mockMvc.perform(post("/api/testcases/{id}/defects/{defectId}", testCaseId, defectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defects", hasSize(1)));

        mockMvc.perform(delete("/api/testcases/{id}/defects/{defectId}", testCaseId, defectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defects", hasSize(0)));
    }

    @Test
    void shouldReturn404ForUnknownDefectOrTestCase() throws Exception {
        mockMvc.perform(get("/api/defects/9999"))
                .andExpect(status().isNotFound());

        MvcResult testCaseResult = mockMvc.perform(post("/api/testcases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateTestCaseRequest(
                                TestCaseType.FUNCTIONAL, TestCasePriority.MEDIUM, TestCaseStatus.DRAFT,
                                "존재하는 케이스", "설명", "사전조건", "단계", null,
                                null, null, null, List.of()
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        long testCaseId = objectMapper.readTree(testCaseResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/testcases/{id}/defects/9999", testCaseId))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/testcases/9999/defects/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectInvalidDefectRequest() throws Exception {
        mockMvc.perform(post("/api/defects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DefectRequest(
                                "", null, null, null, null
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.severity").exists());
    }
}
