# 🔍 Code Review — agent-game (MMO Agent Game)

**Reviewer:** Senior Java Developer  
**Date:** 2026-04-09  
**Stack:** Spring Boot 4.0.4 · Java 25 · PostgreSQL · Redis · Spring AI (Groq/Llama) · WebSocket/STOMP

---

## 📊 Executive Summary

Projekt to MMO idle game z agentami sterowanymi przez AI (LLM). Architektura jest **na surprisingly dobrym poziomie** jak na projekt side-project — widać znajomość DDD, hexagonal architecture, event-driven design i optymalistyczne blokowanie w Redis. Jest jednak sporo problemów krytycznych (bezpieczeństwo, integralność danych), poważnych problemów z wydajnością i kilkanaście mniejszych issue'ów.

**Ogólna ocena: 6/10** — solidna baza, ale wymaga istotnych poprawek zanim trafi do produkcji.

---

## 🏗️ Architektura

### Co jest dobrze ✅

- **Domenowy model (Rich Domain Model):** `Agent` encapulloguje logikę biznesową (metody `assignGoal`, `applyThought`, `completeMovement`, `takeDamage`), zamiast być anemicznym POJO. To jest standard korporacyjny i dobrze zrobione.
- **Hexagonal Architecture (Ports & Adapters):** Interfejs `Brain` jako port, `GeminiBrainAdapter` jako adapter. Czysta separacja — AI jest wymienny bez dotykania domain layer.
- **CQRS-light z Redis + Postgres:** PostgreSQL dla persistent state, Redis dla real-time movement. Sprytne użycie `WorldStateSynchronizer` z `TransactionSynchronization.afterCommit()`.
- **Event-driven:** Spring Application Events do rozprzestrzeniania zmian (arrivals, state updates, goal assignments).
- **Optimistic locking w Redis:** Lua script do atomowych update'ów `AgentWorldState`. Wymaga uwagi (patrz niżej), ale koncepcja jest dobra.
- **Testy:** Jest sporo testów — jednostkowe, integracyjne, E2E, concurrency, load. To rzadkość w projektach hobbystycznych i plus za to.

### Co wymaga poprawy ⚠️

- **Brak clear domain boundaries:** Location module bezpośrednio trzyma logikę portalów zamiast mieć osobny `PortalService`. Controller robi JOIN query w manualny sposób.
- **Engine jako singleton scheduled bean:** Game engine to pojedynczy `@Scheduled` bean — to się nie skaluje. W klastrze wielu instancji będą przetwarzać te same ticki.
- **Brak boundary w API paths:** `/api/v1/auth/**` ale `/api/agents/**` i `/api/locations/**` — niespójne versioning.
- **DataInitializer jako jedyny źródło seed data:** `ddl-auto=create` + ręczny seeder. W korporacji to jest Flyway/Liquibase mandatory.

---

## 🔴 KRYTYCZNE — Security

### 1. ✅ ZROBIONE — 🔥 WebSocket `setAllowedOriginPatterns("*")`

```java
// WebSocketConfig.java
registry.addEndpoint("/ws")
    .setAllowedOriginPatterns("*");  // ← OWASP A5: Security Misconfiguration
```

**Problem:** Pozwala każdemu originowi na połączenie WebSocket. CSRF isn't the concern here — the concern is that any malicious website can open a WebSocket connection to your server and listen to all game events (position updates, agent states) of authenticated users who have an active session.

**Fix:**
```java
registry.addEndpoint("/ws")
    .setAllowedOrigins("http://localhost:8081", "http://localhost:19006");
```

### 2. ✅ ZROBIONE — Actuator endpoint publiczny

```java
// SecurityConfig.java
.requestMatchers("/actuator/**").permitAll()
```

**Problem:** Spring Boot Actuator na produkcji bez auth to potencjalny kataklizm. Endpointy takie jak `/actuator/env`, `/actuator/beans`, `/actuator/mappings` ujawniają:
- JWT secret (`jwt.secret`)
- API keys (`spring.ai.openai.api-key`)
- Pełną konfigurację aplikacji
- Mapowanie wszystkich endpointów

**Fix:**
```java
.requestMatchers("/actuator/health", "/actuator/info").permitAll()
// Reszta actuator endpoints wymaga auth LUB jest wyłączona:
// management.endpoints.web.exposure.include=health,info
```

### 3. 🔥 `RegisterRequest` bez walidacji

```java
public record RegisterRequest(String username, String password) {
    // Brak @NotBlank, @Size, żadnej walidacji
}
```

