Plan granular step-by-step refactoring plan. 
After each step run all tests. 
Do not oversee any suspicious behavior like failing tests or docker build. Do not make ad-hoc decisions to fix unexpected problems. 
Ask me if you see anything strange. Commit after each finished step.

# Tarology Web Service

A Kotlin/Spring Boot microservices application for Tarot card readings and interpretations.

## Architecture

The application consists of 5 microservices with centralized configuration and service discovery:

| Service                | Port | Description                           | Swagger UI                                                                                 |
| ---------------------- | ---- | ------------------------------------- | ------------------------------------------------------------------------------------------ |
| **config-server**      | 8888 | Centralized configuration management  | -                                                                                          |
| **eureka-server**      | 8761 | Service discovery (Netflix Eureka)    | [http://localhost:8761](http://localhost:8761)                                             |
| **user-service**       | 8081 | User management                       | [http://localhost:8081/swagger-ui/index.html](http://localhost:8081/swagger-ui/index.html) |
| **tarot-service**      | 8082 | Cards & LayoutTypes catalog           | [http://localhost:8082/swagger-ui/index.html](http://localhost:8082/swagger-ui/index.html) |
| **divination-service** | 8083 | Spreads & Interpretations             | [http://localhost:8083/swagger-ui/index.html](http://localhost:8083/swagger-ui/index.html) |

**Service Discovery:** Services register with Eureka Server and discover each other dynamically. `divination-service` uses Spring Cloud OpenFeign with Eureka discovery to call `user-service` and `tarot-service`.

**Configuration Management:** All services fetch configuration from Config Server on startup from a Git repository (submodule: `highload-config/`).

**Resilience:** `divination-service` uses Resilience4j circuit breaker, retry, and time limiter for fault tolerance.

**Database:** All services share a single PostgreSQL database with separate Flyway migration history tables.

## Quick Start

```bash
# Initialize configuration submodule (first time only)
git submodule update --init

# Start all services
docker compose up -d

# Verify services are running
curl http://localhost:8888/actuator/health  # Config Server
curl http://localhost:8761/actuator/health  # Eureka Server
curl http://localhost:8081/actuator/health  # User Service
curl http://localhost:8082/actuator/health  # Tarot Service
curl http://localhost:8083/actuator/health  # Divination Service

# Check Eureka dashboard for registered services
open http://localhost:8761

# Run E2E tests (TestContainers automatically manages service lifecycle)
./gradlew :e2e-tests:test
```

## API Overview

### user-service (port 8081)

| Method | Endpoint                 | Description |
| ------ | ------------------------ | ----------- |
| POST   | `/api/v0.0.1/users`      | Create user |
| GET    | `/api/v0.0.1/users`      | List users  |
| GET    | `/api/v0.0.1/users/{id}` | Get user    |
| PUT    | `/api/v0.0.1/users/{id}` | Update user |
| DELETE | `/api/v0.0.1/users/{id}` | Delete user |

### tarot-service (port 8082)

| Method | Endpoint                   | Description                 |
| ------ | -------------------------- | --------------------------- |
| GET    | `/api/v0.0.1/cards`        | List tarot cards (78 cards) |
| GET    | `/api/v0.0.1/layout-types` | List spread layouts         |

### divination-service (port 8083)

| Method | Endpoint                                               | Description                      |
| ------ | ------------------------------------------------------ | -------------------------------- |
| POST   | `/api/v0.0.1/spreads`                                  | Create spread                    |
| GET    | `/api/v0.0.1/spreads?page=N&size=M`                    | List spreads (paginated)         |
| GET    | `/api/v0.0.1/spreads/scroll?after=ID&size=N`           | Scroll spreads (cursor-based)    |
| GET    | `/api/v0.0.1/spreads/{id}`                             | Get spread with cards & interps  |
| DELETE | `/api/v0.0.1/spreads/{id}`                             | Delete spread (author only)      |
| GET    | `/api/v0.0.1/spreads/{id}/interpretations`             | List interpretations for spread  |
| GET    | `/api/v0.0.1/spreads/{id}/interpretations/{interpId}`  | Get interpretation               |
| POST   | `/api/v0.0.1/spreads/{id}/interpretations`             | Add interpretation               |
| PUT    | `/api/v0.0.1/spreads/{id}/interpretations/{interpId}`  | Update interpretation (author)   |
| DELETE | `/api/v0.0.1/spreads/{id}/interpretations/{interpId}`  | Delete interpretation (author)   |

## Configuration Management

Configuration files are stored in the `highload-config/` Git submodule and served by Config Server:

```bash
# View current configuration
curl http://localhost:8888/user-service/default
curl http://localhost:8888/application/default

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

**Configuration files:**
- `application.yml` - Shared configuration (database, JPA, Flyway, SpringDoc)
- `eureka-server.yml` - Eureka server settings
- `user-service.yml` - User service specific
- `tarot-service.yml` - Tarot service specific
- `divination-service.yml` - Divination service + Resilience4j settings

## Development

### Prerequisites

- Java 21
- Docker & Docker Compose

### Build & Test

```bash
# Build all services
./gradlew build

# Run all tests
./gradlew test

# Run tests for specific service
./gradlew :user-service:test
./gradlew :tarot-service:test
./gradlew :divination-service:test

# Clean build artifacts
./gradlew clean
```

Coverage reports are located at:
- `user-service/build/reports/jacoco/test/html/index.html`
- `tarot-service/build/reports/jacoco/test/html/index.html`
- `divination-service/build/reports/jacoco/test/html/index.html`

### Code Quality

```bash
# Run ktlint checks
./gradlew ktlintCheck

# Auto-format code
./gradlew ktlintFormat
```

### Running with Docker

```bash
# Start all services (database + microservices)
docker compose up -d

# Rebuild and restart
docker compose up -d --build

# View logs
docker compose logs -f

# View logs for specific service
docker compose logs -f config-server
docker compose logs -f eureka-server
docker compose logs -f user-service
docker compose logs -f tarot-service
docker compose logs -f divination-service

# Stop all services
docker compose down
```

**Service startup order** (enforced by docker-compose health checks):
1. `config-server` - Must be healthy first
2. `eureka-server` - Fetches config, then starts
3. `postgres` - Database
4. `user-service`, `tarot-service` - Register with Eureka
5. `divination-service` - Discovers other services via Eureka

**Environment variables:**
- `CONFIG_SERVER_URL` - Config Server URL (default: http://localhost:8888)
- `EUREKA_URL` - Eureka Server URL (required, no default)
- Database env vars: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`

### Running Locally (Development)

```bash
# Start infrastructure services (config-server, eureka-server, postgres)
docker compose up -d config-server eureka-server postgres

# Wait for services to be healthy, then run individual services (in separate terminals)
./gradlew :user-service:bootRun
./gradlew :tarot-service:bootRun
./gradlew :divination-service:bootRun

# Or run config-server and eureka-server locally too
./gradlew :config-server:bootRun  # Terminal 1
./gradlew :eureka-server:bootRun  # Terminal 2
# ... then run other services
```

### E2E Testing

The `e2e-tests` module uses TestContainers to automatically manage service lifecycle:

```bash
# TestContainers automatically starts and stops all services
./gradlew :e2e-tests:test
```

**Test coverage (31 tests):**
- User CRUD, duplicate username (409), not found (404), internal endpoint
- Cards pagination (78 total cards), layout types, internal endpoints
- Spreads with inter-service Feign calls, interpretations CRUD
- Delete operations, authorization verification (403)

TestContainers automatically:
- Starts all services from `docker-compose.yml`
- Waits for health checks (5-minute startup timeout)
- Uses dynamic port mapping to avoid conflicts
- Stops containers after tests complete

## Project Structure

```
highload/
├── config-server/        # Config Server (port 8888)
├── eureka-server/        # Eureka Server (port 8761)
├── highload-config/      # Git submodule - configuration repository
├── shared-dto/           # Shared DTOs between services
├── user-service/         # User management (port 8081)
├── tarot-service/        # Cards & Layouts (port 8082)
├── divination-service/   # Spreads & Interpretations (port 8083)
├── e2e-tests/            # End-to-end tests with TestContainers
├── docker-compose.yml    # Docker orchestration
├── settings.gradle.kts   # Multi-project Gradle config
└── CLAUDE.md            # Project instructions for Claude Code
```

## Technology Stack

- **Language:** Kotlin 2.2.10
- **Framework:** Spring Boot 3.5.6
- **Build:** Gradle (Kotlin DSL)
- **JVM:** Java 21
- **Database:** PostgreSQL 15
- **Migrations:** Flyway (per-service)
- **Service Discovery:** Netflix Eureka (Spring Cloud Netflix)
- **Configuration:** Spring Cloud Config Server (Git backend)
- **Inter-service:** Spring Cloud OpenFeign with Eureka discovery
- **Resilience:** Resilience4j (circuit breaker, retry, time limiter)
- **Testing:** TestContainers 1.19.8 for E2E tests
- **API Docs:** SpringDoc OpenAPI (Swagger)
- **Code Style:** ktlint 1.5.0
