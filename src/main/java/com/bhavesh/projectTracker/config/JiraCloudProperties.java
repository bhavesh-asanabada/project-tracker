package com.bhavesh.projectTracker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "project-tracker.jira")
public class JiraCloudProperties {

  private String baseUrl;
  private String email;
  private String apiToken;
  private String projectKey;
  private String defaultJql;
  private String sprintFieldId = "customfield_10020";
  private int maxResults = 100;
  private boolean pollingEnabled = false;
  private long pollingIntervalMs = 300000;

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getApiToken() {
    return apiToken;
  }

  public void setApiToken(String apiToken) {
    this.apiToken = apiToken;
  }

  public String getProjectKey() {
    return projectKey;
  }

  public void setProjectKey(String projectKey) {
    this.projectKey = projectKey;
  }

  public String getDefaultJql() {
    return defaultJql;
  }

  public void setDefaultJql(String defaultJql) {
    this.defaultJql = defaultJql;
  }

  public String getSprintFieldId() {
    return sprintFieldId;
  }

  public void setSprintFieldId(String sprintFieldId) {
    this.sprintFieldId = sprintFieldId;
  }

  public int getMaxResults() {
    return maxResults;
  }

  public void setMaxResults(int maxResults) {
    this.maxResults = maxResults;
  }

  public boolean isPollingEnabled() {
    return pollingEnabled;
  }

  public void setPollingEnabled(boolean pollingEnabled) {
    this.pollingEnabled = pollingEnabled;
  }

  public long getPollingIntervalMs() {
    return pollingIntervalMs;
  }

  public void setPollingIntervalMs(long pollingIntervalMs) {
    this.pollingIntervalMs = pollingIntervalMs;
  }
}
