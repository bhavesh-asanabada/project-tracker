package com.bhavesh.projectTracker.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "jira")
public class Jira {

  @Id
  private String jiraId;

  private String jiraKey;
  private String sprintName;
  private String ticketType;
  private String status;

  private LocalDate createdOn;
  private LocalDate readyForReviewOn;
  private LocalDate doneOn;
  private LocalDate sprintStartOn;
  private LocalDate sprintEndOn;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assignee_user_id")
  private User assignee;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reporter_user_id")
  private User reporter;
}
