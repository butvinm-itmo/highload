# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Guidelines

Strictly follow this guidelines

**CORE RULES:**
1. **NO AD-HOC PATCHES:** Do not disable tests (`@Disabled`) or use reflection hacks. Fix the root cause.
2. **NOT OVERSEE SUSPICIOUS BEHAVIOR:** If e2e tests fail randomly, investigate flakiness rather than retrying blindly.
3. **CONTRACTS FIRST:** If changing `shared-dto` or `shared-clients`, verify impact on ALL consumer services.
4. **TEST-DRIVEN:** Run related tests *before* and *after* changes.
5. **ONLY ESSENTIAL COMMENTS:** Code must be self-explanatory, avoid using docstrings and especially inline comments.

**WORKFLOW:**
1. **EXPLORE:** Do not guess. Read relevant controllers, services, and DTOs first.
2. **PLAN:** Propose changes in steps, stating which services will be affected, which tests must be updated or added.
3. **IMPLEMENT:** Make atomic, compilable changes.
4. **VERIFY:** Run specific tests (e.g., `./gradlew :divination-service:test`) immediately.
5. **COMMIT:** After a complete step, run all tests including e2e and commit (specify files explicitly, avoid `git add .`).
6. **REPORT:** Update PROGRESS.md with current progress and CLAUDE.md with updated project context.

**BEHAVIOR EXAMPLES:**

* **[Handling JPA Lazy Loading]**
    * **BAD:** Accessing collection fields to trigger loading (`val _ = entity.items.size`).
    * **GOOD:** Using `@Query("SELECT e FROM Entity e JOIN FETCH e.items WHERE ...")`.

* **[Handling Test Failures]**
    * **BAD:** Adding `@Disabled("fix later")` or commenting out assertions.
    * **GOOD:** Analyzing TestContainer/WireMock logs and fixing the root cause.

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

| Service | Port | Stack | Responsibility |
|---------|------|-------|----------------|
| **config-server** | 8888 | Spring Cloud Config | Centralized configuration |
| **eureka-server** | 8761 | Netflix Eureka | Service discovery |
| **gateway-service** | 8080 | Spring Cloud Gateway | API Gateway, JWT validation |
| **user-service** | 8081 | Spring MVC + JPA | User management, authentication |
| **tarot-service** | 8082 | Spring MVC + JPA | Cards & layout types reference data |
| **divination-service** | 8083 | WebFlux + R2DBC | Spreads & interpretations (reactive) |
| **notification-service** | 8084 | WebFlux + R2DBC | In-app notifications (reactive) |

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

**spread** - (id UUID PK, question TEXT, layout_type_id UUID FK, author_id UUID FK CASCADE, created_at TIMESTAMPTZ)

**spread_card** - (id UUID PK, spread_id UUID FK CASCADE, card_id UUID FK, position_in_spread INTEGER, is_reversed BOOLEAN)
- Unique constraint: (spread_id, position_in_spread)

**interpretation** - (id UUID PK, text TEXT, author_id UUID FK CASCADE, spread_id UUID FK CASCADE, created_at TIMESTAMPTZ)
- Unique constraint: (author_id, spread_id)

### notification-service tables

**notification** - (id UUID PK, user_id UUID FK CASCADE, type VARCHAR(50), title VARCHAR(255), message TEXT, is_read BOOLEAN, reference_id UUID, reference_type VARCHAR(50), created_at TIMESTAMPTZ)

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
| GET | `/notifications?page=N&size=M` | List user notifications (max 50) | Any |
| GET | `/notifications/unread-count` | Get unread notification count | Any |
| PATCH | `/notifications/{id}/read` | Mark notification as read | Owner |
| POST | `/notifications/mark-all-read` | Mark all notifications as read | Any |

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
- `KAFKA_BOOTSTRAP_SERVERS` - Kafka brokers (required for divination-service, notification-service)

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
