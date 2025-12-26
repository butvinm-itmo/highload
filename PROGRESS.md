# Project Progress

## Current Status: UI Implementation + Backend Integration (Lab 4)

**Branch:** `ui`

**Last Updated:** 2025-12-26

---

## Completed Labs

### Lab 1: Monolith Implementation ✅
- REST API for Tarot readings
- Basic CRUD operations
- PostgreSQL database with Flyway migrations
- OpenAPI specifications

### Lab 2: Microservices Architecture ✅
- Decomposed into 8 services:
  - config-server (Spring Cloud Config)
  - eureka-server (Netflix Eureka)
  - gateway-service (Spring Cloud Gateway)
  - user-service (Spring MVC + JPA)
  - tarot-service (Spring MVC + JPA)
  - divination-service (Spring WebFlux + R2DBC)
  - notification-service (Spring WebFlux + R2DBC)
  - file-storage-service (Spring MVC + MinIO)
- Shared modules: shared-dto, shared-clients
- Service discovery with Eureka
- API Gateway with routing
- Resilience4j (circuit breaker, retry, time limiter)

### Lab 3: Authorization & Security ✅
- JWT authentication (HS256, 24h expiration)
- 3-role model: USER, MEDIUM, ADMIN
- Gateway-level JWT validation
- Role-based access control across all services
- Default admin account for development
- Password validation (min 8 chars, mixed case, digit, special char)

### Lab 4: Messaging & File Management ✅ (Backend Complete)
- **Kafka Event-Driven Architecture** (3 brokers in KRaft mode)
  - `spread-events` topic for spread creation
  - `interpretation-events` topic for interpretation creation
  - Reactive Kafka integration with reactor-kafka
- **notification-service** (port 8084)
  - In-app notifications via Kafka events
  - WebSocket real-time notifications (`/notifications/ws`)
  - REST API for notification management
  - PostgreSQL storage with R2DBC
- **file-storage-service** (port 8085)
  - MinIO object storage integration
  - File upload/download/delete endpoints
  - Support for interpretation attachments (PNG/JPG, 2MB max)
- **divination-service updates**
  - File attachment support for interpretations
  - Kafka event publishing for spreads and interpretations
  - Cascade file deletion on interpretation removal

---

## Current Work: UI Implementation (Lab 4 - UI Phase)

**Goal:** Build React + TypeScript SPA for the Tarology Web Service

**Approach:** Separate frontend service (port 3000) with Docker integration

### Stage 1: Project Foundation & Setup ✅ COMPLETED
- [x] Create PLAN.md with 10-stage implementation plan
- [x] Create PROGRESS.md to track implementation status
- [x] Initialize Vite + React + TypeScript project
- [x] Install dependencies (React Router, Axios, React Query, Tailwind CSS)
- [x] Configure Tailwind CSS
- [x] Set up project structure (api, components, pages, context, types, hooks, utils)
- [x] Create .env configuration for API gateway URL

### Stage 2: TypeScript Types & API Client ✅ COMPLETED
- [x] Create TypeScript types from shared-dto DTOs
- [x] Build Axios API client with JWT interceptor
- [x] Create API service modules (authApi, usersApi, cardsApi, spreadsApi, interpretationsApi)
- [x] Add error handling utilities

### Stage 3: Authentication & Route Protection ✅ COMPLETED
- [x] Create AuthContext with login/logout and token management
- [x] Build Login page with form validation and error handling
- [x] Implement ProtectedRoute component with role-based access
- [x] Set up React Router with authentication flow
- [x] Build Layout component with navbar and role badge
- [x] Configure React Query client

### Stage 4: Spreads Feed & Creation ✅ COMPLETED
- [x] Build spreads feed with infinite scroll using IntersectionObserver
- [x] Create spread creation modal with layout type selection
- [x] Build TarotCard component (text-based placeholder with arcana color)
- [x] Build SpreadCard component for feed display
- [x] Integrate React Query infinite queries
- [x] Add empty state for no spreads

### Stage 5: Spread Detail & Interpretations ✅ COMPLETED
- [x] Create SpreadDetailPage with full spread display
- [x] Display all cards with positions and reversed state
- [x] Build InterpretationList component with edit/delete actions
- [x] Create AddInterpretationForm (MEDIUM/ADMIN only, one per user)
- [x] Build EditInterpretationModal for updating interpretations
- [x] Implement delete spread functionality (author/ADMIN only)
- [x] Add role-based permissions for interpretation CRUD

### Stage 6: User Management (ADMIN) ✅ COMPLETED
- [x] Build UsersPage with paginated table
- [x] Create CreateUserModal with role selection
- [x] Build EditUserModal with optional password update
- [x] Create DeleteUserModal with cascade warning
- [x] Implement ADMIN-only route protection
- [x] Add pagination controls with page navigation

