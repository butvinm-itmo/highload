package com.github.butvinmitmo.shared.client

import com.github.butvinmitmo.shared.dto.FileUploadResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.multipart.MultipartFile

@FeignClient(
    name = "file-storage-service",
    url = "\${services.file-storage-service.url:}",
    configuration = [FeignMultipartConfig::class],
)
interface FileStorageServiceClient {
    @PostMapping(
        "/api/v0.0.1/files",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
    )
    fun uploadFile(
        @RequestPart("file") file: MultipartFile,
        @RequestParam("key") key: String,
    ): ResponseEntity<FileUploadResponse>

    @DeleteMapping("/api/v0.0.1/files")
    fun deleteFile(
        @RequestParam("key") key: String,
    ): ResponseEntity<Void>
}
