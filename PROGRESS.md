# PROGRESS.md

## Current Work: File Attachment for Interpretations

**Status:** COMPLETE

**Branch:** notification-service

---

## All Phases Complete

### Phase 1-2: file-storage-service (COMPLETE)
- Created new `file-storage-service` module
- MinIO integration with upload/download/delete
- Integration tests passing
- Files:
  - `file-storage-service/` (entire module)
  - `settings.gradle.kts` (added include)

### Phase 3: DTOs and Feign Client (COMPLETE)
- Added `FileUploadResponse` DTO
- Added `FileStorageServiceClient` Feign client
- Added `FeignMultipartConfig` for multipart support
- Files:
  - `shared-dto/.../FileUploadResponse.kt`
  - `shared-clients/.../FileStorageServiceClient.kt`
  - `shared-clients/.../FeignMultipartConfig.kt`
  - `shared-clients/build.gradle.kts` (added feign-form deps)

### Phase 4: File Validation (COMPLETE)
- Added `FileValidator` utility (extension + content-type validation, 2MB max)
- Added `InvalidFileException`
- Unit tests passing
- Files:
  - `divination-service/.../util/FileValidator.kt`
  - `divination-service/.../exception/Exceptions.kt`
  - `divination-service/.../exception/GlobalExceptionHandler.kt`
  - `divination-service/src/test/.../FileValidatorTest.kt`

### Phase 5: Entity and DTO Updates (COMPLETE)
- Added `file_key` column to interpretation (migration V4)
- Added `fileKey` field to Interpretation entity
- Added `fileUrl` field to InterpretationDto
- Updated InterpretationMapper to build fileUrl
- Files:
  - `divination-service/.../db/migration/V4__add_file_key_to_interpretation.sql`
  - `divination-service/.../entity/Interpretation.kt`
  - `shared-dto/.../InterpretationDto.kt`
  - `divination-service/.../mapper/InterpretationMapper.kt`

### Phase 6: Controller Endpoints (COMPLETE)
- Added `POST /{id}/file` - upload file to interpretation
- Added `DELETE /{id}/file` - delete file from interpretation
- Updated `deleteInterpretation` to cascade file deletion
- Files:
  - `divination-service/.../controller/InterpretationController.kt`
  - `divination-service/.../service/DivinationService.kt`
  - `divination-service/build.gradle.kts` (added spring-test dep)

### Phase 7: Infrastructure (COMPLETE)
- Added MinIO to docker-compose.yml
- Added file-storage-service to docker-compose.yml
- Created `highload-config/file-storage-service.yml`
- Updated `highload-config/gateway-service.yml` (added route)
- Updated `highload-config/divination-service.yml` (added file config, resilience4j)
- Files:
  - `docker-compose.yml`
  - `highload-config/file-storage-service.yml`
  - `highload-config/gateway-service.yml`
  - `highload-config/divination-service.yml`

### Phase 8: E2E Tests (COMPLETE)
- Created `FileAttachmentE2ETest.kt` with comprehensive tests:
  - Upload PNG file
  - Download file
  - Upload duplicate file (409)
  - Delete file
  - Upload JPG file
  - Invalid file type (400)
  - Oversized file (400)
  - Non-author upload (403)
  - Delete interpretation cascades file deletion
- Files:
  - `e2e-tests/.../FileAttachmentE2ETest.kt`

### Phase 9: Documentation (COMPLETE)
- CLAUDE.md already updated with:
  - file-storage-service in microservices table (port 8085)
  - interpretation schema with file_key column
  - File endpoints in divination-service
  - file-storage-service endpoints
  - MINIO_* environment variables
  - File Storage section

---

## Tests Status

- `./gradlew :divination-service:test` - PASSING
- `./gradlew :file-storage-service:test` - PASSING
- `./gradlew :e2e-tests:compileTestKotlin` - PASSING (E2E tests require docker compose up)

---

## Completed Fixes

1. **DivinationServiceTest fixed** - added mocks for `FileStorageServiceClient` and `FileValidator`
2. **highload-config submodule committed** - changes pushed to submodule
