# PROGRESS.md

## Current Work: File Attachment for Interpretations

**Status:** IN PROGRESS - Implementation done, tests need fixing

**Branch:** notification-service

---

## What Was Done

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
  - `shared-dto/.../FileDto.kt`
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

---

## What Remains

### Phase 8: E2E Tests (NOT STARTED)
- Create `FileAttachmentE2ETest.kt`

### Phase 9: Documentation (IN PROGRESS)
- Update CLAUDE.md with file-storage-service info

---

## Completed Fixes

1. **DivinationServiceTest fixed** - added mocks for `FileStorageServiceClient` and `FileValidator`
2. **highload-config submodule committed** - changes pushed to submodule
