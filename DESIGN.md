# Tarology Web Service - Design Document

## Overview

This document describes the top-down design of Controllers, Services, and Repositories for the Tarology Web Service. The design follows a layered architecture approach, starting from the API layer (Controllers) down to the data access layer (Repositories), ensuring we only implement functionality that is actually required.

## 1. Controller Layer

The Controller layer handles HTTP requests and responses, delegating business logic to the Service layer.

### 1.1 SpreadController

**Path:** `/api/v0.0.1/spreads`

```kotlin
@RestController
@RequestMapping("/api/v0.0.1/spreads")
class SpreadController(
    private val spreadService: SpreadService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createSpread(@RequestBody request: CreateSpreadRequest): SpreadDto

    @GetMapping
    fun getSpreads(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<List<SpreadSummaryDto>>
    // Returns X-Total-Count header

    @GetMapping("/scroll")
    fun getSpreadsByScroll(
        @RequestParam(required = false) after: UUID?,
        @RequestParam(defaultValue = "20") size: Int
    ): List<SpreadSummaryDto>

    @GetMapping("/{id}")
    fun getSpread(@PathVariable id: UUID): SpreadDto

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteSpread(
        @PathVariable id: UUID,
        @RequestBody request: DeleteRequest
    )
}
```

### 1.2 InterpretationController

**Path:** `/api/v0.0.1/spreads/{spreadId}/interpretations`

```kotlin
@RestController
@RequestMapping("/api/v0.0.1/spreads/{spreadId}/interpretations")
class InterpretationController(
    private val interpretationService: InterpretationService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun addInterpretation(
        @PathVariable spreadId: UUID,
        @RequestBody request: CreateInterpretationRequest
    ): InterpretationDto

    @PutMapping("/{id}")
    fun updateInterpretation(
        @PathVariable spreadId: UUID,
        @PathVariable id: UUID,
        @RequestBody request: UpdateInterpretationRequest
    ): InterpretationDto

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteInterpretation(
        @PathVariable spreadId: UUID,
        @PathVariable id: UUID,
        @RequestBody request: DeleteRequest
    )
}
```

### 1.3 UserController

**Path:** `/api/v0.0.1/users`

```kotlin
@RestController
@RequestMapping("/api/v0.0.1/users")
class UserController(
    private val userService: UserService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createUser(@RequestBody request: CreateUserRequest): UserDto

    @GetMapping
    fun getUsers(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): List<UserDto>

    @GetMapping("/{id}")
    fun getUser(@PathVariable id: UUID): UserDto

    @PutMapping("/{id}")
    fun updateUser(
        @PathVariable id: UUID,
        @RequestBody request: UpdateUserRequest
    ): UserDto

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteUser(@PathVariable id: UUID)
}
```

### 1.4 Exception Handling

```kotlin
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(ex: NotFoundException): ErrorResponse

    @ExceptionHandler(ForbiddenException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleForbidden(ex: ForbiddenException): ErrorResponse

    @ExceptionHandler(ConflictException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleConflict(ex: ConflictException): ErrorResponse

    @ExceptionHandler(ValidationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidation(ex: ValidationException): ValidationErrorResponse
}
```

## 2. Service Layer

The Service layer contains business logic and transaction management. Each service method that modifies data is annotated with `@Transactional`.

### 2.1 SpreadService