**Problem:** Można zarejestrować użytkownika z pustym username lub pustym hasłem. Brak minimalnej długości hasła, wymogów complexity, limitu długości username.

**Fix:**
```java
public record RegisterRequest(
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[a-zA-Z0-9_]+$")
    String username,

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128)
    String password
) {}
```

### 4. 🔥 Brak rate limiting na auth endpoints

Brak żadnej ochrony przed brute-force na `/api/v1/auth/login` i `/api/v1/auth/register`. W bankowości to jest absolutnie mandatory — nawet basic Spring `Bucket4J` albo `spring-boot-starter-security` z custom filter.

### 5. ✅ ZROBIONE — 🔥 JWT secret z env var — brak weryfikacji na startup

```properties
jwt.secret=${JWT_SECRET}
```

**Problem:** Jeśli `JWT_SECRET` nie jest ustawiony, aplikacja wystartuje z dosłownym stringiem `${JWT_SECRET}` jako secret. Każdy kto o tym wie, może wygenerować dowolny token.

**Fix:** Dodaj walidację w `JwtService`:
```java
@PostConstruct
public void validateSecret() {
    if (secretKey == null || secretKey.isBlank() || secretKey.startsWith("$")) {
        throw new IllegalStateException("JWT_SECRET is not configured!");
    }
    if (secretKey.length() < 32) {
        throw new IllegalStateException("JWT_SECRET must be at least 32 characters!");
    }
}
```

### 6. ✅ ZROBIONE — 🟡 JWT bez issuer/audience claims

Token zawiera tylko `subject` (username). W korporacyjnych systemach zawsze dodaje się `iss` i `aud`:
```java
.withIssuer("agent-game")
.withAudience("agent-game-api")
```

---

## 🔴 KRYTYCZNE — Poprawność i Integralność Danych

### 7. ✅ ZROBIONE — Kompilacyjny błąd — brakujący `}` w `AgentWebSocketController`

```java
// AgentWebSocketController.java
@EventListener
public void onAgentStateUpdated(AgentStateUpdatedEvent event) {
    // ...
    messagingTemplate.convertAndSend(destination, state);
    
@EventListener  // ← BRAK ZAMYKAJĄCEGO } dla poprzedniej metody!
public void onAgentArrived(AgentArrivedEvent event) {
```

**Problem:** Ten kod się **nie skompiluje**. Brakujący closing brace. To oznacza że repo jest w broken state.

### 8. ✅ ZROBIONE — Race condition: Redis state vs PostgreSQL state

Scenariusz:
1. User wywoła `assignGoal` → transakcja COMMIT → `syncMovementAfterCommit` zapisuje do Redis
2. Jednocześnie Game Engine tick czyta z Redis, updateuje pozycję, zapisuje do Redis
3. `finalizeMovement` zapisuje do PostgreSQL
4. Ale jeśli `updateAtomic` w Redis nie powiedzie się (stale version), tick się cicho ignoruje

**Problem:** Brak synchronizacji z powrotem z Redis → PostgreSQL. Jeśli Redis się zrestartuje, wszyscy agenty "wrócą" do swojej ostatniej PostgreSQL pozycji. Nie ma mechanizmu graceful degradation ani state reconstruction.

**Fix:** Dodaj `@Scheduled` job który periodowo synchronizuje active Redis states do PostgreSQL jako safety checkpoint, lub zaimplementuj replay z event journal.

### 9. ✅ ZROBIONE — `LevelUp()` w `AgentStats` modyfikuje maxHp ale nie odpowiada za heal poprawnie

```java
private AgentStats levelUp() {
    int nextThreshold = getExpThreshold();
    return this.toBuilder()
            .level(this.level + 1)
            .experience(this.experience - nextThreshold)
            .maxHp(this.maxHp + 20)
            .hp(this.maxHp + 20) // ← BUG: this.hp to stare maxHp, nie nowe!
            .build();
}
```

**Problem:** `this.maxHp + 20` to nie to samo co "pełne HP po level up". Przykład: agent ma 80/100 HP. `this.maxHp + 20 = 120`, ale powinno być `this.maxHp + 20 = 120` a HP = 120 (nowy max). W tym wypadku coincydencją jest OK, ale komentarz "Full heal on level up" jest mylący — to działa tylko dlatego że `this.hp` nie jest podane explicite. Wait — `this.hp + 20` gdy hp=80 da 100, nie 120.

**To jest BUG.** Agent z 80/100 HP po level up będzie miał 100/120, nie 120/120.

