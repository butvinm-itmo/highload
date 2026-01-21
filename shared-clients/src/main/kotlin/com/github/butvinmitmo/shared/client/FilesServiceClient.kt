package com.github.butvinmitmo.shared.client

import com.github.butvinmitmo.shared.dto.DownloadUrlResponse
import com.github.butvinmitmo.shared.dto.FileMetadataResponse
import com.github.butvinmitmo.shared.dto.PresignedUploadRequest
import com.github.butvinmitmo.shared.dto.PresignedUploadResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import java.util.UUID

/**
 * Feign client for files-service public API endpoints.
 * Used for E2E tests and external clients.
 */
@FeignClient(name = "files-service-public", url = "\${services.files-service.url:}")
interface FilesServiceClient {
    @PostMapping("/api/v0.0.1/files/presigned-upload")
    fun requestPresignedUpload(
        @RequestBody request: PresignedUploadRequest,
    ): ResponseEntity<PresignedUploadResponse>

    @GetMapping("/api/v0.0.1/files/{uploadId}")
    fun getUploadMetadata(
        @PathVariable uploadId: UUID,
    ): ResponseEntity<FileMetadataResponse>

    @GetMapping("/api/v0.0.1/files/{uploadId}/download-url")
    fun getDownloadUrl(
        @PathVariable uploadId: UUID,
    ): ResponseEntity<DownloadUrlResponse>

    @DeleteMapping("/api/v0.0.1/files/{uploadId}")
    fun deleteUpload(
        @PathVariable uploadId: UUID,
    ): ResponseEntity<Void>
}
