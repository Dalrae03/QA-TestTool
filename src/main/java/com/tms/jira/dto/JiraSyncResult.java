package com.tms.jira.dto;

public record JiraSyncResult(int total, int success, int failed) {
}
