# Plan Naprawy Systemu Combat

> Wygenerowano na podstawie code review | Priorytetyzacja: 🔴 Krytyczne → 🟡 Ważne → 🟢 Optymalizacje

---

## 🔴 FAZA 1 — Krytyczne (fix przed jakimkolwiek deploy)

### Fix 1.1 — Brak autoryzacji w CombatController

**Problem:** `agentId` pochodzi z `@RequestParam` — każdy zalogowany gracz może inicjować walki i wykonywać akcje w imieniu dowolnego agenta. Identyczna luka jak ta naprawiona w `InventoryController`.

**Pliki do zmiany:**

- `web/CombatController.java`

**Kroki:**

1. Dodaj `@PreAuthorize("@agentSecurity.isOwner(#agentId, principal.username)")` na poziomie klasy lub każdej metody.
2. Dodaj `@AuthenticationPrincipal PlayerPrincipal principal` do obu metod kontrolera.
3. Dodaj testy integracyjne: gracz A próbuje wykonać akcję agentem gracza B → oczekiwane 403.

**Przykładowy kod:**

```java
@RestController
@RequestMapping("/api/combat")
@RequiredArgsConstructor
public class CombatController {

    @PostMapping("/initiate")
    @PreAuthorize("@agentSecurity.isOwner(#agentId, principal.username)")
    public ResponseEntity<CombatResponse> initiateCombat(
            @RequestParam UUID agentId,
            @RequestParam UUID creatureId,
            @AuthenticationPrincipal PlayerPrincipal principal) {
        CombatInstance combat = combatService.initiateCombat(agentId, creatureId);
        return ResponseEntity.ok(CombatResponse.from(combat));
    }

    @PostMapping("/action")
    @PreAuthorize("@agentSecurity.isOwner(#agentId, principal.username)")
    public ResponseEntity<Void> executeAction(
            @RequestParam UUID agentId,
            @RequestParam CombatActionType actionType,
            @AuthenticationPrincipal PlayerPrincipal principal) {
        combatService.executeAction(agentId, actionType);
        return ResponseEntity.ok().build();
    }
}
```

---

### Fix 1.2 — Nieskończona pętla w syncTime

**Problem:** Jeśli `agentSpeed == 0` i `creatureSpeed == 0`, pętla `while` w `syncTime` nigdy się nie skończy. Serwer zawiśnie, transakcja nie zostanie zakończona, nastąpi kaskadowe wyczerpanie connection poola.

**Pliki do zmiany:**

- `service/CombatService.java`
- `model/CombatInstance.java` (nowa metoda `abandon()`)
- `model/CombatStatus.java` (weryfikacja że `ABANDONED` jest obsługiwany)

**Kroki:**

1. Dodaj stałą `MAX_TICKS = 10_000` w `CombatService`.
2. W pętli `syncTime` inkrementuj licznik i rzuć `CombatException` po przekroczeniu limitu.
3. Przed rzuceniem wyjątku wywołaj `combat.abandon()` żeby zapisać stan `ABANDONED`.
4. Dodaj metodę `abandon()` do `CombatInstance`.
5. Dodaj walidację w `applyTick` — jeśli oba speed == 0, rzuć `IllegalArgumentException`.

**Przykładowy kod:**

```java
// CombatService.java
private static final int MAX_TICKS = 10_000;

private void syncTime(CombatInstance combat, Agent agent, CreatureInstance creature) {
    if (combat.getStatus() != CombatStatus.ONGOING) return;
    int ticks = 0;
    while (!combat.canAgentAct() && !combat.canCreatureAct()) {
        if (++ticks > MAX_TICKS) {
            log.error("syncTime exceeded max ticks — agentSpeed={}, creatureSpeed={}",
                agent.getStats().getAttackSpeed(), creature.getAttackSpeed());
            combat.abandon();
            throw new CombatException("Combat stalled — possible zero-speed configuration");
        }
        combat.applyTick(agent.getStats().getAttackSpeed(), creature.getAttackSpeed());
    }
}

// CombatInstance.java
public void abandon() {
    this.status = CombatStatus.ABANDONED;
    this.endedAt = LocalDateTime.now();
}
```

