# UI Implementation Plan

## Overview
Build a React + TypeScript SPA as a separate frontend service for the Tarology Web Service microservices application.

**Tech Stack:** React 18 + TypeScript + Vite, Tailwind CSS, React Router, Axios, React Query

**Deployment:** Separate frontend service in Docker Compose (port 3000)

---

## Stage 1: Project Foundation & Setup

**Goal:** Initialize React project with all dependencies and basic configuration

**Tasks:**
- Create `frontend-service/` directory with Vite + React + TypeScript template
- Install dependencies: `react-router-dom`, `axios`, `@tanstack/react-query`, `tailwindcss`
- Configure Tailwind CSS (tailwind.config.js, postcss.config.js, index.css)
- Set up project structure (src/components, src/pages, src/api, src/context, src/types)
- Create basic TypeScript configuration
- Add .env file with API gateway URL configuration

**Commit Message:** "feat: Initialize frontend-service with React + TypeScript + Vite"

---

## Stage 2: TypeScript Types & API Client

**Goal:** Create type definitions and API client infrastructure

**Tasks:**
- Create TypeScript types from shared-dto DTOs (User, Spread, Card, Interpretation, etc.)
- Build Axios API client with base configuration
- Implement JWT token interceptor for authenticated requests
- Create API service modules (authApi, usersApi, spreadsApi, cardsApi, interpretationsApi)
- Add error handling utilities

**Files Created:**
- `src/types/index.ts` - All TypeScript interfaces
- `src/api/client.ts` - Axios instance with interceptors
- `src/api/authApi.ts` - Login endpoint
- `src/api/usersApi.ts` - User CRUD endpoints
- `src/api/spreadsApi.ts` - Spread CRUD endpoints
- `src/api/cardsApi.ts` - Cards and layout types
- `src/api/interpretationsApi.ts` - Interpretation CRUD

**Commit Message:** "feat: Add TypeScript types and API client infrastructure"

---

## Stage 3: Authentication & Route Protection

**Goal:** Implement authentication flow and protected routing

**Tasks:**
- Create AuthContext with login/logout/token management
- Implement localStorage token persistence
- Build Login page with form validation
- Create ProtectedRoute component with role-based access
- Set up React Router with route definitions
- Build basic App layout with navbar

**Files Created:**
- `src/context/AuthContext.tsx` - Auth state management
- `src/pages/LoginPage.tsx` - Login form
- `src/components/ProtectedRoute.tsx` - Route guard
- `src/components/Layout.tsx` - App layout with navbar
- `src/App.tsx` - Router configuration

**Commit Message:** "feat: Implement authentication and protected routing"

---

## Stage 4: Spreads Feed & Creation

**Goal:** Build core spread browsing and creation features

**Tasks:**
- Create Spreads feed page with infinite scroll
- Implement spread creation modal with layout type selection
- Build Card component (text placeholder with styling)
- Add spread deletion functionality (author/ADMIN only)
- Integrate with React Query for data fetching and caching

**Files Created:**
- `src/pages/SpreadsFeedPage.tsx` - Infinite scroll spreads list
- `src/components/CreateSpreadModal.tsx` - Spread creation form
- `src/components/SpreadCard.tsx` - Spread summary card
- `src/components/TarotCard.tsx` - Individual tarot card display

**Commit Message:** "feat: Add spreads feed with infinite scroll and creation"

---

## Stage 5: Spread Detail & Interpretations

**Goal:** Build spread detail view with interpretation CRUD

**Tasks:**
- Create Spread detail page showing question, cards, and interpretations
- Build interpretation list component
- Implement add interpretation form (MEDIUM/ADMIN only)
- Add edit interpretation functionality (author/ADMIN only)
- Add delete interpretation functionality (author/ADMIN only)
- Display card positions and reversed state

**Files Created:**
- `src/pages/SpreadDetailPage.tsx` - Full spread view
- `src/components/InterpretationList.tsx` - Interpretations display
- `src/components/AddInterpretationForm.tsx` - Add interpretation
- `src/components/EditInterpretationModal.tsx` - Edit interpretation

**Commit Message:** "feat: Add spread detail page with interpretation CRUD"

---

## Stage 6: User Management (ADMIN)

**Goal:** Build admin panel for user management

**Tasks:**
- Create Users list page with pagination
- Build Create User modal with role selection
- Add Edit User functionality
- Add Delete User functionality with confirmation
- Implement role-based UI rendering (only show for ADMIN)

**Files Created:**
- `src/pages/UsersPage.tsx` - Users list with pagination
- `src/components/CreateUserModal.tsx` - User creation form
- `src/components/EditUserModal.tsx` - User edit form
- `src/components/DeleteConfirmModal.tsx` - Reusable delete confirmation

**Commit Message:** "feat: Add user management pages for ADMIN role"

---

## Stage 7: UI Polish & Shared Components

**Goal:** Enhance UX with polished shared components

**Tasks:**
- Build Loading spinner component
- Create Error boundary component
- Add Toast notifications for success/error messages
- Build role badge component for navbar
- Implement responsive mobile design
- Add form validation utilities
- Create empty states for lists

**Files Created:**
- `src/components/Loading.tsx` - Loading spinner
- `src/components/ErrorBoundary.tsx` - Error handling
- `src/components/Toast.tsx` - Toast notifications
- `src/components/RoleBadge.tsx` - User role display
- `src/components/EmptyState.tsx` - Empty list states

**Commit Message:** "feat: Add UI polish with shared components and responsive design"

---

## Stage 8: Backend Integration (CORS)

**Goal:** Enable frontend-backend communication

**Tasks:**
- Add CORS configuration to gateway-service
- Update gateway-service application.yml to allow port 3000
- Test CORS with preflight OPTIONS requests

**Files Modified:**
- `gateway-service/src/main/resources/application.yml` (or create CorsConfiguration class)

**Commit Message:** "feat: Add CORS configuration to gateway-service for frontend"

---

## Stage 9: Docker Integration

**Goal:** Containerize frontend and add to Docker Compose

**Tasks:**
- Create multi-stage Dockerfile for frontend-service (build + nginx serve)
- Add frontend-service to docker-compose.yml
- Configure environment variables for API gateway URL
- Add health check for frontend service
- Create .dockerignore file

**Files Created:**
- `frontend-service/Dockerfile` - Multi-stage build
- `frontend-service/.dockerignore` - Exclude node_modules
- `frontend-service/nginx.conf` - Nginx configuration for SPA routing

**Files Modified:**
- `docker-compose.yml` - Add frontend-service

**Commit Message:** "feat: Add Docker configuration for frontend-service"

---

## Stage 10: Documentation & Testing

**Goal:** Document the frontend and verify all flows

**Tasks:**
- Create README.md in frontend-service with setup instructions
- Add frontend section to main project README
- Manual testing of all user flows:
  - Login/logout
  - Create spread (all layout types)
  - View spreads feed with infinite scroll
  - View spread detail
  - Add/edit/delete interpretation (MEDIUM/ADMIN)
  - User management (ADMIN)
- Update PROGRESS.md with completion status
- Update CLAUDE.md with frontend context

**Files Created:**
- `frontend-service/README.md` - Frontend setup guide

**Files Modified:**
- `README.md` - Add frontend documentation
- `PROGRESS.md` - Final status
- `CLAUDE.md` - Add frontend architecture notes

**Commit Message:** "docs: Add frontend documentation and update project status"

---

## Summary

**Total Stages:** 10 independent stages
**Estimated Files:** ~40 new files
**Services Modified:** gateway-service (CORS only)
**New Services:** frontend-service (React SPA)

Each stage is independently committable and adds incremental value to the application.
