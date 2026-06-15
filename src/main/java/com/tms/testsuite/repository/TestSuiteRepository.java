package com.tms.testsuite.repository;

import com.tms.testsuite.entity.TestSuite;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TestSuiteRepository extends JpaRepository<TestSuite, Long> {
    @EntityGraph(attributePaths = "testCases")
    List<TestSuite> findByTestPlanIdOrderByCreatedAtAsc(Long testPlanId);

    @EntityGraph(attributePaths = "testCases")
    List<TestSuite> findAllByTestCases_Id(Long testCaseId);

    long countByTestPlanId(Long testPlanId);

    @Query("select count(distinct testCase.id) from TestSuite suite join suite.testCases testCase where suite.testPlan.id = :planId")
    long countDistinctTestCasesByTestPlanId(@Param("planId") Long planId);

    void deleteByTestPlanId(Long testPlanId);
}
