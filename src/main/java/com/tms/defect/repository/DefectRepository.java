package com.tms.defect.repository;

import com.tms.defect.entity.Defect;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DefectRepository extends JpaRepository<Defect, Long> {
    List<Defect> findAllByOrderByCreatedAtDesc();
    List<Defect> findAllByJiraKeyIsNotNull();
    Optional<Defect> findByJiraKey(String jiraKey);
}
