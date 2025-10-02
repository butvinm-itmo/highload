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
├── entity/          # JPA entities (User, Spread, Card, etc.)
├── repository/      # Spring Data JPA repositories (to be implemented)
├── service/         # Business logic layer (to be implemented)
└── controller/      # REST API controllers (to be implemented)
```

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
