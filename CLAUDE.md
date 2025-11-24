# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Tarology Web Service** - A Kotlin/Spring Boot application for Tarot card readings and interpretations. Users can create spreads, view others' spreads, and add interpretations without authentication (user ID-based identification).

Key features:
- Create tarot spreads with different layouts (one card, three cards, cross/five cards)
- View all spreads in chronological feed
- Add/edit/delete interpretations for spreads
- User management with transactional deletion

## Build & Development Commands

### Build and Run
```bash
# Build the project
./gradlew build

# Run the application locally
./gradlew bootRun

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.github.butvinmitmo.highload.HighloadApplicationTests"

# Clean build artifacts
./gradlew clean
```

### Code Quality
```bash
# Run ktlint checks
./gradlew ktlintCheck

# Auto-format code with ktlint
./gradlew ktlintFormat

# Run ktlint check for specific source set
./gradlew ktlintMainSourceSetCheck
./gradlew ktlintTestSourceSetCheck
```

### Docker Commands
```bash
# Start database and application with Docker Compose
docker-compose up -d

# Start only the database (for local development)
docker-compose up -d postgres

# Stop all services
docker-compose down

# Rebuild and restart
docker-compose up -d --build

# View logs
docker-compose logs -f app
```

### Database Setup
The project uses Flyway for database migrations. Migrations run automatically on application startup. Database configuration can be overridden via environment variables:
- `DB_NAME` - Database name (default: tarot_db)
- `DB_USER` - Database username (default: tarot_user)
- `DB_PASSWORD` - Database password
- `DB_PORT` - Database port (default: 5432)
- `APP_PORT` - Application port (default: 8080)

## Architecture

### Technology Stack
- **Language:** Kotlin 2.2.10
- **Framework:** Spring Boot 3.5.6
- **Build Tool:** Gradle with Kotlin DSL
- **JVM:** Java 21
- **Database:** PostgreSQL 15
- **Migrations:** Flyway
- **ORM:** Spring Data JPA with Hibernate
- **Code Style:** ktlint 1.5.0 (enforced via Gradle plugin)

### Database Schema
Uses UUID-based identifiers for all entities. Main tables:
- `user` - User accounts (id, username)
- `spread` - Tarot spreads (id, question, layout_type_id, created_at, author_id)
- `interpretation` - User interpretations (id, text, created_at, author_id, spread_id)
  - Unique constraint: (author_id, spread_id) - one interpretation per user per spread
- `card` - Tarot cards (id, name, arcana_type_id)
- `spread_card` - Cards in spreads (id, spread_id, card_id, position_in_spread, is_reversed)
- `layout_type` - Spread layouts (ONE_CARD, THREE_CARDS, CROSS)
- `arcana_type` - Card types (MAJOR, MINOR)

### API Endpoints

Base path: `/api/v0.0.1`

**Spreads:**
- `POST /spreads` - Create spread
- `GET /spreads?page=N&size=M` - Paginated list with X-Total-Count header
- `GET /spreads/scroll?after=ID&size=N` - Infinite scroll
- `GET /spreads/{id}` - Get spread details
- `DELETE /spreads/{id}` - Delete spread (author only)

**Interpretations:**
- `POST /spreads/{spreadId}/interpretations` - Add interpretation (409 if duplicate)
- `PUT /spreads/{spreadId}/interpretations/{id}` - Edit interpretation (author only)
- `DELETE /spreads/{spreadId}/interpretations/{id}` - Delete interpretation (author only)

**Users:**
- `POST /users` - Create user (409 if exists)
- `GET /users?page=N&size=M` - List users
- `GET /users/{id}` - Get user
- `PUT /users/{id}` - Update user
- `DELETE /users/{id}` - Delete user with all data (transactional)

### Project Structure
```
src/main/kotlin/com/github/butvinmitmo/highload/
├── controller/      # REST API controllers
├── service/         # Business logic layer
├── repository/      # Spring Data JPA repositories
├── entity/          # JPA entities (User, Spread, Card, etc.)
├── dto/             # Data Transfer Objects for API layer
├── mapper/          # Entity ↔ DTO mappers
├── exception/       # Custom exceptions
└── config/          # Spring configuration

src/test/kotlin/com/github/butvinmitmo/highload/
├── integration/
│   ├── controller/  # Controller integration tests (MockMvc + real DB)
│   └── service/     # Service integration tests (real DB, no HTTP layer)
└── unit/
    └── service/     # Service unit tests (mocked dependencies)
```

### DTO Layer
The project uses DTOs to separate the API layer from the database entities:

**Response DTOs:**
- `UserDto`, `SpreadDto`, `InterpretationDto`, `CardDto`, `SpreadCardDto`
- `LayoutTypeDto`, `ArcanaTypeDto`
- Summary DTOs for optimized list views: `SpreadSummaryDto`, `InterpretationSummaryDto`, `CardSummaryDto`

**Request DTOs:**
- `CreateUserRequest`, `UpdateUserRequest`
- `CreateSpreadRequest`
- `CreateInterpretationRequest`, `UpdateInterpretationRequest`

**Pagination DTOs:**
- `PageRequest`/`PageResponse` for traditional pagination
- `ScrollRequest`/`ScrollResponse` for infinite scroll

**Error DTOs:**
- `ErrorResponse` for general errors
- `ValidationErrorResponse` for field validation errors

### Repository Layer
Spring Data JPA repositories provide data access with the following features:

**Core Repositories:**
- `UserRepository` - User CRUD operations, search by username
- `SpreadRepository` - Spread operations with cursor-based pagination, search by question
- `InterpretationRepository` - Interpretation management with unique constraint handling
- `CardRepository` - Card operations with random selection for spreads
- `SpreadCardRepository` - Spread-card relationships with statistics queries
- `LayoutTypeRepository` - Layout type lookups with usage statistics
- `ArcanaTypeRepository` - Arcana type management with distribution queries

