package com.bhavesh.projectTracker.service;

import com.bhavesh.projectTracker.config.JiraCloudProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Component
public class JiraCloudClient {

  private final JiraCloudProperties jiraCloudProperties;

  public JiraCloudClient(JiraCloudProperties jiraCloudProperties) {
    this.jiraCloudProperties = jiraCloudProperties;
  }

  public List<JsonNode> fetchIssues(String jql) {
    validateConfiguration();

    RestClient restClient = RestClient.builder()
        .baseUrl(jiraCloudProperties.getBaseUrl().trim())
        .defaultHeader(HttpHeaders.AUTHORIZATION, buildBasicAuthHeader())
        .build();

    int startAt = 0;
    int maxResults = Math.max(1, jiraCloudProperties.getMaxResults());
    int total = Integer.MAX_VALUE;
    List<JsonNode> allIssues = new ArrayList<>();

    while (startAt < total) {
      String uri = UriComponentsBuilder.fromPath("/rest/api/3/search")
          .queryParam("jql", jql)
          .queryParam("startAt", startAt)
          .queryParam("maxResults", maxResults)
          .queryParam("fields", buildFieldsParam())
          .build(true)
          .toUriString();

      JsonNode response = restClient.get()
          .uri(uri)
          .retrieve()
          .body(JsonNode.class);

      if (response == null || response.path("issues").isMissingNode() || !response.path("issues").isArray()) {
        break;
      }

      JsonNode issues = response.path("issues");
      total = response.path("total").asInt(issues.size());

      for (JsonNode issue : issues) {
        allIssues.add(issue);
      }

      startAt += issues.size();
      if (issues.isEmpty()) {
        break;
      }
    }

    return allIssues;
  }

  private String buildFieldsParam() {
    return String.join(",",
        "status",
        "issuetype",
        "assignee",
        "reporter",
        "created",
        "resolutiondate",
        jiraCloudProperties.getSprintFieldId()
    );
  }

  private void validateConfiguration() {
    if (!StringUtils.hasText(jiraCloudProperties.getBaseUrl())
        || !StringUtils.hasText(jiraCloudProperties.getEmail())
        || !StringUtils.hasText(jiraCloudProperties.getApiToken())) {
      throw new IllegalStateException("Jira Cloud configuration is missing. Set JIRA_BASE_URL, JIRA_EMAIL, and JIRA_API_TOKEN.");
    }
  }

  private String buildBasicAuthHeader() {
    String auth = jiraCloudProperties.getEmail().trim() + ":" + jiraCloudProperties.getApiToken().trim();
    String encoded = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
    return "Basic " + encoded;
  }
}
