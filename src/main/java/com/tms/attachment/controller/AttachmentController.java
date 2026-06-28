package com.tms.attachment.controller;

import com.tms.attachment.dto.AttachmentResponse;
import com.tms.attachment.entity.AttachmentEntityType;
import com.tms.attachment.service.AttachmentService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @GetMapping("/api/testcases/{id}/attachments")
    public List<AttachmentResponse> getTestCaseAttachments(@PathVariable Long id) {
        return attachmentService.getList(AttachmentEntityType.TEST_CASE, id);
    }

    @PostMapping("/api/testcases/{id}/attachments")
    public ResponseEntity<AttachmentResponse> uploadToTestCase(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        AttachmentResponse response = attachmentService.upload(AttachmentEntityType.TEST_CASE, id, file);
        return ResponseEntity.created(URI.create("/api/attachments/" + response.id())).body(response);
    }

    @GetMapping("/api/defects/{id}/attachments")
    public List<AttachmentResponse> getDefectAttachments(@PathVariable Long id) {
        return attachmentService.getList(AttachmentEntityType.DEFECT, id);
    }

    @PostMapping("/api/defects/{id}/attachments")
    public ResponseEntity<AttachmentResponse> uploadToDefect(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        AttachmentResponse response = attachmentService.upload(AttachmentEntityType.DEFECT, id, file);
        return ResponseEntity.created(URI.create("/api/attachments/" + response.id())).body(response);
    }

    @GetMapping("/api/testcases/{testCaseId}/runs/{runId}/attachments")
    public List<AttachmentResponse> getTestRunAttachments(
            @PathVariable Long testCaseId,
            @PathVariable Long runId
    ) {
        return attachmentService.getList(AttachmentEntityType.TEST_RUN, runId);
    }

    @PostMapping("/api/testcases/{testCaseId}/runs/{runId}/attachments")
    public ResponseEntity<AttachmentResponse> uploadToTestRun(
            @PathVariable Long testCaseId,
            @PathVariable Long runId,
            @RequestParam("file") MultipartFile file
    ) {
        AttachmentResponse response = attachmentService.upload(AttachmentEntityType.TEST_RUN, runId, file);
        return ResponseEntity.created(URI.create("/api/attachments/" + response.id())).body(response);
    }

    @GetMapping("/api/test-runs/{runId}/items/{itemId}/attachments")
    public List<AttachmentResponse> getExecutionItemAttachments(
            @PathVariable Long runId,
            @PathVariable Long itemId
    ) {
        return attachmentService.getList(AttachmentEntityType.EXECUTION_ITEM, itemId);
    }

    @PostMapping("/api/test-runs/{runId}/items/{itemId}/attachments")
    public ResponseEntity<AttachmentResponse> uploadToExecutionItem(
            @PathVariable Long runId,
            @PathVariable Long itemId,
            @RequestParam("file") MultipartFile file
    ) {
        AttachmentResponse response = attachmentService.upload(AttachmentEntityType.EXECUTION_ITEM, itemId, file);
        return ResponseEntity.created(URI.create("/api/attachments/" + response.id())).body(response);
    }

    @GetMapping("/api/attachments/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        AttachmentResponse meta = attachmentService.get(id);
        Resource resource = attachmentService.download(id);

        String encodedName = URLEncoder.encode(meta.originalFilename(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(encodedName).build()
        );
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @DeleteMapping("/api/attachments/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        attachmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
