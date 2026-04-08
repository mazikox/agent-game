# Code Review: System Agentów i AI

**Scope:** Backend Java (pakiety `agent`, `ai`, `engine`, `world`, `player`, `security`, `config`, `exception`)
**Pominięto:** Frontend (mobile)
**Wersja:** Spring Boot 4.0.4, Java 25, Spring AI 2.0.0-M4

---

## Ocena ogólna: 7.5/10

Projekt jest solidnie zaprojektowany jak na etap portfolio/nauki. Architektura hexagonalna jest widoczna i stosowana konsekwentnie. Rich Domain Model jest prawidłowo implementowany w `Agent` i `AgentStats`. System event-driven jest elegancki. Są jednak istotne problemy do rozwiązania.

---

## 🏗️ Architektura i DDD

### ✅ Co jest dobrze

- **Hexagonal Architecture** — czysty port `Brain` z adapterem `GeminiBrainAdapter` oddzielonym profilem `@Profile("!test")`. Wzorcowa implementacja.
- **Rich Domain Model** — `Agent.applyThought()`, `Agent.completeMovement()`, `AgentStats.takeDamage()` z niemodyfikowalnymi Value Objects (toBuilder pattern). Logika jest tam gdzie powinna.
- **Event-driven design** — `GoalAssignedEvent` → `GoalAssignedListener` → `AgentThinkingService`, `AgentArrivedEvent` → `PortalEventListener` + `AiGoalExecutionListener`. Czytelny flow.
- **Separation of Concerns** — `AgentWorldState` (Redis, volatile) vs `Agent` (Postgres, persistent). Dual-storage jest dobrze przemyślany.

### Znalezione problemy

---

## 🔴 Krytyczne (Critical)

### C1. [ZROBIONE] `AgentWorldStateRepository.updateAtomic()` — fałszywe optimistic locking

> [!TIP]
> Rozwiązano poprzez implementację atomowego skryptu Lua, który wykonuje operację CAS (Compare-And-Swap) bezpośrednio w Redis. Wyeliminowano race condition między wątkami silnika gry.

