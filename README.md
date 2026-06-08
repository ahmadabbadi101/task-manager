# Task Manager API

A personal task manager REST API built with Java 17, Spring Boot, Maven, and PostgreSQL. Includes a single-page frontend and an AI-powered task breakdown endpoint. Deployed on Railway.

**Live demo:** https://web-production-7552b0.up.railway.app/

## Prerequisites

- Java 17+
- PostgreSQL database
- Internet access (for the AI breakdown endpoint)

## Run locally

You need a running PostgreSQL instance and the following environment variables set before starting.

**Windows (PowerShell):**
```powershell
$env:DATABASE_URL="jdbc:postgresql://localhost:5432/taskdb"
$env:AI_API_KEY="your_key_here"
$env:AI_API_URL="https://api.groq.com/openai/v1/chat/completions"
$env:AI_MODEL="llama-3.3-70b-versatile"
.\mvnw.cmd spring-boot:run
```

**Mac/Linux:**
```bash
DATABASE_URL=jdbc:postgresql://localhost:5432/taskdb \
AI_API_KEY=your_key_here \
AI_API_URL=https://api.groq.com/openai/v1/chat/completions \
AI_MODEL=llama-3.3-70b-versatile \
./mvnw spring-boot:run
```

`AI_API_URL` and `AI_MODEL` work with any OpenAI-compatible provider: Groq, Together AI, OpenRouter, Mistral, and others. **Gemini and Claude do not**, as they use a different API format.

The app starts on **http://localhost:8081**.

## Database

Tasks are persisted in PostgreSQL. The schema is managed automatically by Hibernate (`ddl-auto=update`) — no manual migrations needed. In tests, H2 in-memory is used instead so no database setup is required to run the test suite.

## Tests

```bash
.\mvnw.cmd test    # Windows
./mvnw test        # Mac/Linux
```

14 tests: 6 unit tests (Mockito, no Spring context) and 8 integration tests (full Spring context, real H2, MockMvc). The AI call is mocked in tests so no API key is needed to run them.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/tasks` | Create a task |
| `GET` | `/tasks` | List all tasks |
| `GET` | `/tasks/{id}` | Get a task |
| `PUT` | `/tasks/{id}` | Update a task |
| `DELETE` | `/tasks/{id}` | Delete a task |
| `POST` | `/tasks/{id}/breakdown` | AI-powered subtask breakdown |

`title`, `priority`, and `status` are required. `priority` must be `LOW`, `MEDIUM`, or `HIGH`. `status` must be `TODO`, `IN_PROGRESS`, or `DONE`. Missing or invalid values return a `400` with a field-level error message.

## AI Endpoint: POST /tasks/{id}/breakdown

Given a task stored in the database, this endpoint asks the AI model to decompose it into a list of concrete, actionable subtasks. It builds a prompt from the task's title, description, priority, and due date, calls the configured model with forced JSON output via `response_format`, and returns the parsed result. Nothing is persisted: the subtasks exist only in the response.

No request body required. The `{id}` in the path identifies which task to break down.

**Example task (POST /tasks to create it first):**
```json
{
  "title": "Build checkout flow",
  "description": "Users need to be able to review their cart, enter payment details, and receive a confirmation",
  "priority": "HIGH",
  "status": "TODO",
  "dueDate": "2026-06-15"
}
```

**Example response (POST /tasks/1/breakdown):**
```json
{
  "taskId": 1,
  "title": "Build checkout flow",
  "subtasks": [
    "Design the checkout page wireframe",
    "Implement cart state management",
    "Integrate payment gateway",
    "Add order confirmation email",
    "Write end-to-end tests for the checkout flow"
  ]
}
```

## Design Decisions

**Enums for Priority and Status.** The valid values are fixed at compile time. Using enums means invalid values (`"URGENT"`, `"low"`) are rejected at the Java type level before reaching service logic. JPA stores them as readable strings via `@Enumerated(EnumType.STRING)`, and Jackson serializes them by name with no extra configuration.

**Plain HTML + JS over Thymeleaf.** Thymeleaf couples the UI to the controllers — `GET /tasks` would return HTML instead of JSON, breaking the REST API. With plain HTML and `fetch`, the API stays clean JSON and the frontend sits on top of it independently.

**Lombok.** `@Data` eliminates boilerplate getters and setters. `@NoArgsConstructor` satisfies JPA's requirement for a zero-arg constructor. `@AllArgsConstructor` pairs with `@Builder` for clean object construction in service code and tests. `@RequiredArgsConstructor` handles constructor injection on `final` fields without writing constructors by hand.

**No DTO for CRUD.** The `Task` entity is flat and maps directly to what the API sends and receives. A separate request/response class would add a mapping layer with no meaningful benefit at this scale. The `BreakdownResponse` is a DTO because it is a derived, AI-generated shape that does not correspond to the entity.

**EntityNotFoundException over a task-specific exception.** The exception takes an entity name and id, making it reusable if other entities are added. A `@RestControllerAdvice` global handler maps it to a `404` response, so no controller carries exception-handling logic.

**AiService decomposed into four methods.** `buildPrompt`, `callApi`, and `parseSubtasks` each have one responsibility. The `breakdown` method orchestrates them and assembles the final DTO.

**Provider-neutral AI integration.** The API key, URL, and model are all environment variables so nothing provider-specific is committed to source. Switching providers requires only changing the values passed at startup, with no code changes.

**PostgreSQL for persistence.** Tasks survive server restarts and redeployments. H2 is kept as a test-scoped dependency so the test suite runs without a database setup.
