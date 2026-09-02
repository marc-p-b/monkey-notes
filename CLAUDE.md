# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Folders

From root folder MonkeyNotes : 
- MonkeyNotes : Spring / Vue project. The web platform, API and UI
- MonkeySyncApp : Flutter App. The mobile companion for the platform, allows syncing from a tablet.

## TODO : Major Upgrade

1. Flaws to fix, in priority order

1. IDOR on image endpoints — security. ImageController.java:32-50 (getImageWithMediaType, streamImageWithMediaType) take username as a client-supplied path parameter and never check it against authService.getUsernameFromContext(). Every other controller scopes data through IdFile.createIdFile(authService.getUsernameFromContext(), fileId); this one trusts the caller.
   Any authenticated user who knows/guesses another user's username + fileId can view their private handwritten-note images. SecurityConfig.java:45 even has a commented-out permitAll for /image/*/*/*, showing this was flagged and never resolved.
2. Single shared SSE emitter for all users. AgentServiceImpl's emitter/lastId/scheduler are plain instance fields on a singleton bean (own //TODO make this multiuser comment, line 80). Two users chatting with agents concurrently will stomp each other's this.emitter. The @Scheduled heartbeat() (every 30s) also dereferences this.emitter unconditionally — NPE loop before
   any agent conversation has ever been started.
3. Silent batch-abort bug. UpdateServiceImpl.java:170-173: inside the for(file2Process : files2Process) loop, an unchanged file triggers return; instead of continue;. This aborts the entire batch (every remaining file in the list) and skips createFileEntities(files2Process) at the end of the method — silently incomplete processing on the main OCR update pipeline
   whenever one file in a batch has no new pages.
4. Manual edits silently lost on OCR re-run. EditServiceImpl.edit() never updates EntityTranscriptPage.transcript or bumps its version — it only stores a diff keyed to the page's current version. UpdateServiceImpl.asyncForcePageUpdate() (line ~608) bumps the version and overwrites the transcript directly. Once that happens, the diff keyed to the old version becomes
   orphaned and is never reapplied — a user's manual correction disappears with no warning the next time OCR is force-rerun on that page. Compounding this: EditServiceImpl.applyPatch() rethrows any PatchFailedException/parse failure as an unchecked RuntimeException, and neither of its two call sites (ViewServiceImpl.buildDtoTranscript, SearchServiceImpl.initLucene) catch
   it — one bad/orphaned diff 500s an entire transcript view, or aborts the whole search re-index.
5. Optional.orElse() eager-evaluation bug. NamedEntitiesServiceImpl.indexNamedEntity():73-75 — repositoryNamedEntityIndex.findById(id).orElse(repositoryNamedEntityIndex.save(new EntityNamedEntityIndex()...)). orElse()'s argument is evaluated unconditionally, so .save() runs on every call, resetting createdAt to now() even when the tag/person/etc. already exists.
   Should be orElseGet(...).
6. Two consumers racing on one process map. ProcessServiceImpl.listProcess():201-207 evicts every completed/failed entry from mapAsyncProcess on each call — including calls from the UI's poller. getCompletedProcessesToNotify() (used by the mailer) reads the same map. Whichever runs first destructively wins: fast UI polling can starve the mailer of ever seeing a
   completed process (no notification email sent).
7. Search breaks entirely after any delete. SearchServiceImpl.search():176-178 does mapT.get(e.getId()).getDocumented_at() with no null-guard. The Lucene index isn't invalidated on delete (ViewServiceImpl.delete() never re-indexes), so a stale hit for a deleted transcript throws an NPE that kills the whole search response for every query, until someone manually hits
   /search/init.
8. Folder PDF export hard-depends on Google Drive even for MonkeySync-only users. TranscriptController.getFolderPdf():119-133 calls driveUtilsService.getFileName(folderId), hitting the live Drive API unconditionally. The single-transcript PDF path was migrated to read the name from repositoryFile (line 84) but the folder path wasn't — this feature is broken for anyone
   not connected to Google Drive.
9. Non-thread-safe shared maps in DriveServiceImpl. mapCredentials/mapDrive are plain HashMap fields on a singleton bean, mutated from concurrent request threads and from scheduled RefreshTokenTasks. Everywhere else in the codebase this pattern correctly uses ConcurrentHashMap (ProcessServiceImpl, MonkeySyncServiceImpl, DriveChangeManagerServiceImpl) — this one
   doesn't.
