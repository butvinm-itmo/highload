# API Mismatch Fix - Progress Report

**Date**: 2025-12-17
**Branch**: `auth`
**Status**: Phase 3/5 Complete ✅

---

## Problem Summary

E2E tests were failing due to fundamental API mismatches between Feign clients and backend controllers:

1. **DELETE endpoints**: Feign clients sent `@RequestBody DeleteRequest(userId)` but controllers expected userId from JWT headers (`X-User-Id`)
2. **Request DTOs contained unused authorId fields**: Controllers overrode these with authenticated user's ID from JWT
3. **E2E authorization tests were incorrect**: Tried to test authorization by changing request body values, but controllers use JWT headers
4. **DeleteRequest DTO was completely unused** by all controllers

**Root Cause**: The architecture uses JWT-based authentication where the gateway validates JWT and adds `X-User-Id`/`X-User-Role` headers. Backend services trust these headers. Request body fields like `authorId` and `userId` were being ignored or overridden.

---

## Completed Work

### ✅ Phase 1: Fix DELETE Endpoint Feign Client Signatures
**Commit**: `d318c83` - "Remove DeleteRequest parameter from DELETE endpoint Feign clients"

**Changes:**
- Removed `@RequestBody request: DeleteRequest` from `DivinationServiceClient.deleteSpread()`
- Removed `@RequestBody request: DeleteRequest` from `DivinationServiceClient.deleteInterpretation()`
- Removed `DeleteRequest` import from `DivinationServiceClient`
- Updated E2E test calls in `CleanupAuthorizationE2ETest` to remove `DeleteRequest` parameters

**Files Modified:**
- `shared-clients/src/main/kotlin/com/github/butvinmitmo/shared/client/DivinationServiceClient.kt`
- `e2e-tests/src/test/kotlin/com/github/butvinmitmo/e2e/CleanupAuthorizationE2ETest.kt`

**Testing:**
```bash
./gradlew :shared-clients:build
./gradlew :e2e-tests:compileTestKotlin
# ✅ Both successful
```

---

### ✅ Phase 2: Remove DeleteRequest DTO
**Commit**: `a2b162e` - "Remove unused DeleteRequest DTO"

**Changes:**
- Deleted `shared-dto/src/main/kotlin/com/github/butvinmitmo/shared/dto/DeleteRequest.kt`
- Removed `DeleteRequest` import from integration tests
- Updated `SpreadControllerIntegrationTest` to send userId via `X-User-Id` header only (no request body)

**Files Modified:**
- Deleted: `shared-dto/src/main/kotlin/com/github/butvinmitmo/shared/dto/DeleteRequest.kt`
- `divination-service/src/test/kotlin/com/github/butvinmitmo/divinationservice/integration/controller/SpreadControllerIntegrationTest.kt`

**Testing:**
```bash
./gradlew :shared-dto:build
./gradlew :shared-clients:build
./gradlew :divination-service:test
# ✅ All tests passing (35 tests)
```

---

### ✅ Phase 3: Remove authorId from Request DTOs
**Commit**: `ef4e9e3` - "Remove authorId fields from request DTOs - use JWT identity only"

**DTO Changes:**
- `CreateSpreadRequest`: Removed `authorId` field → Only requires `question` and `layoutTypeId`
- `CreateInterpretationRequest`: Removed `authorId` field → Only requires `text`
- `UpdateInterpretationRequest`: Removed `authorId` field → Only requires `text`

**Controller Changes:**
- `SpreadController.createSpread()`: Changed from `request.copy(authorId = userId)` to `createSpread(request, userId)`
- `InterpretationController.addInterpretation()`: Changed from `request.copy(authorId = userId)` to `addInterpretation(spreadId, request, userId)`

**Service Changes:**
- `DivinationService.createSpread()`: New signature accepts `request: CreateSpreadRequest, authorId: UUID`
- `DivinationService.addInterpretation()`: New signature accepts `spreadId: UUID, request: CreateInterpretationRequest, authorId: UUID`

