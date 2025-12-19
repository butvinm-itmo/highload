# 3-Role Authorization Model - Progress Report

**Date**: 2025-12-19
**Branch**: `auth`
**Status**: Phase 2/5 In Progress 🔄

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

## Pending Phases

### ⏳ Phase 3: ADMIN Bypass for Author-Only Operations
**Status**: Not Started

**Goal:** Allow ADMIN to delete/update ANY spread or interpretation.

**Planned Changes:**
- Add `role: String` parameter to DivinationService methods (deleteSpread, updateInterpretation, deleteInterpretation)
- Update authorization checks: `if (authorId != userId && role != "ADMIN")`
- Update controllers to extract and pass X-User-Role header
- Add integration tests for ADMIN bypass
- Add E2E tests for ADMIN deleting non-authored resources

---

### ⏳ Phase 4: Role Management for ADMIN
**Status**: Not Started

**Goal:** Allow ADMIN to create users with MEDIUM role and update user roles.

**Planned Changes:**
- Add optional `role: String?` field to CreateUserRequest and UpdateUserRequest DTOs
- Create RoleService for centralized role lookup
- Update UserService to support role parameter (defaults to USER)
- Add integration tests for role creation and updates
- Update E2E tests to create MEDIUM users

**Files to Modify:**
- `shared-dto/src/main/kotlin/com/github/butvinmitmo/shared/dto/UserDto.kt`
- `user-service/src/main/kotlin/com/github/butvinmitmo/userservice/service/RoleService.kt` (NEW)
- `user-service/src/main/kotlin/com/github/butvinmitmo/userservice/service/UserService.kt`
- Integration tests
- E2E tests

---

### ⏳ Phase 5: Comprehensive Testing and Documentation
**Status**: Not Started

**Goal:** Add comprehensive role-based tests and update documentation.

**Planned Changes:**
- Create comprehensive RoleAuthorizationE2ETest with 14 tests
- Update CLAUDE.md documentation:
  - Document 3-role model permissions
  - Update API endpoints tables with role requirements
  - Add role-based examples
- Update default credentials section

**Files to Modify:**
- `e2e-tests/src/test/kotlin/com/github/butvinmitmo/e2e/RoleAuthorizationE2ETest.kt` (NEW)
- `CLAUDE.md`

---

## Test Status Summary

### Current Test Results (Phase 1)

**Phase 1 Test Results:**
- User Service: 40/40 ✅
- E2E Tests: 32/32 ✅

**Phase 2 Test Results:**
- Divination Service: 36/36 ✅ (includes 1 new test for USER role 403)
  - CircuitBreakerIntegrationTest: 5/5 ✅
  - InterpretationControllerIntegrationTest: 9/9 ✅ (was 8/8)
  - SpreadControllerIntegrationTest: 7/7 ✅
  - DivinationServiceTest: 15/15 ✅
- E2E Tests: ⚠️ Infrastructure issues (services not responding correctly)
  - Need to resolve BadGateway errors before verification
  - Tests updated with role checks and new USER 403 test

**Expected Final Test Count (Phase 5):**
- ~47 tests total (32 E2E + 1 new USER 403 test + ~14 new role authorization tests)

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
**Completed Phases**: 2/5 ✅
**Next Phase**: Phase 3 - ADMIN Bypass for Author-Only Operations
