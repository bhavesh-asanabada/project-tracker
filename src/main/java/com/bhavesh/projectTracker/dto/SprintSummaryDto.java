package com.bhavesh.projectTracker.dto;

public record SprintSummaryDto(
    String sprintName,
    long totalAssignedTickets,
    long totalCompletedTickets,
    double completionPercentage
) {
}
