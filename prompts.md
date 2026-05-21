# AI Usage Documentation

Model used: GPT-5.5 Thinking

During the assignment, I used AI assistance as a development and debugging assistant.

A significant part of the AI usage was done in a methodical, section-by-section workflow. I asked the assistant to go through the assignment requirements one section at a time, verify each implemented part, identify missing pieces, and only then continue to the next requirement.

AI was used for:
- Understanding the requirements document.
- Planning the implementation order.
- Going through the assignment section by section in a structured and methodical way.
- Designing the Spring Boot project structure.
- Reviewing API behavior against the README contract.
- Debugging Spring Boot, Maven, PostgreSQL, JWT, and PowerShell issues.
- Improving input validation and error handling.
- Creating smoke-test commands.
- Adding validation-related tests.
- Preparing the final run instructions.

Representative prompts used during development:

```text
I am working on the AT&T TDP IssueFlow Java Spring Boot assignment.
Help me break down the requirements into implementation stages.
```

```text
Let's go through the assignment section by section in a methodical way.
For each section, help me verify what is already implemented, identify what is missing, fix it if needed, and only then move to the next section.
```

```text
Help me implement JWT authentication in Spring Boot.
The login endpoint should return accessToken and protected endpoints should require Authorization: Bearer <token>.
```

```text
Help me implement ticket management APIs with projectId, assigneeId, status, priority, type, dueDate, and soft delete support.
```

```text
Help me implement an audit log mechanism for state-changing actions such as create, update, delete, assign, import, escalation, and auto-assignment.
```

```text
Help me implement ticket dependencies and prevent circular dependencies.
```

```text
Help me implement attachment upload, download, list, and delete APIs for tickets.
```

```text
Help me implement @mention detection in comments and persist the mentioned users.
```

```text
Help me implement automatic escalation for overdue tickets using a scheduled service.
```

```text
Help me implement automatic ticket assignment by workload.
Unassigned tickets should be assigned to the developer with the lowest number of open tickets.
```

```text
The project has spring-boot-starter-validation but no validation annotations.
Help me add input validation and a GlobalExceptionHandler that returns structured JSON errors.
```

```text
Help me add safe validation tests that run during mvnw clean package without requiring a running PostgreSQL container.
```

```text
Help me prepare run.md and prompts.md for final submission.
The documentation should explain setup, database, build, run, authentication, smoke tests, and AI usage.
```
