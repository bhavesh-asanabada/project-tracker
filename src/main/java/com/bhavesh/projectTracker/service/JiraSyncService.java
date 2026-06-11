package com.bhavesh.projectTracker.service;

import com.bhavesh.projectTracker.config.JiraCloudProperties;
import com.bhavesh.projectTracker.dto.JiraSyncResponseDto;
import com.bhavesh.projectTracker.model.Jira;
import com.bhavesh.projectTracker.model.User;
import com.bhavesh.projectTracker.repository.JiraRepository;
import com.bhavesh.projectTracker.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class JiraSyncService {

  private static final String READY_FOR_REVIEW = "ready for review";

  private final JiraCloudClient jiraCloudClient;
  private final JiraCloudProperties jiraCloudProperties;
  private final JiraRepository jiraRepository;
  private final UserRepository userRepository;

  public JiraSyncService(JiraCloudClient jiraCloudClient,
                         JiraCloudProperties jiraCloudProperties,
                         JiraRepository jiraRepository,
                         UserRepository userRepository) {
    this.jiraCloudClient = jiraCloudClient;
    this.jiraCloudProperties = jiraCloudProperties;
    this.jiraRepository = jiraRepository;
    this.userRepository = userRepository;
  }

  @Transactional
  public JiraSyncResponseDto syncIssues(String requestedJql) {
    String jql = resolveJql(requestedJql);
    List<JsonNode> rawIssues = jiraCloudClient.fetchIssues(jql);

    List<Jira> mapped = new ArrayList<>();
    for (JsonNode raw : rawIssues) {
      Jira jiraIssue = mapIssue(raw);
      if (jiraIssue != null) {
        mapped.add(jiraIssue);
      }
    }

    jiraRepository.saveAll(mapped);
    return new JiraSyncResponseDto(jql, rawIssues.size(), mapped.size());
  }

  @Scheduled(fixedDelayString = "${project-tracker.jira.polling-interval-ms:300000}")
  public void scheduledSync() {
    if (!jiraCloudProperties.isPollingEnabled()) {
      return;
    }

    syncIssues(null);
  }

  private String resolveJql(String requestedJql) {
    if (StringUtils.hasText(requestedJql)) {
      return requestedJql.trim();
    }

    if (StringUtils.hasText(jiraCloudProperties.getDefaultJql())) {
      return jiraCloudProperties.getDefaultJql().trim();
    }

    if (StringUtils.hasText(jiraCloudProperties.getProjectKey())) {
      return "project = " + jiraCloudProperties.getProjectKey().trim() + " ORDER BY updated DESC";
    }

    throw new IllegalArgumentException("Provide jql or set JIRA_DEFAULT_JQL / JIRA_PROJECT_KEY.");
  }

  private Jira mapIssue(JsonNode issueNode) {
    String issueId = issueNode.path("id").asText(null);
    if (!StringUtils.hasText(issueId)) {
      return null;
    }

    Jira issue = jiraRepository.findById(issueId).orElseGet(Jira::new);
    issue.setJiraId(issueId);
    issue.setJiraKey(issueNode.path("key").asText(null));

    JsonNode fields = issueNode.path("fields");
    issue.setTicketType(text(fields.path("issuetype").path("name")));
    String status = text(fields.path("status").path("name"));
    issue.setStatus(status);

    issue.setCreatedOn(parseDate(text(fields.path("created"))));
    issue.setDoneOn(parseDate(text(fields.path("resolutiondate"))));
    issue.setReadyForReviewOn(isReadyForReview(status) ? issue.getDoneOn() : null);

    JsonNode sprint = findSprintNode(fields.path(jiraCloudProperties.getSprintFieldId()));
    issue.setSprintName(text(sprint.path("name")));
    issue.setSprintStartOn(parseDate(text(sprint.path("startDate"))));
    issue.setSprintEndOn(parseDate(text(sprint.path("endDate"))));

    issue.setAssignee(upsertUser(fields.path("assignee")));
    issue.setReporter(upsertUser(fields.path("reporter")));

    return issue;
  }

  private User upsertUser(JsonNode userNode) {
    String userId = text(userNode.path("accountId"));
    if (!StringUtils.hasText(userId)) {
      return null;
    }

    User user = userRepository.findById(userId).orElseGet(User::new);
    user.setUserId(userId);

    String displayName = text(userNode.path("displayName"));
    user.setDisplayName(displayName);

    String[] names = splitName(displayName);
    user.setFirstName(names[0]);
    user.setLastName(names[1]);

    String email = text(userNode.path("emailAddress"));
    if (!StringUtils.hasText(email)) {
      email = userId + "@jira.local";
    }
    user.setEmail(email.toLowerCase(Locale.ROOT));

    if (!StringUtils.hasText(user.getOrganization())) {
      user.setOrganization("engineering");
    }

    return userRepository.save(user);
  }

  private String[] splitName(String displayName) {
    if (!StringUtils.hasText(displayName)) {
      return new String[]{"Unknown", "User"};
    }

    String[] tokens = displayName.trim().split("\\s+", 2);
    String first = tokens[0];
    String last = tokens.length > 1 ? tokens[1] : "User";
    return new String[]{first, last};
  }

  private JsonNode findSprintNode(JsonNode sprintFieldNode) {
    if (sprintFieldNode == null || sprintFieldNode.isMissingNode() || sprintFieldNode.isNull()) {
      return com.fasterxml.jackson.databind.node.NullNode.instance;
    }

    if (sprintFieldNode.isArray()) {
      Optional<JsonNode> active = findActiveSprint(sprintFieldNode);
      if (active.isPresent()) {
        return active.get();
      }
      return sprintFieldNode.isEmpty() ? com.fasterxml.jackson.databind.node.NullNode.instance : sprintFieldNode.get(sprintFieldNode.size() - 1);
    }

    return sprintFieldNode;
  }

  private Optional<JsonNode> findActiveSprint(JsonNode sprintFieldNode) {
    for (JsonNode sprint : sprintFieldNode) {
      String state = text(sprint.path("state"));
      if (StringUtils.hasText(state) && state.trim().equalsIgnoreCase("active")) {
        return Optional.of(sprint);
      }
    }
    return Optional.empty();
  }

  private boolean isReadyForReview(String status) {
    return StringUtils.hasText(status) && status.trim().equalsIgnoreCase(READY_FOR_REVIEW);
  }

  private String text(JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return null;
    }

    String value = node.asText(null);
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  private LocalDate parseDate(String dateTime) {
    if (!StringUtils.hasText(dateTime)) {
      return null;
    }

    try {
      return OffsetDateTime.parse(dateTime).toLocalDate();
    } catch (Exception ignored) {
      return LocalDate.parse(dateTime);
    }
  }
}
