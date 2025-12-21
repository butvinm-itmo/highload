# 3-Role Authorization Model - Progress Report

**Date**: 2025-12-19
**Branch**: `auth`
**Status**: Complete ✅ (5/5 Phases)

---

## Implementation Summary

Introducing a 3-role authorization model to replace the current 2-role system (USER, ADMIN):

### New Role Permissions
- **USER**: create spreads, read spreads/interpretations, get users (CANNOT add interpretations)
- **MEDIUM**: all USER permissions + add interpretations to any spread
- **ADMIN**: complete system access (user CRUD, create spreads/interpretations, bypass author-only checks)

### Key Changes
- Only MEDIUM and ADMIN can add interpretations (breaking change from current behavior)
- ADMIN can edit/delete ANY spread or interpretation (bypass author-only checks)
- Existing users remain as USER role (no migration)
- MEDIUM can create spreads and add interpretations

---

## Completed Work

### ✅ Phase 1: Database Migration - Add MEDIUM Role

**Status**: Complete ✅
**Commit**: `be55436` - "Add MEDIUM role to database and RoleType enum"

**Goal:** Add MEDIUM role to database without changing application behavior.

**Changes:**

1. **Updated RoleType enum** (`user-service/src/main/kotlin/com/github/butvinmitmo/userservice/entity/Role.kt`)
   - Added `MEDIUM` enum value between USER and ADMIN
   - Added `MEDIUM_ID` constant: `UUID.fromString("00000000-0000-0000-0000-000000000003")`

   ```kotlin
   enum class RoleType {
       USER,
       MEDIUM,  // NEW
       ADMIN,
       ;

       companion object {
           val USER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
           val MEDIUM_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000003")  // NEW
           val ADMIN_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000002")
       }
   }
   ```

2. **Created database migration** (`user-service/src/main/resources/db/migration/V5__add_medium_role.sql`)
   ```sql
   -- V5: Add MEDIUM role for 3-role authorization model
   -- MEDIUM users can create spreads and add interpretations
   INSERT INTO role (id, name) VALUES
       ('00000000-0000-0000-0000-000000000003', 'MEDIUM');
   ```

**Files Modified:**
- `user-service/src/main/kotlin/com/github/butvinmitmo/userservice/entity/Role.kt`
- `user-service/src/main/resources/db/migration/V5__add_medium_role.sql` (NEW)

**Testing:**
```bash
./gradlew :user-service:test
# ✅ All 40 tests passing

./gradlew :e2e-tests:test
# ✅ All 32 tests passing
```

**Why tests pass:** MEDIUM role exists in database but isn't referenced by any code yet. No behavior changes.

---

### ✅ Phase 2: Interpretation Authorization (MEDIUM/ADMIN only)

**Status**: Complete ✅
**Commits**: `f9264f4` - "Fix inter-service Feign client communication", `3f6a273` - "Restrict interpretation creation to MEDIUM and ADMIN roles"

**Goal:** Restrict interpretation creation to MEDIUM and ADMIN roles only.

**Changes:**

1. **Added role check to InterpretationController** (`divination-service/src/main/kotlin/com/github/butvinmitmo/divinationservice/controller/InterpretationController.kt`)
   - Added `requireMediumOrAdmin(role: String?)` helper method
   - Updated `addInterpretation` to accept `X-User-Role` header parameter
   - Added role check before creating interpretation
   - Updated OpenAPI documentation with 403 response

   ```kotlin
   private fun requireMediumOrAdmin(role: String?) {
       if (role != "MEDIUM" && role != "ADMIN") {
           throw ForbiddenException("Only MEDIUM and ADMIN users can create interpretations")
       }
   }

   fun addInterpretation(
       @PathVariable spreadId: UUID,
       @RequestHeader("X-User-Id") userId: UUID,
       @RequestHeader("X-User-Role") role: String,  // NEW
       @Valid @RequestBody request: CreateInterpretationRequest,
   ): Mono<ResponseEntity<CreateInterpretationResponse>> {
       requireMediumOrAdmin(role)  // NEW
       return divinationService.addInterpretation(spreadId, request, userId)...
   }
   ```

