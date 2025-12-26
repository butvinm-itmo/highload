# PROGRESS.md

## Current Work: Bug Fix - fileUrl is null in GET /spreads/{id} response

**Status:** COMPLETE

**Branch:** fixes

---

## Bug Fix: fileUrl is null in GET /spreads/{id} response

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
