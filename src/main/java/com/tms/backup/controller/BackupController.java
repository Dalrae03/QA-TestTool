package com.tms.backup.controller;

import com.tms.backup.service.BackupService;
import com.tms.backup.service.BackupService.RestoreResult;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class BackupController {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final BackupService backupService;

    public BackupController(BackupService backupService) {
        this.backupService = backupService;
    }

    /** 전체 데이터(+첨부파일) 백업을 zip 으로 내려준다. */
    @GetMapping("/api/backup/export")
    public ResponseEntity<byte[]> export() {
        byte[] zip = backupService.exportBackup();
        String filename = "tms-backup-" + LocalDateTime.now().format(STAMP) + ".zip";
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(encoded).build());
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(zip);
    }

    /** 백업 zip 으로 전체 데이터를 복구한다(기존 데이터는 백업 시점 상태로 덮어쓴다). */
    @PostMapping("/api/backup/import")
    public ResponseEntity<?> restore(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "백업 파일이 비어있습니다."));
        }
        try {
            RestoreResult result = backupService.restoreBackup(file.getInputStream());
            return ResponseEntity.ok(Map.of(
                    "tables", result.tables(),
                    "rows", result.rows(),
                    "files", result.files()
            ));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "복구 실패: " + e.getMessage()));
        }
    }
}
