# Plan Naprawy Systemu Ekwipunku

> Wygenerowano na podstawie code review | Priorytetyzacja: 🔴 Krytyczne → 🟡 Ważne → 🟢 Optymalizacje

---

## 🔴 FAZA 1 — Krytyczne (fix przed jakimkolwiek deploy)

### Fix 1.1 — Autoryzacja właściciela postaci w kontrolerze

**Problem:** Każdy zalogowany gracz może odczytać i modyfikować ekwipunek dowolnej postaci podając jej UUID w URL-u.

**Pliki do zmiany:**

- `web/InventoryController.java`
- (nowy) `security/PlayerPrincipal.java` lub analogiczny w istniejącym module security

**Kroki:**

1. Sprawdź czy w module `security/` istnieje już mechanizm `PlayerPrincipal` lub `AuthenticatedUser`.
2. Jeśli nie — stwórz klasę `PlayerPrincipal` implementującą `UserDetails`, zawierającą listę `ownedCharacterIds`.
3. We wszystkich metodach kontrolera dodaj parametr `@AuthenticationPrincipal PlayerPrincipal principal`.
4. Przed wywołaniem serwisu dodaj asercję: `principal.assertOwns(characterId)` — rzuca `ResponseStatusException(FORBIDDEN)` jeśli gracz nie jest właścicielem.
5. Dodaj testy integracyjne: zalogowany gracz A próbuje pobrać ekwipunek gracza B → oczekiwane 403.

**Przykładowy kod:**

```java
// InventoryController.java
@GetMapping
public InventoryResponse getInventory(
    @PathVariable UUID characterId,
    @AuthenticationPrincipal PlayerPrincipal principal
) {
    principal.assertOwns(characterId);
    Inventory inventory = inventoryService.getInventory(characterId);
    return mapper.toResponse(inventory);
}
```

---

### Fix 1.2 — Thread-safety w ItemDefinitionDictionary

**Problem:** Metoda `reload()` nie jest atomowa — w środowisku wielowątkowym wątek czytający `cache` może zobaczyć `null` podczas podmiany referencji.

**Pliki do zmiany:**

- `infrastructure/db/ItemDefinitionDictionary.java`

**Kroki:**

1. Zmień pole `cache` na `volatile` LUB użyj `AtomicReference<Map<String, ItemDefinition>>`.
2. Upewnij się, że `load()` tworzy nową mapę i atomowo przypisuje referencję (nie modyfikuje istniejącej).
3. Rozważ `ReentrantReadWriteLock` jeśli `reload()` jest wywoływany często (np. hot-reload itemów).

**Przykładowy kod:**

```java
// Opcja A — volatile (prosta, wystarczająca przy rzadkim reload)
private volatile Map<String, ItemDefinition> cache;

// Opcja B — AtomicReference (ekspresywniejsza)
private final AtomicReference<Map<String, ItemDefinition>> cache = new AtomicReference<>();

@PostConstruct
public void load() {
    Map<String, ItemDefinition> newCache = repo.findAll().stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableMap(ItemDefinition::id, d -> d));
    cache.set(newCache); // atomowe przypisanie
}

public ItemDefinition getById(String id) {
    ItemDefinition def = cache.get().get(id);
    if (def == null) throw new ItemDefinitionNotFoundException(id);
    return def;
}
```

---

### Fix 1.3 — Mutacja argumentu wejściowego w tryAutoStack

**Problem:** `tryAutoStack(ItemStack item)` modyfikuje przekazany `item` (wywołuje `item.setQuantity(...)`). Przy retry w `@Retryable` ten sam obiekt jest przekazywany ponownie ze zredukowaną już ilością.

**Pliki do zmiany:**

- `domain/Inventory.java`
- `application/InventoryApplicationService.java`

**Kroki:**

