package com.bhavesh.projectTracker.dto;

import java.util.List;

public record SprintPerformanceResponseDto(
    SprintSummaryDto currentSprint,
    SprintSummaryDto previousSprint,
    List<UserSprintPerformanceDto> users,
    List<String> higherCompletionUsers,
    List<String> lowerCompletionUsers
) {
}