```kotlin
@Service
class SpreadService(
    private val spreadRepository: SpreadRepository,
    private val userRepository: UserRepository,
    private val layoutTypeRepository: LayoutTypeRepository,
    private val cardRepository: CardRepository,
    private val spreadCardRepository: SpreadCardRepository,
    private val interpretationRepository: InterpretationRepository,
    private val spreadMapper: SpreadMapper
) {

    @Transactional
    fun createSpread(request: CreateSpreadRequest): SpreadDto {
        // 1. Validate user exists
        val user = userRepository.findById(request.userId)
            ?: throw NotFoundException("User not found")

        // 2. Get layout type
        val layoutType = layoutTypeRepository.findByName(request.layoutType.name)
            ?: throw NotFoundException("Layout type not found")

        // 3. Create spread entity
        val spread = Spread(
            question = request.question,
            author = user,
            layoutType = layoutType
        )
        val savedSpread = spreadRepository.save(spread)

        // 4. Generate random cards based on layout
        val cards = cardRepository.findRandomCards(layoutType.cardsCount)

        // 5. Create spread-card relationships
        cards.forEachIndexed { index, card ->
            val spreadCard = SpreadCard(
                spread = savedSpread,
                card = card,
                positionInSpread = index,
                isReversed = Random.nextBoolean()
            )
            spreadCardRepository.save(spreadCard)
        }

        // 6. Fetch complete spread with cards
        return spreadMapper.toDto(
            spreadRepository.findByIdWithCards(savedSpread.id)!!
        )
    }

    fun getSpreads(page: Int, size: Int): PageResponse<SpreadSummaryDto> {
        val pageable = PageRequest.of(page, size)
        val spreadsPage = spreadRepository.findAllOrderByCreatedAtDesc(pageable)
        val totalCount = spreadRepository.count()

        return PageResponse(
            content = spreadsPage.map { spreadMapper.toSummaryDto(it) },
            totalElements = totalCount
        )
    }

    fun getSpreadsByScroll(after: UUID?, size: Int): List<SpreadSummaryDto> {
        val spreads = if (after != null) {
            spreadRepository.findSpreadsAfterCursor(after, size)
        } else {
            spreadRepository.findLatestSpreads(size)
        }

        return spreads.map { spreadMapper.toSummaryDto(it) }
    }

    fun getSpread(id: UUID): SpreadDto {
        val spread = spreadRepository.findByIdWithCardsAndInterpretations(id)
            ?: throw NotFoundException("Spread not found")

        return spreadMapper.toDto(spread)
    }

    @Transactional
    fun deleteSpread(id: UUID, userId: UUID) {
        val spread = spreadRepository.findById(id)
            ?: throw NotFoundException("Spread not found")

        if (spread.author.id != userId) {
            throw ForbiddenException("You can only delete your own spreads")
        }

        // Database CASCADE DELETE handles interpretations and spread_cards automatically
        spreadRepository.deleteById(id)
    }
}
```

### 2.2 InterpretationService

```kotlin
@Service
class InterpretationService(
    private val interpretationRepository: InterpretationRepository,
    private val spreadRepository: SpreadRepository,
    private val userRepository: UserRepository,
    private val interpretationMapper: InterpretationMapper
) {

    @Transactional
    fun addInterpretation(
        spreadId: UUID,
        request: CreateInterpretationRequest
    ): InterpretationDto {
        // 1. Validate spread exists
        val spread = spreadRepository.findById(spreadId)
            ?: throw NotFoundException("Spread not found")

        // 2. Validate user exists
        val user = userRepository.findById(request.userId)
            ?: throw NotFoundException("User not found")

        // 3. Check if user already has interpretation for this spread
        if (interpretationRepository.existsByAuthorAndSpread(user.id, spreadId)) {
            throw ConflictException("You already have an interpretation for this spread")
        }

        // 4. Create interpretation
        val interpretation = Interpretation(
            text = request.text,
            author = user,
            spread = spread
        )

        val saved = interpretationRepository.save(interpretation)
        return interpretationMapper.toDto(saved)
    }

    @Transactional
    fun updateInterpretation(
        spreadId: UUID,
        id: UUID,
        request: UpdateInterpretationRequest
    ): InterpretationDto {
        val interpretation = interpretationRepository.findById(id)
            ?: throw NotFoundException("Interpretation not found")

        if (interpretation.author.id != request.userId) {
            throw ForbiddenException("You can only edit your own interpretations")
        }

        interpretation.text = request.text
        val updated = interpretationRepository.save(interpretation)

        return interpretationMapper.toDto(updated)
    }

    @Transactional
    fun deleteInterpretation(
        spreadId: UUID,
        id: UUID,
        userId: UUID
    ) {
        val interpretation = interpretationRepository.findById(id)
            ?: throw NotFoundException("Interpretation not found")

        if (interpretation.author.id != userId) {
            throw ForbiddenException("You can only delete your own interpretations")
        }

        interpretationRepository.deleteById(id)
    }
}
```

### 2.3 UserService

