# Frontend Integration Plan for Backend Changes

**Last Updated:** 2025-12-25
**Branch:** `ui`

This plan outlines the frontend changes required to integrate with the new backend features:
1. **File Storage Service** - MinIO-based file attachments for interpretations
2. **Notification Service** - Real-time notifications with Kafka + WebSocket
3. **Updated InterpretationDto** - Added `fileUrl` field

---

## Stage 1: Update TypeScript Types & API Client

**Goal:** Add support for new DTOs and file upload capabilities

**Tasks:**
1. Update `InterpretationDto` type to include optional `fileUrl: string`
2. Add `NotificationDto`, `NotificationType`, and `ReferenceType` types
3. Add `UnreadCountResponse` and `MarkAllReadResponse` types
4. Add `FileUploadResponse` type
5. Update API client types in `src/types/index.ts`

**Files Modified:**
- `src/types/index.ts`

**Acceptance Criteria:**
- All new backend DTOs have TypeScript equivalents
- Type safety maintained across all API calls
- No compilation errors

**Commit Message:** "feat: Add TypeScript types for file attachments and notifications"

---

## Stage 2: Create File Storage API Module

**Goal:** Add API client for file upload/download/delete operations

**Tasks:**
1. Create `src/api/filesApi.ts` with file operations:
   - `uploadFile(file: File, key: string)` - multipart/form-data upload
   - `deleteFile(key: string)` - delete file by key
   - `getFileUrl(key: string)` - construct download URL
2. Configure Axios to handle multipart/form-data requests
3. Add file validation utilities (PNG/JPG only, max 2MB)
4. Export from `src/api/index.ts`

**Files Created:**
- `src/api/filesApi.ts`
- `src/utils/fileValidation.ts`

**Files Modified:**
- `src/api/index.ts`

**Acceptance Criteria:**
- File upload works with proper content-type headers
- File validation prevents invalid uploads
- Download URLs correctly constructed

**Commit Message:** "feat: Add file storage API client with validation"

---

## Stage 3: Add File Attachment to Interpretations

**Goal:** Enable users to attach images when creating/editing interpretations

**Tasks:**
1. Update `AddInterpretationForm` component:
   - Add file input for image upload (optional)
   - Show file preview before submission
   - Validate file type and size client-side
   - Upload file BEFORE creating interpretation
   - Include file key in interpretation creation
2. Update `EditInterpretationModal`:
   - Show existing attachment if present
   - Allow replacing or removing attachment
   - Handle file upload on update
3. Update `InterpretationList`:
   - Display attached image if `fileUrl` exists
   - Add lightbox/modal for full-size image view
   - Show download link for attachments

**Files Modified:**
- `src/components/AddInterpretationForm.tsx`
- `src/components/EditInterpretationModal.tsx`
- `src/components/InterpretationList.tsx`

**New Components:**
- `src/components/ImageLightbox.tsx` (for viewing full-size images)

**Acceptance Criteria:**
- Users can attach PNG/JPG images (max 2MB) to interpretations
- File preview works before submission
- Existing attachments display correctly
- File upload errors handled gracefully
- Deleting interpretation removes attached file (backend handles this)

**Commit Message:** "feat: Add file attachment support to interpretations"

---

## Stage 4: Create Notifications API Module

**Goal:** Add API client for notification operations

**Tasks:**
1. Create `src/api/notificationsApi.ts` with operations:
   - `getNotifications(page, size)` - paginated list
   - `getUnreadCount()` - fetch unread count
   - `markAsRead(id)` - mark single notification as read
   - `markAllAsRead()` - mark all as read
2. Add React Query hooks for notifications:
   - `useNotifications` - paginated query
   - `useUnreadCount` - auto-refetch every 30s
   - `useMarkAsRead` - mutation with optimistic updates
3. Export from `src/api/index.ts`

**Files Created:**
- `src/api/notificationsApi.ts`
- `src/hooks/useNotifications.ts`

