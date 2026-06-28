package com.tms.excel.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tms.defect.entity.Defect;
import com.tms.defect.entity.DefectSeverity;
import com.tms.defect.entity.DefectStatus;
import com.tms.defect.repository.DefectRepository;
import com.tms.execution.entity.Execution;
import com.tms.execution.entity.ExecutionItem;
import com.tms.execution.entity.ResultStatus;
import com.tms.execution.repository.ExecutionRepository;
import com.tms.testcase.entity.TestCase;
import com.tms.testcase.entity.TestCaseBrowser;
import com.tms.testcase.entity.TestCaseDevice;
import com.tms.testcase.entity.TestCaseOs;
import com.tms.testcase.entity.TestCasePriority;
import com.tms.testcase.entity.TestCaseStatus;
import com.tms.testcase.entity.TestCaseType;
import com.tms.testcase.entity.TestFolder;
import com.tms.testcase.repository.TestCaseRepository;
import com.tms.testcase.repository.TestFolderRepository;
import com.tms.testplan.entity.TestPlan;
import com.tms.testplan.entity.TestPlanStatus;
import com.tms.testplan.repository.TestPlanRepository;
import com.tms.testsuite.entity.TestSuite;
import com.tms.testsuite.repository.TestSuiteRepository;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 테스트케이스/테스트런/결함/플랜 4종 엑셀 내보내기 + 필터링 내보내기를 종단 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ExcelExportTypesIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired TestCaseRepository testCaseRepository;
    @Autowired TestFolderRepository testFolderRepository;
    @Autowired TestSuiteRepository testSuiteRepository;
    @Autowired TestPlanRepository testPlanRepository;
    @Autowired ExecutionRepository executionRepository;
    @Autowired DefectRepository defectRepository;

    @AfterEach
    void tearDown() {
        executionRepository.deleteAll();
        testSuiteRepository.deleteAll();
        testPlanRepository.deleteAll();
        defectRepository.deleteAll();
        testCaseRepository.deleteAll();
        testFolderRepository.deleteAll();
    }

    @Test
    void exportsTestCasesAllAndFiltered() throws Exception {
        TestFolder folder = testFolderRepository.save(new TestFolder("로그인", null, 7L));
        TestCase a = newCase("로그인 성공", TestCasePriority.HIGH, folder);
        newCase("로그인 실패", TestCasePriority.LOW, folder);

        // 전체
        SheetData all = readFirstSheet(getXlsx("/api/export/test-cases/excel?projectId=7"));
        assertThat(all.name()).isEqualTo("테스트케이스");
        assertThat(all.cell(0, 1)).isEqualTo("제목");
        assertThat(all.lastRow()).isEqualTo(2); // 헤더 + 2건

        // 필터링(한 건만 ID로 지정)
        SheetData filtered = readFirstSheet(getXlsx("/api/export/test-cases/excel?projectId=7&ids=" + a.getId()));
        assertThat(filtered.lastRow()).isEqualTo(1); // 헤더 + 1건
        assertThat(filtered.cell(1, 1)).isEqualTo("로그인 성공");
    }

    @Test
    void exportsTestRunResults() throws Exception {
        Execution exec = new Execution("스프린트1 회귀", null, 7L, null, "플랜A", null, "스위트A", "tester");
        exec.addItem(100L, "로그인 성공");
        exec.addItem(101L, "로그인 실패");
        exec = executionRepository.save(exec);
        List<ExecutionItem> items = exec.getItems();
        items.get(0).record(ResultStatus.PASSED, "정상", null);
        items.get(1).record(ResultStatus.FAILED, "재현됨", "https://jira/BUG-1");
        executionRepository.save(exec);

        SheetData sheet = readFirstSheet(getXlsx("/api/export/test-runs/excel?projectId=7"));
        assertThat(sheet.name()).isEqualTo("테스트런 결과");
        assertThat(sheet.cell(0, 0)).isEqualTo("테스트런");
        assertThat(sheet.lastRow()).isEqualTo(2); // 헤더 + 2개 아이템
        assertThat(sheet.cell(0, 8)).isEqualTo("사유/결함"); // 실패 사유/결함 링크 열
        boolean hasFailureReason = false;
        for (int r = 1; r <= sheet.lastRow(); r++) {
            if ("https://jira/BUG-1".equals(sheet.cell(r, 8))) hasFailureReason = true;
        }
        assertThat(hasFailureReason).isTrue();
    }

    @Test
    void exportsDefects() throws Exception {
        defectRepository.save(new Defect("결제 실패", "카드 결제 시 오류", DefectSeverity.CRITICAL, DefectStatus.OPEN, "https://jira/BUG-9"));

        SheetData sheet = readFirstSheet(getXlsx("/api/export/defects/excel"));
        assertThat(sheet.name()).isEqualTo("결함 목록");
        assertThat(sheet.cell(0, 1)).isEqualTo("제목");
        assertThat(sheet.cell(1, 1)).isEqualTo("결제 실패");
        assertThat(sheet.cell(1, 2)).isEqualTo("CRITICAL");
    }

    @Test
    void exportsTestPlanStructure() throws Exception {
        TestFolder folder = testFolderRepository.save(new TestFolder("로그인", null, 7L));
        TestCase a = newCase("로그인 성공", TestCasePriority.HIGH, folder);

        TestPlan plan = new TestPlan("릴리스 1.0", "설명", TestPlanStatus.ACTIVE, "리드",
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
        plan.setProjectId(7L);
        plan = testPlanRepository.save(plan);

        TestSuite suite = new TestSuite(plan, "로그인 스위트", "설명", List.of(a));
        suite.setProjectId(7L);
        testSuiteRepository.save(suite);

        SheetData sheet = readFirstSheet(getXlsx("/api/export/test-plans/excel?projectId=7"));
        assertThat(sheet.name()).isEqualTo("테스트플랜 구조");
        assertThat(sheet.cell(0, 0)).isEqualTo("테스트플랜");
        assertThat(sheet.cell(1, 0)).isEqualTo("릴리스 1.0");
        assertThat(sheet.cell(1, 4)).isEqualTo("로그인 스위트");
        assertThat(sheet.cell(1, 5)).isEqualTo("로그인 성공");
    }

    // ── helpers ──────────────────────────────────────────────────────

    private byte[] getXlsx(String url) throws Exception {
        MvcResult result = mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn();
        byte[] bytes = result.getResponse().getContentAsByteArray();
        assertThat(bytes).isNotEmpty();
        return bytes;
    }

    /** 첫 시트의 이름과 셀 값을 메모리로 읽어 워크북 close 이후에도 검증 가능하게 한다. */
    private SheetData readFirstSheet(byte[] xlsx) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            Sheet sheet = wb.getSheetAt(0);
            int lastRow = sheet.getLastRowNum();
            List<List<String>> grid = new ArrayList<>();
            for (int r = 0; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                List<String> cells = new ArrayList<>();
                if (row != null) {
                    for (int c = 0; c < row.getLastCellNum(); c++) {
                        cells.add(row.getCell(c) != null ? row.getCell(c).getStringCellValue() : "");
                    }
                }
                grid.add(cells);
            }
            return new SheetData(sheet.getSheetName(), grid);
        }
    }

    private TestCase newCase(String title, TestCasePriority priority, TestFolder folder) {
        TestCase tc = new TestCase(TestCaseType.FUNCTIONAL, priority, TestCaseStatus.READY, title,
                "설명 " + title, "사전조건", "1) 진입\n2) 확인", "비고",
                TestCaseOs.MAC, TestCaseBrowser.CHROME, TestCaseDevice.MOBILE,
                new ArrayList<>(), null, null, "tester@example.com", "v1.0");
        tc.moveToFolder(folder);
        tc.setProjectId(7L);
        return testCaseRepository.save(tc);
    }

    private record SheetData(String name, List<List<String>> grid) {
        int lastRow() { return grid.size() - 1; }
        String cell(int r, int c) {
            if (r < 0 || r >= grid.size()) return "";
            List<String> row = grid.get(r);
            return c >= 0 && c < row.size() ? row.get(c) : "";
        }
    }
}
