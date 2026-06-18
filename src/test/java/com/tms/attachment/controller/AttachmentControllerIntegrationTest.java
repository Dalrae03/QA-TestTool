package com.tms.attachment.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.attachment.repository.AttachmentRepository;
import com.tms.defect.dto.DefectRequest;
import com.tms.defect.entity.DefectSeverity;
import com.tms.defect.entity.DefectStatus;
import com.tms.defect.repository.DefectRepository;
import com.tms.testcase.dto.CreateTestCaseRequest;
import com.tms.testcase.entity.TestCasePriority;
import com.tms.testcase.entity.TestCaseStatus;
import com.tms.testcase.entity.TestCaseType;
import com.tms.testcase.repository.TestCaseRepository;
import com.tms.testrun.dto.CreateTestRunRequest;
import com.tms.testrun.entity.TestRunStatus;
import com.tms.testrun.repository.TestRunRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AttachmentControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AttachmentRepository attachmentRepository;
    @Autowired TestCaseRepository testCaseRepository;
    @Autowired DefectRepository defectRepository;
    @Autowired TestRunRepository testRunRepository;

    @AfterEach
    void tearDown() {
        attachmentRepository.deleteAll();
        testRunRepository.deleteAll();
        testCaseRepository.deleteAll();
        defectRepository.deleteAll();
    }

    @Test
    void shouldUploadAndDownloadAttachmentForTestCase() throws Exception {
        long testCaseId = createTestCase("파일 첨부 테스트");

        MockMultipartFile file = new MockMultipartFile(
                "file", "screenshot.png", "image/png", "fake-image-content".getBytes()
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/testcases/{id}/attachments", testCaseId)
                        .file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalFilename").value("screenshot.png"))
                .andExpect(jsonPath("$.contentType").value("image/png"))
                .andExpect(jsonPath("$.fileSize").value("fake-image-content".getBytes().length))
                .andExpect(jsonPath("$.entityType").value("TEST_CASE"))
                .andExpect(jsonPath("$.entityId").value(testCaseId))
                .andReturn();
        long attachmentId = objectMapper.readTree(uploadResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/testcases/{id}/attachments", testCaseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(attachmentId));

        mockMvc.perform(get("/api/attachments/{id}/download", attachmentId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("screenshot.png")))
                .andExpect(content().bytes("fake-image-content".getBytes()));

        mockMvc.perform(delete("/api/attachments/{id}", attachmentId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/testcases/{id}/attachments", testCaseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldUploadAttachmentForDefect() throws Exception {
        long defectId = createDefect("로그인 버그");

        MockMultipartFile file = new MockMultipartFile(
                "file", "log.txt", "text/plain", "error stack trace".getBytes()
        );

        mockMvc.perform(multipart("/api/defects/{id}/attachments", defectId).file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalFilename").value("log.txt"))
                .andExpect(jsonPath("$.entityType").value("DEFECT"))
                .andExpect(jsonPath("$.entityId").value(defectId));

        mockMvc.perform(get("/api/defects/{id}/attachments", defectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void shouldUploadAttachmentForTestRun() throws Exception {
        long testCaseId = createTestCase("런 첨부 테스트");
        long runId = createTestRun(testCaseId);

        MockMultipartFile file = new MockMultipartFile(
                "file", "result.txt", "text/plain", "test result content".getBytes()
        );

        mockMvc.perform(multipart("/api/testcases/{testCaseId}/runs/{runId}/attachments", testCaseId, runId)
                        .file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.entityType").value("TEST_RUN"))
                .andExpect(jsonPath("$.entityId").value(runId));

        mockMvc.perform(get("/api/testcases/{testCaseId}/runs/{runId}/attachments", testCaseId, runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void shouldDeleteAttachmentsWhenTestCaseIsDeleted() throws Exception {
        long testCaseId = createTestCase("삭제 연동 테스트");

        mockMvc.perform(multipart("/api/testcases/{id}/attachments", testCaseId)
                        .file(new MockMultipartFile("file", "a.txt", "text/plain", "content".getBytes())))
                .andExpect(status().isCreated());
        mockMvc.perform(multipart("/api/testcases/{id}/attachments", testCaseId)
                        .file(new MockMultipartFile("file", "b.txt", "text/plain", "content".getBytes())))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/testcases/{id}", testCaseId))
                .andExpect(status().isNoContent());

        // 첨부파일도 DB에서 모두 삭제됐는지 확인
        assert attachmentRepository.count() == 0;
    }

    @Test
    void shouldReturn404ForUnknownAttachment() throws Exception {
        mockMvc.perform(get("/api/attachments/9999/download"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/attachments/9999"))
                .andExpect(status().isNotFound());
    }

    private long createTestCase(String title) throws Exception {
        MvcResult result = mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/testcases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateTestCaseRequest(
                                TestCaseType.FUNCTIONAL, TestCasePriority.HIGH, TestCaseStatus.READY,
                                title, "설명", "사전조건", "단계", null,
                                null, null, null, List.of()
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createDefect(String title) throws Exception {
        MvcResult result = mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/defects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DefectRequest(
                                title, null, DefectSeverity.MAJOR, DefectStatus.OPEN, null
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createTestRun(long testCaseId) throws Exception {
        MvcResult result = mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/testcases/{id}/runs", testCaseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateTestRunRequest(
                                TestRunStatus.PASSED, "실행 결과", null, null
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }
}
