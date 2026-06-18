package com.tms.environment.repository;

import com.tms.environment.entity.ServerEnvironment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServerEnvironmentRepository extends JpaRepository<ServerEnvironment, Long> {
    List<ServerEnvironment> findAllByOrderByNameAsc();
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
