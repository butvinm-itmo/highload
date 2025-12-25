# Tarology Web Service

A Kotlin/Spring Boot microservices application for Tarot card readings and interpretations.

## Architecture

The application consists of 7 microservices with centralized configuration, service discovery, and API gateway:

| Service                  | Port | Description                          | Swagger UI                                       |
| ------------------------ | ---- | ------------------------------------ | ------------------------------------------------ |
| **config-server**        | 8888 | Centralized configuration            | -                                                |
| **eureka-server**        | 8761 | Service discovery (Netflix Eureka)   | [Dashboard](http://localhost:8761)               |
| **gateway-service**      | 8080 | API Gateway (routing, resilience)    | -                                                |
| **user-service**         | 8081 | User management, authentication      | [Swagger](http://localhost:8081/swagger-ui.html) |
| **tarot-service**        | 8082 | Cards & LayoutTypes catalog          | [Swagger](http://localhost:8082/swagger-ui.html) |
| **divination-service**   | 8083 | Spreads & Interpretations (reactive) | [Swagger](http://localhost:8083/swagger-ui.html) |
| **notification-service** | 8084 | In-app notifications (reactive)      | [Swagger](http://localhost:8084/swagger-ui.html) |

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

# Verify services are running
curl http://localhost:8080/actuator/health  # Gateway (main entry point)

# Check Eureka dashboard for registered services
open http://localhost:8761

# Access APIs through the gateway (recommended for external clients)
curl http://localhost:8080/api/v0.0.1/users
curl http://localhost:8080/api/v0.0.1/cards
curl http://localhost:8080/api/v0.0.1/spreads

# Run E2E tests (automatically rebuilds containers and waits for health)
./gradlew :e2e-tests:test
```

## API Overview

**Gateway Access:** All APIs can be accessed through the gateway at `http://localhost:8080` for external clients, or directly via service ports for internal/development use.

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

| Method | Endpoint                                              | Description                     |
| ------ | ----------------------------------------------------- | ------------------------------- |
| POST   | `/api/v0.0.1/spreads`                                 | Create spread                   |
| GET    | `/api/v0.0.1/spreads?page=N&size=M`                   | List spreads (paginated)        |
| GET    | `/api/v0.0.1/spreads/scroll?after=ID&size=N`          | Scroll spreads (cursor-based)   |
| GET    | `/api/v0.0.1/spreads/{id}`                            | Get spread with cards & interps |
| DELETE | `/api/v0.0.1/spreads/{id}`                            | Delete spread (author only)     |
| GET    | `/api/v0.0.1/spreads/{id}/interpretations`            | List interpretations for spread |
| GET    | `/api/v0.0.1/spreads/{id}/interpretations/{interpId}` | Get interpretation              |
| POST   | `/api/v0.0.1/spreads/{id}/interpretations`            | Add interpretation              |
| PUT    | `/api/v0.0.1/spreads/{id}/interpretations/{interpId}` | Update interpretation (author)  |
| DELETE | `/api/v0.0.1/spreads/{id}/interpretations/{interpId}` | Delete interpretation (author)  |

### notification-service (port 8084)

| Method | Endpoint                                  | Description               |
| ------ | ----------------------------------------- | ------------------------- |
| GET    | `/api/v0.0.1/notifications`               | List user notifications   |
| GET    | `/api/v0.0.1/notifications/unread-count`  | Get unread count          |
| PATCH  | `/api/v0.0.1/notifications/{id}/read`     | Mark notification as read |
| POST   | `/api/v0.0.1/notifications/mark-all-read` | Mark all as read          |

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
- `gateway-service.yml` - Gateway routes and Resilience4j settings
- `user-service.yml` - User service specific
- `tarot-service.yml` - Tarot service specific
- `divination-service.yml` - Divination service + Resilience4j settings
- `notification-service.yml` - Notification service + Kafka settings

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
./gradlew :<service-name>:test

# Clean build artifacts
./gradlew clean
```

Coverage reports: `<service-name>/build/reports/jacoco/test/html/index.html`

### Code Quality

```bash
# Run ktlint checks
./gradlew ktlintCheck

# Auto-format code
./gradlew ktlintFormat

# Pre-commit hooks (automatic formatting before commit)
.venv/bin/pre-commit install              # Install hooks (one-time setup)
.venv/bin/pre-commit run --all-files      # Manually run on all files
```

**Pre-commit hooks:** The project uses [pre-commit](https://pre-commit.com/) to automatically run ktlint format before each commit.

First-time setup:

```bash
python -m venv .venv
.venv/bin/pip install pre-commit
.venv/bin/pre-commit install
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
docker compose logs -f <service-name>

# Stop all services
docker compose down
```

**Service startup order** (enforced by docker-compose health checks):

1. `config-server`, `postgres`, `kafka-1/2/3` - Infrastructure
2. `eureka-server` - Service discovery
3. `gateway-service` - API Gateway
4. `user-service`, `tarot-service` - Core services
5. `divination-service`, `notification-service` - Dependent services

**Environment variables:**

- `CONFIG_SERVER_URL` - Config Server URL (default: http://localhost:8888)
- `EUREKA_URL` - Eureka Server URL (required)
- `KAFKA_BOOTSTRAP_SERVERS` - Kafka brokers (required for divination/notification services)
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` - Database connection

### Running Locally (Development)

```bash
# Start infrastructure (config, eureka, gateway, postgres, kafka)
docker compose up -d config-server eureka-server gateway-service postgres kafka-1 kafka-2 kafka-3

# Run services locally (in separate terminals)
./gradlew :user-service:bootRun
./gradlew :tarot-service:bootRun
./gradlew :divination-service:bootRun
./gradlew :notification-service:bootRun
```

### E2E Testing

E2E tests automatically rebuild containers and wait for health checks before running.

```bash
./gradlew :e2e-tests:test
```

**Stop containers after testing:**

```bash
docker compose down
```

**Custom Gateway URL:**

```bash
# Via environment variable
GATEWAY_URL=http://localhost:8080 ./gradlew :e2e-tests:test

# Via system property
./gradlew :e2e-tests:test -DGATEWAY_URL=http://localhost:8080
```

**Test coverage (31 tests):**

- All tests route through gateway-service (simulating external client access)
- User CRUD, duplicate username (409), not found (404), authentication
- Cards pagination (78 total cards), layout types, random cards
- Spreads with inter-service Feign calls, interpretations CRUD
- Delete operations, authorization verification (403)

## Project Structure

```
highload/
├── config-server/        # Centralized configuration
├── eureka-server/        # Service discovery
├── gateway-service/      # API Gateway
├── user-service/         # User management
├── tarot-service/        # Cards & Layouts
├── divination-service/   # Spreads & Interpretations
├── notification-service/ # In-app notifications
├── shared-dto/           # Shared DTOs
├── shared-clients/       # Shared Feign clients
├── e2e-tests/            # End-to-end tests
├── highload-config/      # Configuration repository (git submodule)
└── docker-compose.yml    # Docker orchestration
```

## Technology Stack

- **Language:** Kotlin 2.2.10
- **Framework:** Spring Boot 3.5.6
- **Build:** Gradle (Kotlin DSL)
- **JVM:** Java 21
- **Database:** PostgreSQL 15
- **Migrations:** Flyway (per-service)
- **Service Discovery:** Netflix Eureka (Spring Cloud Netflix)
- **API Gateway:** Spring Cloud Gateway with circuit breaker
- **Configuration:** Spring Cloud Config Server (Git backend)
- **Inter-service:** Spring Cloud OpenFeign with Eureka discovery
- **Resilience:** Resilience4j (circuit breaker, retry, time limiter)
- **Messaging:** Apache Kafka 3.7 (KRaft mode, 3 replicas)
- **Testing:** Spring Boot Test, JUnit 5, TestContainers
- **API Docs:** SpringDoc OpenAPI (Swagger)
- **Code Style:** ktlint 1.5.0
