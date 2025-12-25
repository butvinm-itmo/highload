# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Guidelines

Strictly follow this guidelines

**GLOBAL RULES (Apply to all work):**

1. **NO AD-HOC PATCHES:** Do not disable tests, use hacks, or rely on workarounds. Fix the root cause.
2. **ZERO TOLERANCE FOR FLAKINESS:** If tests fail randomly or unmodified code breaks, investigate the flakiness immediately. Do not retry blindly.
3. **CONTRACTS FIRST:** Verify impact on ALL other project components before modifying a module.
4. **GIT DISCIPLINE:** When you eventually execute the work, **never use `git add .`**. You must strictly stage only the relevant files for that atomic step.
5. **PROACTIVE CONSULTATION:** If you see "suspicious" logic or are uncertain, stop. Explain the risk and **recommend a decision**.

**WORKFLOW (Standard Operating Procedure):**

1. **EXPLORE:** Read relevant code. **Explicitly review** configuration files (Docker, CI/CD, env) alongside service code. Do not guess.
2. **PLAN:** Propose changes in atomic phases.
3. **IMPLEMENT:** Make atomic, compilable changes.
4. **VERIFY:** Run specific tests (e.g., `./gradlew :service:test`) immediately.
5. **REPORT:** Update `PROGRESS.md`, `CLAUDE.md`, and any related project documentation (e.g. README.md).
6. **COMMIT:** Run all tests and commit.

**BEHAVIOR EXAMPLES:**

- **[Handling JPA Lazy Loading]**
  - **BAD:** Accessing collection fields to trigger loading (`val _ = entity.items.size`).
  - **GOOD:** Using `@Query("SELECT e FROM Entity e JOIN FETCH e.items WHERE ...")`.

- **[Handling Test Failures]**
  - **BAD:** Adding `@Disabled("fix later")` or commenting out assertions.
  - **GOOD:** Analyzing TestContainer/WireMock logs and fixing the root cause.

**MOST CRITICAL:** Your latest action when work is finished should be verifying that your work follows guidelines and report to me.
Apply self-reflection to the session to find if you made bad decisions, workarounds or ignored suspicious behavior.

---

## Project Overview

**Tarology Web Service** - A Kotlin/Spring Boot microservices application for Tarot card readings and interpretations.

**Key features:**

- JWT-based authentication with 3-role model (USER, MEDIUM, ADMIN)
- Create tarot spreads with different layouts (one card, three cards, cross)
- View spreads in chronological feed
- Add/edit/delete interpretations (MEDIUM/ADMIN only for creation)
- User management with transactional deletion (ADMIN-only)

## Technology Stack

- **Language:** Kotlin 2.2.10, Java 21
- **Framework:** Spring Boot 3.5.6, Spring Cloud 2025.0.0
- **Build:** Gradle with Kotlin DSL (multi-project)
- **Database:** PostgreSQL 15, Flyway migrations
- **ORM:** Spring Data JPA (user/tarot services), Spring Data R2DBC (divination-service)
- **Service Discovery:** Netflix Eureka
- **API Gateway:** Spring Cloud Gateway
- **Inter-service:** Spring Cloud OpenFeign
- **Resilience:** Resilience4j (circuit breaker, retry, time limiter)
- **Messaging:** Apache Kafka 3.7 (KRaft mode, 3 replicas)
- **Code Style:** ktlint 1.5.0

## Microservices Architecture

| Service                  | Port | Stack                | Responsibility                       |
| ------------------------ | ---- | -------------------- | ------------------------------------ |
| **config-server**        | 8888 | Spring Cloud Config  | Centralized configuration            |
| **eureka-server**        | 8761 | Netflix Eureka       | Service discovery                    |
| **gateway-service**      | 8080 | Spring Cloud Gateway | API Gateway, JWT validation          |
| **user-service**         | 8081 | Spring MVC + JPA     | User management, authentication      |
| **tarot-service**        | 8082 | Spring MVC + JPA     | Cards & layout types reference data  |
| **divination-service**   | 8083 | WebFlux + R2DBC      | Spreads & interpretations (reactive) |
| **notification-service** | 8084 | WebFlux + R2DBC      | In-app notifications (reactive)      |
| **file-storage-service** | 8085 | Spring MVC + MinIO   | File storage for attachments         |

