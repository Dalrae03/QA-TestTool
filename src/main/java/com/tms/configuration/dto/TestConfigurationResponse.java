package com.tms.configuration.dto;

import com.tms.configuration.entity.TestConfiguration;
import com.tms.environment.dto.ServerEnvironmentResponse;
import com.tms.testcase.entity.TestCaseBrowser;
import com.tms.testcase.entity.TestCaseDevice;
import com.tms.testcase.entity.TestCaseOs;
import java.time.LocalDateTime;

public record TestConfigurationResponse(
        Long id,
        String name,
        ServerEnvironmentResponse serverEnvironment,
        TestCaseOs os,
        String osVersion,
        TestCaseBrowser browser,
        String browserVersion,
        TestCaseDevice device,
        String runtimeVersion,
        String dbVersion,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TestConfigurationResponse from(TestConfiguration configuration) {
        return new TestConfigurationResponse(
                configuration.getId(), configuration.getName(),
                configuration.getServerEnvironment() == null
                        ? null : ServerEnvironmentResponse.from(configuration.getServerEnvironment()),
                configuration.getOs(), configuration.getOsVersion(),
                configuration.getBrowser(), configuration.getBrowserVersion(),
                configuration.getDevice(),
                configuration.getRuntimeVersion(), configuration.getDbVersion(),
                configuration.isActive(), configuration.getCreatedAt(), configuration.getUpdatedAt()
        );
    }
}
