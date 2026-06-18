package com.tms.testcase.repository;

import com.tms.testcase.entity.TestFolder;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestFolderRepository extends JpaRepository<TestFolder, Long> {
    List<TestFolder> findAllByParentIsNullOrderByNameAsc();
    List<TestFolder> findAllByParentIdOrderByNameAsc(Long parentId);
    boolean existsByParentId(Long parentId);
}
