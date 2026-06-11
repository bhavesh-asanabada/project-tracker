package com.bhavesh.projectTracker.dto;

public record JiraSyncResponseDto(
    String jql,
    int fetchedIssues,
    int syncedIssues
) {
}
