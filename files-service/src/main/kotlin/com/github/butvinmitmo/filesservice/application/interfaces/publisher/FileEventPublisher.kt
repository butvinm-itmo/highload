package com.github.butvinmitmo.filesservice.application.interfaces.publisher

import com.github.butvinmitmo.filesservice.domain.model.FileUpload
import reactor.core.publisher.Mono

interface FileEventPublisher {
    fun publishCompleted(fileUpload: FileUpload): Mono<Void>

    fun publishDeleted(fileUpload: FileUpload): Mono<Void>
}
