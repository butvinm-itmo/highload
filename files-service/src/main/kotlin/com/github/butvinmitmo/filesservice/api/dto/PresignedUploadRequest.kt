package com.github.butvinmitmo.filesservice.api.dto

import jakarta.validation.constraints.NotBlank

data class PresignedUploadRequest(
    @field:NotBlank(message = "File name is required")
    val fileName: String,
    @field:NotBlank(message = "Content type is required")
    val contentType: String,
)
