package com.tms.jira.service;

import com.tms.defect.dto.DefectResponse;
import com.tms.defect.entity.Defect;
import com.tms.defect.entity.DefectSeverity;
import com.tms.defect.entity.DefectStatus;
import com.tms.defect.repository.DefectRepository;
import com.tms.global.exception.InvalidRequestException;
import com.tms.jira.client.JiraClient;
import com.tms.jira.dto.JiraLinkRequest;
import com.tms.jira.dto.JiraSyncResult;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class JiraService {

    private final DefectRepository defectRepository;
    private final JiraClient jiraClient;

    public JiraService(DefectRepository defectRepository, JiraClient jiraClient) {
        this.defectRepository = defectRepository;
        this.jiraClient = jiraClient;
    }

    /** TMS 결함 → Jira 이슈 생성(또는 업데이트). jiraKey 저장 */
    @Transactional
    public DefectResponse push(Long defectId) {
        Defect defect = findById(defectId);
        if (defect.getJiraKey() == null) {
            String key = jiraClient.createIssue(
                    defect.getTitle(),
                    defect.getDescription(),
                    "Bug",
                    toJiraPriority(defect.getSeverity())
            );
            defect.linkJira(key);
        } else {
            jiraClient.updateIssue(defect.getJiraKey(), defect.getTitle(), defect.getDescription());
            jiraClient.transitionToCategory(defect.getJiraKey(), toJiraCategory(defect.getStatus()));
        }
        return DefectResponse.from(defect);
    }

    /** Jira 이슈 상태 → TMS 결함 상태 동기화 */
    @Transactional
    public DefectResponse pull(Long defectId) {
        Defect defect = findById(defectId);
        if (defect.getJiraKey() == null) {
            throw new InvalidRequestException("연결된 Jira 이슈가 없습니다. /jira/push 또는 /jira/link로 먼저 연결하세요.");
        }
        String category = jiraClient.getStatusCategory(defect.getJiraKey());
        DefectStatus newStatus = fromJiraCategory(category);
        defect.updateStatus(newStatus);
        return DefectResponse.from(defect);
    }

    /** 기존 Jira 이슈 key를 결함에 연결 */
    @Transactional
    public DefectResponse link(Long defectId, JiraLinkRequest request) {
        Defect defect = findById(defectId);
        defect.linkJira(request.jiraKey());
        return DefectResponse.from(defect);
    }

    /** jiraKey가 있는 모든 결함 양방향 동기화 */
    @Transactional
    public JiraSyncResult syncAll() {
        List<Defect> linked = defectRepository.findAllByJiraKeyIsNotNull();
        int success = 0, failed = 0;
        for (Defect defect : linked) {
            try {
                // Jira → TMS 상태 pull
                String category = jiraClient.getStatusCategory(defect.getJiraKey());
                defect.updateStatus(fromJiraCategory(category));
                // TMS → Jira 내용 push
                jiraClient.updateIssue(defect.getJiraKey(), defect.getTitle(), defect.getDescription());
                success++;
            } catch (Exception e) {
                failed++;
            }
        }
        return new JiraSyncResult(linked.size(), success, failed);
    }

    private Defect findById(Long id) {
        return defectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Defect not found. id=" + id));
    }

    // TMS DefectStatus → Jira 상태 카테고리
    static String toJiraCategory(DefectStatus status) {
        return switch (status) {
            case OPEN -> "new";
            case IN_PROGRESS -> "indeterminate";
            case RESOLVED, CLOSED -> "done";
        };
    }

    // Jira 상태 카테고리 → TMS DefectStatus
    static DefectStatus fromJiraCategory(String category) {
        return switch (category) {
            case "new" -> DefectStatus.OPEN;
            case "indeterminate" -> DefectStatus.IN_PROGRESS;
            case "done" -> DefectStatus.RESOLVED;
            default -> DefectStatus.OPEN;
        };
    }

    // TMS DefectSeverity → Jira Priority
    static String toJiraPriority(DefectSeverity severity) {
        return switch (severity) {
            case CRITICAL -> "Highest";
            case MAJOR -> "High";
            case MINOR -> "Medium";
            case TRIVIAL -> "Low";
        };
    }
}