---

### Fix 1.3 — Race condition: nieużywane findByCreatureInstanceIdAndStatus

**Problem:** `CombatRepository` posiada metodę `findByCreatureInstanceIdAndStatus` z komentarzem "Prevents multiple agents from attacking the same creature simultaneously" — ale nigdy nie jest wywoływana w `initiateCombat`. Dwie walki mogą być inicjowane dla tej samej kreatury jednocześnie.

**Pliki do zmiany:**

- `service/CombatService.java`

**Kroki:**

1. Na początku `initiateCombat`, po załadowaniu kreatury, dodaj sprawdzenie:

```java
combatRepository.findByCreatureInstanceIdAndStatus(creatureId, CombatStatus.ONGOING)
    .ifPresent(existing -> {
        throw new CombatException("Creature is already engaged in combat");
    });
```

2. Sprawdzenie musi być **przed** `creature.enterCombat()` i `updateAtomic`.
3. Dodaj test: dwa równoczesne żądania dla tej samej kreatury — tylko jedno powinno się powieść.

---

### Fix 1.4 — CombatInstance serializowana wprost do response (wyciek encji JPA)

**Problem:** `initiateCombat` zwraca `ResponseEntity<CombatInstance>` — encję JPA serializowaną bezpośrednio przez Jackson. Eksponuje wewnętrzne pola bazy danych, może powodować `LazyInitializationException` przy rozbudowie relacji, utrudnia ewolucję API.

**Pliki do zmiany:**

- `web/CombatController.java`
- (nowy) `web/dto/CombatResponse.java`

**Kroki:**

1. Stwórz rekord `CombatResponse`:

```java
public record CombatResponse(
    UUID combatId,
    UUID agentId,
    UUID creatureInstanceId,
    CombatStatus status,
    int agentAp,
    int creatureAp,
    LocalDateTime startedAt
) {
    public static CombatResponse from(CombatInstance combat) {
        return new CombatResponse(
            combat.getId(),
            combat.getAgentId(),
            combat.getCreatureInstanceId(),
            combat.getStatus(),
            combat.getAgentAp(),
            combat.getCreatureAp(),
            combat.getStartedAt()
        );
    }
}
```

2. Zmień sygnaturę kontrolera na `ResponseEntity<CombatResponse>`.
3. Opakuj wynik: `return ResponseEntity.ok(CombatResponse.from(combat))`.

---

### Fix 1.5 — Brak @Version na CombatInstance

**Problem:** `CombatInstance` nie ma pola `@Version`. Przy podwójnym kliknięciu lub lagującym kliencie dwa równoczesne żądania mogą wykonać akcje na tym samym stanie bez żadnego błędu — wynik drugiego żądania nadpisze wynik pierwszego.

**Pliki do zmiany:**

- `model/CombatInstance.java`

**Kroki:**

1. Dodaj pole `@Version` do encji:

```java
@Version
private Long version;
```

2. Upewnij się że `@Retryable(OptimisticLockingFailureException.class)` jest na `executeAction`w serwisie (analogicznie do wzorca z inventory).

---

## 🟡 FAZA 2 — Ważne poprawki (następny sprint)

### Fix 2.1 — Hardkodowana wartość healAmount = 20

**Problem:** `case POTION -> { int healAmount = 20; }` — magiczna liczba bez kontekstu. Przy rozbudowie systemu itemów każda mikstura powinna leczyć inną ilość.

**Pliki do zmiany:**

- `service/CombatService.java`
- `model/CombatActionType.java` lub nowy `PotionItem` / stats agenta

**Opcje rozwiązania:**

- **Krótkoterminowo:** wynieś do stałej `private static final int DEFAULT_POTION_HEAL = 20`i dodaj komentarz TODO.
- **Docelowo:** `executeAction` powinno przyjmować opcjonalny `itemId` dla POTION — serwis pobiera definicję itemu ze słownika i używa jego `healValue`.

```java
// Krótkoterminowa poprawka
private static final int DEFAULT_POTION_HEAL = 20; // TODO: replace with item definition lookup

case POTION -> {
    int healAmount = DEFAULT_POTION_HEAL;
    agent.heal(healAmount);
    ...
}
```

