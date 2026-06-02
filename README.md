# Task Manager API

A personal task manager REST API built with Java 17, Spring Boot, Maven, and H2. Includes a single-page frontend and an AI-powered task breakdown endpoint.

## Prerequisites

- Java 17+
- Internet access (for the AI breakdown endpoint)

## Run

Set the three required environment variables and start the app.

**Windows (PowerShell):**
```powershell
$env:AI_API_KEY="your_key_here"
$env:AI_API_URL="your_api_url"
$env:AI_MODEL="your-model-name"
.\mvnw.cmd spring-boot:run
```

**Mac/Linux:**
```bash
AI_API_KEY=your_key_here \
AI_API_URL=your_api_url \
AI_MODEL=your-model-name \
./mvnw spring-boot:run
```

The values above use Groq with Llama. Swap `AI_API_URL` and `AI_MODEL` for any other OpenAI-compatible provider: Together AI, OpenRouter, Mistral, and others all work. **Gemini and Claude do not**, as they use a different API format.

The app starts on **http://localhost:8081**.

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

**Enums for Priority and Status.** The valid values are fixed at compile time. Using enums means invalid values (`"URGENT"`, `"low"`) are rejected at the Java type level before reaching service logic. JPA stores them as readable strings via `@Enumerated(EnumType.STRING)`, and Jackson serializes them by name with no extra configuration. They were neither a constants class nor plain string fields on `Task` because both still leave the entity holding a string, meaning any value can get through and the constants are trivially bypassed.

**Plain HTML + JS over Thymeleaf.** The assessment requires both a working REST API and a UI. Thymeleaf couples the two: controllers return HTML instead of JSON, so `GET /tasks` would give a reviewer an HTML page rather than a JSON array, breaking the API. With plain HTML and `fetch`, the API stays clean JSON and the frontend sits on top of it independently. Both work on their own terms.

**Lombok.** `@Data` eliminates boilerplate getters and setters. `@NoArgsConstructor` satisfies JPA's requirement for a zero-arg constructor. `@AllArgsConstructor` pairs with `@Builder` for clean object construction in service code and tests. `@RequiredArgsConstructor` handles constructor injection on `final` fields without writing constructors by hand.

**No DTO for CRUD.** The `Task` entity is flat and maps directly to what the API sends and receives. A separate request/response class would add a mapping layer with no meaningful benefit at this scale. The `BreakdownResponse` is a DTO because it is a derived, AI-generated shape that does not correspond to the entity.

**EntityNotFoundException over a task-specific exception.** The exception takes an entity name and id, making it reusable if other entities are added. The message reads `"Task not found with id: 5"`, same behavior with broader reuse. A `@RestControllerAdvice` global handler maps it to a `404` response, so no controller carries exception-handling logic.

**AiService decomposed into four methods.** `buildPrompt`, `callApi`, and `parseSubtasks` each have one responsibility. The `breakdown` method orchestrates them and assembles the final DTO. This makes each piece independently readable and testable rather than one opaque function.

**Provider-neutral AI integration.** The API key, URL, and model are all environment variables so nothing provider-specific is committed to source. Switching providers requires only changing the values passed at startup, with no code changes.