**Fix:**
```java
private AgentStats levelUp() {
    int nextThreshold = getExpThreshold();
    int newMaxHp = this.maxHp + 20;
    return this.toBuilder()
            .level(this.level + 1)
            .experience(this.experience - nextThreshold)
            .maxHp(newMaxHp)
            .hp(newMaxHp)  // Full heal = new max
            .build();
}
```

### 10. 🔥 `ddl-auto=create` w properties

```properties
spring.jpa.hibernate.ddl-auto=create
```

**Problem:** To **KASUJE** i tworzy tabele na nowo przy każdym restarcie. Żadne dane nie przetrwają restartu. W środowisku dev to jeszcze OK jako conscious choice, ale:
- Nie ma Flyway/Liquibase
- Nie ma osobnego profilu `application-dev.properties`
- `DataInitializer` sprawdza `locationRepository.count() > 0` ale przy `create` to zawsze będzie 0

To jest celowe, ale powinno być wyraźnie oznaczone jako dev-only.

---

## 🟠 DUŻE — Design i Performance

### 11. ✅ ZROBIONE — `AgentSecurity.isOwner()` robi DB query w każdym request

```java
public boolean isOwner(UUID agentId) {
    return agentRepository.findById(agentId)
            .map(agent -> {
                if (agent.getOwner() == null) return false;
                return agent.getOwner().getUsername().equals(currentUsername);
            })
            .orElse(false);
}
```

**Problem:** Każdy request do `@PreAuthorize("@agentSecurity.isOwner(#id)")` wykonuje pełne zapytanie do bazy. Przy 100 concurrent requests to 100 zapytań. W bankowości to idzie przez cache.

**Fix:** Cache wyniku:
```java
@Cacheable(value = "agentOwner", key = "#agentId", unless = "#result == false")
public boolean isOwner(UUID agentId) { ... }
```

### 12. `findAll()` w `AgentController` ładuje WSZYSTKICH agentów

```java
@GetMapping
public List<AgentDto> listAll() {
    return agentService.findAll().stream()
            .map(agentMapper::toDto)
            .collect(Collectors.toList());
}
```

**Problem:** Bez pagination, bez filtering, bez security check. Każdy authenticated user widzi agentów wszystkich graczy, włącznie z pozycjami i statystykami. N+1 problem z LAZY loading na `currentLocation`.

**Fix:** Dodaj pagination + ownership filter:
```java
@GetMapping
public Page<AgentDto> listAll(Pageable pageable,
                              @AuthenticationPrincipal UserDetails user) {
    return agentService.findByOwnerUsername(user.getUsername(), pageable)
            .map(agentMapper::toDto);
}
```

### 13. ✅ ZROBIONE — N+1 problem w `LocationController`

```java
List<PortalDto> portals = portalRepository.findAllBySourceLocationId(id)
    .stream()
    .map(p -> PortalDto.builder()
        .targetLocationName(p.getTargetLocation().getName()) // ← LAZY LOAD!
        .build())
    .collect(Collectors.toList());
```

**Problem:** Dla N portali, każdy `getTargetLocation()` generuje osobne zapytanie. `FetchType.LAZY` w `Portal` + brak `@EntityGraph` lub JOIN FETCH.

**Fix:** W `PortalRepository`:
```java
@Query("SELECT p FROM Portal p JOIN FETCH p.targetLocation WHERE p.sourceLocation.id = :id")
List<Portal> findAllBySourceLocationIdWithTarget(@Param("id") UUID id);
```

