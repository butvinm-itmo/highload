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

- [x] **Phase 3:** Remove per-service Swagger UI
  - Changed user-service from webmvc-ui to webmvc-api
  - Changed tarot-service from webflux-ui to webflux-api
  - Changed divination-service from webflux-ui to webflux-api
  - Build successful, services start correctly
  - Per-service Swagger UI disabled, API docs still available for gateway proxying

- [ ] **Phase 4:** Update CLAUDE.md Documentation
