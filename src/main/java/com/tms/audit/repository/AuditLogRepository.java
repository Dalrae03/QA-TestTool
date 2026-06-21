package com.tms.audit.repository;

import com.tms.audit.entity.AuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDescIdDesc(String entityType, Long entityId);

    void deleteAllByEntityTypeAndEntityId(String entityType, Long entityId);
}
