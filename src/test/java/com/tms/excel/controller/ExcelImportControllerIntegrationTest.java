package com.tms.excel.controller;

import static org.assertj.core.api.Assertions.assertThat;
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
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 엑셀 임포트 기능을 실제 .xlsx 생성 → HTTP 업로드 → DB 저장까지 종단 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ExcelImportControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired TestCaseRepository testCaseRepository;
    @Autowired TestFolderRepository testFolderRepository;

    @AfterEach
    void tearDown() {
        testCaseRepository.deleteAll();
        testFolderRepository.deleteAll();
    }

    @Test
    void importsTestCasesFromUploadedExcel() throws Exception {
        byte[] xlsx = buildWorkbook();
        MockMultipartFile file = new MockMultipartFile(
                "file", "회원관리.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsx);

        mockMvc.perform(multipart("/api/import/excel").file(file).param("projectId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCases").value(2))
                .andExpect(jsonPath("$.createdFolders").value(2))   // 루트(파일명) + 시트
                .andExpect(jsonPath("$.errors").isEmpty());

        List<TestCase> saved = testCaseRepository.findAll();
        assertThat(saved).hasSize(2);

        TestCase loginSuccess = saved.stream()
                .filter(tc -> "로그인 성공".equals(tc.getTitle()))
                .findFirst().orElseThrow();
        assertThat(loginSuccess.getPriority()).isEqualTo(TestCasePriority.HIGH);
        assertThat(loginSuccess.getStatus()).isEqualTo(TestCaseStatus.READY);
        assertThat(loginSuccess.getType()).isEqualTo(TestCaseType.FUNCTIONAL);
        assertThat(loginSuccess.getProjectId()).isEqualTo(7L);
        assertThat(loginSuccess.getFolder()).isNotNull();

        // lazy 프록시 탐색을 피하려고 폴더 ID로 다시 조회해 트리 구조를 검증한다.
        TestFolder sheetFolder = testFolderRepository.findById(loginSuccess.getFolder().getId()).orElseThrow();
        assertThat(sheetFolder.getName()).isEqualTo("로그인");                          // 시트 → 하위 폴더
        assertThat(sheetFolder.getProjectId()).isEqualTo(7L);
        TestFolder rootFolder = testFolderRepository.findById(sheetFolder.getParent().getId()).orElseThrow();
        assertThat(rootFolder.getName()).isEqualTo("회원관리");                          // 파일명 → 루트 폴더
        assertThat(rootFolder.getParent()).isNull();
    }

    @Test
    void rejectsNonExcelFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "그냥 텍스트".getBytes());

        mockMvc.perform(multipart("/api/import/excel").file(file))
                .andExpect(status().isBadRequest());
    }

    /** 한글 헤더 + 영문 enum 값 + 빈 제목 행(스킵) 을 포함한 워크북을 만든다. */
    private byte[] buildWorkbook() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("로그인");

            writeRow(sheet, 0, "제목", "설명", "우선순위", "상태");
            writeRow(sheet, 1, "로그인 성공", "정상 계정 로그인", "HIGH", "READY");
            writeRow(sheet, 2, "로그인 실패", "잘못된 비밀번호", "LOW", "DRAFT");
            writeRow(sheet, 3, "", "제목 없는 행 — 무시되어야 함", "MEDIUM", "DRAFT");

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void writeRow(Sheet sheet, int rowIndex, String... values) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
    }
}
