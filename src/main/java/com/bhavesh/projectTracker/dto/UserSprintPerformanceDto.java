package com.bhavesh.projectTracker.dto;

public record UserSprintPerformanceDto(
    String userId,
    String displayName,
    long assignedStories,
    long assignedTasks,
    long assignedBugs,
    long totalAssignedTickets,
    long totalCompletedTickets,
    double completionPercentage,
    Double previousCompletionPercentage,
    Double completionDeltaPercentage
) {
}