2. **Updated integration tests** (`divination-service/src/test/kotlin/com/github/butvinmitmo/divinationservice/integration/controller/InterpretationControllerIntegrationTest.kt`)
   - Added `.header("X-User-Role", "ADMIN")` to all POST interpretation requests (9 occurrences)
   - Added new test: `addInterpretation should return 403 when user is USER role`

3. **Updated E2E tests**
   - `CleanupAuthorizationE2ETest.kt`: Temporarily changed test to use admin instead of userB for interpretation creation (will be fixed in Phase 4 when MEDIUM users can be created)
   - `DivinationServiceE2ETest.kt`: Added new test `POST interpretation as USER should return 403`

**Files Modified:**
- `divination-service/src/main/kotlin/com/github/butvinmitmo/divinationservice/controller/InterpretationController.kt`
- `divination-service/src/test/kotlin/com/github/butvinmitmo/divinationservice/integration/controller/InterpretationControllerIntegrationTest.kt`
- `e2e-tests/src/test/kotlin/com/github/butvinmitmo/e2e/CleanupAuthorizationE2ETest.kt`
- `e2e-tests/src/test/kotlin/com/github/butvinmitmo/e2e/DivinationServiceE2ETest.kt`

**Testing:**
```bash
./gradlew :divination-service:test
# ✅ All 36 tests passing (including new test)

./gradlew :e2e-tests:test
# ✅ All 33 E2E tests passing
```

**Additional Fix:**
Fixed inter-service Feign client communication by making X-User-Id header optional (nullable) for GET endpoints. This allows divination-service to call user-service and tarot-service without authentication headers when bypassing the gateway.

**Breaking Change:** USER role can no longer create interpretations. Only MEDIUM and ADMIN roles can add interpretations.

---

### ✅ Phase 3: ADMIN Bypass for Author-Only Operations

**Status**: Complete ✅
**Commit**: `8fe341f` - "Add ADMIN bypass for author-only operations"

**Goal:** Allow ADMIN to delete/update ANY spread or interpretation.

**Changes:**

1. **Updated DivinationService methods** (`divination-service/src/main/kotlin/com/github/butvinmitmo/divinationservice/service/DivinationService.kt`)
   - `deleteSpread(id, userId, role)` - Added role parameter, ADMIN bypass check
   - `updateInterpretation(spreadId, id, userId, role, request)` - Added role parameter, ADMIN bypass check
   - `deleteInterpretation(spreadId, id, userId, role)` - Added role parameter, ADMIN bypass check

   Authorization logic:
   ```kotlin
   if (authorId != userId && role != "ADMIN") {
       throw ForbiddenException("You can only {operation} your own {resources}")
   }
   ```

2. **Updated Controllers**
   - `SpreadController.deleteSpread()` - Added X-User-Role header parameter
   - `InterpretationController.updateInterpretation()` - Added X-User-Role header parameter
   - `InterpretationController.deleteInterpretation()` - Added X-User-Role header parameter

3. **Updated Integration Tests**
   - `SpreadControllerIntegrationTest.kt` - Added X-User-Role headers to DELETE requests
   - `InterpretationControllerIntegrationTest.kt` - Added X-User-Role headers to PUT/DELETE requests

4. **Updated Unit Tests**
   - `DivinationServiceTest.kt` - Updated all service method calls to include role parameter

**Files Modified:**
- `divination-service/src/main/kotlin/com/github/butvinmitmo/divinationservice/service/DivinationService.kt`
- `divination-service/src/main/kotlin/com/github/butvinmitmo/divinationservice/controller/SpreadController.kt`
- `divination-service/src/main/kotlin/com/github/butvinmitmo/divinationservice/controller/InterpretationController.kt`
- `divination-service/src/test/kotlin/com/github/butvinmitmo/divinationservice/integration/controller/SpreadControllerIntegrationTest.kt`
- `divination-service/src/test/kotlin/com/github/butvinmitmo/divinationservice/integration/controller/InterpretationControllerIntegrationTest.kt`
- `divination-service/src/test/kotlin/com/github/butvinmitmo/divinationservice/unit/service/DivinationServiceTest.kt`

