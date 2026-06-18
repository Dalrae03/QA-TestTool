package com.tms.testplan.dto;

import com.tms.testplan.entity.TestPlanStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TestPlanRequest(
        @NotBlank @Size(max = 200) String name,
        String description,
        @NotNull TestPlanStatus status,
        @Size(max = 100) String assignee,
        LocalDate startDate,
        LocalDate endDate
) {
}
