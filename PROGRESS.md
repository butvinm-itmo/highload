# Centralized Swagger UI Implementation Progress

## Task: Implement single Swagger UI on API Gateway

### Phases

- [x] **Phase 1:** Add springdoc dependency to gateway-service
  - Added `springdoc-openapi-starter-webflux-ui:2.8.4` to gateway-service
  - Build verified successfully

- [ ] **Phase 2:** Configure gateway Swagger UI
  - Add springdoc configuration to gateway-service.yml
  - Add OpenAPI proxy routes
  - Update public paths for JWT filter bypass

- [ ] **Phase 3:** Remove per-service Swagger UI
  - Change service dependencies from -ui to -api variant

- [ ] **Phase 4:** Update CLAUDE.md Documentation