**Key Features:**
- `@EntityGraph` annotations for optimized fetching and N+1 query prevention
- Custom JPQL and native queries for complex operations
- Cursor-based pagination support for infinite scroll
- Statistical queries for analytics
- Bulk delete operations for cascading deletions

Entities use JPA annotations with the following patterns:
- `@Entity` with `@Table(name = "...")` for table mapping
- `@Id` with `@GeneratedValue(strategy = GenerationType.UUID)` for primary keys
- `@ManyToOne(fetch = FetchType.LAZY)` for foreign key relationships
- `@Column` with explicit `columnDefinition` for text fields and UUIDs
- Unique constraints defined at table level (e.g., interpretation has unique constraint on author_id + spread_id)

### Code Style Guidelines
The project uses ktlint for enforcing Kotlin code style. Key rules:
- No wildcard imports (use explicit imports)
- Files must end with a newline
- Proper indentation (4 spaces)
- Trailing commas in multi-line parameter lists
- No trailing whitespace

The build will fail if ktlint checks don't pass. Run `./gradlew ktlintFormat` before committing to auto-fix most issues.

### Transactional Requirements

Critical operations requiring single transaction (mark service methods with `@Transactional`):
1. **Spread creation:** Insert spread + generate random cards + link cards in spread_card
2. **Spread deletion:** Delete interpretations + spread_cards + spread (cascade configured in DB)
3. **User deletion:** Delete user's spreads → interpretations on those spreads → spread_cards + delete user's interpretations on others' spreads + user record (cascade configured in DB)

Database schema enforces referential integrity with `ON DELETE CASCADE` for user-related deletions.

## Testing

### Test Structure
Tests are organized hierarchically to mirror the production code structure:

```
src/test/kotlin/com/github/butvinmitmo/highload/
├── TestEntityFactory.kt              # Shared test utilities for entity creation
├── integration/
│   ├── BaseIntegrationTest.kt        # Shared base for all integration tests
│   ├── controller/
│   │   ├── BaseControllerIntegrationTest.kt  # Base for controller tests (MockMvc)
│   │   ├── UserControllerIntegrationTest.kt
│   │   ├── SpreadControllerIntegrationTest.kt
│   │   ├── InterpretationControllerIntegrationTest.kt
│   │   └── MockMvcExtensions.kt      # Helper functions for MockMvc
│   └── service/
│       ├── UserServiceIntegrationTest.kt
│       ├── SpreadServiceIntegrationTest.kt
│       ├── InterpretationServiceIntegrationTest.kt
│       └── CardServiceIntegrationTest.kt
└── unit/
    └── service/
        ├── UserServiceTest.kt
        ├── SpreadServiceTest.kt
        ├── InterpretationServiceTest.kt
        └── CardServiceTest.kt
```

### Test Types

**Unit Tests** (`unit/service/`)
- Test service layer in isolation
- Mock all dependencies (repositories, other services)
- Use TestEntityFactory for creating test entities
- Fast execution, no database required

**Service Integration Tests** (`integration/service/`)
- Test service layer with real database
- Use TestContainers for PostgreSQL
- Extend BaseIntegrationTest for shared container
- Automatic database cleanup after each test

**Controller Integration Tests** (`integration/controller/`)
- Test full stack: Controller → Service → Repository → Database
- Use MockMvc to simulate HTTP requests (not real HTTP)
- Use TestContainers for PostgreSQL
- Extend BaseControllerIntegrationTest for shared setup
- Note: These are NOT true end-to-end tests (no real HTTP server)

### Running Tests

```bash
# Run all tests
./gradlew test

# Run unit tests only
./gradlew test --tests "com.github.butvinmitmo.highload.unit.*"

# Run integration tests only
./gradlew test --tests "com.github.butvinmitmo.highload.integration.*"

# Run controller integration tests only
./gradlew test --tests "com.github.butvinmitmo.highload.integration.controller.*"

# Run service integration tests only
./gradlew test --tests "com.github.butvinmitmo.highload.integration.service.*"

# Run specific test class
./gradlew test --tests "com.github.butvinmitmo.highload.unit.service.UserServiceTest"
```

### Test Infrastructure

**TestEntityFactory** - Centralized entity creation for unit tests using reflection to set generated fields (id, createdAt)

**BaseIntegrationTest** - Provides:
- Shared PostgreSQL TestContainer (single instance for all service integration tests)
- Automatic database cleanup after each test
- UUID-based seed data filtering

**BaseControllerIntegrationTest** - Provides:
- Shared PostgreSQL TestContainer (single instance for all controller tests)
- MockMvc for simulated HTTP requests
- Jackson ObjectMapper for JSON serialization
- Layout type UUIDs for test data setup
- Automatic database cleanup after each test

**MockMvcExtensions** - Helper functions:
- `postJson()`, `putJson()`, `deleteJson()` - Simplified HTTP request methods
- `getIdFromBody()`, `getLocationId()` - UUID extraction from responses

### Test Best Practices

1. **Use TestEntityFactory for unit tests** - Avoids duplicated reflection code
2. **Extend appropriate base class** - BaseIntegrationTest or BaseControllerIntegrationTest
3. **Cleanup is automatic** - No need to manually clean database in integration tests
4. **Shared containers** - Tests share PostgreSQL containers for performance
5. **Descriptive test names** - Use backtick syntax with clear descriptions
6. **No assertion messages** - Test names and code should be self-documenting
7. **Use JUnit 5 assertions** - Consistent assertion style across all tests

### Git Workflow
- When using git add, specify files explicitly (avoid `git add .`)