package com.tms.execution.repository;

import com.tms.execution.entity.ExecutionItem;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecutionItemRepository extends JpaRepository<ExecutionItem, Long> {
    @EntityGraph(attributePaths = "execution")
    List<ExecutionItem> findAllByTestCaseIdOrderByIdDesc(Long testCaseId);
}