**Testing:**
```bash
./gradlew :divination-service:test
# ✅ All 36 tests passing
```

---

### ✅ Phase 4: Role Management for ADMIN

**Status**: Complete ✅
**Commit**: `a582c5a` - "Add role management for ADMIN users"

**Goal:** Allow ADMIN to create users with MEDIUM role and update user roles.

**Changes:**

1. **Updated DTOs** (`shared-dto/src/main/kotlin/com/github/butvinmitmo/shared/dto/UserDto.kt`)
   - Added optional `role: String?` field to `CreateUserRequest` (defaults to null → USER)
   - Added optional `role: String?` field to `UpdateUserRequest` (defaults to null → no change)
   - Added pattern validation: `^(USER|MEDIUM|ADMIN)$`

2. **Created RoleService** (`user-service/src/main/kotlin/com/github/butvinmitmo/userservice/service/RoleService.kt`)
   - Centralized role lookup by name
   - `getRoleByName(roleName: String?)` - defaults to USER when roleName is null
   - `getRoleByType(roleType: RoleType)` - lookup by enum
   - Throws `NotFoundException` for invalid roles

3. **Updated UserService** (`user-service/src/main/kotlin/com/github/butvinmitmo/userservice/service/UserService.kt`)
   - Injected RoleService
   - `createUser()` - accepts optional role parameter (defaults to USER via RoleService)
   - `updateUser()` - accepts optional role parameter
   - Uses RoleService for all role lookups

4. **Integration Tests** (`user-service/src/test/kotlin/com/github/butvinmitmo/userservice/integration/service/UserServiceIntegrationTest.kt`)
   - Added 6 new tests for role management:
     - `createUser should create user with MEDIUM role when specified`
     - `createUser should create user with ADMIN role when specified`
     - `createUser should default to USER role when role not specified`
     - `createUser should throw NotFoundException for invalid role`
     - `updateUser should update user role`
     - `updateUser should throw NotFoundException for invalid role`

5. **Unit Tests** (`user-service/src/test/kotlin/com/github/butvinmitmo/userservice/unit/service/UserServiceTest.kt`)
   - Added RoleService mock
   - Updated UserService instantiation to include RoleService
   - Updated createUser test to mock RoleService.getRoleByName()

6. **E2E Tests** (`e2e-tests/src/test/kotlin/com/github/butvinmitmo/e2e/CleanupAuthorizationE2ETest.kt`)
   - Updated userB creation to specify `role = "MEDIUM"`
   - Changed test to use userB (MEDIUM) instead of admin for creating interpretations
   - Updated test descriptions:
     - `UserB (MEDIUM) adds interpretation to UserA's spread`
     - `UserA cannot delete UserB's interpretation (403)`
     - `UserB can delete own interpretation (204)`

**Files Modified:**
- `shared-dto/src/main/kotlin/com/github/butvinmitmo/shared/dto/UserDto.kt`
- `user-service/src/main/kotlin/com/github/butvinmitmo/userservice/service/RoleService.kt` (NEW)
- `user-service/src/main/kotlin/com/github/butvinmitmo/userservice/service/UserService.kt`
- `user-service/src/test/kotlin/com/github/butvinmitmo/userservice/integration/service/UserServiceIntegrationTest.kt`
- `user-service/src/test/kotlin/com/github/butvinmitmo/userservice/unit/service/UserServiceTest.kt`
- `e2e-tests/src/test/kotlin/com/github/butvinmitmo/e2e/CleanupAuthorizationE2ETest.kt`

**Testing:**
```bash
./gradlew :user-service:test
# ✅ All 46 tests passing (was 40/40, added 6 new tests)

./gradlew :e2e-tests:test
# ✅ All 33 E2E tests passing
```

---

### ✅ Phase 5: Comprehensive Testing and Documentation

**Status**: Complete ✅

