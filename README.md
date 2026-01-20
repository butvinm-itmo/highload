# Tarology Web Service

A Kotlin/Spring Boot microservices application for Tarot card readings and interpretations.

## Architecture

The application consists of 6 microservices with centralized configuration, service discovery, and API gateway:

| Service                | Port | Description                          |
| ---------------------- | ---- | ------------------------------------ |
| **config-server**      | 8888 | Centralized configuration management |
| **eureka-server**      | 8761 | Service discovery (Netflix Eureka)   |
| **gateway-service**    | 8080 | API Gateway (routing, resilience)    |
| **user-service**       | 8081 | User management & authentication     |
| **tarot-service**      | 8082 | Cards & LayoutTypes catalog          |
| **divination-service** | 8083 | Spreads & Interpretations            |

**API Documentation:** Centralized Swagger UI at [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

**API Gateway:** `gateway-service` provides a unified entry point for external clients. All API requests route through the gateway (port 8080) to backend services via Eureka discovery. The gateway includes circuit breaker protection and centralized monitoring.

**Service Discovery:** Services register with Eureka Server and discover each other dynamically. `divination-service` uses Spring Cloud OpenFeign with Eureka discovery to call `user-service` and `tarot-service`.

**Configuration Management:** All services fetch configuration from Config Server on startup from a Git repository (submodule: `highload-config/`).

**Resilience:** `gateway-service` and `divination-service` use Resilience4j circuit breaker, retry, and time limiter for fault tolerance.

**Database:** All services share a single PostgreSQL database with separate Flyway migration history tables.

## Quick Start

```bash
# Initialize configuration submodule (first time only)
git submodule update --init

# Start all services
docker compose up -d

# Check Eureka dashboard for registered services
open http://localhost:8761

# Access API documentation
open http://localhost:8080/swagger-ui.html

# Run E2E tests
./gradlew :e2e-tests:test
```

## API Overview

**Gateway Access:** All APIs can be accessed through the gateway at `http://localhost:8080` for external clients, or directly via service ports for internal/development use.

### user-service (port 8081)

| Method | Endpoint                 | Description        |
| ------ | ------------------------ | ------------------ |
| POST   | `/api/v0.0.1/auth/login` | Login, returns JWT |
| POST   | `/api/v0.0.1/users`      | Create user        |
| GET    | `/api/v0.0.1/users`      | List users         |
| GET    | `/api/v0.0.1/users/{id}` | Get user           |
| PUT    | `/api/v0.0.1/users/{id}` | Update user        |
| DELETE | `/api/v0.0.1/users/{id}` | Delete user        |

### tarot-service (port 8082)

| Method | Endpoint                       | Description          |
| ------ | ------------------------------ | -------------------- |
| GET    | `/api/v0.0.1/cards`            | List tarot cards     |
| GET    | `/api/v0.0.1/cards/random`     | Get random cards     |
| GET    | `/api/v0.0.1/layout-types`     | List spread layouts  |
| GET    | `/api/v0.0.1/layout-types/{id}`| Get layout type      |

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
- `application.yml` - Shared configuration (database, R2DBC, Flyway, SpringDoc)
- `eureka-server.yml` - Eureka server settings
- `gateway-service.yml` - Gateway routes, Resilience4j, Swagger UI
- `user-service.yml`, `tarot-service.yml`, `divination-service.yml` - Service-specific settings

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
./gradlew :gateway-service:test
./gradlew :user-service:test
./gradlew :tarot-service:test
./gradlew :divination-service:test

# Clean build artifacts
./gradlew clean

# Auto-format code
./gradlew ktlintFormat
```

### Running with Docker

```bash
docker compose up -d           # Start all services
docker compose up -d --build   # Rebuild and restart
docker compose logs -f         # View logs
docker compose down            # Stop all services
```

Service startup order is enforced by docker-compose health checks.

### Running Locally (Development)

```bash
# Start infrastructure
docker compose up -d config-server eureka-server gateway-service postgres

# Run services locally
./gradlew :user-service:bootRun
./gradlew :tarot-service:bootRun
./gradlew :divination-service:bootRun
```

### E2E Testing

E2E tests run against a pre-running application (63 tests).

```bash
# Start services and run tests
docker compose up -d
./gradlew :e2e-tests:test

# Custom gateway URL
GATEWAY_URL=http://localhost:8080 ./gradlew :e2e-tests:test
```

## Project Structure

```
highload/
├── config-server/        # Config Server (port 8888)
├── eureka-server/        # Eureka Server (port 8761)
├── gateway-service/      # API Gateway (port 8080)
├── highload-config/      # Git submodule - configuration repository
├── shared-dto/           # Shared DTOs between services
├── shared-clients/       # Shared Feign clients
├── user-service/         # User management (port 8081)
├── tarot-service/        # Cards & Layouts (port 8082)
├── divination-service/   # Spreads & Interpretations (port 8083)
├── e2e-tests/            # End-to-end tests (requires pre-running services)
├── docker-compose.yml    # Docker orchestration
├── settings.gradle.kts   # Multi-project Gradle config
└── CLAUDE.md            # Project instructions for Claude Code
```

## Technology Stack

- **Language:** Kotlin 2.2.10, Java 21
- **Framework:** Spring Boot 3.5.6, Spring Cloud 2025.0.0
- **Database:** PostgreSQL 15, Spring Data R2DBC, Flyway
- **Infrastructure:** Netflix Eureka, Spring Cloud Gateway, Spring Cloud Config
- **Resilience:** Resilience4j (circuit breaker, retry, time limiter)
- **API Docs:** SpringDoc OpenAPI
