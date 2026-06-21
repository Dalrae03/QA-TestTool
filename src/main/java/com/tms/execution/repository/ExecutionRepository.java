package com.tms.execution.repository;

import com.tms.execution.entity.Execution;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecutionRepository extends JpaRepository<Execution, Long> {

    @EntityGraph(attributePaths = "items")
    List<Execution> findAllByOrderByCreatedAtDesc();
}
