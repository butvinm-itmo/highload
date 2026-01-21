package com.github.butvinmitmo.filesservice.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "upload")
data class UploadProperties(
    val expirationMinutes: Int,
    val maxFileSize: Long,
    val allowedContentTypes: List<String>,
    val cleanupIntervalMs: Long,
)
