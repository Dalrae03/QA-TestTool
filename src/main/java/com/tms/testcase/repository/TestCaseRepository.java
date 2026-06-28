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
}
