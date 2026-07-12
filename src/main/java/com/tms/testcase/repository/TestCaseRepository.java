package com.tms.testcase.repository;

import com.tms.testcase.entity.TestCase;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TestCaseRepository extends JpaRepository<TestCase, Long>, JpaSpecificationExecutor<TestCase> {
    List<TestCase> findAllByAreaTags_Id(Long areaTagId);
    List<TestCase> findAllByServerEnvironmentId(Long serverEnvironmentId);
    List<TestCase> findAllByTestConfigurationId(Long testConfigurationId);
    List<TestCase> findAllByFolderIdOrderByCreatedAtAsc(Long folderId);
    List<TestCase> findAllByFolderIsNullOrderByCreatedAtAsc();
    boolean existsByFolderId(Long folderId);
    List<TestCase> findAllByProjectId(Long projectId);

    /** 특정 Jira 요구사항 key에 연결된 테스트케이스 역방향 조회(요구사항→테스트 추적). */
    @Query("select tc from TestCase tc join tc.jiraRequirementKeys k where k = :jiraKey order by tc.id asc")
    List<TestCase> findAllByJiraRequirementKey(@Param("jiraKey") String jiraKey);

    /**
     * 주어진 케이스들에 연결된 결함 수 집계. [testCaseId, count] 행 목록으로 반환하며,
     * 결함이 없는 케이스는 결과에 나타나지 않는다(호출부에서 0으로 처리).
     */
    @Query(value = "SELECT test_case_id, COUNT(*) FROM test_case_defects WHERE test_case_id IN (:ids) GROUP BY test_case_id", nativeQuery = true)
    List<Object[]> countDefectsByTestCaseIds(@Param("ids") List<Long> ids);
}
