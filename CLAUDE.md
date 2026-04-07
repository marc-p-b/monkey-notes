# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

MonkeyNotes is a document OCR and management platform for eInk tablet users. It automatically extracts handwritten notes from PDF exports using AI-powered OCR (Qwen VL), provides full-text search via Lucene, named entity extraction, and AI agent interaction via OpenAI.

## Components

### Backend

* Provide API endpoints for frontend and mobile app
* Store received PDFs, either :
  * Pushed from MonkeySync App
  * Synced form Google Drive using Google API (original method - complicated)
* Historic architecture comes from legacy Google Drive sync (such as google drive file ids)
* Request OCR (using remote API) to extract text from PDFs (transcripts)
* Store all document data, including transcripts, into a PostGreSQL database
* Provide authentication for multi-user frontends

### Frontend

Main features

* Multi user UI
* Transcript browsing (using folders)
* Transcript view
* Customize user parameters
* Choose sync method : MonkeySync or Google Drive

### MonkeySync App

A mobile App installed on tablet devices such as Boox eInk (Android OS)
Such devices may automatically or manually exports PDFs from notes taken

MonkeySync scan input folders to push notes PDF to the backend

* Setup user credential
* Choose folder to sync
* Background service to scan input folder and process files
* A local database is used to store configs and processed documents

## Tech Stack

- **Backend**: Java 21, Spring Boot 3.3.5, PostgreSQL, Lucene 10.3.1
- **Frontend**: Vue 3.4, PrimeVue 4.2.5, Vite 5.2, Pinia
- **Mobile App**: Flutter
- **Build**: Maven (backend), npm/Vite (frontend)
- **Deployment**: Docker Compose with Nginx

## Build & Run Commands

### Backend
```bash
mvn clean package                    # Build JAR
mvn spring-boot:run                  # Run with default profile
```

### Frontend
```bash
cd ui
npm install                          # Install dependencies
npm run dev                          # Dev server at localhost:5173
npm run build                        # Production build to dist/
```

### Docker
```bash
# From docker/compose/ directory
docker compose up                    # Start full stack
```

## Architecture

### Backend (`src/main/java/fr/monkeynotes/mn/`)
- `controller/` - REST API endpoints (auth, data, transcripts, search, agents, etc.)
- `service/impl/` - Business logic (DriveService, QwenService, SearchService, etc.)
- `data/entity/` - JPA entities (File, Transcript, TranscriptPage, User, etc.)
- `data/repository/` - Spring Data JPA repositories
- `tasks/` - Scheduled/async tasks (token refresh, Drive watch, flush, mailer)
- `JwtFilter.java` / `JwtUtil.java` - JWT authentication

### Frontend (`ui/src/`)
- `components/` - Vue SFCs (Home, TranscriptView, SearchView, AgentView, etc.)
- `router/index.ts` - Route definitions with JWT validation
- `composables/store.js` - Pinia state management
- `requests.ts` - API client utilities

### Key Data Flow
1. Google Drive sync downloads PDFs to process
2. Qwen VL OCR extracts text from handwritten pages
3. Named entities are extracted (titles, tags, dates, people)
4. Lucene indexes transcription text for search
5. Delta updates track page modifications

## Configuration

Spring profiles control environment settings:
- `application-dev.yaml` - Local development (gitignored, contains secrets)
- `application-docker.yaml` - Docker environment
- `application-prod.yaml` - Production
- `application-template.yml` - Template showing required properties

Frontend API endpoint configured in `ui/env.js` for production.

## Named Entity Syntax

The OCR system recognizes these patterns in handwritten notes:
- Markdown titles: `# Title`, `## Subtitle`
- Tags: `[T:tag-name]`
- People: `[p:person-name]`
- Emails: `[@:email@domain.com]`
- EU dates: `[d:DD/MM/YY]`
- US dates: `[du:MM/DD/YY]`
- Checkboxes: `[X:todo]`, `[V:done]`

## External Services

- **Google Drive API** - OAuth 2.0 for syncing PDFs (requires HTTPS callback URL)
- **Qwen VL API** - Handwriting OCR
- **OpenAI API** - Agent interactions (optional)
- **Mailjet** - Email notifications (optional)

## Development Notes

- Backend requires HTTPS for Google Drive OAuth callbacks (use pinggy/ngrok tunnel)
- Frontend dev server runs at localhost:5173, backend at localhost:8080
- On first run, admin password is printed to logs
- JWT-based stateless authentication throughout