### Stage 7: UI Polish & Shared Components ✅ COMPLETED
- [x] Build Loading component with spinner animation
- [x] Create ErrorBoundary component with error recovery
- [x] Build EmptyState component with icons and actions
- [x] Integrate shared components into existing pages
- [x] Responsive design already implemented with Tailwind breakpoints

### Stage 8: Backend Integration (CORS) ✅ COMPLETED
- [x] Create CorsConfig class for gateway-service
- [x] Allow localhost:3000 origin for frontend development
- [x] Configure allowed methods (GET, POST, PUT, DELETE, OPTIONS)
- [x] Expose custom headers (X-Total-Count, X-After)
- [x] Enable credentials for JWT cookie support

### Stage 9: Docker Integration ✅ COMPLETED
- [x] Create multi-stage Dockerfile for frontend-service
- [x] Add nginx.conf for SPA routing and static serving
- [x] Create .dockerignore to exclude unnecessary files
- [x] Update docker-compose.yml to include frontend-service
- [x] Configure health check for frontend container
- [x] Map port 3000:80 for local development

### Stage 10: Documentation & Testing ✅ COMPLETED
- [x] Create comprehensive frontend README.md
- [x] Document all features and architecture
- [x] Add development and deployment instructions
- [x] Include troubleshooting guide
- [x] Document user roles and permissions
- [x] Update PROGRESS.md with final status

## 🎉 Basic UI Implementation Complete!

All 10 stages successfully completed. The Tarology Web Service now has a fully functional React + TypeScript frontend with:
- Complete authentication and authorization
- All CRUD operations for spreads and interpretations
- Admin user management
- Docker integration
- Production-ready deployment

---

## Current Phase: Integrating Backend Messaging & File Features

**Started:** 2025-12-25
**Branch:** `ui` (merging backend changes from `master`)

**Backend Changes Merged:**
- ✅ New file-storage-service (MinIO-based, port 8085)
- ✅ New notification-service (Kafka + WebSocket, port 8084)
- ✅ InterpretationDto updated with optional `fileUrl` field
- ✅ Event-driven architecture with Kafka (3 brokers in KRaft mode)
- ✅ WebSocket endpoint at `/api/v0.0.1/notifications/ws`

**Frontend Integration Plan:** 10 independent stages

### Stage 1: Update TypeScript Types & API Client ⏸️ PENDING
- [ ] Update InterpretationDto with fileUrl field
- [ ] Add NotificationDto and related types
- [ ] Add FileUploadResponse type
- [ ] Ensure type safety across all API calls

### Stage 2: Create File Storage API Module ⏸️ PENDING
- [ ] Create filesApi.ts with upload/download/delete
- [ ] Add file validation utilities
- [ ] Configure Axios for multipart/form-data

### Stage 3: Add File Attachment to Interpretations ⏸️ PENDING
- [ ] Update AddInterpretationForm with file upload
- [ ] Update EditInterpretationModal for attachments
- [ ] Update InterpretationList to display images
- [ ] Create ImageLightbox component

### Stage 4: Create Notifications API Module ⏸️ PENDING
- [ ] Create notificationsApi.ts
- [ ] Add React Query hooks for notifications
- [ ] Implement optimistic updates

### Stage 5: Add WebSocket Support ⏸️ PENDING
- [ ] Create WebSocket client utility
- [ ] Create NotificationContext
- [ ] Integrate with React Query

### Stage 6: Build Notifications UI Components ⏸️ PENDING
- [ ] Create NotificationBell component
- [ ] Create NotificationsPage
- [ ] Add toast notifications

### Stage 7: CORS Verification ⏸️ PENDING
- [ ] Verify WebSocket support in CORS config
- [ ] Test file upload CORS
- [ ] Test file download headers

### Stage 8: Docker Verification ⏸️ PENDING
- [ ] Verify all services in docker-compose.yml
- [ ] Test full stack in Docker
- [ ] Verify WebSocket in Docker

### Stage 9: Loading States & Error Handling ⏸️ PENDING
- [ ] Add file upload progress indicators
- [ ] Add WebSocket error handling
- [ ] Add empty states for notifications

### Stage 10: Documentation ⏸️ PENDING
- [ ] Update frontend-service/README.md
- [ ] Update PROGRESS.md with completion
- [ ] Update CLAUDE.md with new patterns

---

## Testing Status

### Backend Tests
- ✅ Unit tests passing for all services
- ✅ Integration tests passing with TestContainers
- ✅ E2E tests: 31 tests passing (including file attachments and notifications)
- ✅ Kafka integration tests passing
- ✅ WebSocket E2E tests passing

