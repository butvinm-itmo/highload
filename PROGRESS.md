# Centralized Swagger UI Implementation Progress

## Task: Implement single Swagger UI on API Gateway

### Phases

- [x] **Phase 1:** Add springdoc dependency to gateway-service
  - Added `springdoc-openapi-starter-webflux-ui:2.8.4` to gateway-service
  - Build verified successfully

- [x] **Phase 2:** Configure gateway Swagger UI
  - Added springdoc configuration to gateway-service.yml with URL groups for all services
  - Added OpenAPI proxy routes to rewrite /v3/api-docs/{service} to backend /api-docs
  - Updated public paths to allow Swagger UI access without JWT authentication
  - Verified: http://localhost:8080/swagger-ui.html shows dropdown with all 3 services
  - All API endpoints properly documented and accessible

- [ ] **Phase 3:** Remove per-service Swagger UI
  - Change service dependencies from -ui to -api variant

- [ ] **Phase 4:** Update CLAUDE.md Documentation
