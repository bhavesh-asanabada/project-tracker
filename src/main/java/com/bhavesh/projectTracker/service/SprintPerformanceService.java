package com.bhavesh.projectTracker.service;

import com.bhavesh.projectTracker.config.SprintMetricsProperties;
import com.bhavesh.projectTracker.dto.SprintPerformanceResponseDto;
import com.bhavesh.projectTracker.dto.SprintSummaryDto;
import com.bhavesh.projectTracker.dto.UserSprintPerformanceDto;
import com.bhavesh.projectTracker.model.Jira;
import com.bhavesh.projectTracker.model.User;
import com.bhavesh.projectTracker.repository.JiraRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class SprintPerformanceService {

  private final JiraRepository jiraRepository;
  private final String storyType;
  private final String taskType;
  private final String bugType;
  private final Set<String> completedStatuses;

  public SprintPerformanceService(JiraRepository jiraRepository, SprintMetricsProperties metricsProperties) {
    this.jiraRepository = jiraRepository;
    this.storyType = normalize(metricsProperties.getStoryType());
    this.taskType = normalize(metricsProperties.getTaskType());
    this.bugType = normalize(metricsProperties.getBugType());
    this.completedStatuses = normalizeStatuses(metricsProperties.getCompletedStatuses());
  }

  public SprintPerformanceResponseDto getSprintPerformance(String currentSprint, String previousSprint) {
    if (!StringUtils.hasText(currentSprint) || !StringUtils.hasText(previousSprint)) {
      throw new IllegalArgumentException("Both currentSprint and previousSprint are required.");
    }

    List<Jira> currentIssues = jiraRepository.findBySprintNameIgnoreCase(currentSprint.trim());
    List<Jira> previousIssues = jiraRepository.findBySprintNameIgnoreCase(previousSprint.trim());

    Map<String, UserMetrics> currentByUser = aggregateByUser(currentIssues);
    Map<String, UserMetrics> previousByUser = aggregateByUser(previousIssues);

    List<UserSprintPerformanceDto> userMetrics = buildUserDtos(currentByUser, previousByUser);

    SprintSummaryDto currentSummary = summarizeSprint(currentSprint, currentByUser);
    SprintSummaryDto previousSummary = summarizeSprint(previousSprint, previousByUser);

    double averageRate = userMetrics.stream()
        .mapToDouble(UserSprintPerformanceDto::completionPercentage)
        .average()
        .orElse(0.0d);

    List<String> higherUsers = userMetrics.stream()
        .filter(dto -> dto.completionPercentage() > averageRate)
        .map(UserSprintPerformanceDto::displayName)
        .toList();

    List<String> lowerUsers = userMetrics.stream()
        .filter(dto -> dto.completionPercentage() < averageRate)
        .map(UserSprintPerformanceDto::displayName)
        .toList();

    return new SprintPerformanceResponseDto(
        currentSummary,
        previousSummary,
        userMetrics,
        higherUsers,
        lowerUsers
    );
  }

  private Map<String, UserMetrics> aggregateByUser(List<Jira> issues) {
    Map<String, UserMetrics> metricsByUser = new HashMap<>();

    for (Jira issue : issues) {
      String userKey = getUserIdOrFallback(issue.getAssignee());
      String displayName = getDisplayNameOrFallback(issue.getAssignee());

      UserMetrics metrics = metricsByUser.computeIfAbsent(userKey, key -> new UserMetrics(userKey, displayName));
      boolean countedInScope = metrics.countAssigned(issue.getTicketType());

      if (countedInScope && isCompleted(issue)) {
        metrics.completedTickets++;
      }
    }

    return metricsByUser;
  }

  private List<UserSprintPerformanceDto> buildUserDtos(Map<String, UserMetrics> currentByUser,
                                                       Map<String, UserMetrics> previousByUser) {
    List<UserSprintPerformanceDto> rows = new ArrayList<>();

    for (UserMetrics current : currentByUser.values()) {
      UserMetrics previous = previousByUser.get(current.userId);
      Double previousPercentage = previous == null ? null : previous.getCompletionPercentage();
      Double delta = previousPercentage == null ? null : round2(current.getCompletionPercentage() - previousPercentage);

      rows.add(new UserSprintPerformanceDto(
          current.userId,
          current.displayName,
          current.assignedStories,
          current.assignedTasks,
          current.assignedBugs,
          current.getTotalAssignedTickets(),
          current.completedTickets,
          current.getCompletionPercentage(),
          previousPercentage,
          delta
      ));
    }

    rows.sort(Comparator.comparing(UserSprintPerformanceDto::completionPercentage).reversed());
    return rows;
  }

  private SprintSummaryDto summarizeSprint(String sprintName, Map<String, UserMetrics> byUser) {
    long totalAssigned = byUser.values().stream().mapToLong(UserMetrics::getTotalAssignedTickets).sum();
    long totalCompleted = byUser.values().stream().mapToLong(metrics -> metrics.completedTickets).sum();

    return new SprintSummaryDto(
        sprintName,
        totalAssigned,
        totalCompleted,
        percent(totalCompleted, totalAssigned)
    );
  }

  private boolean isCompleted(Jira issue) {
    if (issue.getDoneOn() != null) {
      return true;
    }

    if (!StringUtils.hasText(issue.getStatus())) {
      return false;
    }

    String status = normalize(issue.getStatus());
    return completedStatuses.contains(status);
  }

  private String normalize(String value) {
    return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "";
  }

  private Set<String> normalizeStatuses(List<String> statuses) {
    Set<String> normalized = new HashSet<>();
    if (statuses == null) {
      return normalized;
    }

    for (String status : statuses) {
      String value = normalize(status);
      if (!value.isEmpty()) {
        normalized.add(value);
      }
    }
    return normalized;
  }

  private String getUserIdOrFallback(User assignee) {
    if (assignee == null || !StringUtils.hasText(assignee.getUserId())) {
      return "unassigned";
    }
    return assignee.getUserId();
  }

  private String getDisplayNameOrFallback(User assignee) {
    if (assignee == null || !StringUtils.hasText(assignee.getDisplayName())) {
      return "Unassigned";
    }
    return assignee.getDisplayName();
  }

  private double percent(long completed, long assigned) {
    if (assigned == 0) {
      return 0.0d;
    }
    return round2((completed * 100.0d) / assigned);
  }

  private double round2(double value) {
    return Math.round(value * 100.0d) / 100.0d;
  }

  private class UserMetrics {
    private final String userId;
    private final String displayName;

    private long assignedStories;
    private long assignedTasks;
    private long assignedBugs;
    private long completedTickets;

    private UserMetrics(String userId, String displayName) {
      this.userId = userId;
      this.displayName = displayName;
    }

    private boolean countAssigned(String ticketType) {
      String normalized = normalize(ticketType);
      if (storyType.equals(normalized)) {
        assignedStories++;
        return true;
      } else if (taskType.equals(normalized)) {
        assignedTasks++;
        return true;
      } else if (bugType.equals(normalized)) {
        assignedBugs++;
        return true;
      }

      return false;
    }

    private long getTotalAssignedTickets() {
      return assignedStories + assignedTasks + assignedBugs;
    }

    private double getCompletionPercentage() {
      return percent(completedTickets, getTotalAssignedTickets());
    }
  }
}