**Plik:** [AgentWorldStateRepository.java](file:///c:/AgentGierka/src/main/java/com/agentgierka/mmo/agent/repository/AgentWorldStateRepository.java#L69-L82)

```java
public boolean updateAtomic(AgentWorldState newState) {
    AgentWorldState existing = findById(newState.getAgentId());
    // ... version check ...
    newState.incrementVersion();
    save(newState);
    return true;
}
```

Operacja **nie jest atomowa**. Między `findById()` a `save()` inny wątek (virtual thread z `GameEngine`) może nadpisać stan. Nazwa metody sugeruje atomowość, której nie zapewnia.

> [!CAUTION]
> Przy wielu agentach poruszających się jednocześnie, race condition doprowadzi do utraty pozycji (agent "teleportuje się" wstecz albo pomija kroki).

**Rekomendacja:** Użyj Redis `WATCH`/`MULTI`/`EXEC` (transakcje Redis) lub Lua script do CAS (Compare-And-Swap):

```java
// Pseudokod z Lua script
String luaScript = """
    local current = redis.call('GET', KEYS[1])
    local parsed = cjson.decode(current)
    if parsed.version == tonumber(ARGV[1]) then
        redis.call('SET', KEYS[1], ARGV[2])
        return 1
    end
    return 0
""";
```

---

### C2. [ZROBIONE] `WorldStateSynchronizer.executeAfterCommit()` — cichy failure bez fallback

> [!TIP]
> Dodano blok `else`, który wykonuje synchronizację natychmiast, jeśli nie wykryto aktywnej transakcji Springa. Zapewnia to spójność danych w testach i procesach tła.

**Plik:** [WorldStateSynchronizer.java](file:///c:/AgentGierka/src/main/java/com/agentgierka/mmo/agent/service/WorldStateSynchronizer.java#L47-L56)

```java
private void executeAfterCommit(Runnable action) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
        // ...rejestracja synchronizacji
    }
    // Jeśli synchronizacja NIE jest aktywna → nic się nie dzieje, akcja ginie po cichu
}
```

> [!WARNING]
> Gdy metoda jest wywoływana poza kontekstem transakcji (np. z event listenera AFTER_COMMIT, lub w testach), akcja jest **po cichu ignorowana**. Agent wydaje rozkaz ruchu, ale Redis nigdy nie zostaje zaktualizowany — agent "zamraża się".

**Rekomendacja:** Dodaj gałąź `else`:
```java
} else {
    log.warn("No active transaction synchronization — executing action immediately");
    action.run();
}
```

---

## 🟠 Wysokie (High)

### H1. [ZROBIONE] `Agent — encja JPA z @Setter(AccessLevel.PROTECTED)` łamana przez `@Builder`

> [!TIP]
> Do pełnej hermetyzacji domeny dodano `@Builder(access = AccessLevel.PRIVATE)` oraz ograniczono widoczność konstruktorów (`@AllArgsConstructor(access = AccessLevel.PRIVATE)`). Jedynym publicznym punktem wejścia jest teraz metoda fabrykancka `create()`.

**Plik:** [Agent.java](file:///c:/AgentGierka/src/main/java/com/agentgierka/mmo/agent/model/Agent.java#L13-L21)

Lombok `@Builder` generuje publiczny builder, który pozwala na ominięcie ochrony setterów PROTECTED. Każdy może zrobić:
```java
agent.toBuilder().status(AgentStatus.MOVING).build(); // omija domenowe reguły
```

To nie jest problem samego `@Builder`, ale fakt, że **nie kontrolujesz dostępu builderu**. W serwisach i listenerach istnieje pokusa użycia buildera zamiast metod domenowych.

**Rekomendacja:** Dodaj `@Builder(access = AccessLevel.PRIVATE)` lub `@Builder(access = AccessLevel.PACKAGE)` i udostępnij tylko factory method `Agent.create(...)`. Alternatywnie, jeśli builder jest potrzebny w testach, przenieś go do klasy testowej (TestAgentBuilder).

---

### H2. [ZROBIONE] `GeminiBrainAdapter.think()` — zero error handling na odpowiedzi AI

> [!TIP]
> Wprowadzono wielowarstwową obsługę błędów: try-catch dla API, null-safety dla odpowiedzi oraz "Hallucination Guard" walidujący współrzędne AI względem mapy. W razie błędu agent bezpiecznie przechodzi w stan IDLE.

**Plik:** [GeminiBrainAdapter.java](file:///c:/AgentGierka/src/main/java/com/agentgierka/mmo/ai/adapter/GeminiBrainAdapter.java#L79-L82)

```java
String content = chatModel.call(prompt).getResult().getOutput().getText();
return converter.convert(content);
```

Brak obsługi:
- **NPE** gdy `getResult()`, `getOutput()` lub `getText()` zwrócą `null`
- **Parsing failure** gdy AI odpowie błędnym JSON-em (częste z LLM)
- **Rate limiting / API timeout** — wyjątek leci niekontrolowanie w górę stosu
- **AI halucynacje** — `targetX`/`targetY` poza granicami mapy

> [!IMPORTANT]
> W systemie produkcyjnym AI odpowie błędnie w ~5-15% przypadków. Brak obsługi = agent w nieokreślonym stanie.

**Rekomendacja:**
```java
@Override
public Thought think(Perception perception) {
    try {
        // ... prompt building ...
        String content = chatModel.call(prompt).getResult().getOutput().getText();
        return converter.convert(content);
    } catch (Exception e) {
        log.error("AI thinking failed for agent {}: {}", perception.name(), e.getMessage());
        return Thought.builder()
                .actionSummary("AI had trouble thinking. Standing by.")
                .status("IDLE")
                .build();
    }
}
```

---

### H3. [ZROBIONE] `AgentThinkingService.processThinking()` — NPE na `getCurrentLocation()`

> [!TIP]
> Dodano Guard Clause sprawdzający obecność lokacji zaraz po pobraniu agenta z bazy. Zapobiega to przerwaniu procesu myślenia błędem NPE dla nowych agentów.

**Plik:** [AgentThinkingService.java](file:///c:/AgentGierka/src/main/java/com/agentgierka/mmo/ai/service/AgentThinkingService.java#L38-L48)

```java
portalRepository.findAllBySourceLocationId(agent.getCurrentLocation().getId())
```

Jeśli `currentLocation` jest `null` (agent dopiero stworzony bez lokalizacji), to leci NPE. Brak null-check, a `@ManyToOne(fetch = LAZY)` w `Agent` oznacza, że LazyInitializationException też jest możliwy jeśli sesja Hibernate się zamknie.

**Rekomendacja:** Dodaj guard clause:
```java
if (agent.getCurrentLocation() == null) {
    log.warn("Agent {} has no location, skipping thinking", agent.getName());
    return;
}
```

---

### H4. [ZROBIONE] `PortalEventListener.onAgentArrived()` — niepotrzebne query do DB (N+1 potencjał)

> [!TIP]
> Nazwa agenta (`agentName`) została dodana bezpośrednio do rekordu `AgentArrivedEvent`. Listener korzysta z niej bez dopytywania bazy danych, co znacząco redukuje operacje I/O.

**Plik:** [PortalEventListener.java](file:///c:/AgentGierka/src/main/java/com/agentgierka/mmo/agent/event/PortalEventListener.java#L39)

```java
String name = agentService.findById(event.agentId()).getName();
```

To zapytanie jest robione **tylko po to, żeby zalogować nazwę agenta**. `AgentService.findById()` to:
1. `SELECT * FROM agents WHERE id = ?`
2. `GET agent:state:{id}` z Redis
3. `entityManager.detach(agent)`

...trzy operacje IO dla jednego loga debugowego.

**Rekomendacja:** Dodaj `agentName` do `AgentArrivedEvent`:
```java
public record AgentArrivedEvent(
    UUID agentId,
    String agentName,  // dodane
    Location location,
    Integer x, Integer y,
    MovementType type
) {}
```

---

### H5. Brak walidacji `@RequestBody` w kontrolerach [ZROBIONE]

> [!TIP]
> Wprowadzono dedykowane rekordy Request DTO (`AssignGoalRequest`, `MoveRequest`, `UpdateStatusRequest`) z pełną walidacją JSR-303. Błędy walidacji są teraz obsługiwane centralnie i zwracają kod 400.

**Plik:** [AgentController.java](file:///c:/AgentGierka/src/main/java/com/agentgierka/mmo/agent/web/AgentController.java)

```java
@PostMapping("/{id}/goal")
public AgentDto assignGoal(@PathVariable("id") UUID id, @RequestBody String goal) {
```

- `goal` nie ma `@NotBlank` ani żadnej walidacji
- `@RequestBody String` — przyjmuje raw body, nie JSON. Klient musi wysłać plain text, nie `{"goal": "..."}`. To nieintuicyjne z perspektywy REST API
- Endpoint `updateStatus` parsuje string na enum bez walidacji → `IllegalArgumentException` leci niekontrolowane

**Rekomendacja:** Stwórz request DTO z walidacją:
```java
public record AssignGoalRequest(
    @NotBlank String goal
) {}
```

---

## 🟡 Średnie (Medium)

### M1. [ZROBIONE] `AgentDto` — `@Data` zamiast `record`

> [!TIP]
> Przekształcono `AgentDto` w natywny rekord Javy. Zapewnia to niemutowalność obiektów transferu danych (DTO) oraz czystszą składnię, zgodnie z najlepszymi praktykami nowoczesnej Javy i Springa.

**Plik:** [AgentDto.java](file:///c:/AgentGierka/src/main/java/com/agentgierka/mmo/agent/web/dto/AgentDto.java)

DTO jest mutowalny (`@Data` generuje settery). W DDD i clean architecture DTO powinno być niemutowalne. Java records to naturalny wybór:

```java
@Builder
public record AgentDto(UUID id, String name, ...) {}
```

---

### M2. `DataInitializer` — hardcoded hasło `admin123`

**Plik:** [DataInitializer.java](file:///c:/AgentGierka/src/main/java/com/agentgierka/mmo/config/DataInitializer.java#L87)

```java
Player master = Player.create("MasterAdmin", passwordEncoder.encode("admin123"));
```

> [!WARNING]
> Nawet w dev environment, hardcoded hasło w kodzie źródłowym to problem. Jeśli ten initializer przypadkowo zadziała na produkcji, mamy otwarte konto admina.

**Rekomendacja:** Dodaj `@Profile("dev")` lub przenieś dane inicjalne do Flyway migration z parametryzowanym hasłem.

---

### M3. CORS — duplikacja konfiguracji

**Pliki:** [WebConfig.java](file:///c:/AgentGierka/src/main/java/com/agentgierka/mmo/config/WebConfig.java) + [SecurityConfig.java](file:///c:/AgentGierka/src/main/java/com/agentgierka/mmo/security/SecurityConfig.java#L63-L74)

CORS jest konfigurowany **dwukrotnie** — raz w `WebConfig.addCorsMappings()` i raz w `SecurityConfig.corsConfigurationSource()`. Listy origins się **różnią**:
- `WebConfig`: tylko `http://localhost:8081`
- `SecurityConfig`: `http://localhost:8081`, `http://localhost:19006`, `http://127.0.0.1:8081`

Spring Security CORS ma priorytet, więc `WebConfig` jest de facto **martwy kod**.

**Rekomendacja:** Usuń `WebConfig` albo ujednolić — zostaw jedną konfigurację w `SecurityConfig`.
**Status:** ✅ Rozwiązane (WebConfig usunięty, konfiguracja ujednolicona w SecurityConfig).

---

### M4. [ZROBIONE] `GameEngine` — brak timeout/limit na Virtual Threads

> [!TIP]
> Zaimplementowano natywne wsparcie dla Virtual Threads w Spring Boot 4. Proces ruchu agentów został zrównoleglony za pomocą `CompletableFuture.runAsync()`, a bezpieczeństwo czasowe zapewnia `CompletableFuture.allOf(...).get(timeout)`, który przerywa czekanie po czasie zdefiniowanym w `game.engine.tick-timeout`.

**Plik:** [GameEngine.java](file:///c:/AgentGierka/src/main/java/com/agentgierka/mmo/engine/GameEngine.java)

> [!NOTE]
> Rozwiązano poprzez migrację na natywne Virtual Threads (Spring Boot 4) oraz zastosowanie `CompletableFuture.allOf(...).get(timeout)`. Zapewnia to, że nawet przy zawieszeniu się logiki pojedynczego agenta, silnik gry nie zostanie zablokowany i przejdzie do kolejnego cyklu (tick) po upływie zdefiniowanego czasu.

---

### M5. [ZROBIONE] `AgentStats` — Javadoc łamie zasadę Self-documenting Code

> [!TIP]
> Usunięto nadmiarowy i błędny Javadoc, który podawał nieprawidłowy typ zwracany. Kod jest teraz zgodny z zasadą Self-documenting Code.

Z reguł projektu:
> Stosujemy zasadę Self-documenting Code. Unikamy zbędnych komentarzy opisujących oczywiste działanie.

Komentarz `@return true if still alive, false if died.` na metodzie `takeDamage()` jest **nieprawdziwy** — metoda zwraca `AgentStats`, nie `boolean`. Jakiś pozostały artefakt po refaktoryzacji.

```java
/**
 * Logic for taking damage.
 * @return true if still alive, false if died.  ← BŁĄD
 */
public AgentStats takeDamage(int amount) {
```

**Rekomendacja:** Usuń cały Javadoc — `takeDamage(int amount)` jest samodokumentujące.

---

### M6. [ZROBIONE] `GoalAssignedListener` — FQCN import zamiast normalnego

> [!TIP]
> Skrócono pełne ścieżki klasowe (FQCN) do standardowych importów, co poprawiło czytelność klasy `GoalAssignedListener`.

```java
private final com.agentgierka.mmo.agent.service.AgentService agentService;
```

Pełna ścieżka jako typ pola — wygląda na efekt automatycznego rozwiązywania IDE.

**Rekomendacja:** Zamień na normalny import.

---

### M7. [ZROBIONE] `Agent.strength` / `Agent.dexterity` — osierocone pola

> [!TIP]
> Zweryfikowano strukturę modelu `Agent` i potwierdzono usunięcie nieużywanych pól na rzecz dedykowanej klasy `AgentStats`.

**Plik:** [Agent.java](file:///c:/AgentGierka/src/main/java/com/agentgierka/mmo/agent/model/Agent.java#L42-L43)

Pola `strength` i `dexterity` nie są używane nigdzie w kodzie (zero referencji poza DataInitializer). Powinny albo mieć logikę domenową, albo zostać usunięte/przeniesione do `AgentStats`.

**Rekomendacja:** Przenieś do `AgentStats` jako część RPG statystyk, lub usuń dopóki nie będą potrzebne.

---

## 🟢 Niskie (Low)

### L1. Brak `@Transactional(readOnly = true)` na `findAll()`

[AgentService.java:31](file:///c:/AgentGierka/src/main/java/com/agentgierka/mmo/agent/service/AgentService.java#L31) — `findAll()` nie ma adnotacji `@Transactional(readOnly = true)`. Dla operacji "read-only" to optymalizacja, bo Hibernate pomija dirty check.

### L2. `AgentStatus` — brak stanu `DEAD`

[AgentStatus.java](file:///c:/AgentGierka/src/main/java/com/agentgierka/mmo/agent/model/AgentStatus.java) — Agent może umrzeć (`hp <= 0`), ale nie ma stanu `DEAD`. Komentarz w `Agent.takeDamage()`: `// Future: set status to DEAD or similar`. Warto dodać, aby logika śmierci była kompletna.

### L3. `assignGoal()` w kontrolerze zwraca stale dane

[AgentController.java:57-64](file:///c:/AgentGierka/src/main/java/com/agentgierka/mmo/agent/web/AgentController.java#L57-L64):
```java
public AgentDto assignGoal(...) {
    var agent = agentService.findById(id);  // pobiera PRZED przypisaniem celu
    agentService.assignGoal(id, goal);
    return agentMapper.toDto(agent);  // zwraca STARY stan
}
```
Klient dostaje odpowiedź **bez zaktualizowanego celu**. Powinno być `findById` po `assignGoal`.

### L4. `AgentWorldState` — ruch po skosie jest szybszy niż po prostej (diagonal faster)

[AgentWorldState.java:42-63](file:///c:/AgentGierka/src/main/java/com/agentgierka/mmo/agent/model/AgentWorldState.java#L43-L63)

Algorytm ruchu przesuwa agenta po X **i** Y w jednym ticku. Ruch po skosie (10,10)→(20,20) zajmuje 10 ticków, ale ruch po prostej (10,10)→(20,10) **też** zajmuje 10 ticków. Efektywna prędkość diagonalna jest √2 razy szybsza (ok. 41% szybciej).

**Rekomendacja (na przyszłość):** Użyj Bresenham's line algorithm lub normalizuj wektor kierunku.

---

## 📊 Podsumowanie testów

| Kategoria | Pliki | Ocena |
|---|---|---|
| Testy domenowe | `AgentDomainTest`, `AgentWorldStateTest` | ✅ Bardzo dobre |
| Testy serwisów | `AgentServiceTest`, `AgentThinkingServiceTest` | ✅ Dobre |
| Testy engine'u | `GameEngineTest` | ✅ Dobre |
| Testy E2E | `AgentMovementE2ETest` | ⚠️ Fragile (`Thread.sleep(100)`) |
| Testy integracyjne | `PortalIntegrationTest`, `SecurityIntegrationTest` | ✅ Istnieją |
| Testy AI adaptera | — | ❌ Brak |
| Testy error handling | — | ❌ Brak |

> [!TIP]
> `StubBrain` to bardzo dobry pattern do izolacji AI w testach. Rozważ dodanie testu, który weryfikuje zachowanie systemu gdy `Brain.think()` rzuca wyjątek.

---

## 🎯 Priorytety do wdrożenia

| # | Znalezisko | Wysiłek | Wpływ |
|---|---|---|---|
| 1 | C1. Fałszywe atomic update w Redis | 🔨 Średni | 💥 Krytyczny |
| 2 | C2. Cichy failure w WorldStateSynchronizer | 🔧 Mały | 💥 Krytyczny |
| 3 | H2. Brak error handling w GeminiBrainAdapter | 🔧 Mały | 🔥 Wysoki |
| 4 | H3. NPE na getCurrentLocation() | 🔧 Mały | 🔥 Wysoki |
| 5 | H5. Brak walidacji request body | 🔨 Średni | 🔥 Wysoki |
| 6 | H4. N+1 w PortalEventListener | 🔧 Mały | ⚡ Średni |
| 7 | M2. Hardcoded hasło | 🔧 Mały | ⚡ Średni |
| 8 | M3. Duplikacja CORS | 🔧 Mały | ⚡ Średni |
| 9 | L3. Stale dane w assignGoal response | 🔧 Mały | 💡 Niski |
| 10 | M5. Błędny Javadoc | 🔧 Mały | 💡 Niski |

---

## 💡 Ogólne rekomendacje architektoniczne

1. **Rozważ `StructuredTaskScope`** (Java 25) zamiast raw `ExecutorService` w `GameEngine` — lepsze zarządzanie lifecycle'em virtual threads.
2. **Flyway** — wielokrotnie wspomniany w TODO, pora go dodać. `DataInitializer` jako `CommandLineRunner` z `create-drop` strategią to bomba zegarowa.
3. **Pakiet `ai.adapter`** będzie rósł — rozważ wydzielenie konfiguracji promptu do osobnej klasy (`PromptBuilder`). System prompt jest hardcoded w adapterze.
4. **Brak interfejsu/portu dla `AgentWorldStateRepository`** — to jedyna klasa infrastrukturalna, która jest bezpośrednio referencjonowana przez domenę. W architekturze hexagonalnej powinna mieć port (interfejs w domenie).
5. **Missing `@EqualsAndHashCode`** na encjach `Location`, `Portal`, `Player` — mogą powodować problemy z kolekcjami Set/Map i cachowaniem Hibernate.
