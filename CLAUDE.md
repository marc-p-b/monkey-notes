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


Markdown titles: `# Title`, `## Subtitle`

The OCR system recognizes these patterns in handwritten notes:

- < VERB : VALUE >
- ( VERB : VALUE )
- [ VERB : VALUE ]

- DG : diagram current page
- DGN : diagram next page
- DT : date (DD/MM/YY)
- DI : date inverted (YY/MM/DD)
- T : tag
- P : person
- @ : email
- L : link
- V : checked checkbox
- X : unchecked checkbox

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

## OCR Prompts

handwritten notes in french. extract text from image. do not add other text.

# Syncing test logs

```
Boox export

------------------------------
monkeyFileEvent.filePath: /storage/emulated/0/note/test3/Folder-1/Notebook-1.pdf
monkeyFileEvent.fileName: 2026-06-04_11_46_02.pdf
monkeyFileEvent.rootFolderPath: /storage/emulated/0/note/test3
currentRemoteFolderPath: /storage/emulated/0/note/test3
basePath: /Folder-1
filename: Notebook-1.pdf
virtualPath: /Folder-1/Notebook-1.pdf
msId: ms0a253c889670c4a3fef36915fe106fbd516ca7222601c06d055533f5e2137644
targetFilePath: /home/marc/.monkeynotes/user_data/marc/downloads/ms304df708f2c938aa6733d007a753947bbf6293ef475e32797cfd7f1e2d7c1264/ms0a253c889670c4a3fef36915fe106fbd516ca7222601c06d055533f5e2137644
------------------------------
[a1cbb87bbb42] [MonkeySync.syncPdf] [] Adding file name Notebook-1.pdf id Notebook-1.pdf status created - remote path /Folder-1/Notebook-1.pdf
------------------------------
monkeyFileEvent.filePath: /storage/emulated/0/note/test3/Folder-1/Notebook-2.pdf
monkeyFileEvent.fileName: Notebook-2.pdf
monkeyFileEvent.rootFolderPath: /storage/emulated/0/note/test3
currentRemoteFolderPath: /storage/emulated/0/note/test3
basePath: /Folder-1
filename: Notebook-2.pdf
virtualPath: /Folder-1/Notebook-2.pdf
msId: ms288c629820a9a9644adf5d8b5c1e989e493165a8fbe67be0e3a229e7b6c9b580
targetFilePath: /home/marc/.monkeynotes/user_data/marc/downloads/ms304df708f2c938aa6733d007a753947bbf6293ef475e32797cfd7f1e2d7c1264/ms288c629820a9a9644adf5d8b5c1e989e493165a8fbe67be0e3a229e7b6c9b580
------------------------------
[0eb814b95a64] [MonkeySync.syncPdf] [] Adding file name Notebook-2.pdf id Notebook-2.pdf status created - remote path /Folder-1/Notebook-2.pdf


Boox auto export PDF - new file

------------------------------
monkeyFileEvent.filePath: /storage/emulated/0/note/test3/Folder-1/Notebook-3.pdf
monkeyFileEvent.fileName: Notebook-3.pdf
monkeyFileEvent.rootFolderPath: /storage/emulated/0/note/test3
currentRemoteFolderPath: /storage/emulated/0/note/test3
basePath: /Folder-1
filename: Notebook-3.pdf
virtualPath: /Folder-1/Notebook-3.pdf
msId: msa78d29d465b75783c42599fada477919f2322684df8c5f1b862ea97e82b2c0d3
targetFilePath: /home/marc/.monkeynotes/user_data/marc/downloads/ms304df708f2c938aa6733d007a753947bbf6293ef475e32797cfd7f1e2d7c1264/msa78d29d465b75783c42599fada477919f2322684df8c5f1b862ea97e82b2c0d3
------------------------------
2026-06-04 11:55:14.063  INFO 24893 --- [nio-8080-exec-2] fr.monkeynotes.mn.service.UtilsService   : [c7a669ac2f5a] [MonkeySync.syncPdf] [] Adding file name Notebook-3.pdf id Notebook-3.pdf status created - remote path /Folder-1/Notebook-3.pdf


same file update
------------------------------
monkeyFileEvent.filePath: /storage/emulated/0/note/test3/Folder-1/Notebook-3.pdf
monkeyFileEvent.fileName: Notebook-3.pdf
monkeyFileEvent.rootFolderPath: /storage/emulated/0/note/test3
currentRemoteFolderPath: /storage/emulated/0/note/test3
basePath: /Folder-1
filename: Notebook-3.pdf
virtualPath: /Folder-1/Notebook-3.pdf
msId: msa78d29d465b75783c42599fada477919f2322684df8c5f1b862ea97e82b2c0d3
targetFilePath: /home/marc/.monkeynotes/user_data/marc/downloads/ms304df708f2c938aa6733d007a753947bbf6293ef475e32797cfd7f1e2d7c1264/msa78d29d465b75783c42599fada477919f2322684df8c5f1b862ea97e82b2c0d3
------------------------------
[acd35d525b6b] [MonkeySync.syncPdf] [] Adding file name Notebook-3.pdf id Notebook-3.pdf status created - remote path /Folder-1/Notebook-3.pdf

update existing - originally manual export

------------------------------
monkeyFileEvent.filePath: /storage/emulated/0/note/test3/Folder-1/Notebook-2.pdf
monkeyFileEvent.fileName: Notebook-2.pdf
monkeyFileEvent.rootFolderPath: /storage/emulated/0/note/test3
currentRemoteFolderPath: /storage/emulated/0/note/test3
basePath: /Folder-1
filename: Notebook-2.pdf
virtualPath: /Folder-1/Notebook-2.pdf
msId: ms288c629820a9a9644adf5d8b5c1e989e493165a8fbe67be0e3a229e7b6c9b580
targetFilePath: /home/marc/.monkeynotes/user_data/marc/downloads/ms304df708f2c938aa6733d007a753947bbf6293ef475e32797cfd7f1e2d7c1264/ms288c629820a9a9644adf5d8b5c1e989e493165a8fbe67be0e3a229e7b6c9b580
------------------------------
[4db083a3322c] [MonkeySync.syncPdf] [] Adding file name Notebook-2.pdf id Notebook-2.pdf status created - remote path /Folder-1/Notebook-2.pdf
```

