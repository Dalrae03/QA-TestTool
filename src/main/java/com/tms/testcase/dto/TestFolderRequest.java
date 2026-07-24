package com.tms.testcase.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TestFolderRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 20) String code,
        Long parentId,
        Long projectId
) {
}
