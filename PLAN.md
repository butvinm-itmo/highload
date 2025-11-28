 Microservices Migration Plan

 Overview

 Split the monolith into 3 microservices using Feign Clients for inter-service communication:
 - user-service (port 8081) - User management
 - tarot-service (port 8082) - Cards & LayoutTypes reference data
 - divination-service (port 8083) - Spreads & Interpretations

 Architecture decisions:
 - Separate directories with independent build.gradle.kts files
 - Shared PostgreSQL database (maintains referential integrity)
 - Feign Clients with hardcoded URLs (no Eureka/Gateway)

 ---
 Project Structure

 highload/
 ├── shared-dto/                          # Shared DTOs module
 │   ├── build.gradle.kts
 │   └── src/main/kotlin/com/github/butvinmitmo/shared/dto/
 │       ├── UserDto.kt
 │       ├── CardDto.kt, LayoutTypeDto.kt, ArcanaTypeDto.kt
 │       ├── SpreadDto.kt, SpreadSummaryDto.kt, SpreadCardDto.kt
 │       ├── InterpretationDto.kt, InterpretationSummaryDto.kt
 │       ├── PageResponse.kt, ScrollResponse.kt
 │       └── ErrorResponse.kt, ValidationErrorResponse.kt
 │
 ├── user-service/
 │   ├── build.gradle.kts
 │   └── src/
 │       ├── main/kotlin/com/github/butvinmitmo/userservice/
 │       │   ├── UserServiceApplication.kt
 │       │   ├── controller/UserController.kt
 │       │   ├── service/UserService.kt
 │       │   ├── repository/UserRepository.kt
 │       │   ├── entity/User.kt
 │       │   ├── mapper/UserMapper.kt
 │       │   ├── exception/...
 │       │   └── config/...
 │       ├── main/resources/db/migration/  # User table migrations
 │       └── test/kotlin/...               # Unit + integration tests
 │
 ├── tarot-service/
 │   ├── build.gradle.kts
 │   └── src/
 │       ├── main/kotlin/com/github/butvinmitmo/tarotservice/
 │       │   ├── TarotServiceApplication.kt
 │       │   ├── controller/{CardController, LayoutTypeController}.kt
 │       │   ├── service/TarotService.kt
 │       │   ├── repository/{CardRepository, LayoutTypeRepository}.kt
 │       │   ├── entity/{Card, LayoutType, ArcanaType}.kt
 │       │   └── mapper/...
 │       ├── main/resources/db/migration/  # Card, LayoutType, ArcanaType migrations
 │       └── test/kotlin/...               # Unit + integration tests
 │
 ├── divination-service/
 │   ├── build.gradle.kts
 │   └── src/
 │       ├── main/kotlin/com/github/butvinmitmo/divinationservice/
 │       │   ├── DivinationServiceApplication.kt
 │       │   ├── controller/{SpreadController, InterpretationController}.kt
 │       │   ├── service/DivinationService.kt
 │       │   ├── repository/{SpreadRepository, SpreadCardRepository, InterpretationRepository}.kt
 │       │   ├── entity/{Spread, SpreadCard, Interpretation}.kt
 │       │   ├── mapper/...
 │       │   ├── client/{UserClient, TarotClient}.kt  # Feign clients
 │       │   └── config/...
 │       ├── main/resources/db/migration/  # Spread, SpreadCard, Interpretation migrations
 │       └── test/kotlin/...               # Unit + integration tests
 │
 ├── docker-compose.yml (updated)
 ├── settings.gradle.kts (multi-project build)
 └── src/ (original monolith - can be removed after migration)

 ---
 Implementation Steps

 Step 1: Create user-service

 1.1 Create directory structure:
 user-service/
 ├── build.gradle.kts
 └── src/
     ├── main/
     │   ├── kotlin/com/github/butvinmitmo/userservice/
     │   └── resources/
     │       └── application.yml
     └── test/kotlin/...

 1.2 build.gradle.kts:
 - Copy from original, adjust:
   - group = "com.github.butvinmitmo.userservice"
   - Remove unused dependencies (no Feign needed here)
   - Keep: Spring Boot, JPA, PostgreSQL, Flyway, ktlint

 1.3 Copy and adapt files from monolith:
 | Original                                              | Target                                        |
 |-------------------------------------------------------|-----------------------------------------------|
 | entity/User.kt                                        | user-service/.../entity/User.kt               |
 | repository/UserRepository.kt                          | user-service/.../repository/UserRepository.kt |
 | service/UserService.kt                                | user-service/.../service/UserService.kt       |
 | controller/UserController.kt                          | user-service/.../controller/UserController.kt |
 | mapper/UserMapper.kt                                  | user-service/.../mapper/UserMapper.kt         |
 | dto/{UserDto, CreateUserRequest, ...}                 | user-service/.../dto/                         |
 | exception/{NotFoundException, ConflictException, ...} | user-service/.../exception/                   |
 | config/GlobalExceptionHandler.kt                      | user-service/.../config/                      |

 1.4 application.yml:
 server:
   port: 8081

 spring:
   application:
     name: user-service
   datasource:
     url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:tarot_db}
     username: ${DB_USER:tarot_user}
     password: ${DB_PASSWORD:tarot_password}
   jpa:
     hibernate:
       ddl-auto: validate
   flyway:
     enabled: false  # Migrations managed externally or by one service

 1.5 Add new endpoint for internal calls:
 - GET /api/internal/users/{id}/entity → Returns UserDto (used by divination-service)

 ---
 Step 2: Create tarot-service

 2.1 Create directory structure (similar to user-service)

 2.2 build.gradle.kts:
 - Same pattern as user-service, no Feign needed

 2.3 Copy and adapt files:
 | Original                                             | Target                                    |
 |------------------------------------------------------|-------------------------------------------|
 | entity/{Card, LayoutType, ArcanaType}.kt             | tarot-service/.../entity/                 |
 | repository/{CardRepository, LayoutTypeRepository}.kt | tarot-service/.../repository/             |
 | service/TarotService.kt                              | tarot-service/.../service/TarotService.kt |
 | controller/{CardController, LayoutTypeController}.kt | tarot-service/.../controller/             |
 | mapper/{CardMapper, LayoutTypeMapper}.kt             | tarot-service/.../mapper/                 |
 | dto/{CardDto, LayoutTypeDto, ArcanaTypeDto, ...}     | tarot-service/.../dto/                    |

 2.4 application.yml:
 server:
   port: 8082

 spring:
   application:
     name: tarot-service
   # ... same DB config as user-service

 2.5 Add internal endpoints:
 - GET /api/internal/layout-types/{id} → Returns LayoutTypeDto
 - GET /api/internal/cards/random?count=N → Returns List<CardDto>

 ---
 Step 3: Create divination-service

 3.1 Create directory structure

 3.2 build.gradle.kts:
 - Add Spring Cloud OpenFeign dependency:
 implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
 - Add Spring Cloud dependency management

 3.3 Copy and adapt files:
 | Original                                                                         | Target                                              |
 |----------------------------------------------------------------------------------|-----------------------------------------------------|
 | entity/{Spread, SpreadCard, Interpretation}.kt                                   | divination-service/.../entity/                      |
 | repository/{SpreadRepository, SpreadCardRepository, InterpretationRepository}.kt | divination-service/.../repository/                  |
 | service/DivinationService.kt                                                     | divination-service/.../service/DivinationService.kt |
 | controller/{SpreadController, InterpretationController}.kt                       | divination-service/.../controller/                  |
 | mapper/{SpreadMapper, InterpretationMapper}.kt                                   | divination-service/.../mapper/                      |
 | dto/{SpreadDto, InterpretationDto, ...}                                          | divination-service/.../dto/                         |

 3.4 Create Feign Clients:

 // client/UserClient.kt
 @FeignClient(name = "user-service", url = "\${services.user-service.url}")
 interface UserClient {
     @GetMapping("/api/internal/users/{id}/entity")
     fun getUserById(@PathVariable id: UUID): UserDto
 }

 // client/TarotClient.kt
 @FeignClient(name = "tarot-service", url = "\${services.tarot-service.url}")
 interface TarotClient {
     @GetMapping("/api/internal/layout-types/{id}")
     fun getLayoutTypeById(@PathVariable id: UUID): LayoutTypeDto

     @GetMapping("/api/internal/cards/random")
     fun getRandomCards(@RequestParam count: Int): List<CardDto>
 }

 3.5 Modify DivinationService:
 - Replace UserService injection with UserClient
 - Replace TarotService injection with TarotClient
 - Handle FeignException for 404 responses → throw NotFoundException

 3.6 application.yml:
 server:
   port: 8083

 spring:
   application:
     name: divination-service
   # ... same DB config

 services:
   user-service:
     url: ${USER_SERVICE_URL:http://localhost:8081}
   tarot-service:
     url: ${TAROT_SERVICE_URL:http://localhost:8082}

 3.7 Enable Feign in main application:
 @SpringBootApplication
 @EnableFeignClients
 class DivinationServiceApplication

 ---
 Step 4: Handle Entity References

 Since we're using a shared database but services only have their own entities, we need to handle foreign key references:

 In divination-service entities:

 // Spread.kt - Remove User and LayoutType entity references
 @Entity
 @Table(name = "spread")
 class Spread(
     @Id @GeneratedValue(strategy = GenerationType.UUID)
     val id: UUID? = null,

     val question: String? = null,

     @Column(name = "layout_type_id")
     val layoutTypeId: UUID,  // Store just the ID, not the entity

     @Column(name = "author_id")
     val authorId: UUID,  // Store just the ID, not the entity

     val createdAt: Instant = Instant.now()
 )

 Mappers will fetch related data via Feign clients when building DTOs.

 ---
 Step 5: Update Docker Compose

 version: '3.8'

 services:
   postgres:
     image: postgres:15
     environment:
       POSTGRES_DB: tarot_db
       POSTGRES_USER: tarot_user
       POSTGRES_PASSWORD: tarot_password
     ports:
       - "5432:5432"
     volumes:
       - postgres_data:/var/lib/postgresql/data

   user-service:
     build: ./user-service
     ports:
       - "8081:8081"
     environment:
       DB_HOST: postgres
       DB_PORT: 5432
       DB_NAME: tarot_db
       DB_USER: tarot_user
       DB_PASSWORD: tarot_password
     depends_on:
       - postgres

   tarot-service:
     build: ./tarot-service
     ports:
       - "8082:8082"
     environment:
       DB_HOST: postgres
       # ... same DB config
     depends_on:
       - postgres

   divination-service:
     build: ./divination-service
     ports:
       - "8083:8083"
     environment:
       DB_HOST: postgres
       USER_SERVICE_URL: http://user-service:8081
       TAROT_SERVICE_URL: http://tarot-service:8082
       # ... same DB config
     depends_on:
       - postgres
       - user-service
       - tarot-service

 volumes:
   postgres_data:

 ---
 Step 6: Create shared-dto Module

 6.1 Create directory structure:
 shared-dto/
 ├── build.gradle.kts
 └── src/main/kotlin/com/github/butvinmitmo/shared/dto/

 6.2 build.gradle.kts:
 plugins {
     kotlin("jvm")
     kotlin("plugin.spring")
 }

 dependencies {
     implementation("com.fasterxml.jackson.core:jackson-annotations")
     implementation("jakarta.validation:jakarta.validation-api")
 }

 6.3 Move shared DTOs:
 - UserDto, CreateUserRequest, UpdateUserRequest, CreateUserResponse
 - CardDto, LayoutTypeDto, ArcanaTypeDto
 - SpreadDto, SpreadSummaryDto, SpreadCardDto, CreateSpreadRequest, CreateSpreadResponse
 - InterpretationDto, InterpretationSummaryDto, CreateInterpretationRequest, UpdateInterpretationRequest, CreateInterpretationResponse
 - PageResponse, ScrollResponse, PageRequest, ScrollRequest
 - ErrorResponse, ValidationErrorResponse
 - DeleteRequest

 6.4 Each service depends on shared-dto:
 // In each service's build.gradle.kts
 dependencies {
     implementation(project(":shared-dto"))
 }

 ---
 Step 7: Database Migrations (Split by Service)

 Each service owns migrations for its tables. Use different version prefixes to avoid conflicts.

 user-service migrations:
 src/main/resources/db/migration/
 ├── V1__create_user_table.sql
 └── V2__add_initial_user.sql

 tarot-service migrations:
 src/main/resources/db/migration/
 ├── V1__create_arcana_type_table.sql
 ├── V2__create_layout_type_table.sql
 ├── V3__create_card_table.sql
 └── V4__add_tarot_reference_data.sql

 divination-service migrations:
 src/main/resources/db/migration/
 ├── V1__create_spread_table.sql
 ├── V2__create_spread_card_table.sql
 └── V3__create_interpretation_table.sql

 Important: Run services in order on first startup: tarot-service → user-service → divination-service (or ensure FK tables exist)

 ---
 Step 8: Migrate Tests

 8.1 user-service tests:
 - Copy unit/service/UserServiceTest.kt
 - Copy integration/service/UserServiceIntegrationTest.kt
 - Copy integration/controller/UserControllerIntegrationTest.kt
 - Adapt BaseIntegrationTest.kt and TestEntityFactory.kt

 8.2 tarot-service tests:
 - Copy unit/service/CardServiceTest.kt (if exists, or create)
 - Copy integration/service/CardServiceIntegrationTest.kt
 - Create controller integration tests for CardController, LayoutTypeController

 8.3 divination-service tests:
 - Copy unit/service/SpreadServiceTest.kt, InterpretationServiceTest.kt
 - Copy integration/service/SpreadServiceIntegrationTest.kt, InterpretationServiceIntegrationTest.kt
 - Copy integration/controller/SpreadControllerIntegrationTest.kt, InterpretationControllerIntegrationTest.kt
 - Modify: Mock Feign clients in unit tests
 - Modify: Use WireMock or similar for integration tests to mock external services

 ---
 Critical Files to Modify/Create

 shared-dto (new module)

 - build.gradle.kts
 - All response/request DTOs moved from monolith

 user-service (new)

 - build.gradle.kts (depends on shared-dto)
 - UserServiceApplication.kt
 - application.yml
 - Copy: User.kt, UserRepository.kt, UserService.kt, UserController.kt, UserMapper.kt
 - Copy: Exception classes, GlobalExceptionHandler
 - Migrations: V1__create_user_table.sql, V2__add_initial_user.sql
 - Tests: Unit + integration tests for UserService and UserController

 tarot-service (new)

 - build.gradle.kts (depends on shared-dto)
 - TarotServiceApplication.kt
 - application.yml
 - Copy: Card.kt, LayoutType.kt, ArcanaType.kt
 - Copy: CardRepository.kt, LayoutTypeRepository.kt
 - Copy: TarotService.kt, CardController.kt, LayoutTypeController.kt
 - Copy: CardMapper.kt, LayoutTypeMapper.kt
 - Migrations: V1-V4 for arcana_type, layout_type, card tables + seed data
 - Tests: Unit + integration tests

 divination-service (new)

 - build.gradle.kts (with Feign, depends on shared-dto)
 - DivinationServiceApplication.kt (with @EnableFeignClients)
 - application.yml (with service URLs)
 - Copy: Spread.kt, SpreadCard.kt, Interpretation.kt (modified - store IDs instead of entities)
 - Copy: SpreadRepository.kt, SpreadCardRepository.kt, InterpretationRepository.kt
 - Copy: DivinationService.kt (modified - use Feign clients)
 - Copy: SpreadController.kt, InterpretationController.kt
 - Copy: SpreadMapper.kt, InterpretationMapper.kt (modified - use Feign clients)
 - Create: UserClient.kt, TarotClient.kt
 - Migrations: V1-V3 for spread, spread_card, interpretation tables
 - Tests: Unit tests (mock Feign clients) + integration tests (WireMock)

 settings.gradle.kts (new/modify)

 rootProject.name = "highload"
 include("shared-dto", "user-service", "tarot-service", "divination-service")

 docker-compose.yml (modify)

 - Add 3 service definitions
 - Configure networking and environment variables

 ---
 Execution Order

 1. Create settings.gradle.kts for multi-project build
 2. Create shared-dto module with all DTOs
 3. Create user-service (entity, repo, service, controller, migrations, tests)
 4. Create tarot-service (entities, repos, service, controllers, migrations, tests)
 5. Create divination-service (entities, repos, Feign clients, service, controllers, migrations, tests)
 6. Add Dockerfiles for each service
 7. Update docker-compose.yml
 8. Test each service independently
 9. Test inter-service communication (full integration)

 ---
 Progress Tracking & Git Workflow

 After each significant step:
 1. Update PROGRESS.md with:
   - Current step completed
   - Any new context or learnings discovered during implementation
   - Issues encountered and how they were resolved
   - Next steps
 2. Create a git commit with a descriptive message

 Commit points:
 - After settings.gradle.kts + shared-dto module
 - After user-service complete
 - After tarot-service complete
 - After divination-service complete
 - After Dockerfiles + docker-compose.yml
 - After all tests pass