**Goal:** Add comprehensive role-based tests and update documentation.

**Changes:**

1. **Created RoleAuthorizationE2ETest** (`e2e-tests/src/test/kotlin/com/github/butvinmitmo/e2e/RoleAuthorizationE2ETest.kt`)
   - 22 comprehensive role-based tests covering:
     - USER: can create spreads, read spreads, get users, CANNOT create interpretations/users
     - MEDIUM: can create spreads, create interpretations, interpret others' spreads, CANNOT manage users
     - ADMIN: full access - create users with any role, bypass author checks for delete/update

2. **Updated CLAUDE.md documentation**
   - Added 3-role authorization table with permissions matrix
   - Updated Authorization Model section with detailed role descriptions
   - Updated API endpoints tables with Role column
   - Updated E2E test structure to include RoleAuthorizationE2ETest
   - Updated test coverage count to 52 E2E tests

**Files Modified:**
- `e2e-tests/src/test/kotlin/com/github/butvinmitmo/e2e/RoleAuthorizationE2ETest.kt` (NEW)
- `CLAUDE.md`

**Testing:**
```bash
./gradlew :e2e-tests:test
# ✅ All 55 E2E tests passing (was 33, added 22 new role tests)
```

---

## Test Status Summary

**Phase 1 Test Results:**
- User Service: 40/40 ✅
- E2E Tests: 32/32 ✅

**Phase 2 Test Results:**
- Divination Service: 36/36 ✅ (includes 1 new test for USER role 403)
  - CircuitBreakerIntegrationTest: 5/5 ✅
  - InterpretationControllerIntegrationTest: 9/9 ✅ (was 8/8)
  - SpreadControllerIntegrationTest: 7/7 ✅
  - DivinationServiceTest: 15/15 ✅
- E2E Tests: 33/33 ✅ (includes 1 new USER 403 test)
- Additional Fix: Inter-service Feign client communication (made X-User-Id nullable)

**Phase 3 Test Results:**
- Divination Service: 36/36 ✅ (all tests updated with X-User-Role headers)
  - Integration tests: Added X-User-Role headers to PUT/DELETE requests
  - Unit tests: Updated service method calls to include role parameter

**Phase 4 Test Results:**
- User Service: 46/46 ✅ (was 40/40, added 6 new role management tests)
  - Integration tests: Added 6 new role management tests
  - Unit tests: Updated with RoleService mock
- E2E Tests: 33/33 ✅ (CleanupAuthorizationE2ETest now uses MEDIUM user)

**Phase 5 Test Results:**
- E2E Tests: 55/55 ✅ (was 33, added 22 new role authorization tests)
  - RoleAuthorizationE2ETest: 22 tests covering USER, MEDIUM, ADMIN permissions
  - Total E2E coverage: User CRUD, tarot reference data, spreads, interpretations, role authorization

**Final Test Count:**
- User Service: 46 tests ✅
- Tarot Service: 7 tests ✅
- Divination Service: 36 tests ✅
- E2E Tests: 55 tests ✅
- **Total: 144 tests ✅**

---

## Commands

### Build and Test
```bash
# Build all modules
./gradlew build

# Test specific module
./gradlew :user-service:test
./gradlew :divination-service:test

# E2E tests (requires services running)
docker compose up -d
./gradlew :e2e-tests:test
docker compose down
```

### Git History
```bash
git log --oneline auth
# Will show commits for each phase
```

---

## Implementation Strategy

### Why 5 Phases?
Each phase is:
1. **Independently testable**: All tests pass after each phase
2. **Committable**: Can be committed separately with passing tests
3. **Non-breaking**: Except Phase 2 (intentional breaking change for interpretation creation)

### Testing Approach
- Integration tests use mock headers (X-User-Id, X-User-Role)
- E2E tests use real JWT authentication via AuthContext
- No ad-hoc patches or test disabling
- Proper solutions only

---

**Last Updated**: 2025-12-19
**Branch**: `auth`
**Completed Phases**: 5/5 ✅
**Status**: Implementation Complete - Ready for Review
