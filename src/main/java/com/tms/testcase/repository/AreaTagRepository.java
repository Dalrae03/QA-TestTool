package com.tms.testcase.repository;

import com.tms.testcase.entity.AreaTag;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AreaTagRepository extends JpaRepository<AreaTag, Long> {
    boolean existsByName(String name);
    List<AreaTag> findAllByIdIn(List<Long> ids);
}
