package com.tms.testcase.dto;

import com.tms.testcase.entity.TestFolder;
import java.time.LocalDateTime;
import java.util.List;

public record TestFolderResponse(
        Long id,
        String name,
        Long parentId,
        List<TestFolderResponse> children,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TestFolderResponse from(TestFolder folder) {
        return new TestFolderResponse(
                folder.getId(),
                folder.getName(),
                folder.getParent() == null ? null : folder.getParent().getId(),
                folder.getChildren().stream().map(TestFolderResponse::from).toList(),
                folder.getCreatedAt(),
                folder.getUpdatedAt()
        );
    }

    /** children 없이 단순 참조용 (순환 방지) */
    public static TestFolderResponse shallow(TestFolder folder) {
        if (folder == null) return null;
        return new TestFolderResponse(
                folder.getId(),
                folder.getName(),
                folder.getParent() == null ? null : folder.getParent().getId(),
                List.of(),
                folder.getCreatedAt(),
                folder.getUpdatedAt()
        );
    }
}