### Frontend Tests
- ⏸️ Not yet implemented

---

## Known Issues

None currently identified.

---

## Notes

- All backend services are production-ready
- Database schema is stable with proper migrations
- API endpoints are fully documented in CLAUDE.md
- Docker Compose orchestration includes all 8 services + infrastructure
- Frontend implementation follows CLAUDE.md guidelines
- Kafka cluster running in KRaft mode (no Zookeeper)
- MinIO object storage configured and healthy

---

## Recent Backend Work (Merged from master)

### Bug Fix: fileUrl is null in GET /spreads/{id} response ✅ COMPLETED

### Problem Statement

The `GET /spreads/{id}` endpoint returns `fileUrl: null` for all interpretations, even when files are attached. However, `GET /spreads/{spreadId}/interpretations` correctly returns the `fileUrl`.

### Root Cause

The `SpreadMapper.toDto()` method creates `InterpretationDto` objects inline without using `InterpretationMapper`, and **omits the `fileUrl` field entirely**. The `InterpretationDto` data class has `fileUrl: String? = null` as a default parameter, so when not provided, it defaults to `null`.

**Buggy code location:** `divination-service/src/main/kotlin/com/github/butvinmitmo/divinationservice/mapper/SpreadMapper.kt` (lines 48-64)

### Solution

Refactor `SpreadMapper` to use `InterpretationMapper` for creating `InterpretationDto` objects, ensuring consistent behavior across all endpoints.

---

### Phase 1: Refactor SpreadMapper to use InterpretationMapper

- **Goal:** Eliminate code duplication and ensure `fileUrl` is consistently included in interpretation DTOs.

- **Scope:**
  - Modify: `divination-service/src/main/kotlin/com/github/butvinmitmo/divinationservice/mapper/SpreadMapper.kt`

- **Changes Required:**
  1. Inject `InterpretationMapper` into `SpreadMapper` constructor
  2. Replace inline `InterpretationDto` creation (lines 48-64) with call to `interpretationMapper.toDto(interpretation)`

- **Test Strategy:**
  - Existing unit tests should pass (verify no regression)

- **Verification Cmd:**
  ```bash
  ./gradlew :divination-service:test
  ```

- **Phase Execution:**
  1. Implement: Modify `SpreadMapper` to inject and use `InterpretationMapper`
  2. Verify: Run `./gradlew :divination-service:test` - all tests must pass
  3. Report: Update PROGRESS.md with completion status
  4. Commit: Stage only `divination-service/src/main/kotlin/.../mapper/SpreadMapper.kt`

---

### Phase 2: Add Integration Test for fileUrl in GET /spreads/{id}

- **Goal:** Ensure the bug is covered by an automated test to prevent regression.

- **Scope:**
  - Modify: `divination-service/src/test/kotlin/com/github/butvinmitmo/divinationservice/integration/controller/SpreadControllerIntegrationTest.kt`

- **Changes Required:**
  1. Add test method `getSpread should return interpretations with fileUrl`
  2. Create spread, add interpretation, set `fileKey` on interpretation entity directly in test
  3. Call `GET /spreads/{id}` and verify `interpretations[0].fileUrl` is not null and contains expected path

- **Test Strategy:**
  - New test specifically validates `fileUrl` is present in spread details response

- **Verification Cmd:**
  ```bash
  ./gradlew :divination-service:test --tests "*SpreadControllerIntegrationTest*"
  ```

- **Phase Execution:**
  1. Implement: Add integration test
  2. Verify: Run the new test - must pass
  3. Report: Update PROGRESS.md with completion status
  4. Commit: Stage only `divination-service/src/test/kotlin/.../SpreadControllerIntegrationTest.kt`

---

### Phase 3: E2E Test Verification

- **Goal:** Verify the fix works end-to-end through the gateway.

- **Scope:**
  - Modify: `e2e-tests/src/test/kotlin/com/github/butvinmitmo/e2e/` (appropriate test file)

- **Changes Required:**
  1. Add test method that:
     - Creates spread
     - Creates interpretation
     - Uploads file to interpretation
     - Calls `GET /spreads/{id}` and verifies `interpretations[0].fileUrl` is present

- **Test Strategy:**
  - E2E test validates the complete flow through gateway

- **Verification Cmd:**
  ```bash
  ./gradlew :e2e-tests:test
  ```

- **Phase Execution:**
  1. Implement: Add E2E test for spread details endpoint
  2. Verify: Run E2E tests - must pass
  3. Report: Update PROGRESS.md with completion status
  4. Commit: Stage only E2E test file

---

### Phase 4: Final Verification and Documentation

- **Goal:** Run full test suite and update documentation.

- **Scope:**
  - Run all tests
  - Update CLAUDE.md if any new patterns established

