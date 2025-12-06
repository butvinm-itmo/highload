# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Tarology Web Service** - A Kotlin/Spring Boot microservices application for Tarot card readings and interpretations. Users can create spreads, view others' spreads, and add interpretations without authentication (user ID-based identification).

**Architecture:** Microservices with Feign Clients for inter-service communication.

Key features:
- Create tarot spreads with different layouts (one card, three cards, cross/five cards)
- View all spreads in chronological feed
- Add/edit/delete interpretations for spreads
- User management with transactional deletion

## Microservices Architecture

The application is split into 4 microservices + shared DTO module:

| Service | Port | Responsibility |
|---------|------|----------------|
| **config-server** | 8888 | Centralized configuration management (Spring Cloud Config) |
| **user-service** | 8081 | User management |
| **tarot-service** | 8082 | Cards & LayoutTypes reference data |
| **divination-service** | 8083 | Spreads & Interpretations (uses Feign clients) |
| **shared-dto** | - | Shared DTOs between services |
| **e2e-tests** | - | End-to-end tests using Feign clients |

**Inter-service Communication:**
- `divination-service` → `user-service` via `UserClient` (Feign)
- `divination-service` → `tarot-service` via `TarotClient` (Feign)

**Database:** All services share a single PostgreSQL database with separate Flyway migration history tables.

## Configuration Management

The application uses Spring Cloud Config Server for centralized configuration management.

### Config Server (port 8888)

Centralized configuration service using Git backend from remote repository:
- **Repository:** https://github.com/butvinm-itmo/highload-config.git
- **Branch:** `main`
- **Configuration files:**
  - `application.yml` - Shared configuration (database, JPA, Flyway, SpringDoc)
  - `user-service.yml` - User service specific (port, Flyway table)
  - `tarot-service.yml` - Tarot service specific (port, Flyway table)
  - `divination-service.yml` - Divination service specific (port, Feign clients, Flyway table)

### Service Configuration

Services fetch configuration from Config Server on startup:
```yaml
spring:
  config:
    import: optional:configserver:${CONFIG_SERVER_URL:http://localhost:8888}
```

