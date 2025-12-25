package com.github.butvinmitmo.filestorageservice.exception

class FileNotFoundException(
    message: String,
) : RuntimeException(message)

class FileStorageException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
