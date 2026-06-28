package com.tms.excel.controller;

import com.tms.excel.service.ExcelExportService;
import com.tms.excel.service.ExcelExportService.ExcelFile;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExcelExportController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private static final MediaType CSV = MediaType.parseMediaType("text/csv; charset=UTF-8");
    private static final MediaType ZIP = MediaType.parseMediaType("application/zip");

    private final ExcelExportService excelExportService;

    public ExcelExportController(ExcelExportService excelExportService) {
        this.excelExportService = excelExportService;
    }

    @GetMapping("/api/suites/{suiteId}/export/excel")
    public ResponseEntity<byte[]> exportSuite(@PathVariable Long suiteId) {
        return toResponse(excelExportService.exportSuite(suiteId));
    }

    /** 테스트케이스 목록. ids 가 있으면 그 케이스만(필터링된 결과), 없으면 프로젝트 전체. format=csv 가능. */
    @GetMapping("/api/export/test-cases/excel")
    public ResponseEntity<byte[]> exportTestCases(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) List<Long> ids,
            @RequestParam(required = false, defaultValue = "xlsx") String format
    ) {
        return toResponse(excelExportService.exportTestCases(projectId, ids, format));
    }

    /** 테스트런 결과(케이스별 실행 결과). format=csv 가능. */
    @GetMapping("/api/export/test-runs/excel")
    public ResponseEntity<byte[]> exportTestRuns(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false, defaultValue = "xlsx") String format
    ) {
        return toResponse(excelExportService.exportTestRuns(projectId, format));
    }

    /** 결함 목록. format=csv 가능. */
    @GetMapping("/api/export/defects/excel")
    public ResponseEntity<byte[]> exportDefects(
            @RequestParam(required = false, defaultValue = "xlsx") String format
    ) {
        return toResponse(excelExportService.exportDefects(format));
    }

    /** 테스트플랜 구조(플랜 → 스위트 → 케이스). format=csv 가능. */
    @GetMapping("/api/export/test-plans/excel")
    public ResponseEntity<byte[]> exportTestPlans(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false, defaultValue = "xlsx") String format
    ) {
        return toResponse(excelExportService.exportTestPlans(projectId, format));
    }

    /**
     * 다중 선택 결합 내보내기.
     * types: test-cases, test-cases-filtered, test-runs, defects, test-plans (콤마 구분).
     * 단일 선택이면 단일 파일, 다중 선택이면 xlsx=시트 여러 개 / csv=CSV들의 zip.
     */
    @GetMapping("/api/export/combined")
    public ResponseEntity<byte[]> exportCombined(
            @RequestParam(required = false) Long projectId,
            @RequestParam List<String> types,
            @RequestParam(required = false) List<Long> ids,
            @RequestParam(required = false, defaultValue = "xlsx") String format
    ) {
        return toResponse(excelExportService.exportCombined(projectId, ids, types, format));
    }

    private ResponseEntity<byte[]> toResponse(ExcelFile file) {
        String lower = file.filename().toLowerCase();
        MediaType type = lower.endsWith(".csv") ? CSV : lower.endsWith(".zip") ? ZIP : XLSX;
        String encodedName = URLEncoder.encode(file.filename(), StandardCharsets.UTF_8).replace("+", "%20");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(encodedName).build());
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(type)
                .body(file.content());
    }
}
