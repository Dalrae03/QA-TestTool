package com.tms.jira.dto;

import jakarta.validation.constraints.NotBlank;

public record JiraLinkRequest(@NotBlank String jiraKey) {
}