---

### Fix 2.2 — SKILL rzuca UnsupportedOperationException zamiast CombatException

**Problem:** Klient dostaje niespodziewane HTTP 500 zamiast czytelnego komunikatu o tym, że akcja nie jest jeszcze zaimplementowana.

**Pliki do zmiany:**

- `service/CombatService.java`

**Kroki:**

1. Zastąp `default -> throw new UnsupportedOperationException(...)` przez:

```java
case SKILL -> throw new CombatException("SKILL action is not yet implemented");
default -> throw new CombatException("Unknown action type: " + actionType);
```

2. Upewnij się że `CombatException` jest obsługiwany przez `@ControllerAdvice` z odpowiednim HTTP status (np. `422 UNPROCESSABLE_ENTITY` lub `400 BAD_REQUEST`).

---

### Fix 2.3 — Kolejność operacji w initiateCombat — ryzyko przy rollbacku z hookiem

**Problem:** `worldStateSynchronizer.syncMovementAfterCommit(agent)` jest rejestrowany przed `creatureRepository.updateAtomic(creature)`. Jeśli `updateAtomic` zwróci `false` i transakcja zostanie wycofana, hook może wykonać się w nieoczekiwanym stanie.

**Pliki do zmiany:**

- `service/CombatService.java`

**Kroki:**

1. Przesuń `worldStateSynchronizer.syncMovementAfterCommit(agent)` na koniec metody, po wszystkich operacjach które mogą rzucić wyjątek:

```java
// Na końcu initiateCombat, po combatRepository.save(combat):
worldStateSynchronizer.syncMovementAfterCommit(agent);
```

2. Dodaj komentarz wyjaśniający że hook musi być rejestrowany po potwierdzeniu sukcesu wszystkich operacji domenowych.
3. Napisz test integracyjny sprawdzający że hook NIE jest wywoływany gdy `updateAtomic` zwraca false.

---

## 🟢 FAZA 3 — Refaktory architektoniczne (backlog)

### Fix 3.1 — Podział CombatService (God Service)

**Problem:** `CombatService` w \~200 liniach odpowiada za: inicjację walki, walidację, rozwiązywanie akcji, logikę tury, AP ticking, obsługę zwycięstwa i porażki, persystencję, eventy i synchronizację ruchu. Przy rozbudowie o PvP, statusy i umiejętności przekroczy 1000 linii.

**Docelowy podział:**

```
CombatService              — orkiestracja (initiateCombat, executeAction)
CombatActionResolver       — logika akcji (ATTACK, FLEE, POTION, SKILL)
CombatTurnEngine           — AP ticking, syncTime, kolejność tur
CombatOutcomeHandler       — handleVictory, handleDefeat
```

**Kroki:**

1. Wyodrębnij `CombatTurnEngine` z metodami `syncTime`, `resolveCreatureTurns`.
2. Wyodrębnij `CombatActionResolver` z metodą `resolveAgentAction` i jej case'ami.
3. Wyodrębnij `CombatOutcomeHandler` z metodami `handleVictory`, `handleDefeat`.
4. `CombatService` zostaje jako fasada wstrzykująca powyższe i koordynująca `@Transactional`.

---

## 📋 Kolejność realizacji

```
Faza 1 (krytyczne)              Faza 2 (ważne)                Faza 3 (refaktory)
Fix 1.1 Autoryzacja             Fix 2.1 healAmount            Fix 3.1 podział serwisu
Fix 1.2 Nieskończona pętla      Fix 2.2 SKILL → CombatEx.
Fix 1.3 Race condition          Fix 2.3 kolejność hooków
Fix 1.4 Encja wprost w response
Fix 1.5 Brak @Version
```

---

## ✅ Definicja ukończenia każdego fixa

- \[ \] Kod zmieniony zgodnie z opisem
- \[ \] Testy jednostkowe dla zmienionej logiki
- \[ \] Test negatywny (co się dzieje przy złych danych / edge case)
- \[ \] Code review od drugiej osoby
- \[ \] Brak regresji w istniejących testach (`mvn test`)
