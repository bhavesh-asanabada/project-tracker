package com.bhavesh.projectTracker.controller;

import com.bhavesh.projectTracker.dto.SprintPerformanceResponseDto;
import com.bhavesh.projectTracker.service.SprintPerformanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metrics")
public class SprintPerformanceController {

  private final SprintPerformanceService sprintPerformanceService;

  public SprintPerformanceController(SprintPerformanceService sprintPerformanceService) {
    this.sprintPerformanceService = sprintPerformanceService;
  }

  @GetMapping("/sprint-performance")
  public SprintPerformanceResponseDto getSprintPerformance(
      @RequestParam String currentSprint,
      @RequestParam String previousSprint
  ) {
    return sprintPerformanceService.getSprintPerformance(currentSprint, previousSprint);
  }
}
