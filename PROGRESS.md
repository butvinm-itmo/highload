# Implementation Plan: Removing Cross-Service FK Constraints

## Goal
Refactor microservices to have totally independent database schemas without FK constraints to each other, enabling independent database deployment.

## Current State

**FK Constraints to Remove (divination-service):**

| Table | Constraint | References | Delete Behavior |
|-------|------------|------------|-----------------|
| `spread` | `fk_spread_layout_type` | `layout_type(id)` | ON DELETE RESTRICT |
| `spread` | `fk_spread_author` | `user(id)` | ON DELETE CASCADE |
| `spread_card` | `fk_spread_card_card` | `card(id)` | ON DELETE RESTRICT |
| `interpretation` | `fk_interpretation_author` | `user(id)` | ON DELETE CASCADE |

**Existing Application-Level Validation:**
- `DivinationService` already validates entities via Feign clients before create operations
- CASCADE DELETE behavior must be replicated at application level

---

## Phases

### Phase 1: Create Flyway Migration to Drop FK Constraints
- [x] **Completed**

- **Goal:** Remove cross-service foreign key constraints from divination-service tables while preserving indexes.

- **Scope:**
  - New file: `divination-service/src/main/resources/db/migration/V4__drop_cross_service_fk_constraints.sql`

- **Test Strategy:** Run divination-service integration tests

- **Verification Cmd:** `./gradlew :divination-service:test`

- **Result:** All 35 tests passed

- **Phase Execution:**
  1. **Implement:** Create V4 migration file with ALTER TABLE DROP CONSTRAINT statements ✓
  2. **Verify:** Run `./gradlew :divination-service:test` ✓ (35 tests passed)
  3. **Report:** Update PROGRESS.md (mark Phase 1 complete) ✓
  4. **Commit:**
     ```bash
     git add divination-service/src/main/resources/db/migration/V4__drop_cross_service_fk_constraints.sql
     git add PROGRESS.md
     git commit -m "Remove cross-service FK constraints from divination-service

     - Add V4 Flyway migration to drop fk_spread_author, fk_spread_layout_type,
       fk_spread_card_card, and fk_interpretation_author constraints
     - Preserves indexes for query performance
     - Enables independent database deployment for divination-service

     🤖 Generated with [Claude Code](https://claude.com/claude-code)

     Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
     ```

---

### Phase 2: Update Test Init Script
- [x] **Completed**

- **Goal:** Remove cross-service FK constraints from test database initialization script.

- **Scope:**
  - `divination-service/src/test/resources/init-test-db.sql`

- **Test Strategy:** Run divination-service integration tests

- **Verification Cmd:** `./gradlew :divination-service:test`

- **Result:** All 35 tests passed

- **Phase Execution:**
  1. **Implement:** Remove FK constraint definitions from init-test-db.sql (keep test data for WireMock) ✓
  2. **Verify:** Run `./gradlew :divination-service:test` ✓ (35 tests passed)
  3. **Report:** Update PROGRESS.md (mark Phase 2 complete) ✓
  4. **Commit:**
     ```bash
     git add divination-service/src/test/resources/init-test-db.sql
     git add PROGRESS.md
     git commit -m "Update test init script to remove cross-service FK constraints

     - Remove fk_spread_author, fk_spread_layout_type, fk_spread_card_card,
       fk_interpretation_author constraints from test schema
     - Keep test data (users, layout_types, cards) for WireMock mocking

     🤖 Generated with [Claude Code](https://claude.com/claude-code)

     Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
     ```

---

### Phase 3: Implement Orphan Data Cleanup for User Deletion
- [x] **Completed**

- **Goal:** Add application-level cascade delete logic since database FK CASCADE is removed.

- **Scope:**
  - `shared-clients/.../DivinationServiceClient.kt` - Add cleanup endpoint
  - `divination-service/.../controller/InternalController.kt` - Add internal endpoint (new file)
  - `divination-service/.../service/DivinationService.kt` - Add deleteByAuthorId
  - `divination-service/.../repository/SpreadRepository.kt` - Add findByAuthorId, deleteByAuthorId
  - `divination-service/.../repository/InterpretationRepository.kt` - Add deleteByAuthorId
  - `user-service/.../service/UserService.kt` - Call cleanup before delete
  - `user-service/build.gradle.kts` - Add shared-clients dependency
  - `user-service/.../UserServiceApplication.kt` - Enable Feign clients

- **Design (Synchronous approach):**
  ```
  UserService.deleteUser(id)
    └── DivinationServiceClient.deleteUserData(id)  // Call first
        └── DivinationService.deleteByAuthorId(id)
            ├── Delete all interpretations by author
            └── Delete all spreads by author
    └── userRepository.deleteById(id)              // Then delete user
  ```

- **Test Strategy:**
  - Unit tests for deleteByAuthorId
  - Integration tests with mocked DivinationServiceClient
  - New test: `deleteUser should continue when divination service fails`

- **Verification Cmd:** `./gradlew :divination-service:test :user-service:test`

- **Result:** All 64 tests passed (35 divination-service + 29 user-service)