**Files Modified:**
- `src/api/index.ts`

**Acceptance Criteria:**
- Notifications fetch correctly with pagination
- Unread count updates automatically
- Marking as read works with optimistic UI updates

**Commit Message:** "feat: Add notifications API client and React Query hooks"

---

## Stage 5: Add WebSocket Support for Real-Time Notifications

**Goal:** Implement WebSocket client for live notification updates

**Tasks:**
1. Create WebSocket client utility:
   - Connect to `ws://localhost:8080/ws/notifications` via gateway
   - Include JWT token in connection URL as query param: `?token=<jwt>`
   - Handle connection lifecycle (connect, disconnect, reconnect)
   - Parse incoming notification events
2. Create `NotificationContext`:
   - Manage WebSocket connection state
   - Subscribe to notification events
   - Update unread count on new notifications
   - Trigger toast/snackbar for new notifications
3. Integrate with React Query:
   - Invalidate notification queries on WebSocket event
   - Keep local cache in sync with server

**Files Created:**
- `src/utils/websocket.ts`
- `src/context/NotificationContext.tsx`

**Files Modified:**
- `src/App.tsx` (wrap with NotificationContext)

**Acceptance Criteria:**
- WebSocket connects successfully with JWT authentication
- New notifications appear in real-time without page refresh
- Connection reconnects automatically on disconnect
- Unread count updates immediately on new notification

**Commit Message:** "feat: Add WebSocket support for real-time notifications"

---

## Stage 6: Build Notifications UI Components

**Goal:** Create user-facing notification components

**Tasks:**
1. Create `NotificationBell` component:
   - Bell icon in navbar with unread count badge
   - Dropdown menu showing recent notifications (5-10)
   - Click notification to navigate to referenced spread
   - Mark as read on click
   - "View all" link to full notifications page
2. Create `NotificationsPage`:
   - Full paginated list of all notifications
   - Filter by read/unread status
   - Bulk "Mark all as read" action
   - Click to navigate to spread/interpretation
3. Add toast notifications:
   - Show brief toast when new notification arrives
   - Auto-dismiss after 5 seconds
   - Click toast to navigate to reference

**Files Created:**
- `src/components/NotificationBell.tsx`
- `src/components/NotificationToast.tsx`
- `src/pages/NotificationsPage.tsx`

**Files Modified:**
- `src/components/Layout.tsx` (add NotificationBell to navbar)
- `src/App.tsx` (add route for NotificationsPage)

**Acceptance Criteria:**
- Notification bell shows correct unread count
- Dropdown displays recent notifications
- Clicking notification navigates to correct spread
- Toasts appear for real-time notifications
- Full notifications page works with pagination

**Commit Message:** "feat: Add notification bell and notifications page UI"

---

## Stage 7: Update CORS Configuration (Verification)

**Goal:** Ensure gateway allows WebSocket and file upload requests

**Tasks:**
1. Verify gateway CORS config allows:
   - WebSocket upgrade requests
   - Multipart/form-data content type
   - File download responses with proper content-disposition
2. Test CORS preflight for file uploads
3. Test WebSocket connection through gateway

**Files to Check:**
- `gateway-service/src/main/kotlin/com/github/butvinmitmo/gatewayservice/config/CorsConfig.kt` (backend - verify only)

**Acceptance Criteria:**
- File uploads work through gateway without CORS errors
- WebSocket connection succeeds through gateway
- File downloads work with proper headers

**Commit Message:** None (verification only, no code changes expected)

---

## Stage 8: Update Docker Configuration (Verification)

**Goal:** Ensure frontend container can access new services

**Tasks:**
1. Verify `docker-compose.yml` includes:
   - `file-storage-service` on port 8085
   - `notification-service` on port 8084
   - MinIO service for file storage
   - Kafka + Zookeeper for messaging
2. Update frontend environment variables if needed
3. Test full stack in Docker

**Files Modified:**
- None (verification only, backend already updated)

