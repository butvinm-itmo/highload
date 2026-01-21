package com.github.butvinmitmo.filesservice.infrastructure.storage

import com.github.butvinmitmo.filesservice.application.interfaces.storage.FileStorage
import com.github.butvinmitmo.filesservice.config.MinioProperties
import io.minio.GetPresignedObjectUrlArgs
import io.minio.MinioClient
import io.minio.RemoveObjectArgs
import io.minio.StatObjectArgs
import io.minio.http.Method
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.concurrent.TimeUnit

@Component
class MinioFileStorage(
    private val minioClient: MinioClient,
    private val presignedMinioClient: MinioClient,
    private val minioProperties: MinioProperties,
) : FileStorage {
    override fun generatePresignedUploadUrl(
        path: String,
        contentType: String,
        expirationMinutes: Int,
    ): Mono<String> =
        Mono
            .fromCallable {
                presignedMinioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs
                        .builder()
                        .method(Method.PUT)
                        .bucket(minioProperties.bucket)
                        .`object`(path)
                        .expiry(expirationMinutes, TimeUnit.MINUTES)
                        .extraHeaders(mapOf("Content-Type" to contentType))
                        .build(),
                )
            }.subscribeOn(Schedulers.boundedElastic())

    override fun generatePresignedDownloadUrl(
        path: String,
        expirationMinutes: Int,
    ): Mono<String> =
        Mono
            .fromCallable {
                presignedMinioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs
                        .builder()
                        .method(Method.GET)
                        .bucket(minioProperties.bucket)
                        .`object`(path)
                        .expiry(expirationMinutes, TimeUnit.MINUTES)
                        .build(),
                )
            }.subscribeOn(Schedulers.boundedElastic())

    override fun exists(path: String): Mono<Boolean> =
        Mono
            .fromCallable {
                try {
                    minioClient.statObject(
                        StatObjectArgs
                            .builder()
                            .bucket(minioProperties.bucket)
                            .`object`(path)
                            .build(),
                    )
                    true
                } catch (e: io.minio.errors.ErrorResponseException) {
                    if (e.errorResponse().code() == "NoSuchKey") {
                        false
                    } else {
                        throw e
                    }
                }
            }.subscribeOn(Schedulers.boundedElastic())

    override fun getObjectSize(path: String): Mono<Long> =
        Mono
            .fromCallable {
                val stat =
                    minioClient.statObject(
                        StatObjectArgs
                            .builder()
                            .bucket(minioProperties.bucket)
                            .`object`(path)
                            .build(),
                    )
                stat.size()
            }.subscribeOn(Schedulers.boundedElastic())

    override fun delete(path: String): Mono<Void> =
        Mono
            .fromCallable {
                minioClient.removeObject(
                    RemoveObjectArgs
                        .builder()
                        .bucket(minioProperties.bucket)
                        .`object`(path)
                        .build(),
                )
            }.subscribeOn(Schedulers.boundedElastic())
            .then()
}