- **Phase Execution:**
  1. **Implement:** ✓
     - Added repository methods (findByAuthorId, deleteByAuthorId)
     - Added DivinationService.deleteByAuthorId
     - Created InternalController with DELETE /api/v0.0.1/internal/users/{userId}/data
     - Added DivinationServiceClient.deleteUserData
     - Updated UserService to call cleanup before delete (with graceful error handling)
     - Added/updated unit and integration tests
  2. **Verify:** `./gradlew :divination-service:test :user-service:test` ✓ (64 tests passed)
  3. **Report:** Update PROGRESS.md (mark Phase 3 complete) ✓
  4. **Commit:** See below

---

### Phase 4: Handle Orphaned Data Gracefully
- [x] **Completed**

- **Goal:** Handle cases where referenced entities no longer exist.

- **Scope:**
  - `divination-service/.../mapper/SpreadMapper.kt`
  - `divination-service/src/test/.../unit/mapper/SpreadMapperTest.kt` (new file)

- **Design:** Return placeholder data for deleted users:
  ```kotlin
  private fun getUserOrPlaceholder(userId: UUID): UserDto =
      try {
          userServiceClient.getUserById(userId).body!!
      } catch (e: FeignException.NotFound) {
          logger.warn("User {} not found, returning placeholder", userId)
          createDeletedUserPlaceholder(userId)
      }
  ```

- **Test Strategy:** Unit tests with mocked Feign 404 responses (5 new tests)

- **Verification Cmd:** `./gradlew :divination-service:test`

- **Result:** All 40 tests passed (35 existing + 5 new SpreadMapperTest)

- **Phase Execution:**
  1. **Implement:** Added getUserOrPlaceholder and getLayoutTypeOrPlaceholder methods ✓
  2. **Verify:** Run `./gradlew :divination-service:test` ✓ (40 tests passed)
  3. **Report:** Update PROGRESS.md (mark Phase 4 complete) ✓
  4. **Commit:** See below

---

### Phase 5: Update E2E Tests
- [ ] **Pending**

- **Goal:** Verify E2E tests pass with new cleanup logic.

- **Scope:**
  - `e2e-tests/.../CleanupAuthorizationE2ETest.kt`

- **Additional Test:**
  ```kotlin
  @Test
  fun `DELETE user should cascade delete spreads and interpretations`() {
      // Verify application-level cascade works
  }
  ```

- **Verification Cmd:** `docker compose build && ./gradlew :e2e-tests:test`

- **Phase Execution:**
  1. **Implement:** Add cascade delete verification test
  2. **Verify:** Run `docker compose build && ./gradlew :e2e-tests:test`
  3. **Report:** Update PROGRESS.md (mark Phase 5 complete)
  4. **Commit:**
     ```bash
     git add e2e-tests/src/test/kotlin/com/github/butvinmitmo/e2e/CleanupAuthorizationE2ETest.kt
     git add PROGRESS.md
     git commit -m "Add E2E test for application-level cascade delete

     - Verify user deletion cascades to spreads and interpretations
     - Test cleanup endpoint works through full service stack

     🤖 Generated with [Claude Code](https://claude.com/claude-code)

     Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
     ```

---

### Phase 6: (Optional) Infrastructure for Independent Databases
- [ ] **Pending**

- **Goal:** Enable separate database instance for divination-service.

- **Scope:**
  - `docker-compose.yml` - Add postgres-divination container
  - `highload-config/divination-service.yml` - Update DB config

- **Verification Cmd:** `docker compose up -d && ./gradlew :e2e-tests:test`

- **Phase Execution:**
  1. **Implement:** Add postgres-divination container to docker-compose.yml
  2. **Verify:** Run `docker compose up -d && ./gradlew :e2e-tests:test`
  3. **Report:** Update PROGRESS.md (mark Phase 6 complete), Update CLAUDE.md with new DB architecture
  4. **Commit:**
     ```bash
     git add docker-compose.yml
     cd highload-config && git add divination-service.yml && git commit -m "Update divination-service DB config" && git push ssh main && cd ..
     git add highload-config
     git add PROGRESS.md
     git add CLAUDE.md
     git commit -m "Enable independent database for divination-service

     - Add postgres-divination container in docker-compose.yml
     - Configure divination-service to use separate database
     - Update documentation

     🤖 Generated with [Claude Code](https://claude.com/claude-code)

     Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
     ```

---

### Phase 7: (Optional) Simplify Test Schema
- [ ] **Pending**

- **Goal:** Remove unnecessary tables from test init script (user, layout_type, card).

- **Scope:**
  - `divination-service/src/test/resources/init-test-db.sql`

- **Verification Cmd:** `./gradlew :divination-service:test`

- **Phase Execution:**
  1. **Implement:** Remove user, layout_type, card tables from init-test-db.sql
  2. **Verify:** Run `./gradlew :divination-service:test`
  3. **Report:** Update PROGRESS.md (mark Phase 7 complete)
  4. **Commit:**
     ```bash
     git add divination-service/src/test/resources/init-test-db.sql
     git add PROGRESS.md
     git commit -m "Simplify test schema for independent database testing

     - Remove user, layout_type, card tables from test init script
     - Divination-service tests now fully independent of other service schemas

     🤖 Generated with [Claude Code](https://claude.com/claude-code)

     Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
     ```

---

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| Data inconsistency on user deletion | Synchronous Feign call before delete |
| Orphaned data accumulation | Implement scheduled cleanup (future) |
| E2E test startup timeout | Pre-build Docker images |

## Dependencies

```
Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 5 → Phase 6 → Phase 7
```

Phases 1-2 are prerequisites. Phase 6-7 are optional extensions.
