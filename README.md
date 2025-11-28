# Tarology Web Service

A Kotlin/Spring Boot microservices application for Tarot card readings and interpretations.

## Architecture

The application consists of 3 microservices:

| Service                | Port | Description                 | Swagger UI                                                                                 |
| ---------------------- | ---- | --------------------------- | ------------------------------------------------------------------------------------------ |
| **user-service**       | 8081 | User management             | [http://localhost:8081/swagger-ui/index.html](http://localhost:8081/swagger-ui/index.html) |
| **tarot-service**      | 8082 | Cards & LayoutTypes catalog | [http://localhost:8082/swagger-ui/index.html](http://localhost:8082/swagger-ui/index.html) |
| **divination-service** | 8083 | Spreads & Interpretations   | [http://localhost:8083/swagger-ui/index.html](http://localhost:8083/swagger-ui/index.html) |

**Inter-service communication:** `divination-service` calls `user-service` and `tarot-service` via Feign clients.

**Database:** All services share a single PostgreSQL database.

## Quick Start

```bash
# Start all services
docker-compose up -d

# Verify services are running
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health

# Run E2E tests
./e2e.sh
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

| Method | Endpoint                                        | Description           |
| ------ | ----------------------------------------------- | --------------------- |
| POST   | `/api/v0.0.1/spreads`                           | Create spread         |
| GET    | `/api/v0.0.1/spreads`                           | List spreads          |
| GET    | `/api/v0.0.1/spreads/{id}`                      | Get spread details    |
| DELETE | `/api/v0.0.1/spreads/{id}`                      | Delete spread         |
| POST   | `/api/v0.0.1/spreads/{id}/interpretations`      | Add interpretation    |
| PUT    | `/api/v0.0.1/spreads/{id}/interpretations/{id}` | Update interpretation |
| DELETE | `/api/v0.0.1/spreads/{id}/interpretations/{id}` | Delete interpretation |

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
docker-compose up -d

# Rebuild and restart
docker-compose up -d --build

# View logs
docker-compose logs -f

# View logs for specific service
docker-compose logs -f user-service
docker-compose logs -f tarot-service
docker-compose logs -f divination-service

# Stop all services
docker-compose down
```

### Running Locally (Development)

```bash
# Start only the database
docker-compose up -d postgres

# Run individual services (in separate terminals)
./gradlew :user-service:bootRun
./gradlew :tarot-service:bootRun
./gradlew :divination-service:bootRun
```

### E2E Testing

```bash
# Start services first
docker-compose up -d

# Run end-to-end tests
./e2e.sh
```

The E2E script tests all services and their interactions (55 tests).

## Project Structure

```
highload/
├── shared-dto/           # Shared DTOs between services
├── user-service/         # User management (port 8081)
├── tarot-service/        # Cards & Layouts (port 8082)
├── divination-service/   # Spreads & Interpretations (port 8083)
├── docker-compose.yml    # Docker orchestration
├── e2e.sh               # End-to-end test script
└── settings.gradle.kts   # Multi-project Gradle config
```

## Technology Stack

- **Language:** Kotlin 2.2.10
- **Framework:** Spring Boot 3.5.6
- **Build:** Gradle (Kotlin DSL)
- **Database:** PostgreSQL 15
- **Migrations:** Flyway
- **Inter-service:** Spring Cloud OpenFeign
- **API Docs:** SpringDoc OpenAPI (Swagger)
- **Code Style:** ktlint
