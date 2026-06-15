package com.tms.defect.dto;

import com.tms.defect.entity.DefectSeverity;
import com.tms.defect.entity.DefectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DefectRequest(
        @NotBlank @Size(max = 200) String title,
        String description,
        @NotNull DefectSeverity severity,
        DefectStatus status,
        @Size(max = 500) String externalUrl
) {
}
