# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Agent Guidelines

**ACT AS:** Senior Backend Engineer specializing in Kotlin, Spring Boot, and Microservices.

**CORE RULES:**
1. **NO AD-HOC PATCHES:** Do not disable tests (`@Disabled`) or use reflection hacks. Fix the root cause.
2. **NOT OVERSEE SUSPICIOUS BEHAVIOR:** If e2e tests fail randomly, investigate flakiness rather than retrying blindly.
3. **CONTRACTS FIRST:** If changing `shared-dto` or `shared-clients`, verify impact on ALL consumer services.
4. **TEST-DRIVEN:** Run related tests *before* and *after* changes.

**WORKFLOW:**
1. **EXPLORE:** Do not guess. Read relevant controllers, services, and DTOs first.
2. **PLAN:** Propose changes in steps, stating which services will be affected.
3. **IMPLEMENT:** Make atomic, compilable changes.
4. **VERIFY:** Run specific tests (e.g., `./gradlew :divination-service:test`) immediately.
5. **COMMIT:** After a complete step, run all tests and commit (specify files explicitly, avoid `git add .`).
6. **REPORT:** Update PROGRESS.md with current progress and CLAUDE.md with updated project context.

**BEHAVIOR EXAMPLES:**

* **[Handling Test Failures]**
    * **BAD:** Adding `@Disabled("fix later")` or commenting out assertions.
    * **GOOD:** Analyzing TestContainer/WireMock logs and fixing the root cause.

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
- **ORM:** Spring Data R2DBC (all services - fully reactive)
- **Event Streaming:** Apache Kafka 7.5 (3 brokers, KRaft mode)
- **Service Discovery:** Netflix Eureka
- **API Gateway:** Spring Cloud Gateway
- **Inter-service:** Spring Cloud OpenFeign
- **Resilience:** Resilience4j (circuit breaker, retry, time limiter)
- **Code Style:** ktlint 1.5.0

## Microservices Architecture

| Service | Port | Stack | Responsibility |
|---------|------|-------|----------------|
| **config-server** | 8888 | Spring Cloud Config | Centralized configuration |
| **eureka-server** | 8761 | Netflix Eureka | Service discovery |
| **gateway-service** | 8080 | Spring Cloud Gateway | API Gateway, JWT validation |
| **user-service** | 8081 | WebFlux + R2DBC | User management, authentication (reactive) |
| **tarot-service** | 8082 | WebFlux + R2DBC | Cards & layout types reference data (reactive) |
| **divination-service** | 8083 | WebFlux + R2DBC | Spreads & interpretations (reactive) |
| **notification-service** | 8084 | WebFlux + R2DBC + Kafka | Real-time notifications (reactive) |
| **kafka-1/2/3** | 9092-9094 | Apache Kafka (KRaft) | Event streaming cluster (3 brokers) |
| **kafka-ui** | 8090 | Provectus Kafka UI | Kafka cluster monitoring |

**Shared modules:** `shared-dto` (DTOs, event data classes), `shared-clients` (Feign clients), `e2e-tests`

**Inter-service Communication:**
- Services register with Eureka and discover each other dynamically
- `divination-service` calls other services via Feign clients (synchronous)
- `notification-service` consumes events from Kafka and pushes via WebSocket (asynchronous)
- `user-service` and `divination-service` publish domain events to Kafka (asynchronous)
- External clients access through `gateway-service`
- Currently all services share a single PostgreSQL database with separate Flyway history tables, but are designed for independent database deployment

**Cascade Delete (User Deletion):**
- When a user is deleted, user-service calls divination-service's internal API first
- Internal endpoint: `DELETE /internal/users/{userId}/data`
- Deletes all user's spreads and interpretations before user record is removed
- User deletion fails (503 Service Unavailable) if cleanup fails

**Configuration:** External Git repository (`highload-config/` submodule) served by config-server.

## Clean Architecture

All four backend services (user-service, tarot-service, divination-service, notification-service) follow Clean Architecture with four layers:

```
{service}/
├── domain/
│   └── model/              # Pure domain objects (no annotations)
│
├── application/
│   ├── service/            # Use cases / application services
│   └── interfaces/
│       ├── repository/     # Persistence interfaces
│       ├── provider/       # External service interfaces
│       └── publisher/      # Event publishing interfaces
│
├── infrastructure/
│   ├── persistence/
│   │   ├── entity/         # R2DBC @Table classes
│   │   ├── repository/     # Spring Data R2DBC interfaces (internal)
│   │   └── mapper/         # Entity ↔ Domain mappers
│   ├── messaging/
│   │   ├── mapper/         # Domain ↔ Event DTO mappers
│   │   └── Kafka*.kt       # Kafka publisher implementations
│   ├── external/           # Feign implementations of provider interfaces
│   └── security/           # Security implementations
│
├── api/
│   ├── controller/         # REST controllers
│   └── mapper/             # DTO ↔ Domain mappers
│
└── config/                 # Spring configurations
```

