package com.bhavesh.projectTracker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "project-tracker.sprint-metrics")
public class SprintMetricsProperties {

  private String storyType = "story";
  private String taskType = "task";
  private String bugType = "bug";
  private List<String> completedStatuses = List.of("done", "closed", "resolved");

  public String getStoryType() {
    return storyType;
  }

  public void setStoryType(String storyType) {
    this.storyType = storyType;
  }

  public String getTaskType() {
    return taskType;
  }

  public void setTaskType(String taskType) {
    this.taskType = taskType;
  }

  public String getBugType() {
    return bugType;
  }

  public void setBugType(String bugType) {
    this.bugType = bugType;
  }

  public List<String> getCompletedStatuses() {
    return completedStatuses;
  }

  public void setCompletedStatuses(List<String> completedStatuses) {
    this.completedStatuses = completedStatuses;
  }
}
