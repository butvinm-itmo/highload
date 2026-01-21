package com.github.butvinmitmo.divinationservice.infrastructure.external

import com.github.butvinmitmo.divinationservice.application.interfaces.provider.FileProvider
import com.github.butvinmitmo.shared.client.FilesServiceInternalClient
import com.github.butvinmitmo.shared.dto.FileUploadMetadataDto
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.UUID

@Component
class FeignFileProvider(
    private val filesServiceClient: FilesServiceInternalClient,
) : FileProvider {
    override fun verifyAndCompleteUpload(
        uploadId: UUID,
        userId: UUID,
    ): Mono<FileUploadMetadataDto> =
        Mono
            .fromCallable { filesServiceClient.verifyAndCompleteUpload(uploadId, userId).body!! }
            .subscribeOn(Schedulers.boundedElastic())

    override fun getUploadMetadata(uploadId: UUID): Mono<FileUploadMetadataDto> =
        Mono
            .fromCallable { filesServiceClient.getUploadMetadata(uploadId).body!! }
            .subscribeOn(Schedulers.boundedElastic())

    override fun getDownloadUrl(uploadId: UUID): Mono<String> =
        Mono
            .fromCallable { filesServiceClient.getDownloadUrl(uploadId).body!!["downloadUrl"]!! }
            .subscribeOn(Schedulers.boundedElastic())
}