### Layer Dependencies

| Layer | Contains | Depends on |
|-------|----------|------------|
| Domain | Pure business entities | Nothing |
| Application | Use cases, interfaces | Domain only |
| Infrastructure | R2DBC, Feign, Security | Application, Domain |
| API | HTTP boundary (controllers) | Application, Domain |

### Three Model Types

Each service has three model types:

| Type | Location | Purpose | Annotations |
|------|----------|---------|-------------|
| Domain | `domain/model/` | Pure business objects | None |
| Entity | `infrastructure/persistence/entity/` | Database persistence | `@Table`, `@Id` |
| DTO | `shared-dto` module | API serialization | `@JsonProperty` |

### Naming Convention

No "Port", "Adapter", "Impl" suffixes. Technology prefix for implementations:

| Interface (application/) | Implementation (infrastructure/) |
|--------------------------|----------------------------------|
| `CardRepository` | `R2dbcCardRepository` |
| `UserProvider` | `FeignUserProvider` |
| `TokenProvider` | `JwtTokenProvider` |
| `CurrentUserProvider` | `SecurityContextCurrentUserProvider` |
| `UserEventPublisher` | `KafkaUserEventPublisher` |
| `SpreadEventPublisher` | `KafkaSpreadEventPublisher` |

### Data Flow

```
HTTP Request → DTO → Domain Model → Entity → Database
Database → Entity → Domain Model → DTO → HTTP Response
```

### Key Design Decisions

1. **Application interfaces return `Mono/Flux`** - Pragmatic choice for reactive application, avoids blocking wrapper overhead.

2. **Feign calls in infrastructure, not mappers** - Mappers are pure functions (Entity ↔ Domain, Domain ↔ DTO). External service calls happen in `FeignUserProvider`, `FeignCardProvider`, etc.

3. **Spring Data interfaces are internal** - `SpringDataCardRepository` (extends `R2dbcRepository`) is internal to infrastructure. Application layer uses `CardRepository` interface.

4. **Domain models have no framework annotations** - `domain/model/` classes are plain Kotlin data classes.

## Authentication & Authorization

### Authentication Flow
1. Client sends credentials to `POST /api/v0.0.1/auth/login`
2. user-service validates and generates JWT (24h expiration, HS256)
3. Client includes JWT in `Authorization: Bearer <token>` header
4. gateway-service validates JWT and adds `X-User-Id` + `X-User-Role` headers
5. Backend services trust gateway headers for authorization

### Authorization Model (3-Role System)

| Role | Spreads | Interpretations | Users |
|------|---------|-----------------|-------|
| **USER** | Create, read, delete own | Read only | Read only |
| **MEDIUM** | Create, read, delete own | Create, read, update/delete own | Read only |
| **ADMIN** | Full access | Full access | Full CRUD |

### Default Admin Credentials (Development Only)
```
Username: admin
Password: Admin@123
Role: ADMIN
ID: 10000000-0000-0000-0000-000000000001
```

