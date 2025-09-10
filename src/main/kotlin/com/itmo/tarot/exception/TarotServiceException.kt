package com.itmo.tarot.exception

sealed class TarotServiceException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

class SpreadNotFoundException(id: Long) : TarotServiceException("Spread with id $id not found")

class InterpretationNotFoundException(id: Long) : TarotServiceException("Interpretation with id $id not found")

class UserNotFoundException(id: Long) : TarotServiceException("User with id $id not found")

class UnauthorizedOperationException(message: String) : TarotServiceException(message)

class InterpretationAlreadyExistsException(userId: Long, spreadId: Long) : 
    TarotServiceException("User $userId already has interpretation for spread $spreadId")

class ValidationException(message: String) : TarotServiceException(message)