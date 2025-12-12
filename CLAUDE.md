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

The application is split into 6 microservices + shared modules:

| Service | Port | Responsibility |
|---------|------|----------------|
| **config-server** | 8888 | Centralized configuration management (Spring Cloud Config) |
| **eureka-server** | 8761 | Service discovery (Netflix Eureka) |
| **gateway-service** | 8080 | API Gateway (routing, resilience, monitoring) |
| **user-service** | 8081 | User management (Spring MVC + JPA) |
| **tarot-service** | 8082 | Cards & LayoutTypes reference data (Spring MVC + JPA) |
| **divination-service** | 8083 | Spreads & Interpretations (Spring WebFlux + R2DBC, reactive) |
| **shared-dto** | - | Shared DTOs between services |
| **shared-clients** | - | Shared Feign clients (UserServiceClient, TarotServiceClient, DivinationServiceClient) |
| **e2e-tests** | - | End-to-end tests using shared-clients |

**Inter-service Communication:**
- Services register with Eureka and discover each other dynamically
- `divination-service` uses shared-clients module for inter-service calls
- Feign clients support both Eureka discovery (production) and direct URLs (testing)
- External clients access backend services through `gateway-service`

**Database:** All services share a single PostgreSQL database with separate Flyway migration history tables.

## API Gateway (Spring Cloud Gateway)

The **gateway-service** provides a unified entry point for all external API requests, offering centralized routing, resilience features, and monitoring.

**Architecture:**
- **External Clients** → Gateway (port 8080) → Backend Services via Eureka discovery
- **Internal Services** → Direct Feign calls via Eureka (bypasses gateway for efficiency)

**Key Features:**
- **Intelligent Routing**: Routes requests to backend services using Eureka service discovery
- **Circuit Breaker**: Resilience4j circuit breaker per route for fault tolerance
- **Request Monitoring**: Centralized logging and metrics collection
- **Preserved API Versioning**: Maintains `/api/v0.0.1` versioning across all services

**Route Configuration:**
| Route Pattern | Target Service | Circuit Breaker |
|---------------|----------------|-----------------|
| `/api/v0.0.1/users/**` | user-service | ✓ |
| `/api/v0.0.1/cards/**` | tarot-service | ✓ |
| `/api/v0.0.1/layout-types/**` | tarot-service | ✓ |
| `/api/v0.0.1/spreads/**` | divination-service | ✓ |

**Access Patterns:**
- **External Clients**: Use gateway at `http://localhost:8080`
- **Internal Feign Clients**: Direct service URLs via Eureka (e.g., `lb://user-service`)
- **E2E Tests**: Route through gateway to simulate external access

**Configuration Location:**
- Routes and resilience policies: `highload-config/gateway-service.yml`
- Circuit breaker settings match divination-service patterns
- Actuator endpoints: `/actuator/health`, `/actuator/circuitbreakers`

**Resilience Configuration:**
```yaml
resilience4j:
  circuitbreaker:
    slidingWindowSize: 10
    failureRateThreshold: 50%
    waitDurationInOpenState: 10s
  timelimiter:
    timeoutDuration: 3s
```

## Configuration Management

The application uses Spring Cloud Config Server for centralized configuration management.

### Config Server (port 8888)

Centralized configuration service using Git backend from remote repository:
- **Repository:** https://github.com/butvinm-itmo/highload-config.git (submodule: `highload-config/`)
- **Branch:** `main`
- **Configuration files:**
  - `application.yml` - Shared configuration (database, JPA, Flyway, SpringDoc)
  - `eureka-server.yml` - Eureka server config (port, self-preservation settings)
  - `user-service.yml` - User service specific (port, Flyway table, Eureka client)
  - `tarot-service.yml` - Tarot service specific (port, Flyway table, Eureka client)
  - `divination-service.yml` - Divination service specific (port, Eureka client, Resilience4j, Feign)

### Service Configuration

Services fetch configuration from Config Server on startup:
```yaml
spring:
  config:
    import: optional:configserver:${CONFIG_SERVER_URL:http://localhost:8888}
```

