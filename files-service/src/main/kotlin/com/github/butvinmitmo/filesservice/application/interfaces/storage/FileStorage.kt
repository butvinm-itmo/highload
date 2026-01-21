package com.github.butvinmitmo.filesservice.application.interfaces.storage

import reactor.core.publisher.Mono

interface FileStorage {
    fun generatePresignedUploadUrl(
        path: String,
        contentType: String,
        expirationMinutes: Int,
    ): Mono<String>

    fun generatePresignedDownloadUrl(
        path: String,
        expirationMinutes: Int,
    ): Mono<String>

    fun exists(path: String): Mono<Boolean>

    fun getObjectSize(path: String): Mono<Long>

    fun delete(path: String): Mono<Void>
}
