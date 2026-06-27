package com.tms.excel.controller;

import com.tms.excel.service.ExcelImportService;
import com.tms.excel.service.ExcelImportService.ImportResult;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/import")
public class ExcelImportController {

    private final ExcelImportService excelImportService;

    public ExcelImportController(ExcelImportService excelImportService) {
        this.excelImportService = excelImportService;
    }

    @PostMapping("/excel")
    public ResponseEntity<?> importExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "projectId", required = false) Long projectId
    ) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "파일이 비어있습니다."));
        }

        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "import";
        String ext = filename.toLowerCase();
        if (!ext.endsWith(".xlsx") && !ext.endsWith(".xls")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Excel 파일(.xlsx, .xls)만 업로드할 수 있습니다."));
        }

        try {
            ImportResult result = excelImportService.importExcel(file.getInputStream(), filename, projectId);
            return ResponseEntity.ok(Map.of(
                    "createdFolders", result.createdFolders(),
                    "createdCases", result.createdCases(),
                    "errors", result.errors()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "가져오기 실패: " + e.getMessage()));
        }
    }
}
