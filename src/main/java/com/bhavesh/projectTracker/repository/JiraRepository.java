package com.bhavesh.projectTracker.repository;

import com.bhavesh.projectTracker.model.Jira;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JiraRepository extends JpaRepository<Jira, String> {
  List<Jira> findBySprintNameIgnoreCase(String sprintName);
}