1. Udokumentuj w `addItem()` że metoda mutuje argument — lub lepiej:
2. Przenieś odpowiedzialność tworzenia `ItemStack` do warstwy aplikacyjnej tak, żeby przy każdym retry był tworzony świeży obiekt.
3. Alternatywnie: zrób `addItem` w domenie idempotentnym przez pracę na wartościach zamiast na referencjach.
4. Dodaj test: `addItem` wywołane dwa razy z tym samym `ItemStack` obiektem nie powoduje błędnej ilości.

---

## 🟡 FAZA 2 — Ważne refaktory (następny sprint)

### Fix 2.1 — Usunięcie redundantnego occupiedSlots

**Problem:** `occupiedSlots` i `anchoredItems` przechowują tę samą informację w różnych formach. `findCollidingAnchors` i tak iteruje po `anchoredItems`, więc `occupiedSlots` jest de facto nieużywany w logice kolizji. Publiczna metoda `placeItem` może zostać wywołana z zewnątrz bez aktualizacji `occupiedSlots`.

**Pliki do zmiany:**

- `domain/Inventory.java`

**Kroki:**

1. Usuń pole `occupiedSlots`.
2. Wszędzie gdzie było używane `occupiedSlots` — zastąp wywołaniem `calculateOccupied(anchor, item)` na żądanie.
3. Usuń wszystkie `occupiedSlots.addAll(...)` i `occupiedSlots.removeAll(...)`.
4. Uruchom testy — logika kolizji (`findCollidingAnchors`) już teraz nie używa `occupiedSlots`, więc zmiana powinna być bezpieczna.
5. Rozważ ucywnienie `placeItem` (zrób go `package-private` lub usuń z publicznego API jeśli używa go tylko mapper).

---

### Fix 2.2 — Ujednolicenie stylu obsługi błędów (wyjątek vs InventoryResult)

**Problem:** `InventoryApplicationService` jednocześnie zwraca `InventoryResult` i rzuca wyjątkami przez `ensureSuccess()`. Caller dostaje typ sugerujący wielowariantową odpowiedź, ale w praktyce zawsze dostaje wyjątek przy nie-Success.

**Pliki do zmiany:**

- `application/InventoryApplicationService.java`

**Decyzja do podjęcia (wybierz jeden styl):**

**Opcja A — zostań przy wyjątkach, zmień sygnatury:**

```java
// Sygnatury zwracają void lub konkretny typ, nie InventoryResult
public void moveItem(UUID characterId, int fromIndex, int toIndex) { ... }
```

**Opcja B — zostań przy InventoryResult, usuń ensureSuccess:**

```java
// Caller (kontroler) obsługuje wszystkie przypadki przez pattern matching
InventoryResult result = inventoryService.moveItem(...);
return switch (result) {
    case Success s -> mapper.toResponse(...);
    case NoSpace _ -> throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "No space");
    // ...
};
```

**Rekomendacja:** Opcja B jest lepsza dla MMO — daje kontrolerowi możliwość zwrócenia różnych HTTP status codes bez łapania wyjątków.

---

### Fix 2.3 — Naprawa niespójności toIndex vs otherAnchor w handleSwap

**Problem:** Wynik `Success(fromIndex, toIndex)` po swapie zawiera `toIndex` (slot wskazany przez gracza), podczas gdy faktyczna zakotwiczona pozycja zamienionego przedmiotu to `otherAnchor` — inne pole.

**Pliki do zmiany:**

- `domain/Inventory.java`

**Kroki:**

1. W `handleSwap` zmień zwracany wynik na `Success(fromIndex, otherAnchor)` zamiast `Success(fromIndex, toIndex)`.
2. Zaktualizuj testy sprawdzające wynik swapa.
3. Zaktualizuj dokumentację/komentarz metody.

---

## 🟢 FAZA 3 — Optymalizacje (backlog)

### Fix 3.1 — Eliminacja podwójnego odczytu z bazy w kontrolerze

**Problem:** `moveItem` i `removeItem` w kontrolerze najpierw wykonują operację (odczyt + zapis), a potem wywołują `getInventory` (kolejny odczyt) żeby zbudować response.

