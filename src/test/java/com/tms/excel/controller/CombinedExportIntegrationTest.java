package com.tms.excel.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tms.defect.entity.Defect;
import com.tms.defect.entity.DefectSeverity;
import com.tms.defect.entity.DefectStatus;
import com.tms.defect.repository.DefectRepository;
import com.tms.testcase.entity.TestCase;
import com.tms.testcase.entity.TestCasePriority;
import com.tms.testcase.entity.TestCaseStatus;
import com.tms.testcase.entity.TestCaseType;
import com.tms.testcase.entity.TestFolder;
import com.tms.testcase.repository.TestCaseRepository;
import com.tms.testcase.repository.TestFolderRepository;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 다중 선택 결합 내보내기 — xlsx 멀티시트 / csv zip / 단일선택 폴백을 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CombinedExportIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired TestCaseRepository testCaseRepository;
    @Autowired TestFolderRepository testFolderRepository;
    @Autowired DefectRepository defectRepository;

    @AfterEach
    void tearDown() {
        defectRepository.deleteAll();
        testCaseRepository.deleteAll();
        testFolderRepository.deleteAll();
    }

    private void seed() {
        TestFolder folder = testFolderRepository.save(new TestFolder("로그인", null, 7L));
        TestCase tc = new TestCase(TestCaseType.FUNCTIONAL, TestCasePriority.HIGH, TestCaseStatus.READY,
                "로그인 성공", "설명", "사전조건", "스텝", "비고",
                null, null, null, new ArrayList<>(), null, null, "tester", "v1.0");
        tc.moveToFolder(folder);
        tc.setProjectId(7L);
        testCaseRepository.save(tc);
        defectRepository.save(new Defect("결제 실패", "오류", DefectSeverity.CRITICAL, DefectStatus.OPEN, null));
    }

    @Test
    void multiSelectXlsxHasMultipleSheets() throws Exception {
        seed();
        MvcResult result = mockMvc.perform(get(
                        "/api/export/combined?projectId=7&format=xlsx&types=test-cases,defects,test-plans"))
                .andExpect(status().isOk())
                .andExpect(r -> assertThat(r.getResponse().getContentType())
                        .contains("spreadsheetml.sheet"))
                .andReturn();

        byte[] xlsx = result.getResponse().getContentAsByteArray();
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(3);
            Set<String> names = new HashSet<>();
            for (int i = 0; i < wb.getNumberOfSheets(); i++) names.add(wb.getSheetName(i));
            assertThat(names).contains("테스트케이스", "결함 목록", "테스트플랜 구조");
        }
    }

    @Test
    void multiSelectCsvReturnsZipOfCsvs() throws Exception {
        seed();
        MvcResult result = mockMvc.perform(get(
                        "/api/export/combined?projectId=7&format=csv&types=test-cases,defects"))
                .andExpect(status().isOk())
                .andExpect(r -> assertThat(r.getResponse().getContentType()).contains("application/zip"))
                .andReturn();

        byte[] zip = result.getResponse().getContentAsByteArray();
        Set<String> entries = new HashSet<>();
        String firstContent = null;
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                entries.add(e.getName());
                byte[] data = zis.readAllBytes();
                if (firstContent == null) firstContent = new String(data, StandardCharsets.UTF_8);
            }
        }
        assertThat(entries).containsExactlyInAnyOrder("test-cases.csv", "defects.csv");
        assertThat(firstContent).startsWith("﻿"); // CSV 안에 BOM 유지
    }

    @Test
    void singleSelectFallsBackToSingleFile() throws Exception {
        seed();
        // 단일 선택이면 결합 엔드포인트도 단일 .xlsx(시트 1개)를 반환한다.
        MvcResult result = mockMvc.perform(get(
                        "/api/export/combined?projectId=7&format=xlsx&types=test-cases"))
                .andExpect(status().isOk())
                .andReturn();
        byte[] xlsx = result.getResponse().getContentAsByteArray();
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(1);
            assertThat(wb.getSheetName(0)).isEqualTo("테스트케이스");
        }
    }

    @Test
    void filteredPlusOthersUsesIds() throws Exception {
        seed();
        // test-cases-filtered 는 ids 로 받은 케이스만 → 0건 ids 면 헤더만(행 0)
        TestCase only = testCaseRepository.findAll().get(0);
        MvcResult result = mockMvc.perform(get(
                        "/api/export/combined?projectId=7&format=xlsx&types=test-cases-filtered,defects&ids=" + only.getId()))
                .andExpect(status().isOk())
                .andReturn();
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(2);
            assertThat(wb.getSheet("테스트케이스(필터)")).isNotNull();
            assertThat(wb.getSheet("테스트케이스(필터)").getLastRowNum()).isEqualTo(1); // 헤더 + 1건
        }
    }
}
