package com.github.butvinmitmo.filestorageservice.controller

import com.github.butvinmitmo.filestorageservice.service.FileStorageService
import com.github.butvinmitmo.shared.dto.ErrorResponse
import com.github.butvinmitmo.shared.dto.FileUploadResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v0.0.1/files")
@Tag(name = "Files", description = "File storage operations")
class FileController(
    private val fileStorageService: FileStorageService,
) {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(
        summary = "Upload a file",
        description = "Uploads a file to storage with the specified key",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "File uploaded successfully",
                content = [Content(schema = Schema(implementation = FileUploadResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid request",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun uploadFile(
        @Parameter(description = "File to upload", required = true)
        @RequestParam("file")
        file: MultipartFile,
        @Parameter(description = "Storage key for the file", required = true)
        @RequestParam("key")
        key: String,
    ): ResponseEntity<FileUploadResponse> {
        val url =
            fileStorageService.uploadFile(
                key = key,
                inputStream = file.inputStream,
                contentType = file.contentType ?: "application/octet-stream",
                size = file.size,
            )

        val response =
            FileUploadResponse(
                key = key,
                url = url,
            )

        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @DeleteMapping
    @Operation(
        summary = "Delete a file",
        description = "Deletes a file from storage by its key",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "File deleted successfully"),
            ApiResponse(
                responseCode = "404",
                description = "File not found",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun deleteFile(
        @Parameter(description = "Storage key of the file to delete", required = true)
        @RequestParam("key")
        key: String,
    ): ResponseEntity<Void> {
        fileStorageService.deleteFile(key)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{*key}")
    @Operation(
        summary = "Download a file",
        description = "Downloads a file from storage by its key",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "File retrieved successfully",
                content = [Content(mediaType = "application/octet-stream")],
            ),
            ApiResponse(
                responseCode = "404",
                description = "File not found",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getFile(
        @Parameter(description = "Storage key of the file", required = true)
        @PathVariable("key")
        key: String,
    ): ResponseEntity<InputStreamResource> {
        val contentType = fileStorageService.getContentType(key)
        val inputStream = fileStorageService.getFile(key)

        return ResponseEntity
            .ok()
            .header(HttpHeaders.CONTENT_TYPE, contentType)
            .body(InputStreamResource(inputStream))
    }
}
