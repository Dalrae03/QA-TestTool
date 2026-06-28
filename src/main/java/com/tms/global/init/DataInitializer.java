package com.tms.global.init;

import com.tms.project.entity.Project;
import com.tms.project.repository.ProjectRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class DataInitializer {

    private final ProjectRepository projectRepository;
    private final TransactionTemplate tx;

    @PersistenceContext
    private EntityManager em;

    public DataInitializer(ProjectRepository projectRepository,
                           PlatformTransactionManager txManager) {
        this.projectRepository = projectRepository;
        this.tx = new TransactionTemplate(txManager);
    }

    /**
     * 버전 스냅샷 기능 도입 전에 만들어진 테스트런 항목은 version_number/version_label이 비어 있다.
     * 그 런이 생성된 시각 기준으로 "당시 현재 버전"이었던 스냅샷을 역추적해 채워준다 — 채울 버전이 없으면(그 케이스의
     * 첫 버전보다도 더 오래된 런) NULL로 남아 매 기동마다 재시도된다(이미 채워진 행은 NULL이 아니므로 안전).
     */
    private void backfillExecutionItemVersions() {
        tx.execute(status -> {
            try {
                em.createNativeQuery(
                        "UPDATE test_execution_items tei " +
                        "SET tei.version_number = (" +
                        "    SELECT tcv.version_number FROM test_case_versions tcv " +
                        "    JOIN test_executions te ON te.id = tei.execution_id " +
                        "    WHERE tcv.test_case_id = tei.test_case_id AND tcv.created_at <= te.created_at " +
                        "    ORDER BY tcv.created_at DESC LIMIT 1" +
                        "), " +
                        "tei.version_label = (" +
                        "    SELECT tcv.label FROM test_case_versions tcv " +
                        "    JOIN test_executions te ON te.id = tei.execution_id " +
                        "    WHERE tcv.test_case_id = tei.test_case_id AND tcv.created_at <= te.created_at " +
                        "    ORDER BY tcv.created_at DESC LIMIT 1" +
                        ") " +
                        "WHERE tei.version_number IS NULL")
                        .executeUpdate();
            } catch (Exception ignored) { status.setRollbackOnly(); }
            return null;
        });
    }

    @EventListener(ApplicationReadyEvent.class)
    public void migrate() {
        backfillExecutionItemVersions();

        Long count = tx.execute(status -> projectRepository.count());
        Long pid;
        if (count == null || count == 0) {
            pid = tx.execute(status -> {
                Project p = projectRepository.save(
                        new Project("test프로젝트", "기존 데이터를 위한 기본 프로젝트", null));
                return p.getId();
            });
        } else {
            // 기본 프로젝트가 이미 있으면 그쪽으로, 없으면 가장 오래된 프로젝트로 소속 없는 데이터를 모은다.
            pid = tx.execute(status -> projectRepository.findAll().stream()
                    .filter(p -> "test프로젝트".equals(p.getName()))
                    .map(Project::getId)
                    .findFirst()
                    .orElseGet(() -> projectRepository.findAll().stream()
                            .map(Project::getId)
                            .min(Long::compareTo)
                            .orElse(null)));
        }
        if (pid == null) return;

        // project_id가 비어있는 레거시 행을 기본 프로젝트로 일괄 배정 — 컬럼이 나중에 추가된 테이블(예: test_executions)도
        // 매 기동 시 재시도하므로, NULL이 남아있는 한 계속 보정된다(이미 값이 있는 행은 건드리지 않아 안전).
        for (String table : new String[]{"test_cases", "test_folders", "test_plans",
                                         "test_suites", "test_executions", "area_tags"}) {
            final String t = table;
            tx.execute(status -> {
                try {
                    em.createNativeQuery(
                                    "UPDATE " + t + " SET project_id = :pid WHERE project_id IS NULL")
                            .setParameter("pid", pid)
                            .executeUpdate();
                } catch (Exception ignored) {
                    status.setRollbackOnly();
                }
                return null;
            });
        }
    }
}