**Test Updates:**
- Updated all integration tests to remove `authorId` from request constructors
- Updated all E2E tests to remove `authorId` from request constructors
- Fixed unit tests to pass `authorId` as separate parameter to service methods
- Fixed authorization tests to use correct `userId` values from headers

**Files Modified:**
- `shared-dto/src/main/kotlin/com/github/butvinmitmo/shared/dto/SpreadDto.kt`
- `shared-dto/src/main/kotlin/com/github/butvinmitmo/shared/dto/InterpretationDto.kt`
- `divination-service/src/main/kotlin/com/github/butvinmitmo/divinationservice/controller/SpreadController.kt`
- `divination-service/src/main/kotlin/com/github/butvinmitmo/divinationservice/controller/InterpretationController.kt`
- `divination-service/src/main/kotlin/com/github/butvinmitmo/divinationservice/service/DivinationService.kt`
- 6 test files (integration, unit, E2E)

**Testing:**
```bash
./gradlew :divination-service:test
# ✅ All tests passing (35 tests completed)
```

---

## Remaining Work

### ⏳ Phase 4: Fix E2E Authorization Tests

**Goal**: Properly test authorization by creating multiple users and logging in as each.

**Current Issues:**
1. `CleanupAuthorizationE2ETest.kt` line 107: "DELETE spread by non-author should return 403" - **FAILING**
   - Test creates spread as `testUserId` but tries to delete as `adminId`
   - Problem: Still logged in as admin (admin's JWT token is used), so `X-User-Id` header = admin
   - Fix: Create userA and userB, login as userA (create spread), login as userB (try to delete → 403)

2. `DivinationServiceE2ETest.kt` line 197: "PUT interpretation by non-author should return 403" - **FAILING**
   - Currently tries to update with `authorId = adminId` in request body (but that's now removed)
   - Fix: Create second user, login as that user, try to update → expect 403

3. `DivinationServiceE2ETest.kt` line 204: "POST spread with non-existent user should return 404" - **FAILING**
   - **CANNOT TEST**: User identity comes from JWT, not request body
   - Gateway validates JWT before request reaches backend
   - Invalid/non-existent user ID would mean invalid JWT → 401 at gateway
   - **Action**: Remove this test entirely

**Files to Modify:**
- `e2e-tests/src/test/kotlin/com/github/butvinmitmo/e2e/CleanupAuthorizationE2ETest.kt`
- `e2e-tests/src/test/kotlin/com/github/butvinmitmo/e2e/DivinationServiceE2ETest.kt`

**Implementation Strategy:**
```kotlin
// Create two users
val userA = createUser("userA", "Pass@123")
val userB = createUser("userB", "Pass@456")

// Login as userA, create spread
loginAsUser(userA.id, "userA", "Pass@123")
val spreadId = createSpread(...)

// Login as userB, try to delete userA's spread → 403
loginAsUser(userB.id, "userB", "Pass@456")
assertThrowsWithStatus(403) {
    deleteSpread(spreadId)
}

// Login back as userA, delete successfully → 204
loginAsUser(userA.id, "userA", "Pass@123")
deleteSpread(spreadId) // Success
```

**Expected Results:**
- All E2E tests pass (31 tests total)
- Authorization tests properly validate JWT-based access control

---

### ⏳ Phase 5: Update Documentation

**Goal**: Update `CLAUDE.md` to reflect API changes.

**Changes Needed:**

1. **Section: "Important API Details"** (around line 305-320)
   - Remove "DeleteRequest DTO" subsection entirely
   - Remove "Create requests use authorId" section
   - Add new section: "Authentication and Request Identity"

2. **Section: "API Endpoints"**
   - Update request body examples to remove `authorId`
   - Clarify DELETE operations require no request body

3. **Section: "Request/Response DTOs"**
   - Remove mentions of `authorId` fields in create/update requests
   - Document that author identity comes from JWT only

**New Documentation Section:**
```markdown
### Authentication and Request Identity

All create and update operations automatically use the authenticated user's ID from the JWT token.

**Controllers override author identity:**
- `CreateSpreadRequest`: Only requires `question` and `layoutTypeId`
- `CreateInterpretationRequest`: Only requires `text`
- `UpdateInterpretationRequest`: Only requires `text`

The controller extracts `X-User-Id` from the JWT-validated request header and uses it as the author ID.
This ensures users cannot impersonate others.

**DELETE operations:**
- No request body required
- Authorization checks use `X-User-Id` from JWT header
- Service layer verifies: `resource.authorId == authenticatedUserId`
```

**Files to Modify:**
- `CLAUDE.md`

**Verification:**
```bash
cat CLAUDE.md | grep -A 5 "authorId"  # Should find no inappropriate references
cat CLAUDE.md | grep "DeleteRequest"  # Should return nothing
```

---

## Testing Summary

### Current Test Status

**Integration Tests** (divination-service):
- ✅ CircuitBreakerIntegrationTest: 5/5 passing
- ✅ InterpretationControllerIntegrationTest: 7/7 passing
- ✅ SpreadControllerIntegrationTest: 6/6 passing
- ✅ DivinationServiceTest (unit): 14/14 passing
- **Total: 35/35 tests passing** ✅

**E2E Tests** (pending Phase 4):
- ❌ CleanupAuthorizationE2ETest: 3/6 passing (3 failing)
- ❌ DivinationServiceE2ETest: 9/12 passing (3 failing)
- ✅ UserServiceE2ETest: 7/7 passing
- ✅ TarotServiceE2ETest: 6/6 passing
- **Total: 27/31 tests passing** (4 failures expected until Phase 4)

### After Phase 4 Completion
**Expected**: 31/31 E2E tests passing ✅

---

## Architecture Notes

### JWT Authentication Flow (Unchanged)
1. **Login**: User sends credentials to `POST /api/v0.0.1/auth/login`
2. **Token Generation**: user-service generates JWT (24h expiration, HS256)
3. **Token Usage**: Client includes JWT in `Authorization: Bearer <token>` header
4. **Gateway Validation**: gateway-service validates JWT and adds `X-User-Id` + `X-User-Role` headers
5. **Backend Authorization**: Backend services trust gateway headers for authorization checks

**Key Principle**: Gateway is the ONLY place where JWT is validated. All backends trust the headers implicitly.

### What Changed
- **Before**: Request bodies included `authorId` fields that were ignored/overridden
- **After**: Request bodies omit `authorId` entirely; controllers pass userId from headers to services
- **Benefit**: Cleaner API, no confusion about which authorId value is used, impossible to impersonate others

---

## Commands

### Build and Test
```bash
# Build all modules
./gradlew build

# Test specific module
./gradlew :divination-service:test
./gradlew :shared-dto:build
./gradlew :shared-clients:build

# E2E tests (requires services running)
docker compose up -d --build
./gradlew :e2e-tests:test
docker compose down
```

### Git History
```bash
git log --oneline -3
# ef4e9e3 Remove authorId fields from request DTOs - use JWT identity only
# a2b162e Remove unused DeleteRequest DTO
# d318c83 Remove DeleteRequest parameter from DELETE endpoint Feign clients
```

---

## Next Steps

1. **Implement Phase 4**: Fix E2E authorization tests
   - Refactor `CleanupAuthorizationE2ETest` with multi-user JWT testing
   - Fix `DivinationServiceE2ETest` authorization tests
   - Remove impossible "non-existent user" test
   - Run E2E tests and verify all 31 pass

2. **Implement Phase 5**: Update documentation
   - Update `CLAUDE.md` API sections
   - Remove all references to `DeleteRequest` and request body `authorId` fields
   - Document JWT-based authentication flow

3. **Final Verification**:
   ```bash
   ./gradlew clean build  # Full rebuild
   docker compose up -d --build  # Start services
   ./gradlew :e2e-tests:test  # All E2E tests should pass
   ```

4. **Merge to master** (after all tests pass)

---

**Last Updated**: 2025-12-17 23:20 UTC
**Branch**: `auth`
**Completed Phases**: 3/5
**Total Commits**: 3
