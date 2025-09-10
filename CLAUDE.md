# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot monolithic application for a Tarot card reading web service ("Тарология"). The system allows users to create tarot spreads, view them, and add interpretations without requiring registration.

## Technology Stack

- **Language**: Kotlin
- **Framework**: Spring Boot 3.2.3
- **Database**: PostgreSQL with Flyway migrations
- **Build Tool**: Gradle 8.6 with Kotlin DSL
- **Container**: Docker & Docker Compose
- **API Documentation**: OpenAPI 3 with Swagger UI
- **Testing**: JUnit Jupiter API with Testcontainers and MockK

## Key Development Commands

```bash
# Build the application
./gradlew clean build

# Run tests with coverage report
./gradlew test jacocoTestReport

# Run only integration tests
./gradlew test --tests "*IntegrationTest"

# Run only unit tests
./gradlew test --tests "*Test" --exclude-task integrationTest

# Run application locally (requires PostgreSQL)
./gradlew bootRun

# Run with Docker Compose (recommended for development)
docker-compose up --build

# Start only PostgreSQL for local development
docker-compose up postgres

# View test coverage report
# Located at: build/reports/jacoco/test/html/index.html
```

## Architecture Overview

### Layer Structure
The application follows a clean layered architecture:
- **Controller Layer**: REST endpoints (`/api/*`)
- **Service Layer**: Business logic and transactions
- **Repository Layer**: JPA data access
- **Entity Layer**: JPA entities with relationships

### Key Packages
- `com.itmo.tarot.controller` - REST controllers
- `com.itmo.tarot.service` - Business services
- `com.itmo.tarot.repository` - JPA repositories
- `com.itmo.tarot.entity` - JPA entities
- `com.itmo.tarot.dto.request/response` - DTOs for API
- `com.itmo.tarot.mapper` - Entity/DTO mapping
- `com.itmo.tarot.exception` - Exception handling
- `com.itmo.tarot.config` - Spring configuration

### Core Domain Entities
- **User**: Simple ID-based users (no authentication)
- **Spread**: Tarot card layouts with questions
- **Card**: 78 tarot cards (Major/Minor Arcana)
- **SpreadCard**: Many-to-many with position and reversed status
- **Interpretation**: User comments on spreads (unique per user-spread)

### Database Schema
- Flyway migrations in `src/main/resources/db/migration/`
- PostgreSQL with proper indexes and constraints
- Sample data populated via V6 migration

## Critical Implementation Requirements

### Transactional Operations
These operations MUST be wrapped in `@Transactional`:
- Creating spreads (spread + cards atomically)
- Deleting users (cascade delete all related data)
- Any multi-table operations

### Pagination Requirements
- `/api/spreads` - Traditional pagination with `X-Total-Count` header
- `/api/spreads/scroll` - Infinite scroll without total count
- Maximum 50 records per request

### Entity Relationships
- **Many-to-Many**: Spread ↔ Card (via SpreadCard with position/reversed)
- **One-to-Many**: User → Spread, User → Interpretation
- **Unique Constraint**: One interpretation per user per spread

### API Authorization
User ownership validation required for:
- Spread deletion (only spread owner)
- Interpretation modification/deletion (only interpretation owner)
- User deletion (cascades to all user data)

## Testing Strategy

### Test Structure
- `src/test/kotlin/com/itmo/tarot/integration/` - TestContainers integration tests
- `src/test/kotlin/com/itmo/tarot/unit/` - MockK unit tests
- `src/test/resources/application-test.yml` - Test configuration

### Coverage Target
- Minimum 70% code coverage via Jacoco
- Focus on service layer business logic
- Integration tests for API endpoints

## Configuration

### Environment Variables
```bash
DATABASE_HOST=localhost
DATABASE_PORT=5432
DATABASE_NAME=tarot_db
DATABASE_USERNAME=tarot_user
DATABASE_PASSWORD=tarot_password
SHOW_SQL=false
LOG_LEVEL=INFO
SQL_LOG_LEVEL=WARN
```

### Key Configuration Files
- `src/main/resources/application.yml` - Main configuration
- `src/test/resources/application-test.yml` - Test configuration
- `docker-compose.yml` - PostgreSQL + application setup

## API Documentation

When running:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api-docs
- **Health Check**: http://localhost:8080/actuator/health

## Development Standards

- Follow existing Kotlin coding conventions in the codebase
- Use environment variables for configuration (never hardcode)
- Apply proper validation at both controller and entity levels
- Implement proper error handling with meaningful HTTP status codes
- Use DTOs for API contracts, separate from JPA entities
- Enums stored as strings in database for readability
- Always use transactions for multi-table operations