**Acceptance Criteria:**
- All services start successfully in Docker
- Frontend can reach file-storage-service via gateway
- Frontend can reach notification-service via gateway
- WebSocket connection works in Docker environment

**Commit Message:** None (verification only, no code changes expected)

---

## Stage 9: Add Loading States & Error Handling

**Goal:** Improve UX for file uploads and notifications

**Tasks:**
1. Add file upload progress indicators:
   - Show progress bar during file upload
   - Disable form submission while uploading
   - Handle upload errors with retry option
2. Add notification error handling:
   - Graceful degradation if WebSocket fails
   - Fallback to polling if WebSocket unavailable
   - Show connection status indicator
3. Add empty states:
   - "No notifications" when list is empty
   - "Failed to load notifications" on error

**Files Modified:**
- `src/components/AddInterpretationForm.tsx`
- `src/components/EditInterpretationModal.tsx`
- `src/components/NotificationBell.tsx`
- `src/pages/NotificationsPage.tsx`
- `src/context/NotificationContext.tsx`

**Acceptance Criteria:**
- File upload shows clear progress feedback
- Users can retry failed uploads
- WebSocket failures don't break the app
- Empty states display appropriately

**Commit Message:** "feat: Add loading states and error handling for new features"

---

## Stage 10: Update Documentation

**Goal:** Document new features and architecture changes

**Tasks:**
1. Update `frontend-service/README.md`:
   - Document file attachment feature
   - Document notification system
   - Add WebSocket connection details
   - Update architecture diagram
2. Update `PROGRESS.md`:
   - Mark Lab 4 (Messaging & File Management) as complete
   - Document all 10 stages
3. Update `CLAUDE.md`:
   - Add file upload patterns
   - Add WebSocket best practices
   - Update frontend architecture notes

**Files Modified:**
- `frontend-service/README.md`
- `PROGRESS.md`
- `CLAUDE.md`

**Acceptance Criteria:**
- All new features documented
- README includes usage examples
- Architecture reflects current state

**Commit Message:** "docs: Update documentation for file attachments and notifications"

---

## Testing Strategy

Each stage should include:
1. Manual testing in development mode (`npm run dev`)
2. Manual testing in Docker (`docker compose up -d`)
3. Browser console checks for errors
4. Network tab verification for API calls

**Key Test Scenarios:**
- Upload image attachment to interpretation
- Receive real-time notification when someone adds interpretation to your spread
- Mark notifications as read
- Download attached images
- WebSocket reconnection after network interruption

---

## Rollback Plan

If issues arise:
1. Each stage is independently committable
2. Can revert individual commits without affecting others
3. Frontend remains backward compatible (fileUrl and notifications are optional)
4. Existing features continue to work if new services are down

---

## Dependencies

- Backend changes must be pulled and running (already done)
- MinIO, Kafka, and new services running in Docker
- Gateway routing configured for new services
- JWT authentication working for WebSocket

---

## Estimated Complexity

| Stage | Complexity | Risk |
|-------|------------|------|
| Stage 1 | Low | Low |
| Stage 2 | Medium | Low |
| Stage 3 | High | Medium |
| Stage 4 | Low | Low |
| Stage 5 | High | High |
| Stage 6 | Medium | Medium |
| Stage 7 | Low | Low |
| Stage 8 | Low | Low |
| Stage 9 | Medium | Low |
| Stage 10 | Low | Low |

**Highest Risk:** Stage 5 (WebSocket) - requires careful authentication and connection management
**Most Complex:** Stage 3 (File Attachments) - requires coordinating upload before interpretation creation

---

## Success Criteria

Frontend integration complete when:
1. Users can attach images to interpretations
2. Attached images display and download correctly
3. Real-time notifications work via WebSocket
4. Notification bell shows unread count
5. Users can mark notifications as read
6. All features work in Docker environment
7. No console errors or warnings
8. Documentation updated
9. PROGRESS.md reflects completion
10. Ready to merge to master
