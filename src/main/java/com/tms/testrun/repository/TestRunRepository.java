package com.tms.testrun.repository;

import com.tms.testrun.entity.TestRun;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestRunRepository extends JpaRepository<TestRun, Long> {
    List<TestRun> findByTestCaseIdOrderByExecutedAtDesc(Long testCaseId);
    void deleteAllByTestCaseId(Long testCaseId);
}
