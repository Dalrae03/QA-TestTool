package com.tms.testcase.repository;

import com.tms.testcase.entity.AreaTag;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AreaTagRepository extends JpaRepository<AreaTag, Long> {
    boolean existsByNameIgnoreCase(String name);
    List<AreaTag> findAllByIdIn(List<Long> ids);
    Optional<AreaTag> findByNameIgnoreCase(String name);
}