# Claude devs

## Transcript View

● The changes look correct. Here's a summary of what was done:

TranscriptView.vue:
- Added showImages = ref(false) state
- Added an image toggle button (pi pi-image) in the transcript header, pinned to the right via margin-left: auto
- Passes :showImages="showImages" to each TranscriptPage

TranscriptPage.vue:
- Added showImages: boolean prop
- New branch: when showImages is true and not in edit mode, renders a 50/50 flex row — text on the left, page image on the right
- Watcher on showImages: lazy-loads the image on first toggle (won't re-download if already loaded)
- Added .view-left, .view-right (both flex: 1) and .view-preview-img (width: 100%) CSS for the 50/50 layout

## Updated AgentView

  - Chat layout: fixed header → optional settings panel → scrollable message list → sticky input bar, all within calc(100vh - 56px)
  - Message bubbles: user messages right-aligned (primary color), assistant messages left-aligned (surface), with timestamps
  - Optimistic send: user message is pushed to the list immediately, input clears, then the spinner/thinking bubble appears in the assistant slot while SSE is in flight
  - Thinking indicator: three bouncing dots in an assistant bubble while waiting, disappears when the reply arrives
  - Settings panel: toggled by the cog icon in the header — auto-opens when no thread exists yet, collapsed when resuming an existing one. Contains model, instructions, and reset toggle
  - Back button: arrow in header navigates back to the transcript
  - Empty state: friendly placeholder when no messages yet
  - Enter to send: Ctrl+Enter / bare Enter submits (autoResize textarea grows for multi-line)


## UserView

● Good. I have a clear picture. The current UsersView has no layout, a shared newPassword bug across users, and invalid JSON in changePassword. I'll refactor it to:
- DataTable for user list
- Dialog for "Add User" and "Change Password" (fixing the shared state bug)
- Fix the JSON.stringify bug in changePassword
- Consistent .main-wrapper style matching Preferences

## User password

Backend — POST /user/me/password in AuthController:
- No @PreAuthorize — any authenticated user can call it
- Gets the current username from security context via authService.getUsernameFromContext()
- Reuses the existing userService.setUserPassowrd() method

Preferences.vue:
- "Change Password" button added alongside Logout in the Accounts fieldset
- Same Dialog/Password/loading pattern as UsersView
- On success the dialog closes and the password field clears; errors surface through the existing error dialog

## ProcessView, Preferences

- Container/header — .main-wrapper + .page-header (h2 + a pi-refresh icon button on the right), same as UsersView.vue.
- Each process as a card — reuse TranscriptView's .page-card/.page-card-header look: bordered, rounded, surface-50 header strip.
  - Icon per process kind (AsyncProcessName: flushChanges/flushMonkeySyncs → pi-sync, updateFolder → pi-folder, forcePageUpdate/forceTranscriptUpdate → pi-file-edit) with a human-readable label instead of raw enum text.
  - A PrimeVue Tag for status, color-coded from DtoProcess.Status (running → info, completed → success, failed/error → danger), replacing the current plain-text branching.
  - Metadata row (username via pi-user, duration via pi-clock) styled like TranscriptView's .property-row icon+label+value pattern.
  - Cancel button: icon-only pi-times, text + severity="danger", pinned right via margin-left: auto like .page-edit-btn — only shown when status === running.
- Cancel confirmation — route through useConfirm() / <ConfirmDialog>, the same pattern already used in Preferences.vue for the destructive "wipe data" action, instead of firing on a bare click.
- Live updates — poll process/list every few seconds while any process is running, stop polling once none are; keep the ProgressSpinner only for the initial load so refreshes don't blank the page.

I'll apply the same card-based, consistent design language to Preferences.vue. Note: its bottom <style> block is unscoped and defines .main-wrapper, .dialog-form, .field, .dialog-footer globally — other views like UsersView.vue depend on those, so I'll keep that block intact and add new scoped styles alongside it


## NamedEntities 

- Same page-header / page-card shell as the other views, with a ProgressSpinner loading state and italic empty-state messaging.
- Replaced the deprecated TabView/TabPanel markup with the same Tabs/TabList/Tab/TabPanels/TabPanel API already used in TranscriptView.vue, one tab per verb (Tags/People/Emails) with an icon and a count Tag badge.
- Replaced the raw nested <ul> markup with the same tag-grid pattern as TranscriptView.vue's Tags tab (entity-group/entity-refs, values as bold labels, occurrences as Tag chips).
- Fixed the broken navigation: the old code used a raw <a href="transcript/${fileId}"> (a literal relative link, not a real route); occurrences now use router.push({ name: 'transcript', params: { fileId } }) like Home.vue does, and each chip shows the filename + page number instead of just the filename.

## Inline diagram-next-page image

Backend contract (ViewServiceImpl.buildDtoTranscript): a `diagramNextPage` (DGN) named entity on page N is a purely positional pointer — it always means page N+1 is the actual diagram page (`page.diagram=true`, image at pageNumber+1). No matching by value/title, and the relationship isn't persisted, it's recomputed at DTO-build time from the entity list every request.

- TranscriptView.vue: v-for now tracks `index` and passes `:nextPage="transcript.pages[index + 1] ?? null"` to each TranscriptPage, so a page component can see the next page's `fileId`/`username`/`pageNumber` without a new endpoint.
- TranscriptPage.vue: new `nextPage` prop, `diagramImgSrc` ref, and `downloadNextPageImage()` (same authFetch-blob-URL pattern as the existing `downloadImage()`, kept separate since it targets a different page's image endpoint). `loadPage()` awaits this fetch before building the entity replacements when the page has a `diagramNextPage` entity, then the `diagramNextPage` branch appends `<br/><img class='diagram-inline-img'>` right after the entity span, so the diagram renders inline below the reference instead of only on its own page card further down.
