package com.tms.testsuite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record TestSuiteRequest(
        @NotBlank @Size(max = 200) String name,
        String description,
        List<Long> testCaseIds,
        Long projectId
) {
}
