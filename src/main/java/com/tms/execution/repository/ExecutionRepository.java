package com.tms.execution.repository;

import com.tms.execution.entity.Execution;
import com.tms.execution.entity.ExecutionStatus;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecutionRepository extends JpaRepository<Execution, Long> {

    @EntityGraph(attributePaths = "items")
    List<Execution> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "items")
    List<Execution> findAllByProjectIdOrderByCreatedAtDesc(Long projectId);

    long countByTestPlanIdAndStatus(Long testPlanId, ExecutionStatus status);
}
