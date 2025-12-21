# Analysis of Claude Code Performance & "Vibe Coding" Issues

This document analyzes why "vibe coding" (fast, iterative, low-context prompting) has failed for this specific project and synthesizes findings from previous sessions.

## Why "Vibe Coding" Fails Here

The user reported that Claude Code is "slow, often makes mistakes, and makes ad-hoc patches." This is due to a mismatch between the **project complexity** and the **AI's operational constraints**.

### 1. High-Complexity Architecture vs. Limited Context
This is not a simple script; it is a **distributed microservices system** (6 services) with:
- **Infrastructure dependencies:** Eureka, Config Server, Gateway, Postgres, TestContainers.
- **Complex Inter-service communication:** Feign clients, Reactive vs Blocking stacks (WebFlux vs MVC).
- **Strict Testing Environment:** `TestContainers` spin up real databases and WireMock instances.

**The "Vibe Coding" Friction:**
- **Context Loading:** To make a correct change, the AI needs to understand the *entire* system flow (Gateway -> Auth -> Service A -> Service B). "Vibe coding" relies on the AI guessing context, but here, guessing leads to compilation errors or runtime failures.
- **Slowness:** Every small change requires a heavy build and test cycle (`./gradlew test` spins up Docker containers). The feedback loop is minutes, not seconds.
- **Blindness:** The AI often doesn't see the implicit dependencies (e.g., "Gateway strips the Authorization header, so you must use `X-User-Id`").

### 2. The "Ad-Hoc Patch" Trap
Because the feedback loop is slow and the context is heavy, the AI optimizes for **"making the error go away"** rather than **"fixing the root cause."**

**Common Anti-Patterns Observed:**
- **Disabling Tests:** When a test fails due to complex wiring (e.g., WireMock not starting), the AI suggests `@Disabled` to "move forward."
- **Lazy Loading Hacks:** To fix `LazyInitializationException` without understanding the transaction boundaries, it suggests accessing fields (`val _ = entity.cards.size`) instead of proper `JOIN FETCH` queries.
- **Reactive/Blocking Mismatch:** Mixing `block()` in WebFlux streams to make code "look" synchronous.

---

## AI Development Guidelines & Learnings

To succeed with this project, we must enforce strict guidelines.

### 🚫 Anti-Patterns (Do NOT Do)

1.  **Never Disable Tests:**
    -   **Bad:** `@Disabled("TODO: fix later")`
    -   **Reason:** Tests are the only source of truth in this complex system. If it fails, the code is broken.

2.  **No Lazy Loading Hacks:**
    -   **Bad:** Accessing a collection just to trigger it: `val _ = user.roles.size`
    -   **Fix:** Use specific repository methods: `@Query("SELECT u FROM User u JOIN FETCH u.roles...")`

3.  **No `block()` in Reactive Code:**
    -   **Bad:** `val result = someMono.block()`
    -   **Fix:** Use `flatMap`, `map`, or `zip`.

4.  **No Hardcoded IDs/Secrets:**
    -   **Bad:** `val id = UUID.fromString("...")` inside tests.
    -   **Fix:** Use the test factories or `TestEntityFactory`.

### ✅ Proven Solutions (Context for AI)

1.  **Reactive Feign Clients:**
    -   **Context:** `divination-service` is Reactive (WebFlux) but uses blocking Feign clients.
    -   **Pattern:** Wrap blocking calls in `Mono.fromCallable` on a dedicated scheduler:
        ```kotlin
        Mono.fromCallable { blockingClient.call() }
            .subscribeOn(Schedulers.boundedElastic())
        ```

2.  **TestContainers Strategy:**
    -   **Context:** Tests are slow because they start containers.
    -   **Pattern:** Use `@DirtiesContext` sparingly. Prefer reuse where possible, but be aware of shared state.

3.  **Authentication Flow:**
    -   **Context:** Services do **not** validate JWTs. They trust `X-User-Id` and `X-User-Role` headers from the Gateway.
    -   **Pattern:** In integration tests, mock these headers, don't try to mock the full JWT validation chain unless testing the Gateway itself.

4.  **Database Migration:**
    -   **Context:** Shared database instance but separate schemas/tables managed by Flyway.
    -   **Pattern:** Each service has its own `flyway_schema_history` table configuration.

## Summary

The project requires **Architectural Coding**, not "Vibe Coding." You cannot simply "vibe" your way through a 6-service distributed system with strict type checking and reactive streams. Every change requires verifying the contract between services.
