package com.tms.execution.dto;

import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateExecutionRequest(
        Long suiteId,
        List<Long> testCaseIds,
        Long testPlanId,
        Long projectId,
        Long testConfigurationId,
        @Size(max = 200) String name,
        String description,
        @Size(max = 100) String assignee
) {
}