```kotlin
@Service
class UserService(
    private val userRepository: UserRepository,
    private val spreadRepository: SpreadRepository,
    private val interpretationRepository: InterpretationRepository,
    private val userMapper: UserMapper
) {

    @Transactional
    fun createUser(request: CreateUserRequest): UserDto {
        // Check if user with this ID already exists
        if (userRepository.existsById(request.id)) {
            throw ConflictException("User with this ID already exists")
        }

        val user = User(
            id = request.id,
            username = request.username ?: "user_${request.id}"
        )

        val saved = userRepository.save(user)
        return userMapper.toDto(saved)
    }

    fun getUsers(page: Int, size: Int): List<UserDto> {
        val pageable = PageRequest.of(page, size)
        return userRepository.findAll(pageable)
            .map { userMapper.toDto(it) }
    }

    fun getUser(id: UUID): UserDto {
        val user = userRepository.findById(id)
            ?: throw NotFoundException("User not found")

        return userMapper.toDto(user)
    }

    @Transactional
    fun updateUser(id: UUID, request: UpdateUserRequest): UserDto {
        val user = userRepository.findById(id)
            ?: throw NotFoundException("User not found")

        request.username?.let { user.username = it }

        val updated = userRepository.save(user)
        return userMapper.toDto(updated)
    }

    @Transactional
    fun deleteUser(id: UUID) {
        if (!userRepository.existsById(id)) {
            throw NotFoundException("User not found")
        }

        // Database CASCADE DELETE handles all related deletions automatically:
        // - User's spreads (which cascades to interpretations and spread_cards)
        // - User's interpretations on other spreads
        userRepository.deleteById(id)
    }
}
```

## 3. Repository Layer

The Repository layer provides data access. Based on the Service layer requirements, we need the following repository methods:

### 3.1 UserRepository

```kotlin
interface UserRepository {
    fun findById(id: UUID): User?
    fun existsById(id: UUID): Boolean
    fun save(user: User): User
    fun findAll(pageable: Pageable): Page<User>
    fun deleteById(id: UUID)
}
```

### 3.2 SpreadRepository

```kotlin
interface SpreadRepository {
    fun findById(id: UUID): Spread?
    fun findByIdWithCards(id: UUID): Spread?
    fun findByIdWithCardsAndInterpretations(id: UUID): Spread?
    fun save(spread: Spread): Spread
    fun findAllOrderByCreatedAtDesc(pageable: Pageable): Page<Spread>
    fun findSpreadsAfterCursor(spreadId: UUID, limit: Int): List<Spread>
    fun findLatestSpreads(limit: Int): List<Spread>
    fun count(): Long
    fun deleteById(id: UUID)
}
```

### 3.3 InterpretationRepository

```kotlin
interface InterpretationRepository {
    fun findById(id: UUID): Interpretation?
    fun existsByAuthorAndSpread(authorId: UUID, spreadId: UUID): Boolean
    fun save(interpretation: Interpretation): Interpretation
    fun deleteById(id: UUID)
}
```

### 3.4 CardRepository

```kotlin
interface CardRepository {
    fun findRandomCards(limit: Int): List<Card>
}
```

### 3.5 SpreadCardRepository

```kotlin
interface SpreadCardRepository {
    fun save(spreadCard: SpreadCard): SpreadCard
}
```

### 3.6 LayoutTypeRepository

```kotlin
interface LayoutTypeRepository {
    fun findByName(name: String): LayoutType?
}
```

### 3.7 Repository Implementations

Repository implementations will use Spring Data JPA with custom queries where needed:

