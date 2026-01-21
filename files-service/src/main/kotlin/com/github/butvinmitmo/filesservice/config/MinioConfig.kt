package com.github.butvinmitmo.filesservice.config

import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(MinioProperties::class, UploadProperties::class)
class MinioConfig(
    private val properties: MinioProperties,
) {
    private val logger = LoggerFactory.getLogger(MinioConfig::class.java)

    @Bean
    fun minioClient(): MinioClient =
        MinioClient
            .builder()
            .endpoint(properties.endpoint)
            .credentials(properties.accessKey, properties.secretKey)
            .build()

    @Bean
    fun presignedMinioClient(): MinioClient =
        MinioClient
            .builder()
            .endpoint(properties.getPresignedUrlEndpoint())
            .credentials(properties.accessKey, properties.secretKey)
            .region("us-east-1")
            .build()

    @Bean
    fun initializeBucket(minioClient: MinioClient): ApplicationRunner =
        ApplicationRunner {
            val bucketName = properties.bucket
            val bucketExists =
                minioClient.bucketExists(
                    BucketExistsArgs
                        .builder()
                        .bucket(bucketName)
                        .build(),
                )

            if (!bucketExists) {
                minioClient.makeBucket(
                    MakeBucketArgs
                        .builder()
                        .bucket(bucketName)
                        .build(),
                )
                logger.info("Created MinIO bucket: {}", bucketName)
            } else {
                logger.info("MinIO bucket already exists: {}", bucketName)
            }
        }
}