**Pliki do zmiany:**

- `web/InventoryController.java`
- `application/InventoryApplicationService.java`
- `application/InventoryTransactionService.java`

**Kroki:**

1. Zmień `InventoryTransactionService.moveItem()` żeby zwracał parę `(InventoryResult, Inventory)` — lub sam zaktualizowany `Inventory`.
2. Zmień `InventoryApplicationService` żeby przekazywał `Inventory` do góry.
3. W kontrolerze mapuj response bezpośrednio z już załadowanego `Inventory`, bez dodatkowego `getInventory()`.

---

### Fix 3.2 — Optymalizacja O(n²) w InventoryMapper.toEntity

**Problem:** Dla każdego itemu domenowego wykonywane jest liniowe przeszukiwanie listy encji. Złożoność: O(n²).

**Pliki do zmiany:**

- `infrastructure/db/InventoryMapper.java`

**Kroki:**

1. Na początku `toEntity()` zbuduj `Map<UUID, ItemStackEntity>` z istniejących encji.
2. Zastąp `stream().filter().findFirst()` przez `O(1)` lookup z mapy.

**Przykładowy kod:**

```java
public static InventoryEntity toEntity(Inventory domain, InventoryEntity existingEntity) {
    // O(n) — budowanie mapy raz
    Map<UUID, ItemStackEntity> entityById = existingEntity.getItems().stream()
        .collect(Collectors.toMap(ItemStackEntity::getId, e -> e));

    existingEntity.getItems().removeIf(e -> !domainIds.contains(e.getId()));

    for (Map.Entry<Integer, ItemStack> entry : domain.getAnchoredItems().entrySet()) {
        ItemStack domainItem = entry.getValue();
        int gridIndex = entry.getKey();

        ItemStackEntity existing = entityById.get(domainItem.getId()); // O(1)
        if (existing != null) {
            existing.setGridIndex(gridIndex);
            existing.setQuantity(domainItem.getQuantity());
        } else {
            existingEntity.getItems().add(ItemStackEntity.builder()
                .id(domainItem.getId())
                .inventoryId(existingEntity.getId())
                .itemDefinitionId(domainItem.getDefinition().id())
                .gridIndex(gridIndex)
                .quantity(domainItem.getQuantity())
                .build());
        }
    }
    return existingEntity;
}
```

---

### Fix 3.3 — Lepsza obsługa błędów w ItemDefinitionDictionary

**Problem:** `throw new RuntimeException("Item definition not found: " + id)` — zbyt ogólny wyjątek, trudny do obsługi w górnych warstwach.

**Pliki do zmiany:**

- `infrastructure/db/ItemDefinitionDictionary.java`
- (nowy) `exception/ItemDefinitionNotFoundException.java`

**Kroki:**

1. Stwórz dedykowany `ItemDefinitionNotFoundException extends RuntimeException`.
2. Zastąp `RuntimeException` tym nowym wyjątkiem.
3. Dodaj obsługę w global exception handler (jeśli istnieje `@ControllerAdvice`).

---

## 📋 Kolejność realizacji

```
Faza 1 (krytyczne)     →  Faza 2 (refaktory)    →  Faza 3 (optymalizacje)
Fix 1.1 Autoryzacja        Fix 2.1 occupiedSlots     Fix 3.1 podwójny odczyt
Fix 1.2 Thread-safety      Fix 2.2 styl błędów       Fix 3.2 O(n²) mapper
Fix 1.3 Mutacja arg.       Fix 2.3 swap toIndex       Fix 3.3 wyjątki słownika
```

---

## ✅ Definicja ukończenia każdego fixa

- \[ \] Kod zmieniony zgodnie z opisem
- \[ \] Testy jednostkowe dla zmienionej logiki
- \[ \] Test negatywny (co się dzieje przy złych danych)
- \[ \] Code review od drugiej osoby
- \[ \] Brak regresji w istniejących testach (`mvn test`)
