# Why Claude Code Struggles With This Project

Analysis of ~80 sessions to understand the root causes of slow, error-prone implementation.

---

## The Core Problem

**This project is architecturally hostile to LLM-assisted coding.**

Claude Code is optimized for:
- Single-application codebases
- Fast feedback loops (run test, see result)
- Localized changes (edit file, done)
- Consistent patterns throughout

This project has:
- 6 microservices with complex dependencies
- Slow feedback (E2E tests need all services running)
- Cascading changes (one API change touches 4+ modules)
- Mixed paradigms (blocking JPA + reactive R2DBC)

---

## Root Cause #1: Distributed Architecture

### The Problem

```
96 Kotlin files across 8 modules:
- divination-service: 23 files (reactive WebFlux + R2DBC)
- user-service: 22 files (blocking Spring MVC + JPA)
- tarot-service: 19 files (blocking Spring MVC + JPA)
- shared-dto: 11 files
- e2e-tests: 8 files
- gateway-service: 6 files
- shared-clients: 5 files
- config-server, eureka-server: 2 files
```

### Why This Hurts

1. **Context fragmentation**: Understanding `createSpread` requires reading:
   - `SpreadController` (divination-service)
   - `DivinationService` (divination-service)
   - `SpreadRepository` (divination-service)
   - `UserServiceClient` (shared-clients)
   - `TarotServiceClient` (shared-clients)
   - `CreateSpreadRequest` (shared-dto)
   - Configuration in `highload-config/` (separate repo!)

2. **Cascading changes**: Adding a field to `UserDto` requires:
   - Update `shared-dto`
   - Rebuild `user-service`, `divination-service`, `e2e-tests`
   - Update all services that use the DTO
   - Run tests for each service

3. **Implicit dependencies**: Services discover each other via Eureka. There's no compile-time check that `divination-service` can actually call `user-service`.

### Evidence from Sessions

> "The divination-service started successfully. Let me check the gateway logs..."
> "Still getting 502 BadGateway errors. Let me check if the divination service is actually responding..."
> "The issue is that the Docker containers are running old code. Phase 2 changes haven't been built into the containers yet."

Claude spends significant time debugging inter-service communication that wouldn't exist in a monolith.

---

## Root Cause #2: Mixed Paradigms

### The Problem

Two completely different programming models in one codebase:

| Aspect | user-service, tarot-service | divination-service |
|--------|----------------------------|-------------------|
| Framework | Spring MVC | Spring WebFlux |
| Database | JPA/Hibernate | R2DBC |
| Return types | `ResponseEntity<T>` | `Mono<ResponseEntity<T>>` |
| Testing | `MockMvc` | `WebTestClient` |
| Transactions | `@Transactional` | `@Transactional` (different impl) |
| Collections | Lazy loading | Manual fetching |

### Why This Hurts

1. **Patterns don't transfer**: Code that works in `user-service` fails in `divination-service`
2. **Easy to mix paradigms**: Blocking Feign calls inside reactive chains
3. **Double the learning**: Must understand both JPA quirks AND R2DBC quirks
4. **Testing differs completely**: Different test setup, different assertions, different mocking

### Evidence from Sessions

> "The issue is that `LIMIT` and `JOIN FETCH` don't work well together in JPQL"
> "R2DBC has no relationship support - must load separately"
> "Feign clients need Spring MVC's HttpMessageConverters but app is WebFlux-only"

Each paradigm has its own gotchas. This project has both.

---

## Root Cause #3: Slow Feedback Loops

### The Problem

| Test Type | Time | Requirements |
|-----------|------|--------------|
| Unit test | ~1s | None |
| Integration test | ~10s | TestContainers (PostgreSQL) |
| E2E test | ~30s+ | All 6 services running via docker compose |

### Why This Hurts

1. **Trial-and-error is expensive**: Each "let me try this" takes 30+ seconds
2. **E2E tests are the main verification**: Most features span services, unit tests insufficient
3. **Setup failures waste time**: Docker issues, network timeouts, Eureka registration delays

### Evidence from Sessions

> "Let me wait a bit for service discovery to complete and then run the tests again"
> "Network timeout pulling Docker images. Let me try again"
> "The services are reachable now! The issue is Jackson deserialization..."

Multiple retries due to infrastructure, not code problems.

### The Inverted Testing Pyramid

```
Current state:           Ideal state:
    ▲                         ▼
   /E2E\                     /E2E\  (few)
  /-----\                   /-----\
 / Integ \                 / Integ \ (some)
/---------\               /---------\
   Unit                      Unit     (many)
  (weak)                   (strong)
```

Heavy reliance on slow E2E tests because:
- Business logic spans services
- Feign client behavior hard to unit test
- Database interactions are the core logic