**Environment Variables:**
- `CONFIG_SERVER_URL` - Config Server URL (default: http://localhost:8888)
- Database env vars (DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD) remain unchanged
- Service URLs (USER_SERVICE_URL, TAROT_SERVICE_URL) for divination-service

**Local Development:**
```bash
# Start config server
./gradlew :config-server:bootRun

# Verify config retrieval
curl http://localhost:8888/actuator/health
curl http://localhost:8888/user-service/default
```

**Testing:**
Integration tests disable Config Server via `spring.config.import: none` in `application-test.yml`.
Each service has `@ActiveProfiles("test")` annotation on `BaseIntegrationTest`.

## Build & Development Commands

### Build and Run
```bash
# Build all services
./gradlew build

# Build specific service
./gradlew :config-server:build
./gradlew :user-service:build
./gradlew :tarot-service:build
./gradlew :divination-service:build

# Run tests for all services
./gradlew test

# Run tests for specific service
./gradlew :user-service:test
./gradlew :tarot-service:test
./gradlew :divination-service:test

# Clean build artifacts
./gradlew clean
```

### Config Server Commands
```bash
# Build config server
./gradlew :config-server:build

# Run config server locally
./gradlew :config-server:bootRun

# Test config retrieval
curl http://localhost:8888/actuator/health
curl http://localhost:8888/user-service/default
curl http://localhost:8888/application/default
```

### Configuration Repository Management
```bash
# Clone the config repository
git clone https://github.com/butvinm-itmo/highload-config.git
cd highload-config

# Update configurations
# Edit .yml files as needed
git add .
git commit -m "Update configuration"
git push origin main

# Config Server automatically pulls changes on restart
docker compose restart config-server
```

### Code Quality
```bash
# Run ktlint checks
./gradlew ktlintCheck

# Auto-format code with ktlint
./gradlew ktlintFormat

# Run ktlint for specific service
./gradlew :user-service:ktlintCheck
./gradlew :tarot-service:ktlintFormat
```

### Docker Commands
```bash
# Start all microservices (includes config-server)
docker compose up -d

# Rebuild and restart all services
docker compose up -d --build

# Start only config-server (for local development)
docker compose up -d config-server

# Start only the database (for local development)
docker compose up -d postgres

# Stop all services
docker compose down

# View logs for specific service
docker compose logs -f config-server
docker compose logs -f user-service
docker compose logs -f tarot-service
docker compose logs -f divination-service

# View all logs
docker compose logs -f

# Restart config-server to pull latest configs
docker compose restart config-server

# Rebuild specific service
docker compose up -d --build config-server
```

**Note:** `docker-compose.yml` does not include `container_name` fields to ensure compatibility with TestContainers (see https://github.com/testcontainers/testcontainers-java/issues/2472).

### E2E Testing
```bash
# TestContainers automatically starts and stops services
./gradlew :e2e-tests:test
```

### Database Setup
Each service has its own Flyway migrations with separate history tables:
- `flyway_schema_history_user` - user-service migrations
- `flyway_schema_history_tarot` - tarot-service migrations
- `flyway_schema_history_divination` - divination-service migrations

Environment variables:
- `DB_HOST` - Database host (default: localhost)
- `DB_PORT` - Database port (default: 5432)
- `DB_NAME` - Database name (default: tarot_db)
- `DB_USER` - Database username (default: tarot_user)
- `DB_PASSWORD` - Database password

## Technology Stack

- **Language:** Kotlin 2.2.10
- **Framework:** Spring Boot 3.5.6
- **Build Tool:** Gradle with Kotlin DSL (multi-project)
- **JVM:** Java 21
- **Database:** PostgreSQL 15
- **Migrations:** Flyway (per-service)
- **ORM:** Spring Data JPA with Hibernate
- **Inter-service:** Spring Cloud OpenFeign with Resilience4j circuit breaker
- **Testing:** TestContainers 1.19.8 for E2E tests
- **Code Style:** ktlint 1.5.0

## Project Structure

```
highload/
├── config-server/                 # Config Server (port 8888)
│   ├── Dockerfile
│   ├── build.gradle.kts
│   └── src/main/kotlin/.../configserver/
│       └── ConfigServerApplication.kt
│
├── config-files/                  # Local configuration files (to be pushed to remote repo)
│   ├── application.yml            # Shared configuration
│   ├── user-service.yml
│   ├── tarot-service.yml
│   ├── divination-service.yml
│   └── README.md
│
├── shared-dto/                    # Shared DTOs module
│   └── src/main/kotlin/.../shared/dto/
│       ├── UserDto.kt, CreateUserRequest.kt, ...
│       ├── CardDto.kt, LayoutTypeDto.kt, ArcanaTypeDto.kt
│       ├── SpreadDto.kt, SpreadSummaryDto.kt, ...
│       ├── InterpretationDto.kt, ...
│       ├── PageResponse.kt, ScrollResponse.kt
│       ├── ErrorResponse.kt, ValidationErrorResponse.kt
│       └── DeleteRequest.kt
│
├── user-service/                  # User management (port 8081)
│   └── src/main/kotlin/.../userservice/
│       ├── controller/UserController.kt, InternalUserController.kt
│       ├── service/UserService.kt
│       ├── repository/UserRepository.kt
│       ├── entity/User.kt
│       └── mapper/UserMapper.kt
│
├── tarot-service/                 # Reference data (port 8082)
│   └── src/main/kotlin/.../tarotservice/
│       ├── controller/CardController.kt, LayoutTypeController.kt, InternalTarotController.kt
│       ├── service/TarotService.kt
│       ├── repository/CardRepository.kt, LayoutTypeRepository.kt
│       ├── entity/Card.kt, LayoutType.kt, ArcanaType.kt
│       └── mapper/CardMapper.kt, LayoutTypeMapper.kt
│
├── divination-service/            # Spreads & Interpretations (port 8083)
│   └── src/main/kotlin/.../divinationservice/
│       ├── controller/SpreadController.kt, InterpretationController.kt
│       ├── service/DivinationService.kt
│       ├── repository/SpreadRepository.kt, SpreadCardRepository.kt, InterpretationRepository.kt
│       ├── entity/Spread.kt, SpreadCard.kt, Interpretation.kt
│       ├── mapper/SpreadMapper.kt, InterpretationMapper.kt
│       └── client/UserClient.kt, TarotClient.kt  # Feign clients
│
├── e2e-tests/                     # End-to-end tests module
│   └── src/test/kotlin/.../e2e/
│       ├── E2ETestApplication.kt
│       ├── BaseE2ETest.kt
│       ├── config/FeignConfig.kt, JacksonConfig.kt
│       ├── client/UserServiceClient.kt, TarotServiceClient.kt, DivinationServiceClient.kt
│       ├── UserServiceE2ETest.kt
│       ├── TarotServiceE2ETest.kt
│       ├── DivinationServiceE2ETest.kt
│       └── CleanupAuthorizationE2ETest.kt
│
├── docker-compose.yml
├── settings.gradle.kts            # Multi-project configuration
└── src/                           # Original monolith (deprecated)
```

## API Endpoints

### Response Format Convention

**IMPORTANT:** All paginated endpoints return arrays directly with metadata in HTTP headers, NOT wrapped PageResponse/ScrollResponse objects.

```bash
# Paginated endpoints return:
# Body: [...array of items...]
# Header: X-Total-Count: <total>

# Scroll endpoints return:
# Body: [...array of items...]
# Header: X-After: <cursor-uuid>  (only if more items exist)
```

The `PageResponse` and `ScrollResponse` DTOs are used **internally** between service/controller layers, not in API responses.

### Pagination Limits

All paginated endpoints enforce `@Max(50)` on the `size` parameter. Requesting `size > 50` returns 500 error.

### user-service (port 8081)

Base path: `/api/v0.0.1`

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/users` | Create user (409 if username exists) |
| GET | `/users?page=N&size=M` | List users (X-Total-Count header) |
| GET | `/users/{id}` | Get user by ID |
| PUT | `/users/{id}` | Update user |
| DELETE | `/users/{id}` | Delete user and all associated data |

**Internal endpoints** (for Feign clients):
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/internal/users/{id}/entity` | Get user DTO for internal use |

### tarot-service (port 8082)

Base path: `/api/v0.0.1`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/cards?page=N&size=M` | List cards (max 50/page, X-Total-Count header) |
| GET | `/layout-types?page=N&size=M` | List layout types |

**Note:** No `GET /cards/{id}` endpoint exists. Cards are reference data accessed via list or internal random endpoint.

**Internal endpoints** (for Feign clients):
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/internal/cards/random?count=N` | Get N random cards |
| GET | `/api/internal/layout-types/{id}` | Get layout type by ID |

### divination-service (port 8083)

Base path: `/api/v0.0.1`

**Spreads:**
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/spreads` | Create spread |
| GET | `/spreads?page=N&size=M` | List spreads (X-Total-Count header) |
| GET | `/spreads/scroll?after=ID&size=N` | Scroll spreads (X-After header) |
| GET | `/spreads/{id}` | Get spread with cards and interpretations |
| DELETE | `/spreads/{id}` | Delete spread (author only) |

**Interpretations:**
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/spreads/{spreadId}/interpretations` | List interpretations |
| GET | `/spreads/{spreadId}/interpretations/{id}` | Get interpretation |
| POST | `/spreads/{spreadId}/interpretations` | Add interpretation (409 if duplicate) |
| PUT | `/spreads/{spreadId}/interpretations/{id}` | Update interpretation (author only) |
| DELETE | `/spreads/{spreadId}/interpretations/{id}` | Delete interpretation (author only) |

## Important API Details

### DeleteRequest DTO

**IMPORTANT:** Delete endpoints use `userId` field, NOT `authorId`:

```kotlin
// DeleteRequest.kt
data class DeleteRequest(
    val userId: UUID,  // NOT authorId
)
```

This differs from create/update requests which use `authorId`.

### Request/Response DTOs

**Create requests use `authorId`:**
- `CreateSpreadRequest.authorId`
- `CreateInterpretationRequest.authorId`
- `UpdateInterpretationRequest.authorId`

**Delete requests use `userId`:**
- `DeleteRequest.userId`

### Entity ID Storage in divination-service

Entities in divination-service store foreign key IDs instead of entity references (since User and LayoutType are in other services):

```kotlin
// Spread.kt in divination-service
@Column(name = "layout_type_id")
val layoutTypeId: UUID  // NOT @ManyToOne LayoutType

@Column(name = "author_id")
val authorId: UUID  // NOT @ManyToOne User
```

Mappers fetch related data via Feign clients when building DTOs.

## Database Schema

Shared PostgreSQL database with UUID-based identifiers:

**user-service tables:**
- `user` - (id, username, created_at)

**tarot-service tables:**
- `arcana_type` - (id, name) - MAJOR, MINOR
- `layout_type` - (id, name, cards_count) - ONE_CARD, THREE_CARDS, CROSS
- `card` - (id, name, arcana_type_id)

**divination-service tables:**
- `spread` - (id, question, layout_type_id, author_id, created_at)
- `spread_card` - (id, spread_id, card_id, position_in_spread, is_reversed)
- `interpretation` - (id, text, spread_id, author_id, created_at)
  - Unique constraint: (author_id, spread_id)

## Testing

### Test Structure Per Service

Each service has its own test structure:

```
{service}/src/test/kotlin/.../
├── TestEntityFactory.kt
├── BaseIntegrationTest.kt
├── integration/
│   ├── controller/
│   │   └── *ControllerIntegrationTest.kt
│   └── service/
│       └── *ServiceIntegrationTest.kt
└── unit/
    └── service/
        └── *ServiceTest.kt
```

### Running Tests

```bash
# All tests
./gradlew test

# Specific service tests
./gradlew :user-service:test
./gradlew :tarot-service:test
./gradlew :divination-service:test
```

### divination-service Test Specifics

- Uses WireMock to mock Feign client responses
- `@DirtiesContext(classMode = AFTER_CLASS)` to avoid Spring context caching issues
- Test database setup includes prerequisite tables (user, card, layout_type) via `init-test-db.sql`

### E2E Tests

The `e2e-tests` module contains Kotlin-based end-to-end tests using Spring Cloud OpenFeign and TestContainers:

```
e2e-tests/src/test/kotlin/.../e2e/
├── E2ETestApplication.kt              # Spring Boot app for Feign clients
├── BaseE2ETest.kt                     # Base class with TestContainers setup
├── config/
│   ├── FeignConfig.kt                 # Feign error decoder config
│   └── JacksonConfig.kt               # Jackson ObjectMapper with JavaTimeModule
├── client/
│   ├── UserServiceClient.kt           # Feign client for user-service
│   ├── TarotServiceClient.kt          # Feign client for tarot-service
│   └── DivinationServiceClient.kt     # Feign client for divination-service
├── UserServiceE2ETest.kt              # User CRUD tests (8 tests)
├── TarotServiceE2ETest.kt             # Cards & layout types tests (6 tests)
├── DivinationServiceE2ETest.kt        # Spreads & interpretations tests (12 tests)
└── CleanupAuthorizationE2ETest.kt     # Delete & authorization tests (5 tests)
```

**TestContainers Integration:**
E2E tests use TestContainers `ComposeContainer` to automatically manage service lifecycle:
- Automatically starts all services (config-server, postgres, user-service, tarot-service, divination-service) from `docker-compose.yml`
- Waits for health checks on all services (5-minute startup timeout)
- Uses dynamic port mapping to avoid conflicts
- Automatically stops containers after tests complete
- **Note:** `docker-compose.yml` intentionally omits `container_name` fields for TestContainers compatibility

**Running E2E tests:**
```bash
# TestContainers automatically starts and stops services
./gradlew :e2e-tests:test
```

**Dependencies:**
- `org.testcontainers:testcontainers:1.19.8`
- `org.testcontainers:junit-jupiter:1.19.8`

**Test coverage (31 tests):**
- User CRUD, duplicate username (409), not found (404), internal endpoint
- Cards pagination (78 total cards), layout types, internal endpoints
- Spreads with inter-service Feign calls, interpretations CRUD
- Delete operations, authorization verification (403)

## Code Style Guidelines

ktlint enforces Kotlin code style:
- No wildcard imports
- Files must end with newline
- 4-space indentation
- Trailing commas in multi-line parameter lists
- No trailing whitespace

Run `./gradlew ktlintFormat` before committing.

## Key Implementation Notes

### Spring Cloud OpenFeign Compatibility

Spring Boot 3.5.6 requires disabling compatibility verifier:
```yaml
spring:
  cloud:
    compatibility-verifier:
      enabled: false
```

### Lazy Loading with Microservices

When passing entity data to mappers, pass counts as parameters to avoid `LazyInitializationException`:
```kotlin
fun toDto(spread: Spread, cardsCount: Int, interpretationsCount: Int): SpreadSummaryDto
```

### Flyway with Shared Database

Each service uses separate Flyway history table:
```yaml
spring:
  flyway:
    table: flyway_schema_history_user  # or _tarot, _divination
    baseline-on-migrate: true
    baseline-version: 0
```

### Resilience4j Circuit Breaker

divination-service uses Resilience4j for resilience in Feign client calls:

```yaml
# Configuration in divination-service.yml (external config repo)
resilience4j:
  circuitbreaker:
    configs:
      default:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
  retry:
    configs:
      default:
        maxAttempts: 3
        waitDuration: 500ms
  timelimiter:
    configs:
      default:
        timeoutDuration: 3s
```

**Error handling:**
- `FeignException.NotFound` → 404 (not counted as circuit breaker failure)
- `FeignException` (other) → 502 BAD_GATEWAY
- `CallNotPermittedException` (circuit open) → 503 SERVICE_UNAVAILABLE

### Git Workflow
- When using git add, specify files explicitly (avoid `git add .`)
