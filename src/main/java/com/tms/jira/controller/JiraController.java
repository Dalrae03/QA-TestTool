package com.tms.jira.controller;

import com.tms.defect.dto.DefectResponse;
import com.tms.jira.client.JiraClient;
import com.tms.jira.dto.JiraConnectionTestResult;
import com.tms.jira.dto.JiraLinkRequest;
import com.tms.jira.dto.JiraSettingsRequest;
import com.tms.jira.dto.JiraSettingsView;
import com.tms.jira.dto.JiraSyncResult;
import com.tms.jira.dto.RequirementCoverageResponse;
import com.tms.jira.dto.TestCaseRequirementsResponse;
import com.tms.jira.service.JiraService;
import com.tms.jira.service.JiraSettingsService;
import com.tms.jira.service.JiraTraceabilityService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JiraController {

    private final JiraService jiraService;
    private final JiraTraceabilityService traceabilityService;
    private final JiraSettingsService settingsService;
    private final JiraClient jiraClient;

    public JiraController(JiraService jiraService, JiraTraceabilityService traceabilityService,
                          JiraSettingsService settingsService, JiraClient jiraClient) {
        this.jiraService = jiraService;
        this.traceabilityService = traceabilityService;
        this.settingsService = settingsService;
        this.jiraClient = jiraClient;
    }

    // ===== Jira 연동 설정 =====

    /** 현재 Jira 연동 설정 조회(토큰은 마스킹). */
    @GetMapping("/api/jira/settings")
    public ResponseEntity<JiraSettingsView> getSettings() {
        return ResponseEntity.ok(settingsService.view());
    }

    /** Jira 연동 설정 저장. */
    @PutMapping("/api/jira/settings")
    public ResponseEntity<JiraSettingsView> saveSettings(@Valid @RequestBody JiraSettingsRequest request) {
        return ResponseEntity.ok(settingsService.save(request));
    }

    /** 입력한(또는 저장된) 설정으로 실제 Jira 연결을 테스트. */
    @PostMapping("/api/jira/settings/test")
    public ResponseEntity<JiraConnectionTestResult> testConnection(@RequestBody(required = false) JiraSettingsRequest request) {
        return ResponseEntity.ok(jiraClient.testConnection(settingsService.resolveForTest(request)));
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

    /** 역방향: Jira 이슈 key로 연결된 TMS 결함 조회 */
    @GetMapping("/api/jira/issues/{jiraKey}/defect")
    public ResponseEntity<DefectResponse> getDefectByJiraKey(@PathVariable String jiraKey) {
        return ResponseEntity.ok(jiraService.findByJiraKey(jiraKey));
    }

    // ===== 테스트케이스 ↔ Jira 요구사항 추적성(traceability) =====

    /** 정방향: 테스트케이스에 연결된 Jira 요구사항 목록 조회 */
    @GetMapping("/api/testcases/{id}/jira/requirements")
    public ResponseEntity<TestCaseRequirementsResponse> getRequirements(@PathVariable Long id) {
        return ResponseEntity.ok(traceabilityService.getRequirements(id));
    }

    /** 테스트케이스에 Jira 요구사항 연결 */
    @PostMapping("/api/testcases/{id}/jira/requirements")
    public ResponseEntity<TestCaseRequirementsResponse> linkRequirement(
            @PathVariable Long id,
            @Valid @RequestBody JiraLinkRequest request
    ) {
        return ResponseEntity.ok(traceabilityService.linkRequirement(id, request));
    }

    /** 테스트케이스의 Jira 요구사항 연결 해제 */
    @DeleteMapping("/api/testcases/{id}/jira/requirements/{jiraKey}")
    public ResponseEntity<TestCaseRequirementsResponse> unlinkRequirement(
            @PathVariable Long id,
            @PathVariable String jiraKey
    ) {
        return ResponseEntity.ok(traceabilityService.unlinkRequirement(id, jiraKey));
    }

    /** 역방향: Jira 요구사항을 검증하는 테스트케이스 목록(커버리지) 조회 */
    @GetMapping("/api/jira/requirements/{jiraKey}/testcases")
    public ResponseEntity<RequirementCoverageResponse> getCoverage(@PathVariable String jiraKey) {
        return ResponseEntity.ok(traceabilityService.getCoverage(jiraKey));
    }
}
