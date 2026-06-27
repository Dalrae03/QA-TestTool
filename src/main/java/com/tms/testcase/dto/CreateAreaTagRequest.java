package com.tms.testcase.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAreaTagRequest(
        @NotBlank
        @Size(max = 50)
        String name,
        Long projectId
) {
}