**Environment Variables:**
- `CONFIG_SERVER_URL` - Config Server URL (default: http://localhost:8888)
- `EUREKA_URL` - Eureka Server URL (required, no default - services fail if not set)
- `EUREKA_HOSTNAME` - Eureka server hostname (for eureka-server only)
- Database env vars (DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD) remain unchanged

**Local Development:**
```bash
# Start config server
./gradlew :config-server:bootRun

# Start eureka server
./gradlew :eureka-server:bootRun

# Verify config retrieval
curl http://localhost:8888/actuator/health
curl http://localhost:8888/user-service/default

# Check Eureka dashboard
open http://localhost:8761
```

**Testing:**
- Integration tests disable Config Server via `spring.cloud.config.enabled: false` in `application-test.yml`
- Integration tests disable Eureka via `eureka.client.enabled: false`
- Each service has `@ActiveProfiles("test")` annotation on `BaseIntegrationTest`

## Build & Development Commands

### Build and Run
```bash
# Build all services
./gradlew build

# Build specific service
./gradlew :config-server:build
./gradlew :eureka-server:build
./gradlew :gateway-service:build
./gradlew :user-service:build
./gradlew :tarot-service:build
./gradlew :divination-service:build

# Run tests for all services
./gradlew test

# Run tests for specific service
./gradlew :gateway-service:test
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

### Eureka Server Commands
```bash
# Build eureka server
./gradlew :eureka-server:build

# Run eureka server locally
./gradlew :eureka-server:bootRun

# Check Eureka health and registered services
curl http://localhost:8761/actuator/health
curl http://localhost:8761/eureka/apps

# Eureka dashboard
open http://localhost:8761
```

### Gateway Service Commands
```bash
# Build gateway service
./gradlew :gateway-service:build

# Run gateway service locally (requires config-server and eureka-server)
./gradlew :gateway-service:bootRun

# Check gateway health
curl http://localhost:8080/actuator/health

# View circuit breaker status
curl http://localhost:8080/actuator/circuitbreakers | jq

# Test routes through gateway
curl http://localhost:8080/api/v0.0.1/users
curl http://localhost:8080/api/v0.0.1/cards
curl http://localhost:8080/api/v0.0.1/spreads
```

### Configuration Repository Management

Config files are in the `highload-config/` submodule:

```bash
# Initialize submodule (after clone)
git submodule update --init

# Update configurations
cd highload-config
# Edit .yml files as needed
git add .
git commit -m "Update configuration"
git push ssh main  # Push via SSH remote

# Update submodule reference in main repo
cd ..
git add highload-config
git commit -m "Update config submodule"

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
# Start all microservices (includes config-server, eureka-server)
docker compose up -d

# Rebuild and restart all services
docker compose up -d --build

# Start only infrastructure (for local development)
docker compose up -d config-server eureka-server gateway-service postgres

# Stop all services
docker compose down

# View logs for specific service
docker compose logs -f config-server
docker compose logs -f eureka-server
docker compose logs -f gateway-service
docker compose logs -f user-service
docker compose logs -f tarot-service
docker compose logs -f divination-service

# View all logs
docker compose logs -f

# Restart config-server to pull latest configs
docker compose restart config-server

# Rebuild specific service
docker compose up -d --build eureka-server

# Check gateway health and circuit breakers
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/circuitbreakers | jq
```

**Note:** `docker-compose.yml` does not include `container_name` fields to ensure compatibility with TestContainers (see https://github.com/testcontainers/testcontainers-java/issues/2472).

### E2E Testing
```bash
# TestContainers automatically starts and stops services
./gradlew :e2e-tests:test

# IMPORTANT: If tests fail with startup timeouts, pre-build Docker images first
docker compose build
./gradlew :e2e-tests:test
```

**Note:** TestContainers may time out during the first run if Docker images aren't pre-built. Building images beforehand (via `docker compose build`) ensures faster startup and prevents timeout failures during test execution.

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

## Shared Feign Clients

The `shared-clients` module provides unified Feign client interfaces for inter-service communication, used by both `divination-service` and `e2e-tests`.

**Available Clients:**
- **UserServiceClient** - User CRUD operations
  - create, list, get, update, delete users
- **TarotServiceClient** - Cards and layout types
  - list cards, list layout types, get random cards, get layout type by ID
- **DivinationServiceClient** - Spreads and interpretations
  - Full CRUD for spreads and interpretations
  - Scroll pagination and nested interpretation endpoints

**Configuration Classes:**
- **SharedFeignConfig** - Feign error decoder with `@ConditionalOnMissingBean`
- **SharedJacksonConfig** - ObjectMapper with Kotlin + JavaTimeModule

**Usage Pattern:**
1. Add dependency in `build.gradle.kts`:
   ```kotlin
   implementation(project(":shared-clients"))
   ```

2. Enable Feign clients in Spring Boot application:
   ```kotlin
   @EnableFeignClients(basePackages = ["com.github.butvinmitmo.shared.client"])
   ```

3. Configure URL properties (optional, for testing):
   ```yaml
   services:
     user-service:
       url: http://localhost:8081  # Empty for Eureka discovery
   ```

**URL Configuration:**
- Empty URL (default): Uses Eureka service discovery in production
- Explicit URL: For testing with WireMock or TestContainers
- Pattern: `@FeignClient(name = "service-name", url = "\${services.service-name.url:}")`

**Dependency Exposure:**
- `api` dependencies for transitive exposure: Spring Cloud OpenFeign, Jackson Kotlin/JavaTime
- Consumers automatically get Feign classes like `FeignException` and `@EnableFeignClients`

## Technology Stack

- **Language:** Kotlin 2.2.10
- **Framework:** Spring Boot 3.5.6
- **Build Tool:** Gradle with Kotlin DSL (multi-project)
- **JVM:** Java 21
- **Database:** PostgreSQL 15
- **Migrations:** Flyway (per-service)
- **ORM:**
  - Spring Data JPA with Hibernate (user-service, tarot-service)
  - Spring Data R2DBC (divination-service - reactive/non-blocking)
- **Web Stack:**
  - Spring MVC (user-service, tarot-service, gateway-service)
  - Spring WebFlux with Netty (divination-service - reactive)
- **Service Discovery:** Netflix Eureka (Spring Cloud Netflix)
- **API Gateway:** Spring Cloud Gateway with circuit breaker
- **Inter-service:** Spring Cloud OpenFeign with Eureka discovery
- **Resilience:** Resilience4j circuit breaker, retry, time limiter
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
├── eureka-server/                 # Eureka Server (port 8761)
│   ├── Dockerfile
│   ├── build.gradle.kts
│   └── src/main/kotlin/.../eurekaserver/
│       └── EurekaServerApplication.kt
│
├── gateway-service/               # API Gateway (port 8080)
│   ├── Dockerfile
│   ├── build.gradle.kts
│   └── src/main/kotlin/.../gatewayservice/
│       └── GatewayServiceApplication.kt
│
├── highload-config/               # Git submodule (config repository)
│   ├── application.yml            # Shared configuration
│   ├── eureka-server.yml          # Eureka server config
│   ├── gateway-service.yml        # Gateway routes and resilience config
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
├── shared-clients/                # Shared Feign clients module
│   └── src/main/kotlin/.../shared/
│       ├── client/
│       │   ├── UserServiceClient.kt
│       │   ├── TarotServiceClient.kt
│       │   └── DivinationServiceClient.kt
│       └── config/
│           ├── SharedFeignConfig.kt
│           └── SharedJacksonConfig.kt
│
├── user-service/                  # User management (port 8081)
│   └── src/main/kotlin/.../userservice/
│       ├── controller/UserController.kt
│       ├── service/UserService.kt
│       ├── repository/UserRepository.kt
│       ├── entity/User.kt
│       └── mapper/UserMapper.kt
│
├── tarot-service/                 # Reference data (port 8082)
│   └── src/main/kotlin/.../tarotservice/
│       ├── controller/CardController.kt, LayoutTypeController.kt
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
│       └── mapper/SpreadMapper.kt, InterpretationMapper.kt
│
├── e2e-tests/                     # End-to-end tests module
│   └── src/test/kotlin/.../e2e/
│       ├── E2ETestApplication.kt
│       ├── BaseE2ETest.kt
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

### tarot-service (port 8082)

Base path: `/api/v0.0.1`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/cards?page=N&size=M` | List cards (max 50/page, X-Total-Count header) |
| GET | `/cards/random?count=N` | Get N random cards (1-78) |
| GET | `/layout-types?page=N&size=M` | List layout types |
| GET | `/layout-types/{id}` | Get layout type by ID |

**Note:** No `GET /cards/{id}` endpoint exists. Cards are reference data accessed via list or random endpoint.

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
- Automatically starts all services (config-server, eureka-server, postgres, user-service, tarot-service, divination-service) from `docker-compose.yml`
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

**Test coverage (30 tests):**
- User CRUD, duplicate username (409), not found (404)
- Cards pagination (78 total cards), layout types, random cards, layout type by ID
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

### Service Discovery (Eureka)

Services register with Eureka Server and discover each other dynamically:

**Startup order (enforced by docker-compose health checks):**
1. `config-server` - Must be healthy first
2. `eureka-server` - Fetches config, then starts
3. `gateway-service` - Registers with Eureka for routing
4. `postgres` - Database
5. `user-service`, `tarot-service` - Register with Eureka
6. `divination-service` - Discovers other services via Eureka

**Feign clients use Eureka discovery:**
```kotlin
@FeignClient(name = "user-service", url = "\${services.user-service.url:}")
```
- When `services.user-service.url` is empty (production): uses Eureka discovery
- When set (tests): uses direct URL (for WireMock)

**No fallbacks:** If `EUREKA_URL` is not set, services fail to start. This ensures Eureka is always required in production.

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

### Reactive Programming (divination-service)

**divination-service** uses Spring WebFlux with R2DBC for fully reactive, non-blocking I/O.

#### R2DBC Entity Patterns

R2DBC entities differ from JPA entities:

```kotlin
@Table("spread")
data class Spread(
    @Id val id: UUID? = null,                    // Nullable for DB generation
    @Column("question") val question: String?,
    @Column("layout_type_id") val layoutTypeId: UUID,  // Store ID, not entity
    @Column("author_id") val authorId: UUID,
    @Column("created_at") val createdAt: Instant? = null,
)
```

**Key differences from JPA:**
- Use `@Table` instead of `@Entity`
- Use immutable `data class` with `val` fields
- ID is nullable (`UUID?`) for database generation
- No `@ManyToOne` or `@OneToMany` - store foreign key IDs directly
- No `@GeneratedValue` - database generates via `DEFAULT uuid_generate_v4()`
- Always use the returned entity from `save()`: `repository.save(entity).flatMap { savedEntity -> }`

#### Reactive Repositories

```kotlin
interface SpreadRepository : ReactiveCrudRepository<Spread, UUID> {
    @Query("SELECT * FROM spread ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    fun findAllOrderByCreatedAtDesc(offset: Long, limit: Int): Flux<Spread>

    fun findByAuthorId(authorId: UUID): Flux<Spread>
}
```

Use `Mono<T>` for single results, `Flux<T>` for multiple results.

#### Blocking Feign Clients in Reactive Streams

**Challenge:** Feign clients are blocking, but divination-service is reactive.

**Solution:** Wrap Feign calls in `Mono.fromCallable().subscribeOn(Schedulers.boundedElastic())`:

```kotlin
@Transactional
fun createSpread(request: CreateSpreadRequest): Mono<CreateSpreadResponse> {
    return Mono
        .fromCallable { userServiceClient.getUserById(request.authorId) }
        .subscribeOn(Schedulers.boundedElastic())  // Execute on bounded elastic thread pool
        .flatMap { user ->
            Mono.fromCallable { tarotServiceClient.getLayoutTypeById(request.layoutTypeId).body!! }
                .subscribeOn(Schedulers.boundedElastic())
        }
        .flatMap { layoutType ->
            // Reactive DB operations...
            spreadRepository.save(spread)
        }
}
```

**Why this works:**
- `Schedulers.boundedElastic()` - Dedicated thread pool for blocking operations
- Keeps shared-clients module unchanged (blocking Feign)
- Avoids blocking reactive event loop
- Maintains reactive backpressure

#### Reactive Controllers

```kotlin
@RestController
class SpreadController(private val service: DivinationService) {

    @PostMapping("/spreads")
    fun createSpread(@RequestBody request: CreateSpreadRequest): Mono<ResponseEntity<CreateSpreadResponse>> {
        return service.createSpread(request)
            .map { ResponseEntity.status(HttpStatus.CREATED).body(it) }
    }
}
```

Return `Mono<ResponseEntity<T>>` instead of `ResponseEntity<T>`.

#### Testing Reactive Code

**Integration Tests:**
- Use `WebTestClient` instead of `MockMvc`
- Provide `HttpMessageConverters` bean manually for Feign clients:

```kotlin
@TestConfiguration
class TestFeignConfiguration {
    @Bean
    fun httpMessageConverters(): HttpMessageConverters {
        return HttpMessageConverters(MappingJackson2HttpMessageConverter())
    }
}
```

**Unit Tests:**
- Mock repositories return `Mono.just()` / `Flux.just()`
- Use `.block()` to await results in tests
- Use `StepVerifier` for testing reactive streams

**Why HttpMessageConverters needed:**
- Feign clients need Spring MVC's `HttpMessageConverters` for serialization
- Production app is WebFlux-only (no Spring MVC)
- Tests manually provide this bean to avoid full Spring MVC auto-configuration

#### Database Configuration

divination-service uses **dual database configuration**:
- **Flyway (JDBC):** Runs schema migrations synchronously on startup
- **R2DBC:** All runtime database operations (reactive, non-blocking)

```yaml
spring:
  flyway:
    url: jdbc:postgresql://...
    user: tarot_user
    password: password
  r2dbc:
    url: r2dbc:postgresql://...
    username: tarot_user
    password: password
```

Both use the same database, just different drivers.

### Git Workflow
- When using git add, specify files explicitly (avoid `git add .`)
