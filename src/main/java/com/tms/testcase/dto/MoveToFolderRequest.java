package com.tms.testcase.dto;

/** folderId가 null이면 폴더에서 꺼내 루트로 이동 */
public record MoveToFolderRequest(Long folderId) {
}