### 14. `GoalAssignedListener` łączy event listener z service call

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void onGoalAssigned(GoalAssignedEvent event) {
    var agent = agentService.findById(event.agentId()); // ← oddzielne zapytanie
    agentThinkingService.processThinking(event.agentId()); // ← jeszcze jedno
}
```

**Problem:** Ten listener woła `agentService.findById()` ale nic z nim nie robi — po prostu loguje. Potem woła `processThinking()` który *też* ładuje agenta. Podwójne ładowanie, dodatkowy `REQUIRES_NEW` transaction.

**Fix:** Usuń nieużywane `findById`:
```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void onGoalAssigned(GoalAssignedEvent event) {
    agentThinkingService.processThinking(event.agentId());
}
```

### 15. `WorldStateSynchronizer.executeAfterCommit` — silent failure path

```java
} else {
    action.run(); // ← jeśli nie ma aktywnej transakcji, uruchamia natychmiast
}
```

**Problem:** Jeśli kod zostanie wywołany poza transakcją (np. z testu albo z async context), Redis update się wykona natychmiast, *przed* PostgreSQL commit. To łamie fundamentalny kontrakt "Postgres first, Redis after".

**Fix:** Loguj warning albo rzucaj exception:
```java
} else {
    log.warn("No active transaction — executing synchronously. This may cause state inconsistency.");
    action.run();
}
```

### 16. Redis Lua script — string matching zamiast JSON parsing

```lua
if current:find('\"version\":' .. ARGV[1], 1, true) then
```

**Problem:** Ten Lua script szuka substring `"version":N` w JSON. Jest fragile:
- `"version":10` będzie matchować `"version":100`
- Zmiana serializera (np. dodanie spacji) zepsuje skrypt
- Nie jest to prawdziwe optimistic locking — to probabilistic

**Fix:** Użyj pełnego JSON parsing w Lua 5.1+ albo przejdź na Redis WATCH/MULTI, albo użyj Redis hash fields zamiast serialized JSON:
```
HSET agent:state:{id} version 1 x 10 y 20 ...
```

---

## 🟡 ŚREDNIE — Maintainability

### 17. `DataInitializer` seeduje hasło "admin123"

```java
Player master = Player.create("MasterAdmin", passwordEncoder.encode("admin123"));
```

**Problem:** Hardcoded credentials w kodzie źródłowym. Jeśli to trafi na produkcję, ktoś znajdzie to w repo.

**Fix:** Seeduj tylko przez env vars:
```java
String adminPass = System.getenv("ADMIN_INITIAL_PASSWORD");
if (adminPass == null) {
    log.warn("ADMIN_INITIAL_PASSWORD not set, skipping admin seed");
    return;
}
```

### 18. Brak osobnych service layer w world module

`LocationController` bezpośrednio używa repository. Brak `LocationService`. W korporacji każda domain module ma service layer — nawet jeśli na razie jest trivial.

### 19. Brak API versioningu

```
/api/v1/auth/**       ← v1
/api/agents/**        ← brak wersji
/api/locations/**     ← brak wersji
```

**Fix:** Ujednolicaj do `/api/v1/` dla wszystkiego.

### 20. `@Slf4j` + logowanie business logic

```java
log.info("--- AI THINKING START for Agent: {} ---", agent.getName());
log.info("Perception: Goal='{}', Location='{}', ...", ...);
log.info("--- AI THINKING END ---");
```

**Problem:** Verbose logging w produkcji spali I/O i dyski. AI thinking powinno być na `DEBUG` level. `INFO` powinno być reserved dla significant business events.

### 21. Brak profiles

Tylko `@Profile("!test")` na Groq config. Brak osobnych profili dla:
- `dev` (H2 + mock AI)
- `prod` (PostgreSQL + Redis cluster)
- `test`

### 22. `EngineControl` jako po prostu boolean

Brak żadnego health check czy lifecycle management. W klastrze, jak jeden node nie jest "ready", nie ma sposobu tego wykryć z zewnątrz.

---

## 🔵 MINOR — Style i Best Practices

### 23. `Collectors.toList()` zamiast `Stream.toList()`

Java 16+ ma `toList()`:
```java
// Before
.collect(Collectors.toList());
// After
.toList();
```

### 24. ✅ ZROBIONE — `AgentStats` używa `Integer` zamiast `int`

```java
private Integer hp;
private Integer maxHp;
```

**Problem:** `Integer` pozwala na `null`. W domain model dla RPG stats nie powinno być nullable HP. To powinien być `int` (primitive).

### 25. `Location.java` — domyślny `@NoArgsConstructor` z public access

```java
@NoArgsConstructor  // public constructor
```

Vs `Agent.java`:
```java
@NoArgsConstructor(access = AccessLevel.PROTECTED)
```

Niespójność. Encje JPA potrzebują no-arg constructor, ale nie musi być publiczny.

### 26. Brak `@EqualsAndHashCode.Include` na `Player`

`Agent` ma to dobrze:
```java
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
```

Ale `Player`, `Location`, `Portal` używają domyślnego `@EqualsAndHashCode` z Lombok — co porównuje wszystkie fieldy, włącznie z LAZY associations. To może trigger N+1 queries przy wstawianiu do `HashSet` itp.

### 27. `boot_error.log` w repo

Plik `boot_error.log` (38KB) nie powinien być w repo. Dodaj do `.gitignore`.

### 28. `compose.yaml` — brak version pinning

```yaml
services:
  postgres:
    image: 'postgres:latest'  # ← latest może się zmienić i zepsuć
  redis:
    image: 'redis:latest'
```

**Fix:** Pin versions:
```yaml
image: 'postgres:16-alpine'
image: 'redis:7-alpine'
```

### 29. Brak `.editorconfig` i consistent formatting

Różne pliki mają różne style formatowania. W korporacji to jest standard.
