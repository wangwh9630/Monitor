# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build (Maven wrapper required)
./mvnw clean package              # Windows: ./mvnw.cmd clean package

# Run (env vars required for Alibaba Cloud credentials)
export ALIYUN_ACCESS_KEY_ID=<key>
export ALIYUN_ACCESS_KEY_SECRET=<secret>
./mvnw spring-boot:run

# Run tests (no credentials needed, uses in-memory H2)
./mvnw test

# Run a single test class
./mvnw test -Dtest=AlertControllerTest
```

## Architecture

Spring Boot 3.4.4 / Java 21 application that receives Prometheus Alertmanager webhooks (`POST /alert`) and makes voice phone calls via Alibaba Cloud DYVMS API. Includes a Thymeleaf web UI for management.

### Package Layout

`com.example.phone` — root package containing:
- `PhoneApplication` — entry point
- `AppConfig` — beans: `IAcsClient`, `Semaphore` (rate limiter), `RestTemplate` (5s/30s timeouts)
- `AlertController` — core REST endpoint at package root (not in `controller/`), handles webhook auth, rate limiting, Alibaba Cloud API calls, and call record persistence
- `GlobalExceptionHandler` — `@ControllerAdvice` scoped to `com.example.phone.controller` only (does NOT cover `AlertController`)

`com.example.phone.controller` — web UI controllers (`@Controller` + Thymeleaf):
- `DashboardController` (`GET /`) — stats + paginated call records with search
- `PhoneController` (`/phones`) — CRUD, redirect-after-POST pattern
- `TemplateController` (`/templates`) — same CRUD pattern
- `TestController` (`/test`) — self-calls `http://localhost:8001/alert` via `RestTemplate`
- `HealthController` (`GET /health`) — JSON health check

`com.example.phone.entity` — JPA entities: `PhoneNumber`, `TtsTemplate`, `CallRecord`

`com.example.phone.service` — business logic layer

`com.example.phone.repository` — Spring Data JPA interfaces

### Key Design Decisions

- **AlertController is at the package root**, not in `controller/`. This means `GlobalExceptionHandler` (scoped to `controller` package) does not intercept its exceptions — `AlertController` handles all errors internally and returns `ResponseEntity` with proper HTTP status codes.
- **Semaphore-based rate limiting**: acquired per request with 5s timeout, released in `finally` block. Permits configured via `alert.rate-limit.permits-per-second`.
- **Batch-to-single fallback**: if `BatchCallByTts` returns `InvalidAction.NotFound`, degrades to sequential `SingleCallByTts` per number.
- **Dual TTS template resolution**: DB lookup first (`findFirstByEnabledTrue`), falls back to `aliyun.voice.template-code` property.
- **Phone validation**: server-side regex `^1[3-9]\d{9}$` in `PhoneNumberService.add()`.

### Alert Flow

1. Alertmanager → `POST /alert` (JSON with `alerts[]`)
2. Validate `X-API-Key` header (if configured)
3. Acquire semaphore permit
4. Extract `alertname`/`severity`/`description` from each alert's labels/annotations
5. Query enabled phone numbers + active TTS template from DB
6. Call Alibaba Cloud DYVMS API (batch or single, with fallback)
7. Save `CallRecord` per call (SUCCESS/FAILED + CallId)
8. Release semaphore in `finally`

### Template Engine

Thymeleaf with `layout.html` providing reusable fragments (`head`, `nav`, `alert`, `script`). All pages compose via `th:replace`. Bootstrap 5.3.3 via CDN.

## Configuration

`application.properties` key settings:

| Key | Default | Notes |
|-----|---------|-------|
| `server.port` | `8001` | |
| `aliyun.access-key-id` | `${ALIYUN_ACCESS_KEY_ID}` | Env var required |
| `aliyun.access-key-secret` | `${ALIYUN_ACCESS_KEY_SECRET}` | Env var required |
| `alert.webhook.api-key` | `${ALERT_WEBHOOK_API_KEY:}` | Empty = no auth |
| `spring.datasource.url` | `jdbc:h2:file:./data/monitor;AUTO_SERVER=TRUE` | File-based H2 |
| `spring.datasource.password` | `${H2_DB_PASSWORD:monitor@2024}` | Env var overridable |
| `spring.h2.console.enabled` | `true` | Set `false` in production |

Test config (`src/test/resources/application.properties`): in-memory H2, no password, `create-drop` schema, H2 console disabled. `PhoneApplicationTests` uses `@TestPropertySource` for dummy Alibaba Cloud credentials.

## Testing

Tests use JUnit 5 + Mockito + MockMvc. The `maven-surefire-plugin` has an explicit ByteBuddy agent argLine for Java 21 compatibility. `AlertControllerTest` mocks `IAcsClient` via `@TestConfiguration` and verifies HTTP status codes for valid/invalid/empty alert payloads.

## Deployment

Multi-stage Dockerfile: Maven builder → `eclipse-temurin:21-jre` runtime. Mount `/app/data` volume for H2 persistence across container restarts.
