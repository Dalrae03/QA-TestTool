package com.tms.testcase.repository;

import com.tms.testcase.entity.TestCaseVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestCaseVersionRepository extends JpaRepository<TestCaseVersion, Long> {

    List<TestCaseVersion> findByTestCaseIdOrderByVersionNumberDesc(Long testCaseId);

    Optional<TestCaseVersion> findTopByTestCaseIdOrderByVersionNumberDesc(Long testCaseId);

    Optional<TestCaseVersion> findByIdAndTestCaseId(Long id, Long testCaseId);

    void deleteAllByTestCaseId(Long testCaseId);
}
