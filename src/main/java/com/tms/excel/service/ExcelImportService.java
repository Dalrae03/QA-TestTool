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

    public ExcelImportService(TestCaseRepository testCaseRepository,
                               TestFolderRepository testFolderRepository) {
        this.testCaseRepository = testCaseRepository;
        this.testFolderRepository = testFolderRepository;
    }

    public record ImportResult(int createdFolders, int createdCases, List<String> errors) {}

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

        return new ImportResult(createdFolders, createdCases, errors);
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

        String title = values.get("title");
        if (title == null || title.isBlank()) return null;

        String description = values.getOrDefault("description", "엑셀에서 가져온 테스트케이스");
        String precondition = values.getOrDefault("precondition", "-");
        String steps = values.getOrDefault("steps", "-");
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
                values.get("version"));
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
