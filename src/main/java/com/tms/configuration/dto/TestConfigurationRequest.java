package com.tms.configuration.dto;

import com.tms.testcase.entity.TestCaseBrowser;
import com.tms.testcase.entity.TestCaseDevice;
import com.tms.testcase.entity.TestCaseOs;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TestConfigurationRequest(
        @NotBlank @Size(max = 100) String name,
        Long serverEnvironmentId,
        TestCaseOs os,
        @Size(max = 50) String osVersion,
        TestCaseBrowser browser,
        @Size(max = 50) String browserVersion,
        TestCaseDevice device,
        @Size(max = 50) String runtimeVersion,
        @Size(max = 50) String dbVersion,
        boolean active
) {
}
