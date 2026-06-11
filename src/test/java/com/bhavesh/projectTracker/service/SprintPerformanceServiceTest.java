package com.bhavesh.projectTracker.service;

import com.bhavesh.projectTracker.config.SprintMetricsProperties;
import com.bhavesh.projectTracker.dto.SprintPerformanceResponseDto;
import com.bhavesh.projectTracker.dto.UserSprintPerformanceDto;
import com.bhavesh.projectTracker.model.Jira;
import com.bhavesh.projectTracker.model.User;
import com.bhavesh.projectTracker.repository.JiraRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SprintPerformanceServiceTest {

  @Test
  void computesCurrentAndPreviousSprintMetrics() {
    JiraRepository repository = mock(JiraRepository.class);
    SprintMetricsProperties properties = new SprintMetricsProperties();
    SprintPerformanceService service = new SprintPerformanceService(repository, properties);

    User john = new User();
    john.setUserId("u1");
    john.setDisplayName("John Doe");

    Jira storyDone = new Jira();
    storyDone.setJiraId("J-1");
    storyDone.setTicketType("Story");
    storyDone.setStatus("Done");
    storyDone.setDoneOn(LocalDate.now());
    storyDone.setAssignee(john);

    Jira taskOpen = new Jira();
    taskOpen.setJiraId("J-2");
    taskOpen.setTicketType("Task");
    taskOpen.setStatus("In Progress");
    taskOpen.setAssignee(john);

    Jira previousStory = new Jira();
    previousStory.setJiraId("J-3");
    previousStory.setTicketType("Story");
    previousStory.setStatus("Done");
    previousStory.setDoneOn(LocalDate.now().minusDays(10));
    previousStory.setAssignee(john);

    when(repository.findBySprintNameIgnoreCase("Sprint 12")).thenReturn(List.of(storyDone, taskOpen));
    when(repository.findBySprintNameIgnoreCase("Sprint 11")).thenReturn(List.of(previousStory));

    SprintPerformanceResponseDto result = service.getSprintPerformance("Sprint 12", "Sprint 11");

    assertThat(result.currentSprint().totalAssignedTickets()).isEqualTo(2);
    assertThat(result.currentSprint().totalCompletedTickets()).isEqualTo(1);
    assertThat(result.currentSprint().completionPercentage()).isEqualTo(50.0d);

    UserSprintPerformanceDto user = result.users().getFirst();
    assertThat(user.userId()).isEqualTo("u1");
    assertThat(user.assignedStories()).isEqualTo(1);
    assertThat(user.assignedTasks()).isEqualTo(1);
    assertThat(user.assignedBugs()).isEqualTo(0);
    assertThat(user.completionPercentage()).isEqualTo(50.0d);
    assertThat(user.previousCompletionPercentage()).isEqualTo(100.0d);
    assertThat(user.completionDeltaPercentage()).isEqualTo(-50.0d);
  }
}