**Shared modules:** `shared-dto` (DTOs), `shared-clients` (Feign clients), `e2e-tests`

**Inter-service Communication:**

- Services register with Eureka and discover each other dynamically
- `divination-service` calls other services via Feign clients
- External clients access through `gateway-service`
- All services share a single PostgreSQL database with separate Flyway history tables

**Configuration:** External Git repository (`highload-config/` submodule) served by config-server.

## Authentication & Authorization

### Authentication Flow

1. Client sends credentials to `POST /api/v0.0.1/auth/login`
2. user-service validates and generates JWT (24h expiration, HS256)
3. Client includes JWT in `Authorization: Bearer <token>` header
4. gateway-service validates JWT and adds `X-User-Id` + `X-User-Role` headers
5. Backend services trust gateway headers for authorization

### Authorization Model (3-Role System)

| Role       | Spreads                  | Interpretations                 | Users     |
| ---------- | ------------------------ | ------------------------------- | --------- |
| **USER**   | Create, read, delete own | Read only                       | Read only |
| **MEDIUM** | Create, read, delete own | Create, read, update/delete own | Read only |
| **ADMIN**  | Full access              | Full access                     | Full CRUD |

### Default Admin Credentials (Development Only)

```
Username: admin
Password: Admin@123
Role: ADMIN
ID: 10000000-0000-0000-0000-000000000001
```

### Password Requirements

