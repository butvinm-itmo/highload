package com.github.butvinmitmo.filesservice.api.controller

import com.github.butvinmitmo.filesservice.api.dto.FileUploadMetadataResponse
import com.github.butvinmitmo.filesservice.api.dto.PresignedUploadRequest
import com.github.butvinmitmo.filesservice.api.dto.PresignedUploadResponse
import com.github.butvinmitmo.filesservice.application.service.FileUploadService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.UUID

@RestController
@RequestMapping("/api/v0.0.1/files")
@Tag(name = "Files", description = "File upload operations")
@Validated
class FileUploadController(
    private val fileUploadService: FileUploadService,
) {
    @PostMapping("/presigned-upload")
    @Operation(
        summary = "Request presigned upload URL",
        description =
            "Generates a presigned URL for direct file upload to object storage. " +
                "Client should upload the file directly to the returned URL via HTTP PUT.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Presigned URL generated successfully",
                content = [Content(schema = Schema(implementation = PresignedUploadResponse::class))],
            ),
            ApiResponse(responseCode = "400", description = "Invalid content type or file name"),
            ApiResponse(responseCode = "401", description = "Not authenticated"),
        ],
    )
    fun requestPresignedUpload(
        @Valid @RequestBody request: PresignedUploadRequest,
    ): Mono<ResponseEntity<PresignedUploadResponse>> =
        fileUploadService.requestUpload(request.fileName, request.contentType).map { result ->
            ResponseEntity.ok(
                PresignedUploadResponse(
                    uploadId = result.uploadId,
                    uploadUrl = result.uploadUrl,
                    expiresAt = result.expiresAt,
                ),
            )
        }

    @GetMapping("/{uploadId}")
    @Operation(
        summary = "Get file upload metadata",
        description = "Retrieves metadata for a completed file upload.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "File metadata retrieved successfully",
                content = [Content(schema = Schema(implementation = FileUploadMetadataResponse::class))],
            ),
            ApiResponse(responseCode = "404", description = "Upload not found or not completed"),
            ApiResponse(responseCode = "401", description = "Not authenticated"),
        ],
    )
    fun getUploadMetadata(
        @Parameter(description = "Upload ID", required = true)
        @PathVariable
        uploadId: UUID,
    ): Mono<ResponseEntity<FileUploadMetadataResponse>> =
        fileUploadService.getUploadMetadata(uploadId).map { metadata ->
            ResponseEntity.ok(
                FileUploadMetadataResponse(
                    uploadId = metadata.uploadId,
                    originalFileName = metadata.originalFileName,
                    contentType = metadata.contentType,
                    fileSize = metadata.fileSize,
                    completedAt = metadata.completedAt,
                ),
            )
        }

    @GetMapping("/{uploadId}/download-url")
    @Operation(
        summary = "Get presigned download URL",
        description = "Generates a presigned URL for downloading the file from object storage.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Download URL generated successfully",
                content = [Content(schema = Schema(implementation = DownloadUrlResponse::class))],
            ),
            ApiResponse(responseCode = "404", description = "Upload not found or not completed"),
            ApiResponse(responseCode = "401", description = "Not authenticated"),
        ],
    )
    fun getDownloadUrl(
        @Parameter(description = "Upload ID", required = true)
        @PathVariable
        uploadId: UUID,
    ): Mono<ResponseEntity<DownloadUrlResponse>> =
        fileUploadService.getDownloadUrl(uploadId).map { url ->
            ResponseEntity.ok(DownloadUrlResponse(downloadUrl = url))
        }

    @DeleteMapping("/{uploadId}")
    @Operation(
        summary = "Delete file upload",
        description =
            "Deletes a file upload and removes the file from object storage. " +
                "Only the upload owner can delete their uploads.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "File deleted successfully"),
            ApiResponse(responseCode = "404", description = "Upload not found or does not belong to user"),
            ApiResponse(responseCode = "401", description = "Not authenticated"),
        ],
    )
    fun deleteUpload(
        @Parameter(description = "Upload ID", required = true)
        @PathVariable
        uploadId: UUID,
    ): Mono<ResponseEntity<Void>> =
        fileUploadService.deleteUpload(uploadId).then(
            Mono.just(ResponseEntity.noContent().build()),
        )
}

data class DownloadUrlResponse(
    val downloadUrl: String,
)