### Password Requirements
Minimum 8 chars, uppercase, lowercase, digit, special character (@$!%*?&#).

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

**IMPORTANT:** divination-service has NO foreign keys to tables owned by other services (user, card, layout_type). This enables independent database deployment. Data integrity is enforced at the application level via Feign client validation.

**spread** - (id UUID PK, question TEXT, layout_type_id UUID, author_id UUID, created_at TIMESTAMPTZ)
- layout_type_id and author_id are stored as UUIDs without FK constraints

**spread_card** - (id UUID PK, spread_id UUID FK CASCADE, card_id UUID, position_in_spread INTEGER, is_reversed BOOLEAN)
- FK to spread only (internal); card_id stored without FK constraint
- Unique constraint: (spread_id, position_in_spread)

**interpretation** - (id UUID PK, text TEXT, author_id UUID, spread_id UUID FK CASCADE, created_at TIMESTAMPTZ)
- FK to spread only (internal); author_id stored without FK constraint
- Unique constraint: (author_id, spread_id)

### notification-service tables

**notification** - (id UUID PK, recipient_id UUID, interpretation_id UUID UNIQUE, interpretation_author_id UUID, spread_id UUID, title VARCHAR(256), message TEXT, is_read BOOLEAN, created_at TIMESTAMPTZ)
- UNIQUE on interpretation_id for deduplication (one notification per interpretation)
- Index on (recipient_id, is_read, created_at DESC)
- No foreign keys to other services (service independence)

## API Endpoints

Base path: `/api/v0.0.1`

**Response Convention:** Paginated endpoints return arrays with `X-Total-Count` header. Scroll endpoints use `X-After` header for cursor.

### user-service
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/auth/login` | Login, returns JWT | Public |
| POST | `/users` | Create user | ADMIN |
| GET | `/users?page=N&size=M` | List users (max 50) | Any |
| GET | `/users/{id}` | Get user | Any |
| PUT | `/users/{id}` | Update user | ADMIN |
| DELETE | `/users/{id}` | Delete user (cascades) | ADMIN |

### tarot-service
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/cards?page=N&size=M` | List cards (max 50) |
| GET | `/cards/random?count=N` | Get N random cards (1-78) |
| GET | `/layout-types?page=N&size=M` | List layout types |
| GET | `/layout-types/{id}` | Get layout type |

### divination-service
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/spreads` | Create spread | Any |
| GET | `/spreads?page=N&size=M` | List spreads | Any |
| GET | `/spreads/scroll?after=ID&size=N` | Scroll spreads | Any |
| GET | `/spreads/{id}` | Get spread with cards/interpretations | Any |
| DELETE | `/spreads/{id}` | Delete spread | Author/ADMIN |
| GET | `/spreads/{spreadId}/interpretations` | List interpretations | Any |
| POST | `/spreads/{spreadId}/interpretations` | Add interpretation | MEDIUM/ADMIN |
| PUT | `/spreads/{spreadId}/interpretations/{id}` | Update interpretation | Author/ADMIN |
| DELETE | `/spreads/{spreadId}/interpretations/{id}` | Delete interpretation | Author/ADMIN |

### notification-service
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/notifications?page=N&size=M&isRead=bool` | List notifications for current user | Any |
| GET | `/notifications/unread-count` | Get unread count for current user | Any |
| PUT | `/notifications/{id}/read` | Mark notification as read | Any |

### notification-service (WebSocket)
| Endpoint | Description | Auth |
|----------|-------------|------|
| `/ws/notifications?token=JWT` | Real-time notification stream | JWT via query param |

WebSocket connections receive real-time notifications when interpretations are added to user's spreads.

### divination-service (Internal API)
| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| DELETE | `/internal/users/{userId}/data` | Delete all user's spreads and interpretations | Service-to-service only |
| GET | `/internal/spreads/{spreadId}/owner` | Get spread author ID | Service-to-service only |

**Note:** Internal endpoints are not exposed through the gateway and are only accessible via Eureka service discovery.

## API Documentation (Swagger UI)

**Centralized Swagger UI** is available at the API Gateway:
- **URL:** `http://localhost:8080/swagger-ui.html`
- **Features:**
  - Dropdown selector to switch between services (User Service, Tarot Service, Divination Service, Notification Service)
  - "Try it out" functionality for testing endpoints
  - Full OpenAPI 3.1 specification for each service

**Architecture:**
- Gateway hosts the Swagger UI (`springdoc-openapi-starter-webflux-ui`)
- Backend services expose only `/api-docs` (no UI) using `springdoc-openapi-starter-*-api`
- Gateway proxies `/v3/api-docs/{service}` to backend `/api-docs` endpoints
- Swagger-related paths are excluded from JWT authentication

**Configuration:**
- Springdoc settings: `highload-config/gateway-service.yml` (springdoc section)
- Proxy routes: `highload-config/gateway-service.yml` (openapi-* routes)
- Public paths: `security.public-paths` includes `/swagger-ui`, `/v3/api-docs`, `/webjars/swagger-ui`

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

# E2E tests (requires running services)
docker compose up -d && ./gradlew :e2e-tests:test
```

**Environment Variables:**
- `CONFIG_SERVER_URL` - Config Server URL (default: http://localhost:8888)
- `EUREKA_URL` - Eureka Server URL (required)
- `JWT_SECRET` - JWT signing key (required for user-service, gateway-service)
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` - Database connection
- `KAFKA_BOOTSTRAP_SERVERS` - Kafka broker addresses (required for user-service, divination-service, notification-service)

## Testing

### Integration Tests
- Each service has unit and integration tests with TestContainers
- Tests disable Config Server and Eureka via `application-test.yml`
- divination-service uses WireMock to mock Feign client responses

### E2E Tests
- Located in `e2e-tests` module
- Require services to be running: `docker compose up -d`
- Route through gateway (configurable via `GATEWAY_URL` env var)
- Verify gateway health before execution

## Key Implementation Notes

### Reactive Programming (All Services)

All backend services (user-service, tarot-service, divination-service) use Spring WebFlux + R2DBC for fully reactive, non-blocking operation.

**Blocking Feign in Reactive Context:**
Feign clients are blocking. Wrap all Feign calls with `Mono.fromCallable().subscribeOn(Schedulers.boundedElastic())` to avoid blocking the reactive event loop.

**R2DBC Entities (infrastructure/persistence/entity/):**
- Use `@Table` annotation (not JPA `@Entity`)
- Store foreign key IDs directly (no `@ManyToOne`)
- ID is nullable for database generation
- Always use returned entity from `save()`
- Separate from domain models - use `EntityMapper` to convert

**Reactive Security (user-service):**
- Uses `@EnableWebFluxSecurity` and `@EnableReactiveMethodSecurity`
- Authentication filter implements `WebFilter` (not servlet filter)
- Security context via `ReactiveSecurityContextHolder`

**Testing:**
- Use `WebTestClient` for controller tests (not MockMvc)
- Use `StepVerifier` for service tests
- Reactive cleanup with `repository.deleteAll().block()` in `@BeforeEach`

### Service Discovery (Eureka)

Feign clients use pattern: `@FeignClient(name = "service-name", url = "${services.service-name.url:}")`
- Empty URL (default): Eureka discovery
- Explicit URL: For testing with WireMock

### Flyway with Shared Database

Each service uses separate history table: `flyway_schema_history_user`, `flyway_schema_history_tarot`, `flyway_schema_history_divination`, `flyway_schema_history_notification`.

### Configuration Repository (highload-config)

Config files are in the `highload-config/` submodule. After changes, push to submodule and restart config-server.

**Two remotes setup:**
- `origin` (HTTPS) - Used for git submodule so anyone can clone without SSH keys
- `ssh` - Used for pushing changes (owner uses SSH authentication)

**To push config changes:**
```bash
cd highload-config
git add <files>
git commit -m "message"
git push ssh <branch>
```

**To update submodule reference in main project:**
```bash
cd ..  # back to main project
git add highload-config
git commit -m "Update highload-config"
```

## Event-Driven Architecture

### Kafka Infrastructure

3-broker Kafka cluster running in KRaft mode (no Zookeeper):
- **Brokers:** kafka-1 (9092), kafka-2 (9093), kafka-3 (9094)
- **Internal port:** 29092 (used by services within Docker network)
- **Replication factor:** 3, min.insync.replicas: 2
- **Monitoring:** kafka-ui at http://localhost:8090

### Topics

| Topic | Publisher | Events |
|-------|-----------|--------|
| `users-events` | user-service | CREATED, UPDATED, DELETED |
| `spreads-events` | divination-service | CREATED, DELETED |
| `interpretations-events` | divination-service | CREATED, UPDATED, DELETED |

### Event Consumers

| Topic | Consumer | Action |
|-------|----------|--------|
| `interpretations-events` | notification-service | Creates notification for spread owner when interpretation CREATED (if not self-action) |

### Event Message Format

Events use Kafka headers for metadata and JSON body for payload:

**Structure:**
- **Key:** Entity ID (UUID string) - enables partitioning by entity
- **Headers:**
  - `eventType`: `CREATED` | `UPDATED` | `DELETED`
  - `timestamp`: ISO-8601 instant
- **Value:** Full entity state (JSON)

**Example (users-events):**
```
Key: "550e8400-e29b-41d4-a716-446655440000"
Headers: { eventType: "CREATED", timestamp: "2026-01-20T20:00:00Z" }
Value: {"id":"550e8400-...","username":"john_doe","role":"USER","createdAt":"2026-01-20T20:00:00Z"}
```

### Event DTOs

Located in `shared-dto` module (`com.github.butvinmitmo.shared.dto.events`):
- `EventType` - Enum: CREATED, UPDATED, DELETED
- `UserEventData` - id, username, role, createdAt
- `SpreadEventData` - id, question, layoutTypeId, authorId, createdAt
- `InterpretationEventData` - id, text, authorId, spreadId, createdAt

### Publishing Pattern

**Post-commit, at-least-once semantics:**
1. Application service saves entity to database
2. After successful save, publishes event to Kafka
3. If Kafka publish fails, database transaction already committed (event may be lost)

**Implementation:**
- Publisher interfaces in `application/interfaces/publisher/`
- Kafka implementations in `infrastructure/messaging/`
- Blocking Kafka calls wrapped with `Mono.fromCallable().subscribeOn(Schedulers.boundedElastic())`

**Testing:**
- Unit tests mock publisher interfaces
- Integration tests use `@MockBean` for publishers to avoid Kafka dependency

### Notification Flow

```
interpretations-events (CREATED)
    ↓
InterpretationEventConsumer
    ↓
SpreadProvider.getSpreadOwnerId(spreadId)
    ↓
If authorId != spreadOwnerId:
    ↓
NotificationService.create()
    ↓
Save to DB + WebSocketSessionManager.sendToUser()
```

**WebSocket Authentication:**
- Gateway validates JWT from query param (`?token=JWT`) for WebSocket connections
- X-User-Id header is set by gateway after JWT validation
- NotificationWebSocketHandler extracts user ID from header for session management