Minimum 8 chars, uppercase, lowercase, digit, special character (@$!%\*?&#).

## Database Schema

### user-service tables

**role** - (id UUID PK, name VARCHAR(50) UNIQUE)

- Seeded: USER, MEDIUM, ADMIN

**user** - (id UUID PK, username VARCHAR(128) UNIQUE, password_hash VARCHAR(255), role_id UUID FK, created_at TIMESTAMPTZ)

### tarot-service tables

**arcana_type** - (id UUID PK, name VARCHAR(16))

- Seeded: MAJOR, MINOR

**layout_type** - (id UUID PK, name VARCHAR(32), cards_count INTEGER)

- Seeded: ONE_CARD (1), THREE_CARDS (3), CROSS (5)

**card** - (id UUID PK, name VARCHAR(128), arcana_type_id UUID FK)

- Seeded: 78 cards (22 Major + 56 Minor Arcana)

### divination-service tables

**spread** - (id UUID PK, question TEXT, layout_type_id UUID FK, author_id UUID FK CASCADE, created_at TIMESTAMPTZ)

**spread_card** - (id UUID PK, spread_id UUID FK CASCADE, card_id UUID FK, position_in_spread INTEGER, is_reversed BOOLEAN)

- Unique constraint: (spread_id, position_in_spread)

**interpretation** - (id UUID PK, text TEXT, author_id UUID FK CASCADE, spread_id UUID FK CASCADE, file_key VARCHAR(512), created_at TIMESTAMPTZ)

- Unique constraint: (author_id, spread_id)
- file_key: Optional path to attached image in MinIO

### notification-service tables

**notification** - (id UUID PK, user_id UUID FK CASCADE, type VARCHAR(50), title VARCHAR(255), message TEXT, is_read BOOLEAN, reference_id UUID, reference_type VARCHAR(50), created_at TIMESTAMPTZ)

## API Endpoints

Base path: `/api/v0.0.1`

**Response Convention:** Paginated endpoints return arrays with `X-Total-Count` header. Scroll endpoints use `X-After` header for cursor.

### user-service

| Method | Endpoint               | Description            | Auth   |
| ------ | ---------------------- | ---------------------- | ------ |
| POST   | `/auth/login`          | Login, returns JWT     | Public |
| POST   | `/users`               | Create user            | ADMIN  |
| GET    | `/users?page=N&size=M` | List users (max 50)    | Any    |
| GET    | `/users/{id}`          | Get user               | Any    |
| PUT    | `/users/{id}`          | Update user            | ADMIN  |
| DELETE | `/users/{id}`          | Delete user (cascades) | ADMIN  |

### tarot-service

| Method | Endpoint                      | Description               |
| ------ | ----------------------------- | ------------------------- |
| GET    | `/cards?page=N&size=M`        | List cards (max 50)       |
| GET    | `/cards/random?count=N`       | Get N random cards (1-78) |
| GET    | `/layout-types?page=N&size=M` | List layout types         |
| GET    | `/layout-types/{id}`          | Get layout type           |

### divination-service

| Method | Endpoint                                   | Description                           | Auth         |
| ------ | ------------------------------------------ | ------------------------------------- | ------------ |
| POST   | `/spreads`                                 | Create spread                         | Any          |
| GET    | `/spreads?page=N&size=M`                   | List spreads                          | Any          |
| GET    | `/spreads/scroll?after=ID&size=N`          | Scroll spreads                        | Any          |
| GET    | `/spreads/{id}`                            | Get spread with cards/interpretations | Any          |
| DELETE | `/spreads/{id}`                            | Delete spread                         | Author/ADMIN |
| GET    | `/spreads/{spreadId}/interpretations`      | List interpretations                  | Any          |
| POST   | `/spreads/{spreadId}/interpretations`      | Add interpretation                    | MEDIUM/ADMIN |
| PUT    | `/spreads/{spreadId}/interpretations/{id}` | Update interpretation                 | Author/ADMIN |
| DELETE | `/spreads/{spreadId}/interpretations/{id}` | Delete interpretation                 | Author/ADMIN |
| POST   | `/spreads/{spreadId}/interpretations/{id}/file` | Upload file to interpretation (PNG/JPG, 2MB max) | Author/ADMIN |
| DELETE | `/spreads/{spreadId}/interpretations/{id}/file` | Delete file from interpretation       | Author/ADMIN |

### notification-service

| Method | Endpoint                       | Description                      | Auth  |
| ------ | ------------------------------ | -------------------------------- | ----- |
| GET    | `/notifications?page=N&size=M` | List user notifications (max 50) | Any   |
| GET    | `/notifications/unread-count`  | Get unread notification count    | Any   |
| PATCH  | `/notifications/{id}/read`     | Mark notification as read        | Owner |
| POST   | `/notifications/mark-all-read` | Mark all notifications as read   | Any   |
| WS     | `/notifications/ws`            | Real-time notification WebSocket | Any   |

### file-storage-service

| Method | Endpoint                | Description          | Auth |
| ------ | ----------------------- | -------------------- | ---- |
| POST   | `/files?key={key}`      | Upload file          | Any  |
| DELETE | `/files?key={key}`      | Delete file          | Any  |
| GET    | `/files/{key}`          | Download file        | Any  |

## Build & Development Commands

```bash
# Build & test
./gradlew build                           # Build all
./gradlew test                            # Test all
./gradlew :user-service:test              # Test specific service

# Code quality
./gradlew ktlintFormat                    # Auto-format code

# Docker
docker compose up -d                      # Start all services
docker compose up -d --build              # Rebuild and start
docker compose logs -f <service>          # View logs
docker compose down                       # Stop all

# E2E tests (automatically rebuilds containers and waits for health)
./gradlew :e2e-tests:test
```

**Environment Variables:**

- `CONFIG_SERVER_URL` - Config Server URL (default: http://localhost:8888)
- `EUREKA_URL` - Eureka Server URL (required)
- `JWT_SECRET` - JWT signing key (required for user-service, gateway-service)
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` - Database connection
- `KAFKA_BOOTSTRAP_SERVERS` - Kafka brokers (required for divination-service, notification-service)
- `MINIO_URL` - MinIO URL (required for file-storage-service)
- `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY` - MinIO credentials
- `MINIO_BUCKET` - MinIO bucket name (default: tarot-files)

## Testing

### Integration Tests

- Each service has unit and integration tests with TestContainers
- Tests disable Config Server and Eureka via `application-test.yml`
- divination-service uses WireMock to mock Feign client responses

### E2E Tests

- Located in `e2e-tests` module
- Automatically rebuild containers and wait for health checks before running
- Route through gateway (configurable via `GATEWAY_URL` env var)

## Key Implementation Notes

### Reactive Programming (divination-service)

**IMPORTANT:** The `tarot-service` uses blocking JPA - this is intentional, not a bug. The `divination-service` is reactive (WebFlux + R2DBC) while other services use traditional Spring MVC + JPA.

**Blocking Feign in Reactive Context:**
Feign clients are blocking. In divination-service, wrap calls with `Mono.fromCallable().subscribeOn(Schedulers.boundedElastic())` to avoid blocking the reactive event loop.

**R2DBC Entities:**

- Use `@Table` instead of `@Entity`
- Store foreign key IDs directly (no `@ManyToOne`)
- ID is nullable for database generation
- Always use returned entity from `save()`

### Service Discovery (Eureka)

Feign clients use pattern: `@FeignClient(name = "service-name", url = "${services.service-name.url:}")`

- Empty URL (default): Eureka discovery
- Explicit URL: For testing with WireMock

### Flyway with Shared Database

Each service uses separate history table: `flyway_schema_history_user`, `flyway_schema_history_tarot`, `flyway_schema_history_divination`, `flyway_schema_history_notification`.

### Configuration Repository

Config files are in the `highload-config/` submodule. After changes, push to submodule and restart config-server.

### Kafka Event-Driven Communication

**Infrastructure:** 3 Kafka brokers in KRaft mode (kafka-1, kafka-2, kafka-3), no Zookeeper.

**Topics:**

- `spread-events` - Published when a spread is created
- `interpretation-events` - Published when an interpretation is added

**Flow:**

1. `divination-service` publishes events after creating spreads/interpretations
2. `notification-service` consumes events and creates in-app notifications
3. Notifications are created when someone adds an interpretation to another user's spread

**Reactor-Kafka:** Both services use `reactor-kafka` for reactive Kafka integration.

### WebSocket Real-Time Notifications

**Endpoint:** `ws://gateway:8080/api/v0.0.1/notifications/ws`

**Authentication:** JWT token via `Authorization: Bearer <token>` header during WebSocket handshake. Gateway validates JWT and forwards `X-User-Id` header to notification-service.

**Message Format (JSON):**

```json
{
  "id": "uuid",
  "type": "NEW_INTERPRETATION",
  "title": "New interpretation on your spread",
  "message": "username added an interpretation...",
  "isRead": false,
  "createdAt": "2025-01-01T00:00:00Z",
  "referenceId": "uuid",
  "referenceType": "INTERPRETATION"
}
```

**Architecture:**

- `WebSocketSessionRegistry` - Maps user IDs to active sessions (supports multiple tabs/clients per user)
- `NotificationBroadcaster` - Sends notifications to all connected sessions for a user
- `EventConsumer` - On Kafka event, saves notification and broadcasts to connected WebSocket clients

### File Storage (file-storage-service)

**Infrastructure:** MinIO object storage (S3-compatible)

**File Attachments:**

- Interpretations can have one optional image attachment (PNG/JPG, max 2MB)
- Files stored in MinIO with key format: `interpretations/{interpretationId}/{filename}`
- `divination-service` validates file type and coordinates upload via Feign client
- `InterpretationDto` includes `fileUrl` for direct download when file is attached
- Deleting interpretation cascades to delete attached file

### Frontend Service (React SPA)

**Tech Stack:** React 19 + TypeScript + Vite + Tailwind CSS + React Query

**Architecture:**
- Separate service served by Nginx on port 3000 (Docker) or 5173 (dev)
- Communicates with backend via gateway-service on port 8080
- JWT token stored in localStorage, added via Axios interceptor
- Role-based UI rendering with hierarchical permissions

**Key Files:**
- `src/api/client.ts` - Axios instance with JWT interceptor and 401 handling
- `src/context/AuthContext.tsx` - Global auth state, login/logout, role checking
- `src/components/ProtectedRoute.tsx` - Route guard with role requirements
- `src/App.tsx` - Router configuration with ErrorBoundary wrapper

**Important Patterns:**
- Use React Query for all API calls (automatic caching, refetching, error handling)
- Infinite scroll uses `useInfiniteQuery` with cursor-based pagination (`X-After` header)
- All modals use controlled state with `isOpen` prop
- Empty states use EmptyState component with icon variants
- Loading states use Loading component (fullScreen or inline)

**CORS:** Gateway has CorsConfig allowing localhost:3000 and localhost:5173 origins.
