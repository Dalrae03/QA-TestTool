package com.tms.excel.service;

import com.tms.defect.entity.Defect;
import com.tms.defect.repository.DefectRepository;
import com.tms.execution.entity.Execution;
import com.tms.execution.entity.ExecutionItem;
import com.tms.testcase.entity.AreaTag;
import com.tms.testcase.entity.TestCase;
import com.tms.testcase.entity.TestFolder;
import com.tms.testcase.repository.TestCaseRepository;
import com.tms.execution.repository.ExecutionRepository;
import com.tms.global.exception.InvalidRequestException;
import com.tms.testplan.entity.TestPlan;
import com.tms.testplan.repository.TestPlanRepository;
import com.tms.testsuite.entity.TestSuite;
import com.tms.testsuite.repository.TestSuiteRepository;
import jakarta.persistence.EntityNotFoundException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 테스트 스위트를 엑셀(.xlsx)로 내보낸다. 스위트에 속한 테스트케이스를 폴더별 시트로 묶어 출력하며,
 * 헤더는 ExcelImportService 가 인식하는 한글 별칭을 사용해 가져오기와 라운드트립이 가능하다.
 */
@Service
@Transactional(readOnly = true)
public class ExcelExportService {

    /** 출력 헤더(한글) — ExcelImportService.COLUMN_ALIASES 와 호환된다. */
    private static final String[] HEADERS = {
            "제목", "설명", "전제조건", "스텝", "메모",
            "우선순위", "상태", "유형", "OS", "브라우저", "디바이스", "담당자", "버전"
    };

    private static final String UNFILED_SHEET = "(미분류)";

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final TestSuiteRepository testSuiteRepository;
    private final TestCaseRepository testCaseRepository;
    private final ExecutionRepository executionRepository;
    private final DefectRepository defectRepository;
    private final TestPlanRepository testPlanRepository;

    public ExcelExportService(TestSuiteRepository testSuiteRepository,
                              TestCaseRepository testCaseRepository,
                              ExecutionRepository executionRepository,
                              DefectRepository defectRepository,
                              TestPlanRepository testPlanRepository) {
        this.testSuiteRepository = testSuiteRepository;
        this.testCaseRepository = testCaseRepository;
        this.executionRepository = executionRepository;
        this.defectRepository = defectRepository;
        this.testPlanRepository = testPlanRepository;
    }

    public record ExcelFile(String filename, byte[] content) {}

