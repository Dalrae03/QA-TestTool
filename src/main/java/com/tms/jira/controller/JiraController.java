package com.tms.jira.controller;

import com.tms.defect.dto.DefectResponse;
import com.tms.jira.dto.JiraLinkRequest;
import com.tms.jira.dto.JiraSyncResult;
import com.tms.jira.service.JiraService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JiraController {

    private final JiraService jiraService;

    public JiraController(JiraService jiraService) {
        this.jiraService = jiraService;
    }

    /** TMS 결함 → Jira 이슈 생성 또는 업데이트 */
    @PostMapping("/api/defects/{id}/jira/push")
    public ResponseEntity<DefectResponse> push(@PathVariable Long id) {
        return ResponseEntity.ok(jiraService.push(id));
    }

    /** Jira 이슈 상태 → TMS 결함 상태 동기화 */
    @PostMapping("/api/defects/{id}/jira/pull")
    public ResponseEntity<DefectResponse> pull(@PathVariable Long id) {
        return ResponseEntity.ok(jiraService.pull(id));
    }

    /** 기존 Jira 이슈 key를 결함에 연결 */
    @PostMapping("/api/defects/{id}/jira/link")
    public ResponseEntity<DefectResponse> link(
            @PathVariable Long id,
            @Valid @RequestBody JiraLinkRequest request
    ) {
        return ResponseEntity.ok(jiraService.link(id, request));
    }

    /** jiraKey가 있는 모든 결함 양방향 동기화 */
    @PostMapping("/api/jira/sync-all")
    public ResponseEntity<JiraSyncResult> syncAll() {
        return ResponseEntity.ok(jiraService.syncAll());
    }
}
