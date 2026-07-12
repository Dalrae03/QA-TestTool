package com.tms.testplan.repository;

import com.tms.testplan.entity.TestPlan;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestPlanRepository extends JpaRepository<TestPlan, Long> {
    List<TestPlan> findAllByOrderByUpdatedAtDesc();
    List<TestPlan> findAllByProjectIdOrderByUpdatedAtDesc(Long projectId);
    List<TestPlan> findAllByCoreTestCases_Id(Long testCaseId);
}
