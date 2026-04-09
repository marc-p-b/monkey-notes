# MonkeyNotes Development Skill

You are helping develop MonkeyNotes, a document OCR platform for eInk tablets.

## Architecture Patterns

### Backend (Spring Boot)
- **Controllers**: `src/main/java/fr/monkeynotes/mn/controller/` - REST endpoints with `@RestController`, `@RequestMapping("/api/...")`
- **Services**: Interface in `service/`, implementation in `service/impl/` - Business logic with `@Service`
- **Entities**: `data/entity/Entity*.java` - JPA entities with `@Entity`, `@Table`
- **Repositories**: `data/repository/*Repository.java` - Spring Data JPA with `JpaRepository<Entity, Long>`
- **DTOs**: `data/dto/` - Data transfer objects for API responses

### Frontend (Vue 3 + PrimeVue)
- **Components**: `ui/src/components/*.vue` - Single File Components
- **Router**: `ui/src/router/index.ts` - Vue Router with JWT validation
- **Store**: `ui/src/composables/store.js` - Pinia state management
- **API calls**: Use fetch with JWT token from localStorage

## Code Conventions

### Java
- Package: `fr.monkeynotes.mn`
- Entity prefix: `Entity` (e.g., `EntityFile`, `EntityTranscript`)
- Service interfaces define contracts, `*Impl` classes implement them
- Use `@Autowired` constructor injection
- JWT auth via `JwtFilter` - endpoints under `/api/` require authentication

### Vue
- PrimeVue components for UI (Button, DataTable, Dialog, etc.)
- Composition API with `<script setup>`
- API base URL from `window.env.API_BASE`

## When Adding Features

1. **New Endpoint**: Create controller method, add service interface method, implement in service
2. **New Entity**: Create entity class, repository interface, update related services
3. **New Page**: Create Vue component, add route, link from navigation

## Named Entity Patterns
OCR recognizes: `[T:tag]`, `[p:person]`, `[@:email]`, `[d:EU-date]`, `[du:US-date]`, `[X:todo]`, `[V:done]`
