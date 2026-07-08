package com.tms.excel.service;

import com.tms.testcase.entity.AreaTag;
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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ExcelImportService {

    private static final Map<String, String> COLUMN_ALIASES = new HashMap<>();

    static {
        // 제목
        put("title", "title"); put("제목", "title"); put("이름", "title"); put("name", "title");
        put("테스트케이스", "title"); put("테스트 케이스", "title"); put("케이스명", "title");
        // 설명
        put("description", "description"); put("설명", "description"); put("내용", "description");
        // 전제조건
        put("precondition", "precondition"); put("전제조건", "precondition"); put("사전조건", "precondition"); put("전제 조건", "precondition");
        // 스텝
        put("steps", "steps"); put("스텝", "steps"); put("단계", "steps"); put("테스트단계", "steps"); put("테스트 단계", "steps"); put("step", "steps");
        // 예상결과
        put("expectedresult", "expectedResult"); put("예상결과", "expectedResult"); put("예상 결과", "expectedResult"); put("expected", "expectedResult");
        // 메모
        put("notes", "notes"); put("메모", "notes"); put("비고", "notes"); put("note", "notes"); put("노트", "notes");
        // 우선순위
        put("priority", "priority"); put("우선순위", "priority");
        // 상태
        put("status", "status"); put("상태", "status");
        // 유형
        put("type", "type"); put("유형", "type"); put("타입", "type");
        // OS
        put("os", "os");
        // 브라우저
        put("browser", "browser"); put("브라우저", "browser");
        // 디바이스
        put("device", "device"); put("디바이스", "device");
        // 담당자
        put("assignee", "assignee"); put("담당자", "assignee"); put("작성자", "assignee");
        // 버전
        put("version", "version"); put("버전", "version");
    }

    private static void put(String alias, String field) {
        COLUMN_ALIASES.put(alias.toLowerCase().trim(), field);
    }

    private final TestCaseRepository testCaseRepository;
    private final TestFolderRepository testFolderRepository;
    private final com.tms.audit.service.AuditLogService auditLogService;

    public ExcelImportService(TestCaseRepository testCaseRepository,
                               TestFolderRepository testFolderRepository,
                               com.tms.audit.service.AuditLogService auditLogService) {
        this.testCaseRepository = testCaseRepository;
        this.testFolderRepository = testFolderRepository;
        this.auditLogService = auditLogService;
    }

    public record ImportResult(int createdFolders, int createdCases, List<String> errors) {}

    private void logImport(String kind, String filename, ImportResult result) {
        auditLogService.log(com.tms.audit.service.AuditLogService.IMPORT, 0L,
                com.tms.audit.entity.AuditAction.IMPORTED,
                kind + " 가져오기 '" + filename + "': 테스트케이스 " + result.createdCases()
                        + "건, 폴더 " + result.createdFolders() + "개 생성");
    }

    public ImportResult importExcel(InputStream inputStream, String filename, Long projectId) throws Exception {
        int createdFolders = 0;
        int createdCases = 0;
        List<String> errors = new ArrayList<>();

        // 파일명(확장자 제거)을 최상위 폴더로 생성
        String rootFolderName = filename.replaceAll("\\.[^.]+$", "");
        TestFolder rootFolder = testFolderRepository.save(new TestFolder(rootFolderName, null, projectId));
        createdFolders++;

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            for (int si = 0; si < workbook.getNumberOfSheets(); si++) {
                Sheet sheet = workbook.getSheetAt(si);
                String sheetName = sheet.getSheetName();

                if (sheet.getPhysicalNumberOfRows() < 2) continue;

                // 시트 → 하위 폴더
                TestFolder sheetFolder = testFolderRepository.save(new TestFolder(sheetName, rootFolder, projectId));
                createdFolders++;

                // 첫 행 = 헤더
                Row headerRow = sheet.getRow(sheet.getFirstRowNum());
                if (headerRow == null) continue;

                Map<Integer, String> colMap = buildColumnMap(headerRow);

                for (int ri = sheet.getFirstRowNum() + 1; ri <= sheet.getLastRowNum(); ri++) {
                    Row row = sheet.getRow(ri);
                    if (row == null || isRowEmpty(row)) continue;

                    try {
                        TestCase tc = rowToTestCase(row, colMap, sheetFolder);
                        if (tc != null) {
                            tc.setProjectId(projectId);
                            testCaseRepository.save(tc);
                            createdCases++;
                        }
                    } catch (Exception e) {
                        errors.add("시트 '" + sheetName + "' " + (ri + 1) + "행: " + e.getMessage());
                    }
                }
            }
        }

        ImportResult result = new ImportResult(createdFolders, createdCases, errors);
        logImport("Excel", filename, result);
        return result;
    }

    /** CSV 의 폴더 지정 컬럼으로 인식할 헤더(소문자). */
    private static final java.util.Set<String> FOLDER_HEADERS = java.util.Set.of("폴더", "folder", "시트", "sheet");

    /** CSV 가져오기 — 파일명을 루트 폴더로, '폴더' 컬럼이 있으면 그 값별로 하위 폴더를 만든다. */
    public ImportResult importCsv(InputStream inputStream, String filename, Long projectId) throws Exception {
        int createdFolders = 0;
        int createdCases = 0;
        List<String> errors = new ArrayList<>();

        String rootFolderName = filename.replaceAll("\\.[^.]+$", "");
        TestFolder rootFolder = testFolderRepository.save(new TestFolder(rootFolderName, null, projectId));
        createdFolders++;

        List<List<String>> table = parseCsv(inputStream);
        if (table.isEmpty()) {
            ImportResult empty = new ImportResult(createdFolders, createdCases, errors);
            logImport("CSV", filename, empty);
            return empty;
        }

        // 헤더 → 필드 매핑 + 폴더 컬럼 탐지
        List<String> header = table.get(0);
        Map<Integer, String> colMap = new HashMap<>();
        int folderCol = -1;
        for (int i = 0; i < header.size(); i++) {
            String h = header.get(i) != null ? header.get(i).toLowerCase().trim() : "";
            if (FOLDER_HEADERS.contains(h)) { folderCol = i; continue; }
            String field = COLUMN_ALIASES.get(h);
            if (field != null) colMap.put(i, field);
        }

        Map<String, TestFolder> subFolders = new HashMap<>();
        for (int r = 1; r < table.size(); r++) {
            List<String> cells = table.get(r);
            if (isCsvRowEmpty(cells)) continue;
            try {
                Map<String, String> values = new HashMap<>();
                colMap.forEach((idx, field) -> {
                    if (idx < cells.size() && cells.get(idx) != null) values.put(field, cells.get(idx).trim());
                });

                TestFolder folder = rootFolder;
                if (folderCol >= 0 && folderCol < cells.size()) {
                    String fname = cells.get(folderCol) != null ? cells.get(folderCol).trim() : "";
                    if (!fname.isBlank()) {
                        TestFolder sub = subFolders.get(fname);
                        if (sub == null) {
                            sub = testFolderRepository.save(new TestFolder(fname, rootFolder, projectId));
                            subFolders.put(fname, sub);
                            createdFolders++;
                        }
                        folder = sub;
                    }
                }

                TestCase tc = buildTestCase(values, folder);
                if (tc != null) {
                    tc.setProjectId(projectId);
                    testCaseRepository.save(tc);
                    createdCases++;
                }
            } catch (Exception e) {
                errors.add((r + 1) + "행: " + e.getMessage());
            }
        }
        ImportResult result = new ImportResult(createdFolders, createdCases, errors);
        logImport("CSV", filename, result);
        return result;
    }

    /** RFC4180 CSV 파서(따옴표·내부 콤마/개행 처리). UTF-8 BOM 은 제거한다. */
    private List<List<String>> parseCsv(InputStream inputStream) throws java.io.IOException {
        String content = new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        if (content.startsWith("﻿")) content = content.substring(1);

        List<List<String>> rows = new ArrayList<>();
        List<String> cur = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < content.length() && content.charAt(i + 1) == '"') { field.append('"'); i++; }
                    else inQuotes = false;
                } else {
                    field.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    cur.add(field.toString()); field.setLength(0);
                } else if (c == '\n') {
                    cur.add(field.toString()); field.setLength(0); rows.add(cur); cur = new ArrayList<>();
                } else if (c != '\r') {
                    field.append(c);
                }
            }
        }
        if (field.length() > 0 || !cur.isEmpty()) {
            cur.add(field.toString());
            rows.add(cur);
        }
        return rows;
    }

    private boolean isCsvRowEmpty(List<String> cells) {
        for (String c : cells) {
            if (c != null && !c.isBlank()) return false;
        }
        return true;
    }

    private Map<Integer, String> buildColumnMap(Row headerRow) {
        Map<Integer, String> map = new HashMap<>();
        Iterator<Cell> cells = headerRow.cellIterator();
        while (cells.hasNext()) {
            Cell cell = cells.next();
            String header = getCellString(cell).toLowerCase().trim();
            String field = COLUMN_ALIASES.get(header);
            if (field != null) {
                map.put(cell.getColumnIndex(), field);
            }
        }
        return map;
    }

    private TestCase rowToTestCase(Row row, Map<Integer, String> colMap, TestFolder folder) {
        Map<String, String> values = new HashMap<>();
        colMap.forEach((colIdx, field) -> {
            Cell cell = row.getCell(colIdx);
            if (cell != null) values.put(field, getCellString(cell));
        });
        return buildTestCase(values, folder);
    }

    /** 필드명→값 맵으로 TestCase 를 만든다. 엑셀/CSV 가 공용으로 사용한다. title 이 없으면 null. */
    private TestCase buildTestCase(Map<String, String> values, TestFolder folder) {
        String title = values.get("title");
        if (title == null || title.isBlank()) return null;

        String description = values.getOrDefault("description", "엑셀에서 가져온 테스트케이스");
        String precondition = values.getOrDefault("precondition", "-");
        String steps = values.getOrDefault("steps", "-");
        String expectedResult = values.get("expectedResult");
        String notes = values.get("notes");

        TestCasePriority priority = parseEnum(TestCasePriority.class, values.get("priority"), TestCasePriority.MEDIUM);
        TestCaseStatus status = parseEnum(TestCaseStatus.class, values.get("status"), TestCaseStatus.DRAFT);
        TestCaseType type = parseEnum(TestCaseType.class, values.get("type"), TestCaseType.FUNCTIONAL);
        TestCaseOs os = parseEnum(TestCaseOs.class, values.get("os"), null);
        TestCaseBrowser browser = parseEnum(TestCaseBrowser.class, values.get("browser"), null);
        TestCaseDevice device = parseEnum(TestCaseDevice.class, values.get("device"), null);

        TestCase tc = new TestCase(type, priority, status, title.trim(),
                description.isBlank() ? "엑셀에서 가져온 테스트케이스" : description,
                precondition.isBlank() ? "-" : precondition,
                steps.isBlank() ? "-" : steps,
                notes, os, browser, device,
                new ArrayList<>(),
                null, null,
                values.get("assignee"),
                values.get("version"),
                expectedResult);
        tc.moveToFolder(folder);
        return tc;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> clazz, String value, E defaultVal) {
        if (value == null || value.isBlank()) return defaultVal;
        try {
            return Enum.valueOf(clazz, value.toUpperCase().trim().replace(" ", "_").replace("-", "_"));
        } catch (IllegalArgumentException e) {
            return defaultVal;
        }
    }

    private String getCellString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double d = cell.getNumericCellValue();
                yield d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try { yield cell.getStringCellValue().trim(); }
                catch (Exception e) { yield String.valueOf(cell.getNumericCellValue()); }
            }
            default -> "";
        };
    }

    private boolean isRowEmpty(Row row) {
        Iterator<Cell> cells = row.cellIterator();
        while (cells.hasNext()) {
            Cell cell = cells.next();
            if (cell.getCellType() != CellType.BLANK && !getCellString(cell).isBlank()) return false;
        }
        return true;
    }
}
