package com.bhavesh.projectTracker.controller;

import com.bhavesh.projectTracker.dto.JiraSyncResponseDto;
import com.bhavesh.projectTracker.service.JiraSyncService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/integrations/jira")
public class JiraIntegrationController {

  private final JiraSyncService jiraSyncService;

  public JiraIntegrationController(JiraSyncService jiraSyncService) {
    this.jiraSyncService = jiraSyncService;
  }

  @PostMapping("/sync")
  public JiraSyncResponseDto sync(@RequestParam(required = false) String jql) {
    return jiraSyncService.syncIssues(jql);
  }
}
