package com.tms.testrun.repository;

import com.tms.testrun.entity.TestRun;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TestRunRepository extends JpaRepository<TestRun, Long>, JpaSpecificationExecutor<TestRun> {
    List<TestRun> findByTestCaseIdOrderByExecutedAtDesc(Long testCaseId);
    void deleteAllByTestCaseId(Long testCaseId);
}
