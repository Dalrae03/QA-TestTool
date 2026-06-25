package com.tms.testcase.repository;

import com.tms.testcase.entity.TestCase;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TestCaseRepository extends JpaRepository<TestCase, Long>, JpaSpecificationExecutor<TestCase> {
    List<TestCase> findAllByAreaTags_Id(Long areaTagId);
    List<TestCase> findAllByServerEnvironmentId(Long serverEnvironmentId);
    List<TestCase> findAllByTestConfigurationId(Long testConfigurationId);
    List<TestCase> findAllByFolderIdOrderByCreatedAtAsc(Long folderId);
    List<TestCase> findAllByFolderIsNullOrderByCreatedAtAsc();
    boolean existsByFolderId(Long folderId);
    List<TestCase> findAllByProjectId(Long projectId);
}
