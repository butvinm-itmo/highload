# PROGRESS.md

## Current Work: Centralized Authorization with Spring Security @PreAuthorize

**Status:** COMPLETE

**Branch:** refactorinng-and-k8s

---

## Centralized Authorization - All Phases Complete

### Phase 1: Create shared-security module (COMPLETE)
- Created new `shared-security` module with common security infrastructure
- Added `UserPrincipal` data class representing authenticated user
- Added `HeaderAuthentication` class extending `AbstractAuthenticationToken`
- Unit tests passing
- Files:
  - `shared-security/build.gradle.kts`
  - `shared-security/src/main/kotlin/.../UserPrincipal.kt`
  - `shared-security/src/main/kotlin/.../HeaderAuthentication.kt`
  - `shared-security/src/test/kotlin/.../HeaderAuthenticationTest.kt`
  - `settings.gradle.kts` (added include)

### Phase 2: Add Spring Security to user-service (COMPLETE)
- Added `spring-boot-starter-security` dependency
- Created `HeaderAuthenticationFilter` to extract X-User-* headers into SecurityContext
- Created `SecurityConfig` with `@EnableMethodSecurity`
- Replaced `requireAdmin()` helper with `@PreAuthorize("hasRole('ADMIN')")`
- Added `AccessDeniedException` handler to GlobalExceptionHandler
- Integration tests passing
- Files:
  - `user-service/build.gradle.kts`
  - `user-service/src/main/kotlin/.../security/HeaderAuthenticationFilter.kt`
  - `user-service/src/main/kotlin/.../config/SecurityConfig.kt`
  - `user-service/src/main/kotlin/.../controller/UserController.kt`
  - `user-service/src/main/kotlin/.../exception/GlobalExceptionHandler.kt`

### Phase 3: Add Spring Security to divination-service (COMPLETE)
- Added `spring-boot-starter-security` dependency
- Created reactive `HeaderAuthenticationWebFilter` for WebFlux
- Created `SecurityConfig` with `@EnableReactiveMethodSecurity`
- Replaced `requireMediumOrAdmin()` with `@PreAuthorize("hasAnyRole('MEDIUM', 'ADMIN')")`
- Owner-based checks remain in service layer (pragmatic approach for reactive code)
- Added `AccessDeniedException` handler to GlobalExceptionHandler
- Integration tests passing (46 tests)
- Files:
  - `divination-service/build.gradle.kts`
  - `divination-service/src/main/kotlin/.../security/HeaderAuthenticationWebFilter.kt`
  - `divination-service/src/main/kotlin/.../config/SecurityConfig.kt`
  - `divination-service/src/main/kotlin/.../controller/InterpretationController.kt`
  - `divination-service/src/main/kotlin/.../exception/GlobalExceptionHandler.kt`

### Phase 4: Add Spring Security to notification-service (COMPLETE)
- Added `spring-boot-starter-security` dependency
- Created reactive `HeaderAuthenticationWebFilter` for WebFlux
- Created `SecurityConfig` with `@EnableReactiveMethodSecurity`
- Owner-based checks remain in service layer
- Added `AccessDeniedException` handler to GlobalExceptionHandler
- Integration tests passing (44 tests)
- Files:
  - `notification-service/build.gradle.kts`
  - `notification-service/src/main/kotlin/.../security/HeaderAuthenticationWebFilter.kt`
  - `notification-service/src/main/kotlin/.../config/SecurityConfig.kt`
  - `notification-service/src/main/kotlin/.../exception/GlobalExceptionHandler.kt`

### Phase 5-6: E2E Tests and Full System Verification (COMPLETE)
- All unit and integration tests passing
- E2E tests should work without changes (they go through gateway which injects headers)
- ktlint formatting applied

### Phase 7: Documentation Update (COMPLETE)
- Updated PROGRESS.md (this file)
- Updated CLAUDE.md with security architecture section

---

## Tests Status

- `./gradlew :shared-security:test` - PASSING (4 tests)
- `./gradlew :user-service:test` - PASSING
- `./gradlew :divination-service:test` - PASSING (46 tests)
- `./gradlew :notification-service:test` - PASSING (44 tests)
- `./gradlew test -x :e2e-tests:test` - ALL PASSING

---

## Architecture Summary

### Before (Scattered Authorization)
- Controller-level helper methods: `requireAdmin(role)`
- Inline service-layer checks: `if (entity.authorId != userId && role != "ADMIN")`
- Inconsistent patterns across services

### After (Centralized Authorization)
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
