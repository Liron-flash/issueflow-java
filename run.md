# IssueFlow - Run Instructions

This document explains how to set up, build, run, and test the IssueFlow Spring Boot application.

---

## Prerequisites

Install the following:

- Java 21
- Docker Desktop
- Git
- PowerShell

The project includes the Maven Wrapper, so Maven does not need to be installed globally.

---

## Default Seed Data

On application startup, the system creates default demo data if it does not already exist.

Default users:

| Username | Password | Role |
|---|---|---|
| jdoe | secret | DEVELOPER |
| admin2 | adminpass | ADMIN |
| test_dev | secret | DEVELOPER |

Default projects are also created automatically so the API can be tested immediately after starting the application on a fresh database.
## Project Directory

Run all commands from the project root:

```powershell
cd C:\Users\liron\Desktop\TDP2026HW\issueflow-java
```

---

## Java Version

The project should be built and run with Java 21.

If another Java version is the default on the machine, set Java 21 manually:

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

Verify:

```powershell
java -version
```

Expected: Java 21.

---

## Start PostgreSQL

The project includes a Docker Compose file:

```text
compose.yml
```

Start PostgreSQL:

```powershell
docker compose up -d
```

Verify that the database container is running:

```powershell
docker ps
```

Expected container:

```text
issueflow-java-db-1
```

Expected port:

```text
5432
```

---

## Database Configuration

The application connects to PostgreSQL using the configuration in:

```text
src/main/resources/application.yaml
```

Database settings:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/issueflow
    username: issueflow
    password: issueflow
```

The database schema is managed by Hibernate/JPA:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
```

---

## Build and Run Tests

Run:

```powershell
.\mvnw.cmd clean package
```

This command:

- Compiles the project
- Runs the test suite
- Builds the Spring Boot jar under `target/`

Expected result:

```text
BUILD SUCCESS
```

---

## Run the Application

Run:

```powershell
.\mvnw.cmd spring-boot:run
```

The application starts on:

```text
http://localhost:8080
```

---

## Authentication

Login returns an `accessToken`.

Developer login:

```powershell
$login = Invoke-RestMethod -Method Post http://localhost:8080/auth/login `
  -ContentType "application/json" `
  -Body '{"username":"jdoe","password":"secret"}'

$token = $login.accessToken
```

Admin login:

```powershell
$adminLogin = Invoke-RestMethod -Method Post http://localhost:8080/auth/login `
  -ContentType "application/json" `
  -Body '{"username":"admin2","password":"adminpass"}'

$adminToken = $adminLogin.accessToken
```

Use the token in protected requests:

```text
Authorization: Bearer <accessToken>
```

---

## Smoke Tests

### List projects

```powershell
curl.exe -i -X GET "http://localhost:8080/projects" `
  -H "Authorization: Bearer $token"
```

Expected:

```text
HTTP 200
```

---

### List tickets

```powershell
curl.exe -i -X GET "http://localhost:8080/tickets" `
  -H "Authorization: Bearer $token"
```

Expected:

```text
HTTP 200
```

---

### Audit logs

```powershell
curl.exe -i -X GET "http://localhost:8080/audit-logs" `
  -H "Authorization: Bearer $token"
```

Expected:

```text
HTTP 200
```

---

## Validation Smoke Test

Invalid ticket request without a title:

```powershell
$badTicketBody = @{
  description = "Bad validation test"
  status = "TODO"
  priority = "HIGH"
  type = "BUG"
  projectId = 1
} | ConvertTo-Json

try {
  Invoke-WebRequest -Method Post "http://localhost:8080/tickets" `
    -Headers @{ Authorization = "Bearer $token" } `
    -ContentType "application/json" `
    -Body $badTicketBody
} catch {
  $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
  $body = $reader.ReadToEnd()
  Write-Host "Status:" ([int]$_.Exception.Response.StatusCode)
  Write-Host $body
}
```

Expected:

```text
Status: 400
"message":"Validation failed"
"validationErrors":{"title":"Ticket title is required"}
```

Invalid project request without a name:

```powershell
$badProjectBody = @{
  description = "Bad project"
  ownerId = 3
} | ConvertTo-Json

try {
  Invoke-WebRequest -Method Post "http://localhost:8080/projects" `
    -Headers @{ Authorization = "Bearer $token" } `
    -ContentType "application/json" `
    -Body $badProjectBody
} catch {
  $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
  $body = $reader.ReadToEnd()
  Write-Host "Status:" ([int]$_.Exception.Response.StatusCode)
  Write-Host $body
}
```

Expected:

```text
Status: 400
"message":"Validation failed"
"validationErrors":{"name":"Project name is required"}
```

---

## Stop the Application

Stop Spring Boot with:

```text
Ctrl + C
```

Stop PostgreSQL:

```powershell
docker compose down
```

To also remove the database volume:

```powershell
docker compose down -v
```
