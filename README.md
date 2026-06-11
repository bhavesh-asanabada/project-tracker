# Project Tracker

The Project Tracker is a web application designed to help users manage and track their Jira Work IDs efficiently. It provides
features for creating, updating, and deleting projects, as well as assigning tasks to team members. The application
leverages Spring Boot for the backend, JPA for database interactions, and Maven for project management.

## Features
* User management with unique user IDs and email addresses.
* Project and task management with assignment capabilities.
* Integration with Jira for issue tracking.
* RESTful APIs for interacting with the application.
* Sprint performance metrics for current vs previous sprint comparisons.

## Sprint Performance API

Endpoint:

`GET /api/metrics/sprint-performance?currentSprint={current}&previousSprint={previous}`

What it returns:

* Current and previous sprint totals (assigned, completed, completion percentage).
* Per-user Story/Task/Bug assignment counts.
* Per-user completion percentage and delta from the previous sprint.
* Users with comparatively higher and lower completion rates in the current sprint.

Example response shape:

```json
{
	"currentSprint": {
		"sprintName": "Sprint 12",
		"totalAssignedTickets": 21,
		"totalCompletedTickets": 16,
		"completionPercentage": 76.19
	},
	"previousSprint": {
		"sprintName": "Sprint 11",
		"totalAssignedTickets": 19,
		"totalCompletedTickets": 15,
		"completionPercentage": 78.95
	},
	"users": [
		{
			"userId": "u123",
			"displayName": "John Doe",
			"assignedStories": 4,
			"assignedTasks": 3,
			"assignedBugs": 2,
			"totalAssignedTickets": 9,
			"totalCompletedTickets": 8,
			"completionPercentage": 88.89,
			"previousCompletionPercentage": 71.43,
			"completionDeltaPercentage": 17.46
		}
	],
	"higherCompletionUsers": ["John Doe"],
	"lowerCompletionUsers": ["Jane Smith"]
}
```

## Jira Cloud Integration

The project now supports direct Jira Cloud REST API sync.

Configuration is in [jira-cloud.properties](src/main/resources/jira-cloud.properties) and can be driven by environment variables:

* `JIRA_BASE_URL` (example: `https://your-domain.atlassian.net`)
* `JIRA_EMAIL`
* `JIRA_API_TOKEN`
* `JIRA_PROJECT_KEY` (optional if `JIRA_DEFAULT_JQL` is set)
* `JIRA_DEFAULT_JQL` (optional)
* `JIRA_SPRINT_FIELD_ID` (defaults to `customfield_10020`)
* `JIRA_POLLING_ENABLED` (`true` or `false`)
* `JIRA_POLLING_INTERVAL_MS`

Manual sync endpoint:

`POST /api/integrations/jira/sync`

Optional query param:

`jql` (if omitted, service uses `JIRA_DEFAULT_JQL` or builds JQL from `JIRA_PROJECT_KEY`)

# Database Model
The database model for the Project Tracker application consists of the following entities:

![img.png](docs/static/db_model.png)