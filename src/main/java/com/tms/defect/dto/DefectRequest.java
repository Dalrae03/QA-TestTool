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
        @Size(max = 500) String externalUrl,
        Long projectId
) {
    /** projectId 없는 하위 호환 생성자 — 전역 결함(프로젝트 미지정)으로 만든다. */
    public DefectRequest(String title, String description, DefectSeverity severity, DefectStatus status, String externalUrl) {
        this(title, description, severity, status, externalUrl, null);
    }
}
