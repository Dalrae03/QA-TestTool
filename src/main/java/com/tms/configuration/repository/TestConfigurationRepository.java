package com.tms.configuration.repository;

import com.tms.configuration.entity.TestConfiguration;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestConfigurationRepository extends JpaRepository<TestConfiguration, Long> {
    List<TestConfiguration> findAllByOrderByNameAsc();
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
    List<TestConfiguration> findAllByServerEnvironmentId(Long serverEnvironmentId);
}
