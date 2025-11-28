# Microservices Migration Progress

## Step 1-2: Multi-project Setup and Shared DTOs ✅

**Completed:** 2025-11-28

### What was done:
- Updated `settings.gradle.kts` to include 4 subprojects: shared-dto, user-service, tarot-service, divination-service
- Created `shared-dto` module with all shared DTOs:
  - UserDto, CreateUserRequest, UpdateUserRequest, CreateUserResponse
  - CardDto, ArcanaTypeDto, LayoutTypeDto
  - SpreadDto, SpreadSummaryDto, SpreadCardDto, CreateSpreadRequest, CreateSpreadResponse
  - InterpretationDto, CreateInterpretationRequest, UpdateInterpretationRequest, CreateInterpretationResponse
  - PageResponse, ScrollResponse
  - ErrorResponse, ValidationErrorResponse
  - DeleteRequest
- Created stub `build.gradle.kts` for all 3 services
- `divination-service` includes Spring Cloud OpenFeign and WireMock dependencies

### Key learnings:
- Gradle multi-project build requires all included directories to exist before configuration
- Package renamed from `com.github.butvinmitmo.highload.dto` to `com.github.butvinmitmo.shared.dto`

---

## Step 3: User Service ✅

**Completed:** 2025-11-28

### What was done:
- Created full user-service implementation:
  - `UserServiceApplication.kt` - Spring Boot main class
  - `entity/User.kt` - JPA entity
  - `repository/UserRepository.kt` - Spring Data JPA repository
  - `service/UserService.kt` - Business logic layer
  - `controller/UserController.kt` - Public REST API endpoints
  - `controller/InternalUserController.kt` - Internal endpoint for Feign clients (`/api/internal/users/{id}/entity`)
  - `mapper/UserMapper.kt` - Entity to DTO mapper
  - `exception/Exceptions.kt` - NotFoundException, ConflictException
  - `exception/GlobalExceptionHandler.kt` - REST exception handling
- Created Flyway migrations:
  - `V1__create_user_table.sql` - Creates user table
  - `V2__add_initial_user.sql` - Adds admin seed user
- Created tests:
  - Unit tests: `UserServiceTest.kt` (13 tests)
  - Integration tests: `UserServiceIntegrationTest.kt` (8 tests)
  - Controller integration tests: `UserControllerIntegrationTest.kt` (8 tests)
- All 29 tests passing

### Key learnings:
- Need to add `jackson-module-kotlin` dependency for proper Kotlin data class deserialization
- Services need explicit package structure: `com.github.butvinmitmo.userservice`

### Next steps:
- Create tarot-service

---

## Step 4: Tarot Service ✅

**Completed:** 2025-11-28

### What was done:
- Created full tarot-service implementation:
  - `TarotServiceApplication.kt` - Spring Boot main class
  - `entity/ArcanaType.kt`, `entity/LayoutType.kt`, `entity/Card.kt` - JPA entities
  - `repository/CardRepository.kt`, `repository/LayoutTypeRepository.kt` - Spring Data JPA repositories
  - `service/TarotService.kt` - Business logic layer
  - `controller/CardController.kt`, `controller/LayoutTypeController.kt` - Public REST API endpoints
  - `controller/InternalTarotController.kt` - Internal endpoints for Feign clients:
    - `GET /api/internal/layout-types/{id}`
    - `GET /api/internal/cards/random?count=N`
  - `mapper/ArcanaTypeMapper.kt`, `mapper/LayoutTypeMapper.kt`, `mapper/CardMapper.kt` - Entity to DTO mappers
  - `exception/NotFoundException.kt` - Custom exception
  - `exception/GlobalExceptionHandler.kt` - REST exception handling
- Created Flyway migrations:
  - `V1__create_arcana_type_table.sql` - Creates arcana_type table
  - `V2__create_layout_type_table.sql` - Creates layout_type table
  - `V3__create_card_table.sql` - Creates card table with FK
  - `V4__add_tarot_reference_data.sql` - Adds 78 tarot cards and layout types
- Created tests:
  - Service integration tests: `TarotServiceIntegrationTest.kt` (6 tests)
  - Controller integration tests: `CardControllerIntegrationTest.kt`, `LayoutTypeControllerIntegrationTest.kt`, `InternalTarotControllerIntegrationTest.kt` (6 tests)
- All 12 tests passing

### Key learnings:
- ktlint has strict rules about single-class files and expression bodies
- Need to run ktlintFormat to auto-fix style issues

### Next steps:
- Create divination-service

---

## Step 5: Divination Service ✅

**Completed:** 2025-11-28

### What was done:
- Created full divination-service implementation:
  - `DivinationServiceApplication.kt` - Spring Boot main class with `@EnableFeignClients`
  - `entity/Spread.kt`, `entity/SpreadCard.kt`, `entity/Interpretation.kt` - JPA entities (store IDs instead of entity references for cross-service data)
  - `repository/SpreadRepository.kt`, `repository/SpreadCardRepository.kt`, `repository/InterpretationRepository.kt` - Spring Data JPA repositories
  - `service/DivinationService.kt` - Business logic layer using Feign clients
  - `controller/SpreadController.kt` - Spread REST API endpoints
  - `controller/InterpretationController.kt` - Interpretation REST API endpoints
  - `mapper/SpreadMapper.kt`, `mapper/InterpretationMapper.kt` - Entity to DTO mappers (using Feign clients for related data)
  - `client/UserClient.kt` - Feign client for user-service
  - `client/TarotClient.kt` - Feign client for tarot-service
  - `exception/Exceptions.kt` - NotFoundException, ForbiddenException, ConflictException
  - `exception/GlobalExceptionHandler.kt` - REST exception handling with Feign exception support
- Created Flyway migrations:
  - `V1__create_spread_table.sql` - Creates spread table with FKs to user and layout_type
  - `V2__create_spread_card_table.sql` - Creates spread_card table
  - `V3__create_interpretation_table.sql` - Creates interpretation table with unique constraint
- Created `application.yml` with service URLs configuration
- Created tests:
  - Unit tests: `DivinationServiceTest.kt` (15 tests)
  - Controller integration tests with WireMock: `SpreadControllerIntegrationTest.kt` (7 tests), `InterpretationControllerIntegrationTest.kt` (8 tests)
  - Test utilities: `TestEntityFactory.kt`, `BaseIntegrationTest.kt`, `BaseControllerIntegrationTest.kt`
  - Test database setup: `init-test-db.sql` (creates all prerequisite tables)
- All 30 tests passing

### Key learnings:
- Spring Cloud OpenFeign with Spring Boot 3.5.6 requires disabling compatibility verifier (`spring.cloud.compatibility-verifier.enabled=false`)
- WireMock JUnit 5 extension uses `wireMock.baseUrl()` for getting the mock server URL
- Need to use `@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)` to avoid Spring context caching issues when multiple test classes share WireMock
- For shared database with microservices, entities should store foreign key IDs instead of entity references to avoid cross-service entity loading
- Lazy-loaded collections (`@OneToMany`) need to be handled carefully - pass counts as parameters to mappers to avoid `LazyInitializationException`

### Next steps:
- Add Dockerfiles for each service
- Update docker-compose.yml for microservices
- Full integration testing
