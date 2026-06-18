package com.tms.defect.dto;

import com.tms.defect.entity.Defect;
import com.tms.defect.entity.DefectSeverity;
import com.tms.defect.entity.DefectStatus;
import java.time.LocalDateTime;

public record DefectResponse(
        Long id,
        String title,
        String description,
        DefectSeverity severity,
        DefectStatus status,
        String externalUrl,
        String jiraKey,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static DefectResponse from(Defect defect) {
        return new DefectResponse(
                defect.getId(),
                defect.getTitle(),
                defect.getDescription(),
                defect.getSeverity(),
                defect.getStatus(),
                defect.getExternalUrl(),
                defect.getJiraKey(),
                defect.getCreatedAt(),
                defect.getUpdatedAt()
        );
    }
}