- **Verification Cmd:**
  ```bash
  ./gradlew build
  ./gradlew :e2e-tests:test
  ```

- **Phase Execution:**
  1. Implement: N/A (verification only)
  2. Verify: CI passes (ktlint, unit tests, integration tests, E2E tests)
  3. Report: Mark all phases complete in PROGRESS.md
  4. Commit: Final commit with any documentation updates

---

### Implementation Details

#### SpreadMapper.kt Change (Phase 1)

**Before (buggy):**
```kotlin
@Component
class SpreadMapper(
    private val userServiceClient: UserServiceClient,
    private val tarotServiceClient: TarotServiceClient,
) {
    // ...
    fun toDto(...): SpreadDto {
        // ...
        return SpreadDto(
            // ...
            interpretations =
                interpretations.map { interpretation ->
                    val interpAuthor = userServiceClient.getUserById(...)
                    InterpretationDto(
                        id = interpretation.id!!,
                        text = interpretation.text,
                        author = interpAuthor,
                        spreadId = interpretation.spreadId,
                        createdAt = interpretation.createdAt!!,
                        // BUG: fileUrl is missing!
                    )
                },
            // ...
        )
    }
}
```

**After (fixed):**
```kotlin
@Component
class SpreadMapper(
    private val userServiceClient: UserServiceClient,
    private val tarotServiceClient: TarotServiceClient,
    private val interpretationMapper: InterpretationMapper,  // ADD THIS
) {
    // ...
    fun toDto(...): SpreadDto {
        // ...
        return SpreadDto(
            // ...
            interpretations = interpretations.map { interpretationMapper.toDto(it) },  // USE MAPPER
            // ...
        )
    }
}
```

---

### Risk Assessment

- **Low Risk:** The fix is straightforward - injecting an existing mapper and using it instead of inline DTO creation
- **No Breaking Changes:** The `InterpretationDto` contract remains unchanged; we're just populating the `fileUrl` field that was previously always null
- **Existing Tests:** All existing tests should continue to pass; the fix only adds data that was missing
- **CI/CD Impact:** None - no changes to Docker, CI configuration, or deployment

---

### TODO Checklist

- [x] Phase 1: Refactor SpreadMapper to use InterpretationMapper (COMPLETE)
  - [x] Implement: Modify SpreadMapper to inject and use InterpretationMapper
  - [x] Verify: Run `./gradlew :divination-service:test` - 46 tests passed
  - [x] Report: Update PROGRESS.md
  - [x] Commit: 80fae2b - Fix fileUrl being null in GET /spreads/{id} response

- [x] Phase 2: Add Integration Test for fileUrl in GET /spreads/{id} (COMPLETE)
  - [x] Implement: Add integration test to SpreadControllerIntegrationTest
  - [x] Verify: Run `./gradlew :divination-service:test --tests "*SpreadControllerIntegrationTest*"` - 8 tests passed
  - [x] Report: Update PROGRESS.md
  - [x] Commit: 8fd1128 - Add integration test for fileUrl in GET /spreads/{id} response

- [x] Phase 3: E2E Test Verification (COMPLETE)
  - [x] Implement: Add E2E test for fileUrl in spread details
  - [x] Verify: Run `./gradlew :e2e-tests:test` - All E2E tests passed
  - [x] Report: Update PROGRESS.md
  - [x] Commit: 28180c8 - Add E2E test for fileUrl in spread details endpoint

- [x] Phase 4: Final Verification and Documentation (COMPLETE)
  - [x] Verify: All tests passed
  - [x] Report: Mark all phases complete in PROGRESS.md
  - [x] Commit: b6851b7 - Update PROGRESS.md - fileUrl bug fix complete

---

## Previous Work: Centralized Authorization with Spring Security @PreAuthorize (COMPLETE)

### Architecture Summary

- **@PreAuthorize for role-based checks:**
  - `@PreAuthorize("hasRole('ADMIN')")` - ADMIN-only operations
  - `@PreAuthorize("hasAnyRole('MEDIUM', 'ADMIN')")` - MEDIUM or ADMIN
- **Service-layer for owner-based checks:**
  - Owner-or-admin checks remain in service (avoids duplicate DB lookups in reactive code)
  - Throws `ForbiddenException` which maps to 403
- **Shared security infrastructure:**
  - `shared-security` module with `UserPrincipal` and `HeaderAuthentication`
  - `HeaderAuthenticationFilter` (MVC) / `HeaderAuthenticationWebFilter` (WebFlux)
  - `SecurityConfig` with `@EnableMethodSecurity` / `@EnableReactiveMethodSecurity`

---

## Previous Work: File Attachment for Interpretations (COMPLETE)

See git history for details on file-storage-service implementation.
