package com.tms.excel.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tms.testcase.entity.TestCase;
import com.tms.testcase.entity.TestCasePriority;
import com.tms.testcase.entity.TestCaseStatus;
import com.tms.testcase.entity.TestCaseType;
import com.tms.testcase.entity.TestFolder;
import com.tms.testcase.repository.TestCaseRepository;
import com.tms.testcase.repository.TestFolderRepository;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

/**
 * CSV 내보내기/가져오기 + 라운드트립을 종단 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CsvImportExportIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired TestCaseRepository testCaseRepository;
    @Autowired TestFolderRepository testFolderRepository;

    @AfterEach
    void tearDown() {
        testCaseRepository.deleteAll();
        testFolderRepository.deleteAll();
    }

    @Test
    void exportsTestCasesAsCsv() throws Exception {
        TestFolder folder = testFolderRepository.save(new TestFolder("로그인", null, 7L));
        newCase("로그인 성공", TestCasePriority.HIGH, folder);
        // 콤마가 포함된 제목 → CSV 따옴표 이스케이프 확인
        newCase("결제, 카드", TestCasePriority.LOW, folder);

        byte[] csv = mockMvc.perform(get("/api/export/test-cases/excel?projectId=7&format=csv"))
                .andExpect(status().isOk())
                .andExpect(header -> assertThat(header.getResponse().getContentType()).contains("text/csv"))
                .andReturn().getResponse().getContentAsByteArray();

        String text = new String(csv, StandardCharsets.UTF_8);
        assertThat(text).startsWith("﻿"); // UTF-8 BOM
        assertThat(text).contains("ID,제목,폴더");
        assertThat(text).contains("\"결제, 카드\""); // 콤마 셀은 따옴표로 감싼다
        System.out.println("[csv-export] 미리보기:\n" + text.substring(1, Math.min(text.length(), 200)));
    }

    @Test
    void importsCsvWithFolderColumn() throws Exception {
        String csv = "﻿제목,우선순위,상태,폴더\r\n"
                + "로그인 성공,HIGH,READY,로그인\r\n"
                + "\"결제, 카드\",LOW,DRAFT,결제\r\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "회귀.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/import/excel").file(file).param("projectId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCases").value(2))
                .andExpect(jsonPath("$.createdFolders").value(3))  // 루트(회귀) + 로그인 + 결제
                .andExpect(jsonPath("$.errors").isEmpty());

        List<TestCase> saved = testCaseRepository.findAll();
        assertThat(saved).hasSize(2);

        TestCase payment = saved.stream().filter(tc -> "결제, 카드".equals(tc.getTitle())).findFirst().orElseThrow();
        assertThat(payment.getPriority()).isEqualTo(TestCasePriority.LOW);
        assertThat(payment.getProjectId()).isEqualTo(7L);
        assertThat(payment.getFolder()).isNotNull();
        // 폴더 컬럼 값이 하위 폴더가 됐는지(다시 조회해 lazy 프록시 회피)
        TestFolder sub = testFolderRepository.findById(payment.getFolder().getId()).orElseThrow();
        assertThat(sub.getName()).isEqualTo("결제");
    }

    @Test
    void csvRoundTrip() throws Exception {
        // 1) 데이터 생성 → CSV export
        TestFolder folder = testFolderRepository.save(new TestFolder("로그인", null, 7L));
        newCase("로그인 성공", TestCasePriority.HIGH, folder);
        newCase("로그인 실패", TestCasePriority.MEDIUM, folder);

        byte[] csv = mockMvc.perform(get("/api/export/test-cases/excel?projectId=7&format=csv"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        long casesBefore = testCaseRepository.count();

        // 2) 내려받은 CSV 를 그대로 다시 import
        MockMultipartFile file = new MockMultipartFile(
                "file", "재가져오기.csv", "text/csv", csv);
        mockMvc.perform(multipart("/api/import/excel").file(file).param("projectId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCases").value(2))
                .andExpect(jsonPath("$.errors").isEmpty());

        // 3) 케이스가 추가로 복원됨 + 값 보존
        assertThat(testCaseRepository.count()).isEqualTo(casesBefore + 2);
        long reimported = testCaseRepository.findAll().stream()
                .filter(tc -> "로그인 성공".equals(tc.getTitle()))
                .filter(tc -> tc.getPriority() == TestCasePriority.HIGH)
                .count();
        assertThat(reimported).isEqualTo(2); // 원본 1 + 재가져오기 1
    }

    private TestCase newCase(String title, TestCasePriority priority, TestFolder folder) {
        TestCase tc = new TestCase(TestCaseType.FUNCTIONAL, priority, TestCaseStatus.READY, title,
                "설명 " + title, "사전조건", "1) 진입\n2) 확인", "비고",
                null, null, null, new ArrayList<>(), null, null, "tester@example.com", "v1.0");
        tc.moveToFolder(folder);
        tc.setProjectId(7L);
        return testCaseRepository.save(tc);
    }
}