---

## Root Cause #4: Documentation Compensating for Complexity

### The Problem

CLAUDE.md is **1,103 lines** trying to capture:
- Architecture overview
- All 6 services' details
- API endpoints
- Authentication flow
- Testing setup
- Docker configuration
- Common gotchas
- Implementation patterns

### Why This Hurts

1. **Too much to read**: Claude may miss critical details in the wall of text
2. **Gets stale**: Code changes faster than documentation updates
3. **False confidence**: Having docs doesn't mean they're consulted effectively
4. **Symptom, not solution**: The need for 1000+ lines of docs indicates the project is too complex

### Evidence from Sessions

> "Let me first read the plan file to see what needs to be done"
> "Warmup" (repeated ~10 times across sessions - starting from scratch)
> Multiple sessions re-discovering the same patterns

Documentation exists but context is still lost between sessions.

---

## Root Cause #5: Configuration Complexity

### The Problem

Configuration is distributed across:
- `highload-config/` (git submodule, separate repo)
- `application.yml` in each service
- `application-test.yml` for tests
- `docker-compose.yml`
- Environment variables
- Spring Cloud Config Server

### Why This Hurts

1. **Indirection**: Finding "where is the database URL configured?" requires checking 4 places
2. **Submodule friction**: Changes to config require commit in submodule, then in main repo
3. **Runtime vs build-time**: Some config applies at build, some at runtime
4. **Environment differences**: Local dev, Docker, CI all have different configs

### Evidence from Sessions

> "The issue is that X-User-Id header is required but Feign clients don't provide it"
> "Making X-User-Id nullable for GET endpoints" (ad-hoc fix for config/architecture mismatch)

Configuration mismatches between environments cause runtime surprises.

---

## What Would Actually Help

### 1. Simplify the Architecture

**Not realistic for this project**, but the ideal is:
- Fewer services (consolidate where possible)
- One paradigm (all reactive OR all blocking)
- In-process testing over E2E where possible

### 2. Better Test Strategy

**Practical improvements**:
- More unit tests with mocked Feign clients
- Contract testing (Pact) instead of E2E for API compatibility
- Integration tests that don't need all services

### 3. Reduce Context Switching

**For Claude Code specifically**:
- Work on one service at a time when possible
- Use PROGRESS.md for multi-session work
- Smaller, focused changes over big refactors

### 4. Accept the Friction

**Some tasks are inherently slow in this architecture**:
- Cross-service features require E2E verification
- Reactive code requires careful thought about blocking
- Infrastructure setup takes time

**Don't fight it** - plan for it. A task that seems like "add one endpoint" may legitimately take an hour when it spans services.

---

## Specific Anti-Patterns Observed

### 1. "Let me explore the codebase"

**Problem**: Claude spawns exploration agents that re-read files already documented in CLAUDE.md.

**Better**: Trust CLAUDE.md for architecture. Explore only for specific unknowns.

### 2. "Let me run the tests to see if this works"

**Problem**: Using E2E tests as primary feedback (slow).

**Better**: Unit test first, integration test second, E2E last.

### 3. "The test fails, let me fix it"

**Problem**: Fixing without understanding root cause leads to ad-hoc patches.

**Better**: Diagnose why the failure happened. Is it a real bug or a test setup issue?

### 4. "I'll update the documentation later"

**Problem**: CLAUDE.md gets stale, future sessions hit the same issues.

**Better**: Update docs as part of the change, not after.

---

## The Honest Assessment

This project is **genuinely difficult** for LLM-assisted coding because:

1. **Distributed systems are hard** - for humans too
2. **Mixed paradigms multiply complexity** - reactive + blocking = worst of both
3. **Slow feedback is inherent** - can't escape E2E tests in microservices
4. **Context doesn't fit in one window** - 96 files across 8 modules

Claude Code isn't "bad" here. The project is asking it to:
- Understand distributed system interactions
- Hold context across many modules
- Debug infrastructure issues
- Navigate two different programming paradigms
- Wait for slow test suites

These are hard problems for any developer, LLM or human.

---

## Recommendations for This Project

### Short-term (per session)
1. Read CLAUDE.md and PROGRESS.md first
2. Work on one service at a time when possible
3. Run unit tests before integration/E2E
4. Commit frequently with clear messages

### Medium-term (architecture)
1. Add more unit tests to reduce E2E dependency
2. Document the "common gotchas" section better
3. Consider making all services use same paradigm

### Long-term (honesty)
1. Accept that some tasks are slow
2. Don't expect "vibe coding" speed on distributed systems
3. Plan for infrastructure friction in estimates

---

*Last updated: 2025-12-20*
