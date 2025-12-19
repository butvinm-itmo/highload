# 3-Role Authorization Model - Progress Report

**Date**: 2025-12-19
**Branch**: `auth`
**Status**: Phase 1/5 Complete ✅

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
**Commit**: Pending

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

## Pending Phases

### ⏳ Phase 2: Interpretation Authorization (MEDIUM/ADMIN only)
**Status**: Not Started

**Goal:** Restrict interpretation creation to MEDIUM and ADMIN roles only.

**Planned Changes:**
- Add `requireMediumOrAdmin()` helper to InterpretationController
- Add `X-User-Role` header parameter to `addInterpretation` endpoint
- Update all integration tests to include role header
- Add E2E test for USER role returning 403

**Files to Modify:**
- `divination-service/src/main/kotlin/com/github/butvinmitmo/divinationservice/controller/InterpretationController.kt`
- Integration tests for interpretation controller
- E2E tests (DivinationServiceE2ETest, CleanupAuthorizationE2ETest)

---

### ⏳ Phase 3: ADMIN Bypass for Author-Only Operations
**Status**: Not Started

**Goal:** Allow ADMIN to delete/update ANY spread or interpretation.

**Planned Changes:**
- Add `role: String` parameter to DivinationService methods (deleteSpread, updateInterpretation, deleteInterpretation)
- Update authorization checks: `if (authorId != userId && role != "ADMIN")`
- Update controllers to extract and pass X-User-Role header
- Add integration tests for ADMIN bypass
- Add E2E tests for ADMIN deleting non-authored resources

**Files to Modify:**
- `divination-service/src/main/kotlin/com/github/butvinmitmo/divinationservice/service/DivinationService.kt`
- `divination-service/src/main/kotlin/com/github/butvinmitmo/divinationservice/controller/SpreadController.kt`
- `divination-service/src/main/kotlin/com/github/butvinmitmo/divinationservice/controller/InterpretationController.kt`
- Integration tests
- E2E tests (CleanupAuthorizationE2ETest)

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

**User Service Tests:**
- AuthControllerIntegrationTest: 5/5 ✅
- UserControllerIntegrationTest: 8/8 ✅
- UserServiceIntegrationTest: 8/8 ✅
- JwtUtilTest: 7/7 ✅
- UserServiceTest: 12/12 ✅
- **Total: 40/40 tests passing** ✅

**E2E Tests:**
- CleanupAuthorizationE2ETest: 8/8 ✅
- DivinationServiceE2ETest: 11/11 ✅
- TarotServiceE2ETest: 6/6 ✅
- UserServiceE2ETest: 7/7 ✅
- **Total: 32/32 tests passing** ✅

**Expected Final Test Count (Phase 5):**
- ~45 tests total (32 existing + ~14 new role authorization tests)

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
**Completed Phases**: 1/5
**Next Phase**: Phase 2 - Interpretation Authorization
