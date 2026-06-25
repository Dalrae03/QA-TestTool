package com.tms.testsuite.repository;

import com.tms.testsuite.entity.TestSuite;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TestSuiteRepository extends JpaRepository<TestSuite, Long> {

    @EntityGraph(attributePaths = "testCases")
    List<TestSuite> findByTestPlanIdOrderByCreatedAtAsc(Long testPlanId);

    @EntityGraph(attributePaths = "testCases")
    List<TestSuite> findByTestPlanIdIsNullOrderByCreatedAtAsc();

    @EntityGraph(attributePaths = "testCases")
    List<TestSuite> findByProjectIdAndTestPlanIdIsNullOrderByCreatedAtAsc(Long projectId);

    @EntityGraph(attributePaths = "testCases")
    List<TestSuite> findAllByTestCases_Id(Long testCaseId);

    @EntityGraph(attributePaths = "testCases")
    List<TestSuite> findAllByOrderByCreatedAtAsc();

    @EntityGraph(attributePaths = "testCases")
    List<TestSuite> findAllByProjectIdOrderByCreatedAtAsc(Long projectId);

    long countByTestPlanId(Long testPlanId);

    @Query("select count(distinct testCase.id) from TestSuite suite join suite.testCases testCase where suite.testPlan.id = :planId")
    long countDistinctTestCasesByTestPlanId(@Param("planId") Long planId);

    @Modifying
    @Query("update TestSuite s set s.testPlan = null where s.testPlan.id = :planId")
    void detachFromPlan(@Param("planId") Long planId);
}