10. Stub endpoint lies about success. AuthController.removeUser():81-86 is a //TODO stub that does nothing but unconditionally returns "User X removed" (200 OK). An admin has no way to know the deletion never happened.
11. Smaller, worth a pass: UserServiceImpl.saveAllUsers():100-104 assumes the client-submitted user list is a complete superset of the DB (NPEs otherwise — latent, not currently hit by UsersView.vue's load-all/save-all pattern); TranscriptController.getTranscriptPdf():84 and ViewServiceImpl have several Optional.get() calls with no isPresent() check that will throw
    NoSuchElementException instead of a clean 404.

2. Upgrades / refactors for a better backend

- Split the two "god" services. UpdateServiceImpl (742 lines) mixes a generic OCR pipeline with Drive-specific and MonkeySync-specific branches inline (SyncOption checks scattered through runListAsyncProcess, asyncForceTranscriptUpdate, etc.). AgentServiceImpl (550 lines) mixes OpenAI REST plumbing (6+ private methods hand-parsing JSON via JsonPath) with
  business/persistence logic. Both are natural candidates for a strategy-object split (sync-source strategy; a dedicated OpenAiAssistantClient).
- No consistent error contract. Success/failure is communicated inconsistently — plain ResponseEntity<String>("OK"), silent return null, logged-and-swallowed exceptions, or an uncaught RuntimeException reaching Spring's default handler. There's no @ControllerAdvice/@ExceptionHandler anywhere. The frontend's generic !response.ok handling is a symptom of this.
- Per-call RestTemplate instantiation. Both AgentServiceImpl and QwenServiceImpl new RestTemplate() (or new RestTemplate(factory)) on every single API call instead of a shared, pooled, pre-configured bean — no connection reuse, no centralized timeout/retry/interceptor policy.
- Dual "current user" mechanisms. SecurityContextHolder for real requests and NoAuthContextHolder (raw ThreadLocal) for webhook/scheduled contexts are both read ad hoc; AuthServiceImpl.getUserDataFromContext() doesn't check NoAuthContextHolder at all (its own comment: "no applicable ?"), unlike getUsernameFromContext(). Worth unifying behind one CurrentUserProvider.
- Preferences as a flat key/value table. PreferencesServiceImpl.fromMap/toEntities hand-maps every PreferenceKey through a switch; every new preference means touching the enum, the DTO, and both switch directions. A typed settings object (or JSON column) would remove this repetitive growth pattern.
- Constructor injection. Everything uses @Autowired field injection — makes partial construction easy in tests and obscures real dependencies; standard modern-Spring guidance is constructor injection with final fields.
- No pagination on /user/list, /agent/list, /ne/verb/{verb} etc. — fine today, won't scale.

3. Global code advice

- Null/Optional handling is inconsistent — some call sites check isPresent() properly, others call .get() blind (TranscriptController:84, ViewServiceImpl:214,338), others return raw null from a service method (ViewServiceImpl.getTranscript():75). Pick one convention.
- Logging is inconsistent: real LOG.error/warn mixed with stray System.out.println (MonkeySyncServiceImpl:105-115) and e.printStackTrace() (DriveServiceUtilsImpl:98, followed immediately by code that dereferences the now-possibly-null result).
- Several empty/log-only catch blocks let execution continue with partially-initialized state rather than failing fast or propagating a clear error (MailServiceImpl:128-130).
- Widespread == true / == false comparisons instead of the boolean itself (AuthController, NamedEntitiesServiceImpl, UpdateServiceImpl, DriveServiceUtilsImpl, dozens more) — purely stylistic but pervasive.
- setUserPassowrd is misspelled consistently across UserService/UserServiceImpl/AuthController — safe, mechanical rename since it's uniformly wrong.
- Bean Validation (@Valid/JSR-303) is essentially unused at controller boundaries — request bodies/params are trusted as-is (e.g. AgentController.agentPrepare's fileIds CSV, MonkeySync.syncPdf's event payload).
- Large amounts of commented-out code are kept in place rather than deleted (see below) — git history is the changelog; comments shouldn't be.

4. Dead code

- ImageController.getImageWithMediaType():36-39 — defines a StreamingResponseBody stream that's never used (a separate ByteArrayOutputStream path is used instead); the whole commented-out /imagetemp/... endpoint at lines 71-84.
- DataController.exportUserData():46-53 — large commented-out streaming-response alternative.
- DriveChangeManagerServiceImpl.getStatus():427-443 and watchStop():418-422 — bodies entirely commented out; the endpoints exist (AdminController /status, /watch/stop) but do nothing.
- AgentServiceImpl — commented @Value defaultModel field (lines 56-57), commented name/threadName build lines in newAssistant (175-177), and a fully commented-out "touch lastUsageDate" block in addMessage() (397-401) that's now redundant with the working version in getOrCreateAssistant.
- TranscriptController.java:146-149 — a dangling commented-out /folder/list mapping sitting between two active methods.
- QwenServiceImpl:67-68 — old content1_url/commented image_url approach left beside the active implementation.
- ProcessServiceImpl.listProcess():152 — stray commented-out getMapAsyncProcess() line.

5. Google Drive as a deactivable module?

Feasible, but not free — today it's load-bearing in a couple of places:

- Startup hard-dependency: DriveServiceImpl's @Value fields (app.drive.auth.client-id, client-secret, etc.) have no defaults, so the Spring context fails to start without real Google OAuth credentials configured — a MonkeySync-only deployment still has to supply dummy Drive credentials just to boot.
- Shared pipeline coupling: UpdateServiceImpl is genuinely shared between SyncOption.gdrive and SyncOption.monkey — Drive-only logic (updateAncestorsFoldersGDrive, recursRefreshFolder, asyncUpdateFolder, the gdrive branch inside asyncForceTranscriptUpdate) is interleaved with the generic OCR pipeline in the same class rather than living behind a clean seam.
- Feature #8 above (folder PDF export) already silently assumes Drive is available, which is itself evidence the separation isn't currently clean.
- Cleanly isolated pieces that would toggle off easily: DriveChangeManagerServiceImpl's webhook watch/flush cycle, AuthWebhooksController's /grant-callback and /notify, PreferencesController.authGoogleDrive(), and AdminController (which is Drive-only end to end).

To make it a real app.drive.enabled toggle: make the three Drive @Value properties optional with safe defaults, gate the Drive beans/controllers behind @ConditionalOnProperty, and extract the Drive-specific branches out of UpdateServiceImpl into a strategy that's simply not registered when the flag is off. The webhook/admin/preferences side is easy;
UpdateServiceImpl's entanglement is the real work.


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

Convention: after finishing a non-trivial task (feature, bugfix, refactor) in this repo, append an entry below — a `##` header naming it, then bullet points per file/component covering what changed and, most importantly, *why* (especially for non-obvious root causes). Don't just restate the diff; capture the reasoning a future reader would otherwise have to rediscover. Keep entries concise, and append under the last existing entry rather than rewriting the section.

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

## Per-page edit/image icons + global bulk image toggle

- TranscriptView.vue: each page-card-header now has a `.page-header-actions` group, right-aligned, with a per-page `pi-image` toggle (`pageShowImages: Record<number, boolean>`) next to the existing `pi-pencil` edit button (still gated on `store.transcript_edit_mode`).
- Removed the old header-level global `showImages` toggle. Replaced with a "Show Images"/"Hide Images" button in the action-row next to Edit/Lock — `toggleAllImages()` force-sets every page's `pageShowImages` entry to the same open/closed state, overriding individual per-page toggles (bulk action, not a merge).

## Scroll-to-page navigation from Named Entities / Search results

- NamedEntitiesView.vue and SearchView.vue: clicking an entity reference or a search result page tag now navigates via `router.push({ name: 'transcript', params: { fileId }, hash: '#pageNumber' + pageNumber })` instead of just opening the file. Reuses the anchor `<span :id="'pageNumber' + page.pageNumber" />` already rendered at the top of every `TranscriptPage.vue`.
- SearchView.vue's "Title match" tag and per-page `p. N` tags now each link to their specific page (previously all links jumped to `pages[0]`); `titleMatchPage()` reads the title-type result's `pageNumber`.
- TranscriptView.vue can't just `scrollIntoView` once on mount: pages with a full diagram image fetch it async and unawaited, which grows page height and pushes the scroll target out of view after a naive one-shot scroll (confirmed by the bug only reproducing when *not* single-stepping in the debugger — the extra time let images settle first). Fixed properly (not with a timeout/rAF-polling hack) by making it deterministic: `TranscriptPage.vue` now awaits its own image download in `loadPage()` and emits `pageReady` from `onMounted` (wrapped in try/finally so a failed fetch still emits); `TranscriptView.vue` counts `expectedReadyPages` (rendered pages, i.e. `pageDiagram !== inline`) and only calls `scrollToHashAnchor()` once every page has reported ready.

## Update Search Index button

- Preferences.vue: added an "Update Search Index" button to the Data Management card's action row, next to Export/Import/Wipe. Calls `GET search/init` (`SearchController.init()` → `SearchService.initLucene()`) to force-rebuild the Lucene index on demand.
- Follows the existing `updateAllTranscripts()`/`googleDisconnect()` pattern: a dedicated `rebuildingIndex` loading ref drives the button spinner, failures surface through the shared `message`/`errorDialogVisibility` error dialog.

## Search reset bug

- Root cause: the header search box (App.vue) always did `router.push({ name: 'search' })` on every search, but Vue Router 4 treats navigating to the same route (no param/query change) as a no-op — so re-searching while already on `/search` never remounted `SearchView.vue`, which only fetched once in `onMounted` with no watcher on `store.search`.
- Fix: added `watch(() => store.search, () => request())` in SearchView.vue so the fetch reruns on any search-term change regardless of navigation. Also fixed `App.vue` passing the raw `query` ref into `store.setSearch()` instead of `query.value` (worked before only because Pinia auto-unwraps refs assigned into state).

## Search NPE / result grouping fix

- Root cause of "search finds nothing": `SearchServiceImpl.initLucene()` had the `tDoc.add(...FIELD_TYPE...)`/`cDoc.add(...FIELD_TYPE...)` lines commented out (a half-applied fix left on top of a prior commit), so every indexed doc had no `type` field. `search()`'s `switch (doc.get(FIELD_TYPE))` then switched on `null` for any actual match, throwing an uncaught `NullPointerException` → 500 → frontend silently showed no results. Any query matching zero docs looked fine, masking the bug — it only broke once something actually matched, e.g. a transcript named "260121-yasser.pdf" titled "yasser" searched for "yasser".
- Fixed by restoring the two `FIELD_TYPE` field-adds, plus wrapped the `IndexReader` in try-with-resources in `search()` (it was never closed before — leaked a reader per query), and added `FIELD_NAME` to the searched fields so the original filename is searchable too, not just the parsed title.
- Also changed result grouping from `Collectors.groupingBy(DtoSearchResult::getTitle)` to `getId()` — grouping by title merged results from different transcripts that happened to share a title into one card, using the first item's id for navigation (wrong doc). Grouping by id (unique per transcript) fixes that.
- SearchView.vue updated to match: iterates `results` by `docId` instead of by title text; added `docTitle()` helper to read the display title from the item list (prefers the `srType === 'title'` entry, falls back to the first item) since the map key is now an id, not display text.

## Preferences: prompt textareas, config-driven model dropdowns, advanced toggle

- `ocrPrompt` switched from `InputText` to `Textarea` (`autoResize`, 3 rows) — the OCR prompt can run to a full sentence (see `app.defaults.qwen.prompt` in the yaml), a single line was cramped.
- Model selection now follows the `Prefs` + `AIModel` data model backing it: `DtoPreferences.AIModel` is a `record(name, label)`, and `ocrModels`/`agentModels` are `Set<AIModel>` built by `PreferencesServiceImpl.aiModelsFromConfig()` parsing the yaml's `value=Label(value)` comma-separated format (`app.defaults.qwen.models.available` / `app.openai.models.available`). The OCR Model `<Select>` and the new Agent Model `<Select>` (Agent card, backed by `selectedAgentModel`) both bind `optionLabel="label" optionValue="name"` to match — previously the OCR select passed a plain string array with no option binding, so it rendered raw config tokens instead of clean labels.
- Added a "Show advanced" / "Hide advanced" toggle (eye icon, top-right of the page header) gating `inputFolderId` (both sync-option variants), `cropImage`, `qwenConnectTimeout`, `qwenReadTimeout`, and `qwenMaxTokens` behind `v-if="showAdvanced"` — these are rarely-touched fields that were cluttering the default view.

## TranscriptView: actions embedded in header

- The action-row (Edit/Lock, Show/Hide Images, Agent, Update, PDF) used to live inside the "Properties" `TabPanel`, so it disappeared whenever the Tags or TOC tab was selected. Moved it out of the `Tabs` block entirely and embedded it directly in `.transcript-header` (next to the back button and title), right-aligned via `margin-left: auto` on `.transcript-header .action-row`, so the actions are always visible regardless of which tab is active.

## Home: header with stats + select/sort actions

- Added a page header to `Home.vue`: a title/stat line (`{{ totalDocuments }} documents · {{ totalFolders }} folders` — dummy placeholder refs, no counts endpoint exists yet) plus a left-aligned action row (unlike TranscriptView's right-aligned one) with the same small/outlined button styling: a "Select"/"Exit Select" mode toggle, an order-by `<Select>` (Name/Date), and an asc/desc icon toggle.
- `selectMode`/`orderBy`/`orderDir` are passed as props into `TreeView.vue` and threaded recursively into `TreeNode.vue` (each folder's children need the same props to keep sorting/checkboxes consistent at every depth). Sorting is done via a new shared `sortNodes()` util (`ui/src/utils/treeSort.ts`) — by `name` (localeCompare) or by `dtoFile.discovered_at` (works for both files and folders since `DtoFile` carries `discovered_at` regardless of type) — applied as a `computed` at each tree level rather than mutating `nodes`/`children` in place.
- When `selectMode` is on, each `TreeNode` row renders a `Checkbox` (binary, `@click.stop` so it doesn't trigger the row's expand/navigate handler). No bulk-action wiring yet (nothing selected is tracked centrally) — only the checkbox UI was requested so far.

## AgentView: accept a single id or a set of ids

- Router: `/agent/:fileId` path param made optional (`/agent/:fileId?`) so the route also works when only a `?ids=` query is present.
- `AgentView.vue`: normalizes the two entry points into one `fileIds: string[]` computed — a comma-separated `?ids=` query (multi-select case, from Home's select mode) takes priority, falling back to the single `:fileId` route param (TranscriptView's "Ask Agent" button). `primaryFileId` (`fileIds[0]`) and `isMultiple` derive from it; the back button goes to `/transcript/:id` for a single doc or `/` otherwise.
- Scoped as frontend-only plumbing at the time (backend still took one `fileId`); superseded by the backend multi-id rework below.

## Home: bulk selection wired up + "Ask Agent" action

- Closed the gap left by the previous entry ("nothing selected is tracked centrally"): selection state now lives in `Home.vue` as `selectedIds = reactive(new Set<string>())`, shared via `provide('selectedIds', ...)`/`provide('toggleSelectedId', ...)` rather than threaded through props/emits — `TreeNode.vue` is recursive and `TreeView.vue` sits in between, so provide/inject reaches every depth without touching `TreeView.vue` at all.
- `TreeNode.vue`'s local `checked` ref was replaced with a get/set `computed` backed by the injected Set, keyed by `String(node.dtoFile.fileId)`. Checking a folder only adds the folder's own id — no cascade to children — consistent with the backend already expanding a folder id into all its recursive transcripts.
- `selectedIds` clears via `watch(selectMode, ...)` whenever select mode is exited.
- Added a right-aligned "Actions" button + PrimeVue `Menu` popup in `.action-row`, visible only when `selectMode && selectedIds.size > 0`; its first (and only, for now) entry "Ask Agent" calls `router.push({ name: 'agent', query: { ids: [...selectedIds].join(',') } })`.
- Also switched `.action-row` to `justify-content: flex-end` (was left-aligned per the previous entry) so all controls — Select toggle, sort dropdown/direction, Actions — align together on the right, matching the rest of the app's right-pinned action-row convention.

## Agent backend rework: uuid-keyed conversations + AgentListView

- Backend shifted `EntityAgent`'s key from the old `(username, fileId)` `IdFile` embeddable to a standalone `uuid` string PK, with `fileIds` stored as a comma-joined column, plus new `threadName`/`createdDate`/`lastUsageDate` columns — needed because a conversation can now span an arbitrary set of documents (or a folder), not just one file, so there's no single natural key left. `AgentController`'s prepare endpoint moved from `GET /agent/prepare/{fileId}` to `GET /agent/prepare?fileIds=...`; `DtoAgentPrepare`/`DtoAgent` grew `uuid`/`fileIds` (`Set<String>`) accordingly. `AgentView.vue` was adapted to match: posts the full `fileIds` set instead of one id, and its interface fields follow `uuid`/`fileIds` rather than `fileId`.
- `AgentServiceImpl.listThreads()` (backing new `GET /agent/list`) was left mid-refactor (didn't compile — missing `return`/`.toList()`) and finished: maps each `EntityAgent` to a `DtoAgentPrepare` (`uuid`, `threadName`, `createdAt`, new `lastThreadUpdate` field), sorted most-recent-first by `lastUsageDate`.
- Added `AgentService.prepareExistingAssistant(uuid)` / `GET /agent/prepare/{uuid}` to actually resume a listed conversation — restores what a commented-out block in `prepareAssistant()` was trying to do (rebuild messages via the existing private `getAssistant()`/`getThreadMessages()` helpers), but keyed correctly off a real existing `uuid` via `repositoryAgent.findById()` instead of the old dead code's broken heuristic (treating a single fileId as if it were the agent's PK).
- `getOrCreateAssistant()`'s reuse branch now stamps `lastUsageDate = now()` before saving — without this, "Last update" in the list would always equal "Created" since nothing else touched that column after initial creation.
- New `ui/src/components/AgentListView.vue` (route `/agents`, nav entry added in `App.vue`): fetches `agent/list`, renders each thread as a `.page-card` (same shell as `ProcessesView.vue`) showing `threadName`/`createdAt`/`lastThreadUpdate`; clicking one navigates to `{ name: 'agent', query: { uuid } }`. `AgentView.vue` now treats a `?uuid=` query as a third entry path alongside `fileId`/`ids` — when present it calls `agent/prepare/{uuid}` instead of the fileIds flow, and its back button returns to `/agents`.

## Counts endpoint (folders / transcripts)

- New `GET /transcript/count` in `TranscriptController` returning `DtoCounts { folders, transcripts }` — fills the gap noted in the "Home: header with stats" entry, where `totalDocuments`/`totalFolders` were dummy placeholder refs because no counts endpoint existed.
- `DtoCounts` is a plain fluent-setter DTO (same style as `DtoFile`) rather than a record, so it can grow more counters without breaking the JSON shape.
- `ViewService.countFiles()` / `ViewServiceImpl` implement it. Put in `ViewService` (not a new service) because it's browsing-shell data, alongside `listRootLevel`/`listRecentTranscripts`, and it already holds the `authService.getUsernameFromContext()` + `repositoryFile` pair this needs.
- Counting is done with a derived `RepositoryFile.countByIdFile_UsernameAndType(username, type)` — two `SELECT count(*)` queries scoped to the current user — instead of loading `findAllByIdFile_Username` and filtering in memory, so it stays O(1) in transferred rows as libraries grow.
- Note on route ordering: `/transcript/count` sits next to the existing `/transcript/{fileId}` mapping; Spring prefers the literal path over the path-variable one, same as the pre-existing `/transcript/recent`.
- `Home.vue` follow-up: dropped the `totalDocuments`/`totalFolders` dummy refs for a `counts` ref (`DtoCounts | null`) filled by a `fetchCounts()` call to `transcript/count` in `onMounted`, alongside `fetchRecentTranscripts()`. The subtitle is `v-if="counts"` so the header shows nothing rather than a flash of `0 documents · 0 folders` before the response lands. Deliberately kept out of the `homeLoading()` / `store.setLoading()` aggregation (unlike the recent-list and tree fetches) — it's a one-line stat, not worth blocking the global spinner on.

## Quicknotes phase 1 — backend foundation

Typed, keyboard-entered notes as a first-class content kind: captured offline in MonkeySyncApp (later phases), stored server-side, and sharing the named-entity index and Lucene index with handwritten transcripts. This entry covers the backend only; Lucene indexing is phase 2, the Vue feed phase 3, the Flutter side phases 4–5.

- **New entity rather than a reused transcript.** `EntityQuickNote` / `IdQuickNote` (`@Entity(name="quicknote")`, composite `@EmbeddedId` of `username` + `uuid`, following the `IdFile` / `IdNamedEntity` convention). Deliberately *not* modelled as an `EntityTranscript` + `EntityFile` pair: that model is bound to a PDF, an md5, page images, a folder tree and OCR version bumps, none of which a typed note has — forcing it in would mean fake file rows and a `pageCount=1` fiction, and would pollute the folder tree/`FileNode` browsing.
- **The uuid comes from the client, and `save()` is an upsert.** `QuickNoteService.save()` handles both create and update by design, because `FileSyncService` in the companion app retries every failed push on each sync tick — a server-generated id plus a lost response would duplicate the note on every retry. `POST /quicknote` and `PUT /quicknote/{uuid}` both delegate to it; the PUT path variable overrides any uuid in the payload so a mismatched body can't write to a different note. A re-push resurrects a tombstoned note (clears `deletedAt`), which is the only coherent reading while the device is the single source of truth.
- **`updatedAt` + `deletedAt` exist from day one even though nothing reads them yet.** Deletes are soft. Both columns are the seam for a later two-way sync (a `?since=` pull endpoint and last-write-wins reconciliation) — cheap now, a data migration later. A hard delete can't be propagated to a device that was offline when it happened.
- **Named entities reuse the existing table, marked by `pageNumber = -1`.** `QuickNoteServiceImpl` calls the transcript pipeline's own `NamedEntitiesService.saveNamedEntitiesFromContent(uuid, -1, body)` — that method already deletes the prior entities for its `(username, fileId, pageNumber)` triple before re-extracting, so editing a note doesn't accumulate stale tags. Extraction works unchanged because `TranscriptUtils.identifyNamedIdentities()` is a pure function over a string with no PDF/page dependency. Storing quicknote entities in `named_entity` (rather than a parallel table) is what keeps "everything tagged #invoice" a single query and a single view across both content kinds; the marker constant lives on `QuickNoteService.QUICKNOTE_PAGE_NUMBER` so the controller and the service agree on it.
- `deriveTitle()` prefers a leading markdown heading via the shared `TranscriptUtils.identifyTitles()`, falling back to the first non-blank line, truncated to the column width. So `# Foo` means the same thing in a quicknote as on a scanned page.

### Two pre-existing bugs this turned into blockers

- **`SearchServiceImpl.search()` NPE (backlog flaw #7) — fixed.** The tail of the method did `mapT.get(e.getId()).getDocumented_at()` unguarded, where `mapT` is built from the transcript repository. Any hit without a matching transcript row killed the *entire* search response, not just that hit. Until now this only fired on a stale index entry after a delete, which is why it survived; once quicknotes are indexed (phase 2) every query matching one would 500. Now filters unresolvable hits out with a warn log — which also fixes the original delete case properly.
- **`NamedEntitiesServiceImpl.indexNamedEntity()` eager `orElse` (backlog flaw #5) — fixed** to `orElseGet`. `orElse`'s argument is evaluated unconditionally, so the `save()` ran on every call and reset `createdAt` to `now()` for entities that already existed. Quicknotes multiply named-entity writes by an order of magnitude, turning an occasionally-wrong timestamp into a permanently wrong one.

### NamedEntityController: one owner resolver instead of two

Both display endpoints resolved the owning document themselves and neither handled the other's case — `getValues()` went through `viewService.getTranscript(...).getName()` (which returns raw `null` for a non-transcript id, so a quicknote entity would NPE — the method even carried a `//TODO check for exc`), while `getVerbs()` looked the id up in `repositoryFile` and would silently label anything else `"unknown"`. Replaced with a single private `resolveOwnerLabel(DtoNamedEntity)` that branches on `pageNumber == QUICKNOTE_PAGE_NUMBER` and reads a quicknote title for those. Side effects: the transcript path no longer builds a whole `DtoTranscript` just to read a name, and `viewService` / `ViewOptions` are no longer dependencies of the controller.

Known gaps left deliberately: `RepositoryNamedEntity.countByValue()` still isn't user-scoped (its own pre-existing `//TODO`) and now counts quicknote rows too; and `named_entity_index` still isn't pruned when the last user of a tag disappears, exactly as it isn't for transcripts today.

## Quicknotes phase 2 — incremental Lucene indexing

Quicknotes become searchable. The bulk of the work wasn't the new document type, it was that the index had no incremental write path at all.

- **The `IndexWriter` is now a field, held open for the life of the bean**, instead of a local closed at the end of `initLucene()`. That close was the blocker: with it, the only way to reflect any change was a full rebuild that re-reads every transcript and every page from the database. Acceptable for an OCR pipeline that writes in batches, unworkable for notes typed all day. `initLucene()` now ends with `commit()` and leaves the writer open; a re-init via `/search/init` calls `closeWriter()` first, or the old writer would leak and keep holding the orphaned `ByteBuffersDirectory`. `@PreDestroy` closes it on shutdown.
- **Reads needed no change at all.** `search()` already opened a fresh `DirectoryReader` per query, so it picks up each commit on its own — no `SearcherManager`, no reader lifecycle to manage.
- `indexQuickNote()` uses `updateDocument(new Term(FIELD_ID, uuid), doc)`, i.e. an upsert, so editing a note replaces its document rather than adding a second one. This is only exact because a quicknote is exactly **one** document — a transcript spreads a title document plus one per page across the same `FIELD_ID`, so the same trick would not work there.
- **A failed index write is logged and swallowed, not propagated.** The note is already persisted by then; failing the caller's request would lose a real write to protect a derived index, and `/search/init` rebuilds it. Same reasoning for `removeQuickNote()`.
- `indexEnabled` guards both write methods, so `QuickNoteService`'s save path does not depend on Lucene being switched on (it is off unless the `index_lucene` profile or `INDEX_LUCENE=true` is set). `search()` now checks the same flag and returns an empty map instead of dereferencing a null `analyzer` and 500-ing — the previous behaviour if anyone searched with indexing disabled.
- A quicknote document stores its title into `FIELD_NAME` as well as `FIELD_TITLE`. `MultiFieldQueryParser` searches `{title, content, name}`, and a note has no filename, so without this a `name:`-qualified query could never match one.

### search() enrichment now resolves two kinds of hit

The tail of `search()` previously mapped every hit through the transcript repository. Quicknote hits are never in that map, so the phase-1 stale-hit filter would have silently dropped all of them. It now splits by `srType`: transcript-backed hits resolve through `repositoryTranscript`, quicknote hits through a new `RepositoryQuickNote.findByIdQuickNote_UsernameAndIdQuickNote_UuidInAndDeletedAtIsNull()`, and a hit that resolves to neither is dropped with a warn as before. `documented_at` for a quicknote comes from its `createdAt` — the field the feed and the result list sort on.

**Worth knowing, because it is load-bearing and easy to break:** the Lucene index carries no username, so every user's documents match every user's query. These two repository lookups are not just date enrichment, they are also the tenant isolation — a hit that doesn't resolve to a row owned by the caller is what drops another user's document from the results. Anything that later "optimises" the enrichment away, or falls back to values stored in the index instead of the database, reintroduces a cross-user leak. Filtering tombstones in the same query is also what makes a stale quicknote index entry self-healing.

Known trade-offs left in place: `indexQuickNote()` is called inside `QuickNoteServiceImpl.save()`'s transaction, so a rollback after the index write leaves an orphaned document — harmless, because the enrichment lookup above drops any hit with no matching row, and the next `/search/init` clears it. Moving it to an after-commit hook was not worth the machinery.

### Frontend is intentionally not updated yet

`DtoSearchResult.SRType` gained a `quicknote` value but `SearchView.vue` still only knows `title` and `content` (phase 3). Until then a quicknote hit renders as a card with no page tags, and clicking its title calls `clickedTranscript()`, which routes to `/transcript/<note uuid>` and lands on a broken transcript view. Not fixable in this phase — there is no `/quicknotes` route to link to yet. In practice the window is narrow: it needs the index switched on *and* notes to exist, and nothing creates notes yet except a direct API call.

## Quicknotes phase 3 — the web feed

`/quicknotes`: a reverse-chronological feed of notes grouped by day, read-only (the device stays the source of truth).

- **New `ui/src/utils/namedEntityRender.ts`, extracted verbatim from `TranscriptPage.vue`.** The per-verb replacement chain and the `lFix` offset-drift trick now live in one place, so a new verb is added once instead of twice. `TranscriptPage.vue` calls `renderNamedEntities(transcript, entities, { diagramImageSrc })` — the `diagramNextPage` inline image was the only transcript-specific branch, so it became an option rather than a second code path.
- The extraction also removed a latent trap: `loadPage()` used to assign its rendered HTML back over `transcript`, so the raw text and the rendered text shared one variable. It happened to work only because `save()` reset `transcript` from `textEdit` before re-rendering; any other second call would have rendered already-rendered HTML. The renderer is now pure and `transcript` stays raw.
- **`lFix` assumes entities arrive sorted by `start` and non-overlapping** — it walks them in order, shifting later offsets by the length each replacement added. Nothing guaranteed that order for quicknotes (neither the table nor the in-memory grouping), so `QuickNoteServiceImpl.sortedByStart()` sorts before handing them out. Transcripts happened to be fine already; this is why the assumption is now written down next to the code.
- `DtoQuickNote` gained `listNamedEntities`, populated on the read paths only (`list()` / `get()`), not by `fromEntity()` — the Lucene indexer uses the same DTO and doesn't need them. `list()` fetches the whole feed's entities in **one** query via a new `RepositoryNamedEntity.findByPageNumber(username, pageNumber)` and groups them by `fileId` in memory, rather than one query per note.
- `QuickNotesView.vue` follows the established `.main-wrapper` / `.page-header` / `.page-card` shell (`.main-wrapper` still comes from `Preferences.vue`'s unscoped style block, which the router imports eagerly). Day labels resolve to Today / Yesterday / a full date, keyed on the **local** date so an evening note groups under the day the user actually experienced.
- Filter rail chips are derived from the loaded notes' own tag/person entities, not from `/ne/verbs` — no second request, and it can't offer a filter that matches nothing. **Selecting several chips widens the result set (OR), not narrows it**: clicking two tags and getting zero notes back is the more surprising outcome. The text filter matches the raw body and ANDs with the chips.

### Closes the rough edge phase 2 left

`SearchView.vue` now knows `srType === 'quicknote'`. `clickedTranscript()` became `clickedResult()` and branches: a quicknote hit routes to `{ name: 'quicknotes', hash: '#note' + uuid }` instead of `/transcript/<uuid>`, which landed on a broken transcript view. Quicknote cards get a `pi-bolt` icon and a "Quicknote" tag, since `hasTitleMatch`/`contentPages` are both empty for them and the card body would otherwise render blank. The feed carries a `.note-anchor` span offset `-4rem` per card and scrolls to it after the fetch resolves — the browser can't act on the hash itself because the list loads async, the same reason `TranscriptView` waits for its pages.

### Known, deliberately not changed

The feed renders note bodies through `v-html`, exactly as `TranscriptPage.vue` has always rendered transcripts. That means markup in the source text is live: `<script>` won't run (browsers don't execute scripts inserted via innerHTML) but `<img onerror=...>` will. For transcripts the content came from OCR; for quicknotes it is typed directly by the note's own author, so today this is self-inflicted only. It stops being self-inflicted the moment notes are shared between users or rendered in any cross-user view. Hardening it means escaping the segments *between* entities during the walk rather than up front — offsets refer to the unescaped string, so a pre-pass would misalign every entity — which is a change to the shared renderer and therefore to transcript rendering too. Left alone here rather than folded silently into a feed task.

## Gradle build alongside Maven

- Added `build.gradle` + `settings.gradle` (Groovy DSL) as a straight translation of `pom.xml`: `org.springframework.boot` 3.3.5 + `io.spring.dependency-management` replace the `spring-boot-starter-parent`, so starters and Jackson/Postgres stay version-managed by the BOM exactly as before; only the explicitly-pinned third-party libs carry versions.
- Scope mapping: `<scope>runtime</scope>` → `runtimeOnly` (jjwt-impl, jjwt-jackson), test → `testImplementation`, everything else → `implementation`. Postgres moved to `runtimeOnly` (it's a JDBC driver, never compiled against). The duplicate `json-path` block in `pom.xml` was collapsed to a single entry.
- `org.apache.commons:commons-io:1.3.2` kept verbatim — it's a wrong/legacy coordinate (real one is `commons-io:commons-io`), but changing it isn't part of a build-system port. Gradle resolves it to `commons-io:commons-io:1.3.2` transitively anyway.
- `<finalName>mn-api</finalName>` → `bootJar { archiveFileName = 'mn-api.jar' }`. **Output path differs from Maven**: `build/libs/mn-api.jar`, not `target/mn-api.jar` — any packaging script or Dockerfile copying the jar needs that path.
- Added the Gradle wrapper (8.10.2) because the system `gradle` here is 4.4.1, far below the Spring Boot plugin's 7.5+ floor. Wrapper had to be generated in an empty scratch dir: `gradle wrapper` evaluates `build.gradle` first, so it failed inside the project on that same version check.
- Verified with `./gradlew bootJar` — compiles and produces the executable jar. `pom.xml` was left in place; both build systems currently coexist.

## nginx/env.js config injection: placeholders removed instead of templated

- Both `docker/compose/data/nginx/app-run.conf` and `.../env.js` carried a hand-edited `<<HOST>>` placeholder (a documented deploy step in `docker/readme.md`). Nothing in the repo substituted them — `grep -rn "<<HOST>>"` matched only those two files, so they were purely manual. The fix was to remove the *need* for injection rather than add a templating mechanism.
- `app-run.conf`: dropped `server_name <<HOST>>`. It was dead weight — this is the only `server` block in the image, so it's nginx's default server and matches every Host, and Traefik has already routed by `Host(${HOST})` (compose label `traefik.http.routers.ui.rule`) before the request reaches nginx. Verified the file still parses with `nginx -t` on the current `nginx:1.15-alpine` base.
- `env.js`: `API_URL` is now the relative `'/api/'` instead of `https://<<HOST>>/api/`. The API is same-origin in this deployment (Traefik routes `Host(${HOST}) && PathPrefix(/api)` to the `api` service), and `joinUrl` in `ui/src/requests.ts:1` is plain string concat, so a relative base composes correctly for both `fetch` and the `EventSource` in `AgentView.vue:261`. `ui/public/env.js` still overrides it with an absolute tunnel URL for local dev, which is the case that actually needs a host.
- Side effect worth knowing: the browser no longer makes cross-origin API calls in the dist deployment, so `CORS_DOMAINS` is no longer exercised from the UI (left configured — the mobile app and any external caller still rely on it).
- Considered and rejected for now: the official nginx image's envsubst templating (`/etc/nginx/templates/*.template` + `NGINX_ENVSUBST_OUTPUT_DIR`) driven off compose `.env`. It works, but the current base `nginx:1.15-alpine` predates it — verified it has no `/docker-entrypoint.d/` and no `docker-entrypoint.sh` — so it would need a base bump. It's the right fallback *only* if the API ever moves to a different domain than the UI; with `server_name` gone, a single envsubst pass could then target the web root for `env.js` alone.
- Not done (would need a rebuild via `build_dist.sh`): both files are now fully static and no longer deployment-specific, so they'd be better baked into `docker/nginx-dist/Dockerfile` than bind-mounted. Note the current `./data/nginx:/etc/nginx/conf.d` mount also drops `env.js` into nginx's config dir, harmless only because the include glob is `conf.d/*.conf`.

## Fix: 504 on all /api/* — traefik dialing the api container on net_int

- Symptom: every `/api/*` request returned 504 after a flat ~30s (login, `/api/`, and even `/api/does-not-exist`), while the UI and `/adminer/` served normally in under 300ms. Looked like a backend outage; the api container was in fact healthy.
- Root cause is visible only in the traefik access log, in the backend address per router: `ui@docker -> 172.20.0.2:80`, `adminer@docker -> 172.20.0.4:8080`, but `api@docker -> 172.21.0.4:8080`. `nginx` is attached to `net_ext` only and is `172.20.0.2`, which pins `net_ext` = 172.20.x, so 172.21.x is `net_int` — which is `internal: true` and which traefik has no interface on. Traefik was dialing an address it cannot route to, so the connection never completed and it timed out at 30s. The uniform `30000ms`/`30001ms`/`30011ms` is the signature of a hung dial rather than a slow application.
- Why it picked the wrong network: `api` and `adminer` both carried `traefik.docker.network=monkeynotes-compose_net_ext`, but compose names networks `<project>_net_ext` and the project defaults to the directory name (`compose`), so the real network was `compose_net_ext`. The label matched nothing, and traefik's fallback for a container on multiple networks is whichever network Docker enumerates first — unordered. `adminer` happened to draw `net_ext` and worked; `api` drew `net_int` and hung. Identical labels, different outcomes, which is what makes this misleading to diagnose.
- Why it surfaced when it did: nothing to do with the env.js/app-run.conf edit that preceded it. Redeploying to pick up that edit recreated the containers and re-rolled the arbitrary network pick. The bug was latent in the compose file the whole time and would have reappeared on any `up` — worth knowing, because it can silently "fix itself" on a restart and come back later.
- Fix: gave both networks explicit `name:` keys (`monkeynotes_net_int` / `monkeynotes_net_ext`) so the names no longer depend on the project/directory name, and pointed the two `traefik.docker.network` labels at `monkeynotes_net_ext`. Verified with `docker compose --env-file .env-monkey-dist config` that the resolved names and labels now match exactly.
- Deploying this needs `docker compose down && docker compose up -d`, not a plain `up` — networks cannot be renamed in place. Plain `down` is safe here: all state is in bind mounts (`./data/postgres`), there are no named volumes. Do not use `down -v`.

## Default sync option for new preferences: monkey (config-driven)

- `PreferencesServiceImpl.initPreferences()` hardcoded `SyncOption.none`, so any user whose preferences were created for the first time landed on "No Sync" and had to pick MonkeySync by hand. Now defaults to `monkey`.
- Made it config-driven rather than swapping the literal, matching how every other default in that class already works (`app.defaults.qwen.*`, `app.defaults.ai.*` injected via `@Value`): added `app.defaults.sync-option: monkey` to both `application-dist.yml` and `application-dev.yaml`, injected as `@Value("${app.defaults.sync-option}") private SyncOption dftSyncOption`. Lets dev and prod differ without a rebuild.
- Injecting straight into the enum type (not `String` + `valueOf`) is deliberate: spring's `DefaultConversionService` carries `StringToEnumConverterFactory`, so a bad yaml value throws `ConversionFailedException` during context startup instead of failing later at first use. Verified both the conversion and the fail-fast against spring-core 6.2.8, the version boot 3.3.5 resolves.
- Scope: this is the seeding default only — it applies when a user's preference rows are first created. Existing users keep whatever `syncOption` row they already have; there is no backfill. Note `initPreferences()` is reached lazily from `listPreferences()`, i.e. the first time a user opens the Preferences page, so an existing user with no rows yet will also pick up `monkey`.
- Frontend needed no change: `Preferences.vue` renders whatever the API returns. Its local `syncOptions` array (`:233`) still duplicates the `SyncOption` enum's labels — pre-existing, left alone.
- Part 2 of the plan (seeding preferences at account creation, so a new user has them before ever opening the Preferences page) was NOT implemented — only part 1 was requested. Without it, `getPreference()` still throws `ServiceException("Preferences not set")` for a never-visited user, which `MonkeySyncServiceImpl.java:77` absorbs via catch-as-control-flow.
- Verified with `mvn compile` and by parsing both yaml files to confirm the key lands at `app.defaults.sync-option` with its siblings intact.

## Import: drop the connected account's data only, not the whole database

`ExportServiceImpl.dbLoad()` rewrote every imported id to the connected username and then called
`deleteAll()` on eight repositories before saving. On a multi-user instance that wiped *every*
account's transcripts, pages, diffs, named entities, quicknotes and preferences — a single user
restoring a backup destroyed everyone else's library. Same defect the quicknote line added on top
(`repositoryQuickNote.deleteAll()`).

- The drop is now `dropUserData(connectedUsername)`, one `deleteAllBy…_Username` per repository, in
  the same order as before (index → entities → diffs → pages → transcripts → files → quicknotes →
  preferences). Kept as a private helper rather than inlined so the username scoping is stated once
  and the `LOG.info` reports whose data went.
- Each new delete is `@Modifying @Transactional @Query("DELETE FROM … where …username = :username")`,
  matching the existing `RepositoryNamedEntity.delete(...)`. **Bulk JPQL rather than a derived
  `deleteAllBy…` on purpose**: a derived delete loads the rows and queues `em.remove` calls, and
  Hibernate's `ActionQueue` flushes inserts *before* deletes — so wrapping drop-then-save in one
  transaction (or joining an outer one) would replay the account's own rows as PK collisions. The
  bulk statement is issued when called, which is also why `importUserData` was deliberately *not*
  made `@Transactional`: the import is still drop-commit-then-load, exactly as `deleteAll()` was.
- `RepositoryConfig.deleteByConfigId_Username` was folded into the new
  `deleteAllByConfigId_Username` (one method, one meaning); `PreferencesServiceImpl.resetPreference`
  follows. Its `@Transactional` is now redundant but harmless.

### Three bugs in the WIP quicknote export that this uncovered

- `RepositoryQuickNote.findAllByIdFile_Username` was copy-pasted from a transcript repository —
  `EntityQuickNote` has no `idFile`, so Spring Data would have failed the context at startup, not at
  call time. Renamed to `findAllByIdQuickNote_Username`.
- The import stamped `UUID.randomUUID()` into each note's id. **The uuid is the `fileId` its named
  entities are keyed on** (`QuickNoteService.QUICKNOTE_PAGE_NUMBER` rows in `named_entity`), so
  randomising it orphaned every imported note's tags/people while the entities themselves imported
  fine — silent, and invisible until someone opened the tag view. Now keeps the original uuid, which
  is also what makes a re-import idempotent now that the drop is scoped to the same account.
- `DtoExport.quickNotes` defaults to an empty list. Any export taken before quicknotes existed has
  no such key, Jackson leaves the field null, and `dbLoad` iterates it — i.e. every pre-existing
  backup would have NPE'd on import.

Not changed, same defect one endpoint away: `UtilsServiceImpl.deleteAllData()` (behind
`DELETE /data/wipe`, the "Wipe all data" button on the per-user Preferences page) still calls
`deleteAll()` on seven repositories and wipes every account. It can now reuse the same
`deleteAllBy…_Username` methods; left alone because that endpoint's intended scope (per-user vs
admin-wide) wasn't part of this task.

## Collapsed 18 of the 21 one-to-one service interfaces

Every interface in `service/` had exactly one implementation in `service/impl/` and no external
implementer, and none of them was technically required: Boot defaults to
`spring.aop.proxy-target-class=true`, so `@Transactional`, `@Async`, `@Scheduled`, the
`ctx.getBean(X.class)` calls in `tasks/*` and both `@annotation(...)` aspects in `monitoring/` all
work on plain classes. Evidence the split had already stopped being a contract: `AuthService`
imported its own implementation (`AuthServiceImpl.UserData` as a return type), and 7 impls carried
public methods the interface never declared — mostly `@EventListener` / `@PreDestroy` / `@Scheduled`
methods Spring calls directly, i.e. the load-bearing parts were outside the "contract" anyway.

- **18 collapsed, the Drive trio kept.** `DriveService`, `DriveUtilsService` and
  `DriveChangeManagerService` stay interfaces to mark the boundary of the deactivable Drive module
  (`app.drive.enabled` in the upgrade list above); their impls stay in `service/impl/` alongside
  `SecurityConfig`, so no empty package is left behind. Note an interface is *not* what that toggle
  needs — `@ConditionalOnProperty` breaks injection either way; the real seam there is a future
  `SyncSourceStrategy` with two implementations, which is a new interface, not one of these.
- **The class reuses the interface's exact fully-qualified name.** `service/impl/ExportServiceImpl`
  became `service/ExportService`, so every `import fr.monkeynotes.mn.service.ExportService` and every
  `@Autowired private ExportService` in `controller/`, `tasks/`, `interceptor/` and the surviving
  Drive impls is untouched. Bean types, bean names and the injection graph are identical, so the
  `AuthService -> UserService -> PreferencesService` cycle documented in
  `src/main/java/fr/monkeynotes/mn/CLAUDE.md` is neither fixed nor worsened. Per file: package line,
  class declaration, ~94 now-meaningless `@Override`s, the `getLogger(...Impl.class)` argument, and
  the imports that now resolve to the class's own package.
- **Consequence of reusing the FQN, worth knowing before you `git log`:** the new `service/X.java` is
  a heavy modification of the *interface's* history, and `service/impl/XImpl.java` shows as a plain
  delete — git can't pair them as a rename because the destination path already existed. The impl's
  history stops at its old path; `git log --follow src/.../service/ExportService.java` traces the
  interface, not the implementation.

### Four things that would have broken silently

- **`QuickNoteService.QUICKNOTE_PAGE_NUMBER` was declared on the interface**, therefore implicitly
  `public static final`, and referenced *bare* four times inside the impl (inherited) plus qualified
  from `NamedEntityController:109`. Deleting the interface would have deleted the constant. It is now
  `public static final int` on the class, keeping both reference styles working.
- **`AuthServiceImpl.UserData`** (nested record) is now `AuthService.UserData` — the only call-site
  churn in the sweep, three lines in `AuthController` (import + `:45` + `:46`).
- **`UserServiceImpl implements UserService, UserDetailsService`** — the Spring Security interface
  stays, so `loadUserByUsername` keeps its `@Override` while the other 7 in that file were stripped.
- **Javadoc only ever lived on two interfaces** (`QuickNoteService`, `SearchService`) and would have
  been thrown away with them; carried onto the class and its methods.

Also renamed in prose: the bean-graph javadoc on `data/event/UserCreatedEvent`, and a `SecurityConfig`
comment pointing at `QwenServiceImpl`. The backlog and older entries in this file still use the old
`XServiceImpl` names — left as written, since they are a record; read `XServiceImpl` as `XService`.

**Verification — read this before trusting it: the build was NOT run.** The change was made and
checked statically only, at the user's instruction. What was verified: 21 `@Service` beans under
`service/` before and after (a dropped annotation is the one error that compiles and then fails at
startup); zero remaining references to any of the 18 `*Impl` names in Java sources; zero
`import fr.monkeynotes.mn.service.impl` anywhere; every file's declared type matches its filename;
exactly one `@Override` left in the 18 files (`UserService.loadUserByUsername`); no string literal
touched by the token rename; brace balance unchanged from `HEAD` per file. **Still to do: `mvn clean
package`, then start the stack (`docker/compose`) and confirm no `UnsatisfiedDependencyException`.**

## Empty document tree is an empty state, not an error

`ViewService.listRootLevel()` logged `ERROR ... Failed to list root level` with a full stack trace
(`ServiceException: Folder not found id `, note the blank id) on **every** root listing for an
account that had never synced, then returned an empty list — so the endpoint answered `200 []` and
the UI drew an empty panel with no explanation. Two separate defects: a normal state reported as a
failure, and no empty state in the tree.

- `getRootFolder()` threw for three different non-exceptional conditions (preferences never
  initialised, `inputFolderId` set but blank, id pointing at a folder no longer in the database) and
  both callers immediately caught and discarded it. Replaced by `findRootFolder()` returning
  `Optional<EntityFile>`, built on the existing `PreferencesService.getPreferenceOpt()` rather than
  `getPreference()` + try/catch — which is what removes the throw at the source rather than moving
  the catch. `listAllNodes()`/`listRootLevel()` are now `findRootFolder().map(...).orElseGet(...)`,
  and the ERROR log is gone entirely; there is nothing left to report.
- `TreeView.vue` grew the `.empty-state` block the other views use: *"No documents yet — notes synced
  from your tablet appear here once processed"*, worded to be true for all three causes above since
  the frontend can't distinguish them (the endpoint returns a bare list). Distinguishing them would
  need a richer payload than `List<FileNode>`, which the message avoids needing.
- **The component already had an `error` ref that was set on a failed fetch and never rendered
  anywhere** — dead state since it was written. Now displayed, with `pi-exclamation-triangle`, so a
  genuine load failure is visually distinct from an empty account instead of both showing nothing.
- Error handling is now scoped to the root fetch (`isRoot`): `fetchFolder()` is shared between the
  root listing and subfolder expansion, and surfacing a child's failure through the same `error` ref
  would blank out the tree already on screen. A failed expansion still only logs to the console, as
  before.
- `loading` starts `true` because a root fetch always fires on mount — otherwise the empty state
  renders for one frame before the first response lands. TreeView keeps no spinner of its own; it
  still reports upward through `loading-status` to Home's global one.

Verified statically only, not built (`mvn` not run, per instruction): types checked by hand against
`getPreferenceOpt` → `Optional<String>`, `idFile(String)` → `IdFile`,
`repositoryFile.findById(IdFile)` → `Optional<EntityFile>`; `LOG` still has 6 other uses in the file;
`java.util.*` already covers `Collections`/`Optional`. **Still to do: rebuild, then load Home on an
account with no synced documents and confirm the message replaces the silent empty panel.**

## Transcript view: Copy menu (raw now, markdown later)

The action row could only get a document out as a PDF blob; there was no way to get the *text*.
Added a **Copy** button next to PDF opening a popup `Menu` with **Copy raw** and **Copy as MD**, the
latter registered but deliberately inert — the menu shape is settled now so the markdown renderer
can land later without re-touching the layout. All in `TranscriptView.vue`; `<Menu>` needs no import
because `vite.config.js` runs `unplugin-vue-components` with `PrimeVueResolver`. Same popup pattern
as `Home.vue`'s bulk "Actions" button, `<Menu>` kept inside the flex `.action-row` as it is there.

- **Raw means the stored text, not the rendered DOM.** `page.transcript` is unrendered, so the named
  entity syntax (`<T : tag>` and friends) is copied verbatim and none of `renderNamedEntities`' HTML
  is involved. That difference is the whole reason "Copy as MD" exists as a separate option.
- Pages are joined by a `--- page N ---` marker line, `pageNumber + 1` to match the `p. N` display
  convention used by the tag links in the same view. **Pages with no text are skipped** rather than
  pasted as a bare marker — a transcript still being OCR'd is the common case — and the gap stays
  visible in the marker numbering, so nothing is silently dropped.
- `copyActionItems` binds `command:` to hoisted `function` declarations, not `const` arrows. The
  array is built at setup time, above where those functions appear in the file; arrows would be in
  the temporal dead zone and throw a ReferenceError on mount.
- **Feedback goes on the button** (`Copied` + `pi-check`, or `Copy failed` + `pi-times`, reverting
  after 1.5s/2.5s) rather than through this view's `error` ref, because that ref is set in three
  places (load failure, update failure) and **is never rendered anywhere in the template** — dead
  state, same as the one found in `TreeView.vue`. Registering PrimeVue's `ToastService` was the
  alternative and was rejected as infrastructure this task doesn't need; `main.ts` currently
  registers only `ConfirmationService`. The reset timer is cleared in `onUnmounted` so it cannot
  write to a ref after teardown.
- `navigator.clipboard` is `undefined` outside a secure context, so over a plain-http dev tunnel the
  call throws rather than rejects — it lands in the same `catch` and degrades to the failed state.
  No deprecated `execCommand` fallback.

Verified statically only — **this project has no type checking at all**: there is no `tsc`/`vue-tsc`
in `node_modules` and `npm run build` is a bare `vite build`, which strips TS types via esbuild
without checking them, so the annotations here are decoration. A build was also deliberately not run
because it overwrites `ui/dist/`, a deployment artifact. Checked by hand: no duplicate identifiers,
every new template identifier declared, `computed`/`onUnmounted` added to the `vue` import, both
menu commands hoisted. **Still to do: `npm run dev`, then copy a multi-page transcript and paste it.**