    public ExcelFile exportSuite(Long suiteId) {
        TestSuite suite = testSuiteRepository.findById(suiteId)
                .orElseThrow(() -> new EntityNotFoundException("TestSuite not found. id=" + suiteId));

        // 폴더명 기준으로 케이스를 묶는다(폴더 없으면 미분류). 입력 순서를 보존한다.
        Map<String, List<TestCase>> byFolder = new LinkedHashMap<>();
        for (TestCase tc : suite.getTestCases()) {
            TestFolder folder = tc.getFolder();
            String key = folder != null ? folder.getName() : UNFILED_SHEET;
            byFolder.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(tc);
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = headerStyle(workbook);

            if (byFolder.isEmpty()) {
                // 빈 스위트라도 헤더만 있는 시트를 하나 만든다.
                writeSheet(workbook, headerStyle, suite.getName(), List.of());
            } else {
                java.util.Set<String> usedNames = new java.util.HashSet<>();
                for (Map.Entry<String, List<TestCase>> entry : byFolder.entrySet()) {
                    String sheetName = uniqueSheetName(entry.getKey(), usedNames);
                    writeSheet(workbook, headerStyle, sheetName, entry.getValue());
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return new ExcelFile(safeFilename(suite.getName()) + ".xlsx", out.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("엑셀 생성에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /** 하나의 표(시트) 데이터. 여러 종류를 조합해 시트 여러 개 또는 CSV 여러 개로 묶을 때 사용한다. */
    public record Tabular(String sheetName, String fileBase, String[] headers, List<List<String>> rows) {}

    // ── 테스트케이스 목록 (전체 또는 필터링된 일부) ───────────────────

    /** caseIds 가 주어지면 그 케이스만, 없으면 프로젝트(또는 전체)의 모든 케이스를 한 시트로 내보낸다. */
    public ExcelFile exportTestCases(Long projectId, List<Long> caseIds, String format) {
        return build(format, tabularTestCases(projectId, caseIds, caseIds != null && !caseIds.isEmpty()));
    }

    private Tabular tabularTestCases(Long projectId, List<Long> caseIds, boolean filtered) {
        List<TestCase> cases;
        if (caseIds != null && !caseIds.isEmpty()) {
            // 전달된 ID 순서를 보존한다(화면에 보이는 순서대로).
            Map<Long, TestCase> byId = testCaseRepository.findAllById(caseIds).stream()
                    .collect(Collectors.toMap(TestCase::getId, java.util.function.Function.identity()));
            cases = caseIds.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
        } else {
            cases = projectId != null ? testCaseRepository.findAllByProjectId(projectId) : testCaseRepository.findAll();
        }

        String[] headers = {"ID", "제목", "폴더", "우선순위", "상태", "유형",
                "OS", "브라우저", "디바이스", "담당자", "버전", "영역태그", "설명", "전제조건", "스텝", "메모"};
        List<List<String>> rows = new ArrayList<>();
        for (TestCase tc : cases) {
            rows.add(List.of(
                    str(tc.getId()),
                    nz(tc.getTitle()),
                    tc.getFolder() != null ? nz(tc.getFolder().getName()) : "",
                    enumName(tc.getPriority()),
                    enumName(tc.getStatus()),
                    enumName(tc.getType()),
                    enumName(tc.getOs()),
                    enumName(tc.getBrowser()),
                    enumName(tc.getDevice()),
                    nz(tc.getAssignee()),
                    nz(tc.getVersion()),
                    tc.getAreaTags().stream().map(AreaTag::getName).collect(Collectors.joining(", ")),
                    nz(tc.getDescription()),
                    nz(tc.getPrecondition()),
                    nz(tc.getSteps()),
                    nz(tc.getNotes())
            ));
        }
        String sheet = filtered ? "테스트케이스(필터)" : "테스트케이스";
        String fileBase = filtered ? "test-cases-filtered" : "test-cases";
        return new Tabular(sheet, fileBase, headers, rows);
    }

    // ── 테스트런 결과 ────────────────────────────────────────────────

    /** 모든 테스트런의 케이스별 실행 결과를 평면 목록으로 내보낸다. */
    public ExcelFile exportTestRuns(Long projectId, String format) {
        return build(format, tabularTestRuns(projectId));
    }

    private Tabular tabularTestRuns(Long projectId) {
        List<Execution> executions = projectId != null
                ? executionRepository.findAllByProjectIdOrderByCreatedAtDesc(projectId)
                : executionRepository.findAllByOrderByCreatedAtDesc();

        String[] headers = {"테스트런", "런 상태", "플랜", "스위트", "담당자",
                "테스트케이스", "결과", "사유/결함", "비고", "버전", "실행일시"};
        List<List<String>> rows = new ArrayList<>();
        for (Execution exec : executions) {
            for (ExecutionItem item : exec.getItems()) {
                String version = item.getVersionNumber() != null
                        ? "v" + item.getVersionNumber() + (item.getVersionLabel() != null ? " " + item.getVersionLabel() : "")
                        : "";
                rows.add(List.of(
                        nz(exec.getName()),
                        enumName(exec.getStatus()),
                        nz(exec.getPlanName()),
                        nz(exec.getSuiteName()),
                        nz(exec.getAssignee()),
                        nz(item.getCaseTitle()),
                        enumName(item.getStatus()),
                        nz(item.getFailureReason()),
                        nz(item.getComment()),
                        version,
                        dt(item.getExecutedAt())
                ));
            }
        }
        return new Tabular("테스트런 결과", "test-run-results", headers, rows);
    }

    // ── 결함 목록 ────────────────────────────────────────────────────

    public ExcelFile exportDefects(String format) {
        return build(format, tabularDefects());
    }

    private Tabular tabularDefects() {
        List<Defect> defects = defectRepository.findAllByOrderByCreatedAtDesc();
        String[] headers = {"ID", "제목", "심각도", "상태", "Jira 키", "외부 URL", "설명", "생성일", "수정일"};
        List<List<String>> rows = new ArrayList<>();
        for (Defect d : defects) {
            rows.add(List.of(
                    str(d.getId()),
                    nz(d.getTitle()),
                    enumName(d.getSeverity()),
                    enumName(d.getStatus()),
                    nz(d.getJiraKey()),
                    nz(d.getExternalUrl()),
                    nz(d.getDescription()),
                    dt(d.getCreatedAt()),
                    dt(d.getUpdatedAt())
            ));
        }
        return new Tabular("결함 목록", "defects", headers, rows);
    }

    // ── 테스트플랜 구조 ──────────────────────────────────────────────

    /** 플랜 → 스위트 → 테스트케이스 계층을 한 시트에 펼쳐 내보낸다. */
    public ExcelFile exportTestPlans(Long projectId, String format) {
        return build(format, tabularTestPlans(projectId));
    }

    private Tabular tabularTestPlans(Long projectId) {
        List<TestPlan> plans = projectId != null
                ? testPlanRepository.findAllByProjectIdOrderByUpdatedAtDesc(projectId)
                : testPlanRepository.findAllByOrderByUpdatedAtDesc();

        String[] headers = {"테스트플랜", "플랜 상태", "담당자", "기간",
                "스위트", "테스트케이스", "케이스 우선순위", "케이스 상태"};
        List<List<String>> rows = new ArrayList<>();
        for (TestPlan plan : plans) {
            String period = (plan.getStartDate() != null ? plan.getStartDate().toString() : "")
                    + " ~ " + (plan.getEndDate() != null ? plan.getEndDate().toString() : "");
            List<TestSuite> suites = testSuiteRepository.findByTestPlanIdOrderByCreatedAtAsc(plan.getId());
            if (suites.isEmpty()) {
                rows.add(planRow(plan, period, "", "", "", ""));
                continue;
            }
            for (TestSuite suite : suites) {
                List<TestCase> cases = suite.getTestCases();
                if (cases.isEmpty()) {
                    rows.add(planRow(plan, period, suite.getName(), "", "", ""));
                    continue;
                }
                for (TestCase tc : cases) {
                    rows.add(planRow(plan, period, suite.getName(),
                            nz(tc.getTitle()), enumName(tc.getPriority()), enumName(tc.getStatus())));
                }
            }
        }
        return new Tabular("테스트플랜 구조", "test-plans", headers, rows);
    }

    private List<String> planRow(TestPlan plan, String period, String suite, String caseTitle,
                                 String casePriority, String caseStatus) {
        return List.of(nz(plan.getName()), enumName(plan.getStatus()), nz(plan.getAssignee()),
                period.trim().equals("~") ? "" : period, suite, caseTitle, casePriority, caseStatus);
    }

    // ── 다중 선택 결합 내보내기 ──────────────────────────────────────

    /**
     * 여러 종류를 한 번에 내보낸다.
     * types: test-cases | test-cases-filtered | test-runs | defects | test-plans (순서 보존, 중복 제거).
     * xlsx → 시트 여러 개의 워크북, csv → 1개면 단일 CSV, 여러 개면 CSV들을 zip 으로 묶는다.
     */
    public ExcelFile exportCombined(Long projectId, List<Long> filteredIds, List<String> types, String format) {
        if (types == null || types.isEmpty()) {
            throw new InvalidRequestException("내보낼 항목을 1개 이상 선택하세요.");
        }
        java.util.LinkedHashSet<String> ordered = new java.util.LinkedHashSet<>(types);
        List<Tabular> tabs = new ArrayList<>();
        for (String type : ordered) {
            switch (type) {
                case "test-cases" -> tabs.add(tabularTestCases(projectId, null, false));
                case "test-cases-filtered" -> tabs.add(tabularTestCases(projectId, filteredIds, true));
                case "test-runs" -> tabs.add(tabularTestRuns(projectId));
                case "defects" -> tabs.add(tabularDefects());
                case "test-plans" -> tabs.add(tabularTestPlans(projectId));
                default -> throw new InvalidRequestException("알 수 없는 내보내기 항목: " + type);
            }
        }

        // 단일 선택이면 기존 단일 파일과 동일하게 처리한다.
        if (tabs.size() == 1) {
            return build(format, tabs.get(0));
        }

        String stamp = java.time.LocalDate.now().toString();
        if ("csv".equalsIgnoreCase(format)) {
            return new ExcelFile("tms-export-" + stamp + ".zip", csvZip(tabs));
        }
        return new ExcelFile("tms-export-" + stamp + ".xlsx", multiSheetWorkbook(tabs));
    }

    /** 여러 표를 시트 여러 개의 단일 워크북으로. */
    private byte[] multiSheetWorkbook(List<Tabular> tabs) {
        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = headerStyle(workbook);
            java.util.Set<String> used = new java.util.HashSet<>();
            for (Tabular t : tabs) {
                String sheetName = uniqueSheetName(t.sheetName(), used);
                Sheet sheet = workbook.createSheet(sheetName);
                Row header = sheet.createRow(0);
                for (int c = 0; c < t.headers().length; c++) {
                    Cell cell = header.createCell(c);
                    cell.setCellValue(t.headers()[c]);
                    cell.setCellStyle(headerStyle);
                }
                int r = 1;
                for (List<String> row : t.rows()) {
                    Row excelRow = sheet.createRow(r++);
                    for (int c = 0; c < row.size(); c++) {
                        excelRow.createCell(c).setCellValue(row.get(c));
                    }
                }
                for (int c = 0; c < t.headers().length; c++) sheet.autoSizeColumn(c);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("엑셀 생성에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /** 여러 표를 각각 CSV 로 만들어 하나의 zip 으로. */
    private byte[] csvZip(List<Tabular> tabs) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(out)) {
            java.util.Set<String> used = new java.util.HashSet<>();
            for (Tabular t : tabs) {
                String name = t.fileBase();
                int n = 2;
                while (used.contains(name)) name = t.fileBase() + "-" + n++;
                used.add(name);
                zip.putNextEntry(new java.util.zip.ZipEntry(name + ".csv"));
                zip.write(toCsv(t.headers(), t.rows()));
                zip.closeEntry();
            }
        } catch (IOException e) {
            throw new IllegalStateException("CSV 압축에 실패했습니다: " + e.getMessage(), e);
        }
        return out.toByteArray();
    }

    // ── 공통 빌더 (xlsx / csv) ──────────────────────────────────────

    private ExcelFile build(String format, Tabular t) {
        return build(format, t.sheetName(), t.fileBase(), t.headers(), t.rows());
    }

    /** format("csv" 이면 CSV, 그 외 xlsx)에 따라 같은 표 데이터를 다른 파일로 출력한다. */
    private ExcelFile build(String format, String sheetName, String fileBase, String[] headers, List<List<String>> rows) {
        if ("csv".equalsIgnoreCase(format)) {
            return new ExcelFile(fileBase + ".csv", toCsv(headers, rows));
        }
        return singleSheet(sheetName, fileBase, headers, rows);
    }

    /** RFC4180 CSV. 한글이 엑셀에서 깨지지 않도록 UTF-8 BOM 을 붙인다. */
    private byte[] toCsv(String[] headers, List<List<String>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append('﻿');
        csvRow(sb, java.util.Arrays.asList(headers));
        for (List<String> row : rows) {
            csvRow(sb, row);
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private void csvRow(StringBuilder sb, List<String> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(csvCell(cells.get(i)));
        }
        sb.append("\r\n");
    }

    private String csvCell(String value) {
        String v = value != null ? value : "";
        if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    /** 헤더 1행 + 문자열 행들로 단일 시트 워크북을 만든다. */
    private ExcelFile singleSheet(String sheetName, String fileBase, String[] headers, List<List<String>> rows) {
        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = headerStyle(workbook);
            Sheet sheet = workbook.createSheet(WorkbookUtil.createSafeSheetName(sheetName));

            Row header = sheet.createRow(0);
            for (int c = 0; c < headers.length; c++) {
                Cell cell = header.createCell(c);
                cell.setCellValue(headers[c]);
                cell.setCellStyle(headerStyle);
            }
            int r = 1;
            for (List<String> row : rows) {
                Row excelRow = sheet.createRow(r++);
                for (int c = 0; c < row.size(); c++) {
                    excelRow.createCell(c).setCellValue(row.get(c));
                }
            }
            for (int c = 0; c < headers.length; c++) {
                sheet.autoSizeColumn(c);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return new ExcelFile(fileBase + ".xlsx", out.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("엑셀 생성에 실패했습니다: " + e.getMessage(), e);
        }
    }

    private String str(Object value) {
        return value != null ? String.valueOf(value) : "";
    }

    private String dt(java.time.LocalDateTime value) {
        return value != null ? value.format(DT_FMT) : "";
    }

    private void writeSheet(Workbook workbook, CellStyle headerStyle, String sheetName, List<TestCase> cases) {
        Sheet sheet = workbook.createSheet(sheetName);

        Row header = sheet.createRow(0);
        for (int c = 0; c < HEADERS.length; c++) {
            Cell cell = header.createCell(c);
            cell.setCellValue(HEADERS[c]);
            cell.setCellStyle(headerStyle);
        }

        int r = 1;
        for (TestCase tc : cases) {
            Row row = sheet.createRow(r++);
            int c = 0;
            row.createCell(c++).setCellValue(nz(tc.getTitle()));
            row.createCell(c++).setCellValue(nz(tc.getDescription()));
            row.createCell(c++).setCellValue(nz(tc.getPrecondition()));
            row.createCell(c++).setCellValue(nz(tc.getSteps()));
            row.createCell(c++).setCellValue(nz(tc.getNotes()));
            row.createCell(c++).setCellValue(enumName(tc.getPriority()));
            row.createCell(c++).setCellValue(enumName(tc.getStatus()));
            row.createCell(c++).setCellValue(enumName(tc.getType()));
            row.createCell(c++).setCellValue(enumName(tc.getOs()));
            row.createCell(c++).setCellValue(enumName(tc.getBrowser()));
            row.createCell(c++).setCellValue(enumName(tc.getDevice()));
            row.createCell(c++).setCellValue(nz(tc.getAssignee()));
            row.createCell(c).setCellValue(nz(tc.getVersion()));
        }

        for (int c = 0; c < HEADERS.length; c++) {
            sheet.autoSizeColumn(c);
        }
    }

    private CellStyle headerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    /** 시트명을 POI 규칙(31자·금지문자)에 맞추고 중복되지 않게 보정한다. */
    private String uniqueSheetName(String raw, java.util.Set<String> used) {
        String base = WorkbookUtil.createSafeSheetName(raw == null || raw.isBlank() ? UNFILED_SHEET : raw);
        String name = base;
        int n = 2;
        while (used.contains(name)) {
            String suffix = " (" + n++ + ")";
            int max = 31 - suffix.length();
            name = (base.length() > max ? base.substring(0, max) : base) + suffix;
        }
        used.add(name);
        return name;
    }

    private String safeFilename(String name) {
        String cleaned = (name == null || name.isBlank() ? "test-suite" : name)
                .replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return cleaned.isEmpty() ? "test-suite" : cleaned;
    }

    private String nz(String value) {
        return value != null ? value : "";
    }

    private String enumName(Enum<?> value) {
        return value != null ? value.name() : "";
    }
}