```kotlin
@Repository
interface UserRepositoryJpa : JpaRepository<User, UUID>, UserRepository {
    // Most methods inherited from JpaRepository
}

@Repository
interface SpreadRepositoryJpa : JpaRepository<Spread, UUID>, SpreadRepository {

    @EntityGraph(attributePaths = ["author", "layoutType", "spreadCards.card"])
    override fun findByIdWithCards(id: UUID): Spread?

    @EntityGraph(attributePaths = ["author", "layoutType", "spreadCards.card", "interpretations.author"])
    override fun findByIdWithCardsAndInterpretations(id: UUID): Spread?

    @Query("SELECT s FROM Spread s ORDER BY s.createdAt DESC")
    override fun findAllOrderByCreatedAtDesc(pageable: Pageable): Page<Spread>

    @Query("""
        SELECT s FROM Spread s
        WHERE s.createdAt < (SELECT s2.createdAt FROM Spread s2 WHERE s2.id = :spreadId)
        OR (s.createdAt = (SELECT s2.createdAt FROM Spread s2 WHERE s2.id = :spreadId) AND s.id < :spreadId)
        ORDER BY s.createdAt DESC, s.id DESC
        LIMIT :limit
    """)
    override fun findSpreadsAfterCursor(spreadId: UUID, limit: Int): List<Spread>

    @Query("SELECT s FROM Spread s ORDER BY s.createdAt DESC LIMIT :limit")
    override fun findLatestSpreads(limit: Int): List<Spread>
}

@Repository
interface InterpretationRepositoryJpa : JpaRepository<Interpretation, UUID>, InterpretationRepository {

    @Query("SELECT COUNT(i) > 0 FROM Interpretation i WHERE i.author.id = :authorId AND i.spread.id = :spreadId")
    override fun existsByAuthorAndSpread(authorId: UUID, spreadId: UUID): Boolean
}

@Repository
interface CardRepositoryJpa : JpaRepository<Card, UUID>, CardRepository {

    @Query(value = "SELECT * FROM card ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    override fun findRandomCards(limit: Int): List<Card>
}

@Repository
interface SpreadCardRepositoryJpa : JpaRepository<SpreadCard, UUID>, SpreadCardRepository

@Repository
interface LayoutTypeRepositoryJpa : JpaRepository<LayoutType, UUID>, LayoutTypeRepository {
    override fun findByName(name: String): LayoutType?
}
```

## 4. Mapper Layer

Mappers convert between entities and DTOs:

```kotlin
@Component
class SpreadMapper {
    fun toDto(spread: Spread): SpreadDto
    fun toSummaryDto(spread: Spread): SpreadSummaryDto
}

@Component
class InterpretationMapper {
    fun toDto(interpretation: Interpretation): InterpretationDto
}

@Component
class UserMapper {
    fun toDto(user: User): UserDto
}
```

## 5. Exception Classes

```kotlin
class NotFoundException(message: String) : RuntimeException(message)
class ForbiddenException(message: String) : RuntimeException(message)
class ConflictException(message: String) : RuntimeException(message)
class ValidationException(val errors: Map<String, String>) : RuntimeException()
```

## 6. Key Design Decisions

1. **Top-Down Design**: Started with Controllers to define the API contract, then Services for business logic, and finally Repositories for only the required data access.

2. **Minimal Repository Interface**: Repository methods are limited to exactly what the Service layer needs, avoiding unnecessary features.

3. **Transaction Management**: All data-modifying operations in the Service layer are marked `@Transactional` to ensure data consistency.

4. **Separation of Concerns**:
   - Controllers handle HTTP concerns only
   - Services contain all business logic and validation
   - Repositories handle data access only

5. **Error Handling**: Centralized exception handling in `GlobalExceptionHandler` with appropriate HTTP status codes.

6. **Entity Graph Loading**: Using `@EntityGraph` annotations to optimize fetching and avoid N+1 query problems.

7. **Cursor-Based Pagination**: Implemented for infinite scroll functionality using spread ID as cursor.

8. **Database-Driven Simplifications**:
   - **CASCADE DELETE**: All deletion operations rely on database constraints, eliminating manual cascade logic
   - **Unique Constraints**: Database enforces one interpretation per user per spread (unique on author_id + spread_id)
   - **Referential Integrity**: Foreign keys ensure data consistency without additional validation
   - **Default Values**: Database handles timestamps (created_at) automatically via DEFAULT CURRENT_TIMESTAMP

## 7. Transaction Boundaries

Critical transactional operations:

1. **Spread Creation**: Single transaction for creating spread, generating cards, and linking them.

2. **Spread Deletion**: Cascading delete of interpretations and spread_cards via DB constraints.

3. **User Deletion**: Simple delete operation - database CASCADE DELETE constraints handle all related deletions automatically.

All transactions use Spring's `@Transactional` annotation with default propagation (REQUIRED) and isolation (READ_COMMITTED) levels.