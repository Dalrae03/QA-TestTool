package com.tms.jira.service;

import com.tms.defect.dto.DefectResponse;
import com.tms.defect.entity.Defect;
import com.tms.defect.entity.DefectSeverity;
import com.tms.defect.entity.DefectStatus;
import com.tms.defect.repository.DefectRepository;
import com.tms.global.exception.InvalidRequestException;
import com.tms.jira.client.JiraClient;
import com.tms.jira.config.JiraProperties;
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
    private final JiraProperties jiraProperties;

    public JiraService(DefectRepository defectRepository, JiraClient jiraClient, JiraProperties jiraProperties) {
        this.defectRepository = defectRepository;
        this.jiraClient = jiraClient;
        this.jiraProperties = jiraProperties;
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
            // 새로 생성된 Jira 이슈에 TMS 결함을 가리키는 역링크를 남긴다(양방향 추적).
            createRemoteLink(defect);
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

    /** 역방향: Jira 이슈 key로 연결된 TMS 결함 조회(Jira → TMS 추적) */
    public DefectResponse findByJiraKey(String jiraKey) {
        Defect defect = defectRepository.findByJiraKey(jiraKey)
                .orElseThrow(() -> new EntityNotFoundException("연결된 결함이 없습니다. jiraKey=" + jiraKey));
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

    /** TMS 웹 주소가 설정돼 있으면 Jira 이슈에 TMS 결함을 가리키는 원격 링크를 남긴다(best-effort). */
    private void createRemoteLink(Defect defect) {
        if (!jiraProperties.hasWebBaseUrl() || defect.getJiraKey() == null) {
            return;
        }
        String url = jiraProperties.webBaseUrl().replaceAll("/+$", "") + "/defects/" + defect.getId();
        String title = "TMS 결함 #" + defect.getId() + ": " + defect.getTitle();
        try {
            jiraClient.addRemoteLink(defect.getJiraKey(), url, title);
        } catch (RuntimeException e) {
            // 역링크 실패가 결함 동기화를 막지 않도록 무시한다(연결은 jiraKey로 이미 저장됨).
        }
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
