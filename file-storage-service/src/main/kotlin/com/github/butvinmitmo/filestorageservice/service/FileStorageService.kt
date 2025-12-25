package com.github.butvinmitmo.filestorageservice.service

import com.github.butvinmitmo.filestorageservice.exception.FileNotFoundException
import com.github.butvinmitmo.filestorageservice.exception.FileStorageException
import io.minio.GetObjectArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.RemoveObjectArgs
import io.minio.StatObjectArgs
import io.minio.errors.ErrorResponseException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.InputStream

@Service
class FileStorageService(
    private val minioClient: MinioClient,
    @Value("\${minio.bucket}") private val bucket: String,
    @Value("\${minio.public-url}") private val publicUrl: String,
) {
    fun uploadFile(
        key: String,
        inputStream: InputStream,
        contentType: String,
        size: Long,
    ): String =
        try {
            minioClient.putObject(
                PutObjectArgs
                    .builder()
                    .bucket(bucket)
                    .`object`(key)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build(),
            )
            buildFileUrl(key)
        } catch (e: Exception) {
            throw FileStorageException("Failed to upload file: ${e.message}", e)
        }

    fun deleteFile(key: String): Boolean =
        try {
            minioClient.removeObject(
                RemoveObjectArgs
                    .builder()
                    .bucket(bucket)
                    .`object`(key)
                    .build(),
            )
            true
        } catch (e: ErrorResponseException) {
            if (e.errorResponse().code() == "NoSuchKey") {
                false
            } else {
                throw FileStorageException("Failed to delete file: ${e.message}", e)
            }
        } catch (e: Exception) {
            throw FileStorageException("Failed to delete file: ${e.message}", e)
        }

    fun getFile(key: String): InputStream =
        try {
            minioClient.getObject(
                GetObjectArgs
                    .builder()
                    .bucket(bucket)
                    .`object`(key)
                    .build(),
            )
        } catch (e: ErrorResponseException) {
            if (e.errorResponse().code() == "NoSuchKey") {
                throw FileNotFoundException("File not found: $key")
            }
            throw FileStorageException("Failed to get file: ${e.message}", e)
        } catch (e: Exception) {
            throw FileStorageException("Failed to get file: ${e.message}", e)
        }

    fun fileExists(key: String): Boolean =
        try {
            minioClient.statObject(
                StatObjectArgs
                    .builder()
                    .bucket(bucket)
                    .`object`(key)
                    .build(),
            )
            true
        } catch (e: ErrorResponseException) {
            if (e.errorResponse().code() == "NoSuchKey") {
                false
            } else {
                throw FileStorageException("Failed to check file existence: ${e.message}", e)
            }
        } catch (e: Exception) {
            throw FileStorageException("Failed to check file existence: ${e.message}", e)
        }

    fun getContentType(key: String): String =
        try {
            val stat =
                minioClient.statObject(
                    StatObjectArgs
                        .builder()
                        .bucket(bucket)
                        .`object`(key)
                        .build(),
                )
            stat.contentType()
        } catch (e: ErrorResponseException) {
            if (e.errorResponse().code() == "NoSuchKey") {
                throw FileNotFoundException("File not found: $key")
            }
            throw FileStorageException("Failed to get file info: ${e.message}", e)
        } catch (e: Exception) {
            throw FileStorageException("Failed to get file info: ${e.message}", e)
        }

    fun buildFileUrl(key: String): String = "$publicUrl/api/v0.0.1/files/$key"
}
