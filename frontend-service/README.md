# Frontend Service - Tarology Web Application

React + TypeScript single-page application for the Tarology tarot reading service.

## Technology Stack

- **Framework:** React 19 + TypeScript 5.9
- **Build Tool:** Vite 7
- **Styling:** Tailwind CSS 3.4
- **Routing:** React Router 6.28
- **State Management:** React Query (TanStack Query) 5.62
- **HTTP Client:** Axios 1.7
- **Production Server:** Nginx (Alpine)

## Features

### Authentication & Authorization
- JWT-based authentication with token persistence
- Role-based access control (USER, MEDIUM, ADMIN)
- Protected routes with automatic redirect
- Hierarchical role permissions (ADMIN > MEDIUM > USER)

### Core Functionality
- **Spreads Feed:** Infinite scroll with cursor-based pagination
- **Create Spread:** Modal with layout type selection, automatic card drawing
- **Spread Detail:** Full view with cards, interpretations, and actions
- **Interpretations:** CRUD operations (MEDIUM/ADMIN only)
- **User Management:** Admin panel for user CRUD (ADMIN only)

### UI/UX
- Responsive design (mobile-first approach)
- Loading states and error boundaries
- Empty states with call-to-action
- Toast notifications for success/error
- Color-coded role badges
- Text-based tarot card placeholders

## Project Structure

```
frontend-service/
├── src/
│   ├── api/              # API client and service modules
│   │   ├── client.ts     # Axios instance with JWT interceptor
│   │   ├── authApi.ts    # Authentication endpoints
│   │   ├── usersApi.ts   # User CRUD
│   │   ├── cardsApi.ts   # Cards and layout types
│   │   ├── spreadsApi.ts # Spreads with pagination
│   │   └── interpretationsApi.ts # Interpretation CRUD
│   ├── components/       # Reusable React components
│   │   ├── Layout.tsx    # App layout with navbar
│   │   ├── ProtectedRoute.tsx # Route guard
│   │   ├── TarotCard.tsx # Card display component
│   │   ├── Loading.tsx   # Loading spinner
│   │   ├── ErrorBoundary.tsx # Error catching
│   │   └── EmptyState.tsx # Empty list states
│   ├── context/          # React context providers
│   │   └── AuthContext.tsx # Authentication state
│   ├── pages/            # Page components
│   │   ├── LoginPage.tsx
│   │   ├── SpreadsFeedPage.tsx
│   │   ├── SpreadDetailPage.tsx
│   │   └── UsersPage.tsx
│   ├── types/            # TypeScript type definitions
│   │   └── index.ts      # All DTOs and interfaces
│   ├── utils/            # Utility functions
│   │   └── errorHandling.ts # Error message extraction
│   ├── App.tsx           # Main app component with routing
│   └── main.tsx          # Entry point
├── Dockerfile            # Multi-stage production build
├── nginx.conf            # Nginx configuration for SPA
└── package.json          # Dependencies and scripts
```

## Development

### Prerequisites
- Node.js 20+
- npm 10+

### Environment Variables

Create `.env` file:
```bash
VITE_API_BASE_URL=http://localhost:8080/api/v0.0.1
```

### Commands

```bash
# Install dependencies
npm install

# Start development server (port 5173)
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Run linter
npm run lint
```

### Development Server

The dev server runs on `http://localhost:5173` with hot module replacement.

**Note:** Update the API base URL in `.env` if your gateway is running on a different port.

## Production Deployment

### Docker

```bash
# Build image
docker build -t frontend-service .

# Run container
docker run -p 3000:80 frontend-service
```

Access at `http://localhost:3000`

### Docker Compose

From project root:
```bash
docker compose up -d frontend-service
```

The frontend service will:
- Build from `frontend-service/Dockerfile`
- Serve on port 3000
- Wait for gateway-service to be healthy
- Serve via Nginx with SPA routing

### Manual Deployment

```bash
# Build
npm run build

# Serve dist/ directory with any static file server
# Ensure server is configured for SPA routing (fallback to index.html)
```

## API Integration

### Base URL Configuration

The frontend connects to the gateway service at the URL specified in `VITE_API_BASE_URL`.

**Local Development:** `http://localhost:8080/api/v0.0.1`
**Docker Compose:** `http://localhost:8080/api/v0.0.1` (gateway accessible from host)

### Authentication Flow

1. User submits credentials to `POST /auth/login`
2. Backend returns JWT token and user info
3. Token stored in `localStorage` as `auth_token`
4. Axios interceptor adds `Authorization: Bearer <token>` to all requests
5. On 401 response, token cleared and user redirected to login

### CORS Configuration

The gateway service must allow the frontend origin:
- Development: `http://localhost:3000`, `http://localhost:5173`
- Production: Configure as needed

See `gateway-service/src/main/kotlin/.../config/CorsConfig.kt`

## User Roles & Permissions

| Feature | USER | MEDIUM | ADMIN |
|---------|------|--------|-------|
| View spreads | ✓ | ✓ | ✓ |
| Create spread | ✓ | ✓ | ✓ |
| Delete own spread | ✓ | ✓ | ✓ |
| Delete any spread | ✗ | ✗ | ✓ |
| View interpretations | ✓ | ✓ | ✓ |
| Add interpretation | ✗ | ✓ | ✓ |
| Edit own interpretation | ✗ | ✓ | ✓ |
| Edit any interpretation | ✗ | ✗ | ✓ |
| Delete own interpretation | ✗ | ✓ | ✓ |
| Delete any interpretation | ✗ | ✗ | ✓ |
| View users | ✓ | ✓ | ✓ |
| Manage users | ✗ | ✗ | ✓ |

## Default Accounts

**Admin Account (Development Only):**
- Username: `admin`
- Password: `Admin@123`
- Role: ADMIN

## Browser Support

- Chrome/Edge 90+
- Firefox 88+
- Safari 14+

Modern browsers with ES2020+ support required.

## Troubleshooting

### CORS Errors

Ensure gateway-service has CORS configured for your frontend URL.

### 401 Unauthorized

- Check JWT token in localStorage
- Verify token hasn't expired (24h lifetime)
- Login again to refresh token

### Infinite Loading

- Check gateway-service is running on port 8080
- Verify `VITE_API_BASE_URL` in .env
- Check browser network tab for failed requests

### Docker Build Fails

- Ensure Node 20+ is used in Dockerfile
- Clear Docker cache: `docker builder prune`
- Check .dockerignore doesn't exclude necessary files

## License

This project is part of the Tarology Web Service microservices application.
