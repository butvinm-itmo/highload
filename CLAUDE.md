# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Tarology Web Service** - A Kotlin/Spring Boot application for Tarot card readings and interpretations. Users can create spreads, view others' spreads, and add interpretations without authentication (user ID-based identification).

Key features:
- Create tarot spreads with different layouts (one card, three cards, cross/five cards)
- View all spreads in chronological feed
- Add/edit/delete interpretations for spreads
- User management with transactional deletion

## Build & Development Commands

### Build and Run
```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.github.butvinm_itmo.highload.HighloadApplicationTests"
```

## Architecture

### Technology Stack
- **Language:** Kotlin 2.2.10
- **Framework:** Spring Boot 4.0.0-M3
- **Build Tool:** Gradle with Kotlin DSL
- **JVM:** Java 21

### Database Schema
Uses UUID-based identifiers for all entities. Main tables:
- `user` - User accounts (id, username)
- `spread` - Tarot spreads (id, question, layout_type_id, created_at, author_id)
- `interpretation` - User interpretations (id, text, created_at, author_id, spread_id)
  - Unique constraint: (author_id, spread_id) - one interpretation per user per spread
- `card` - Tarot cards (id, name, arcana_type_id)
- `spread_card` - Cards in spreads (id, spread_id, card_id, position_in_spread, is_reversed)
- `layout_type` - Spread layouts (ONE_CARD, THREE_CARDS, CROSS)
- `arcana_type` - Card types (MAJOR, MINOR)

### API Endpoints

Base path: `/api/v0.0.1`

**Spreads:**
- `POST /spreads` - Create spread
- `GET /spreads?page=N&size=M` - Paginated list with X-Total-Count header
- `GET /spreads/scroll?after=ID&size=N` - Infinite scroll
- `GET /spreads/{id}` - Get spread details
- `DELETE /spreads/{id}` - Delete spread (author only)

**Interpretations:**
- `POST /spreads/{spreadId}/interpretations` - Add interpretation (409 if duplicate)
- `PUT /spreads/{spreadId}/interpretations/{id}` - Edit interpretation (author only)
- `DELETE /spreads/{spreadId}/interpretations/{id}` - Delete interpretation (author only)

**Users:**
- `POST /users` - Create user (409 if exists)
- `GET /users?page=N&size=M` - List users
- `GET /users/{id}` - Get user
- `PUT /users/{id}` - Update user
- `DELETE /users/{id}` - Delete user with all data (transactional)

### Transactional Requirements

Critical operations requiring single transaction:
1. **Spread creation:** Insert spread + generate cards + link cards in spread_card
2. **Spread deletion:** Delete interpretations + spread_cards + spread
3. **User deletion:** Delete user's spreads → interpretations on those spreads → spread_cards + delete user's interpretations on others' spreads + user record
