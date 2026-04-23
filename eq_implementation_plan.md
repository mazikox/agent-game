# Projekt Systemu Ekwipunku i Zapasów (Inventory System)

Ten dokument stanowi kompleksowy plan implementacji systemu zarządzania ekwipunkiem gracza, opartego o zasady Domain-Driven Design (DDD) i Architekturę Heksagonalną. Docelowy system ma naśladować rozwiązania chociażby z gry Metin 2 (siatka przedmiotów o różnych rozmiarach w ekwipunku, system ulepszeń, unikalne id przedmiotów). Zgodnie z założeniami, system **nie będzie posiadał mechaniki "Auto-Sort"**, skupiając się w zamian na poprawności operacyjnej i wydajności bazodanowej.

## User Review Required

> [!IMPORTANT]
> Proszę o zapoznanie się z zaktualizowanym o optymalizacje planem przed rozpoczęciem implementacji (Krok 1 to obiekty Domeny).

---

## 1. Architektura i Założenia (Aktualizacja)

Stosujemy **Rich Domain Model** i czystą abstrakcję przez **Architekturę Heksagonalną**.

**Mechanizmy obronne i optymalizacyjne:**

1. **Optimistic Locking (@Version) + Retry:** Równoległe manipulacje ekwipunkiem zakończą się konfliktem wersji w bazie. By gracz nie odczuł "błędu bazy" podczas nieszczęśliwego timing'u, warstwa aplikacyjna automatycznie ponowi próbę. **Uwaga krytyczna:** `@Retryable` i `@Transactional` NIE mogą być na tej samej metodzie — `OptimisticLockingFailureException` jest rzucany przez Hibernate przy commicie, czyli *po* wyjściu z metody, poza zasięgiem proxy `@Retryable`. Wymagana jest separacja na dwa beany (patrz sekcja 3).

2. **JOIN FETCH:** Potężna ochrona przed efektem N+1 w Hibernate dla Ekwipunku.

3. **Pamięć wymiarów:** Mapper DB tworzący obiekty domeny od razu podpina dla nich referencje do wymiarów. Metody domenowe `Inventory` nie są zaśmiecane nadmiernym przepychaniem parametrów słownikowych przez kontrolery z góry.

4. **UUID TIME:** Baza będzie korzystała z sekwencyjnego UUID (v7). Chroni to indeksy drzew B-Tree w bazach typu Postgres przed morderczą fragmentacją przy rozroście bazy MMO.

5. **Kalkulacja Siatki:** Z racji na zapis w postaci `int gridIndex` — stanowi on punkt zakotwiczenia i jest zawsze *lewym górnym rogiem* (top-left) pudełka. Klasa `Inventory` musi wyliczyć pozostałe miejsca blokujące siatkę na podstawie `width` oraz `height`. **Krytyczna walidacja wrap-around:** dla każdego rzędu zajmowanego przez przedmiot musi być spełniony warunek `(topLeftCol + itemWidth) <= inventoryWidth`. Przedmiot wymagający miejsca poza prawą krawędź siatki jest pozycją nielegalną i operacja musi zostać odrzucona z wynikiem `OUT_OF_BOUNDS`.

6. **Dual-Structure dla Occupied Slots:** `Inventory` trzyma dwie struktury danych dla wydajności kolizji:
   - `Map<Integer, ItemStack> slots` — klucze to wyłącznie top-left anchory posiadanych przedmiotów
   - `Set<Integer> occupiedSlots` — wszystkie zajęte indeksy (łącznie z polami zajmowanymi przez duże przedmioty)

   Obie struktury są aktualizowane atomowo przy każdej mutacji. Dzięki temu sprawdzenie kolizji to O(1) lookup na `occupiedSlots`, a nie O(n × w × h) iteracja po anchored items.

7. **Concurrency & Locking Strategy (Kwestia Spójności):** Agregat `Inventory` posiada swoją encję nadrzędną `InventoryEntity` z polem `@Version`. Każda zmiana (ruch, dodanie, usnięcie przedmiotu) wymusza podbicie wersji rodzica. Zapobiega to sytuacjom, w których dwa równoległe procesy "widzą" wolne miejsce i próbują tam umieścić różne przedmioty.

---

## 2. Model Domenowy (Core Domain)

Proponowana struktura pakietów dla czystej Javy z pełną separacją od frameworka: `com.agentgierka.mmo.inventory.domain`

### `ItemDefinition` (Entity referencyjna, read-only)

> **Zmiana nazwy z `ItemTemplate`:** Value Object w DDD jest identyfikowany wyłącznie przez swoje wartości — bez pola `id`. Ponieważ potrzebujemy identyfikatora do słownikowania, jest to Entity referencyjna (read-only). Nazwa `ItemDefinition` oddaje tę semantykę.

```java
// NIE jest to Value Object — ma id, więc to Entity referencyjna (read-only dictionary entry)
public record ItemDefinition(
    String id,       // klucz słownikowy (np. "SWORD_LONG", "ARMOR_HEAVY")
    String name,
    int width,
    int height,
    int maxStack
) {}
```

### `ItemStack` (Entity)

Klasa nafaszerowana wiedzą. Sama trzyma referencję (definicję)! Odciąża to API biznesowe od pytania o obiekty pomocnicze przy każdym ruchu.

```java
public class ItemStack {
    private final UUID id;
    private int quantity;
    // bonuses, enchantments itp.

    // Obiekt domeny przechowuje wewnątrz definicję przekazaną przy budowaniu przez DB Mapper.
    private final ItemDefinition definition;

    public int getWidth()  { return definition.width(); }
    public int getHeight() { return definition.height(); }

    public boolean canStackWith(ItemStack other) {
        return this.definition.id().equals(other.definition.id()) 
            && this.quantity < definition.maxStack();
    }

    public void addQuantity(int amount) {
        this.quantity = Math.min(this.quantity + amount, definition.maxStack());
    }
}
```

### `InventoryResult` (Sealed Hierarchy)

Zwracany wynik operacji domenowych — zamiast rzucania wyjątków lub prostego enum bez danych. WebSocket handler i kontroler REST muszą wiedzieć *które pola* kolidują, aby podświetlić je w UI gracza.

```java
public sealed interface InventoryResult permits
    InventoryResult.Success,
    InventoryResult.Collision,
    InventoryResult.OutOfBounds {

    record Success(int fromIndex, int toIndex) implements InventoryResult {}

    // Zawiera indeksy kolidujących pól — UI może je podświetlić
    record Collision(int attemptedIndex, Set<Integer> collidingSlots) implements InventoryResult {}

    // Przedmiot wychodziłby poza prawą krawędź lub dół siatki
    record OutOfBounds(int attemptedIndex, int itemWidth, int itemHeight) implements InventoryResult {}
}
```

### `Inventory` (Aggregate Root)

Zmieniona koncepcja z dualną strukturą danych dla O(1) kolizji i ścisłą walidacją wrap-around.

```java
public class Inventory {
    private final int width;   // szerokość EQ np. 5
    private final int height;  // wysokość EQ np. 9

    // Klucze to WYŁĄCZNIE top-left anchory posiadanych przedmiotów.
    private final Map<Integer, ItemStack> slots;

    // Wszystkie zajęte indeksy (anchor + pola zajęte przez duże przedmioty).
    // Aktualizowane atomowo razem z `slots`. Umożliwia kolizję O(1).
    private final Set<Integer> occupiedSlots;

    /**
     * Przesuwa przedmiot. Nigdy nie rzuca wyjątków Javy — zwraca sealed InventoryResult.
     * Aggregate Root jest zawsze w spójnym stanie: przy wyniku != Success żaden stan nie ulega zmianie.
     */
    public InventoryResult moveItem(int fromIndex, int toIndex) {
        ItemStack item = slots.get(fromIndex);
        if (item == null) return new InventoryResult.OutOfBounds(fromIndex, 0, 0);

        // 1. Walidacja wrap-around dla nowej pozycji
        InventoryResult boundsCheck = validateBounds(toIndex, item);
        if (!(boundsCheck instanceof InventoryResult.Success)) return boundsCheck;

        // 2. Oblicz pola zajęte przez item w nowej pozycji
        Set<Integer> newOccupied = calculateOccupied(toIndex, item);

        // 3. Sprawdź kolizje (wyłącz stare pola tego samego itemu)
        Set<Integer> currentOccupied = calculateOccupied(fromIndex, item);
        Set<Integer> collision = newOccupied.stream()
            .filter(idx -> occupiedSlots.contains(idx) && !currentOccupied.contains(idx))
            .collect(Collectors.toSet());

        if (!collision.isEmpty()) {
            return new InventoryResult.Collision(toIndex, collision);
        }

        // 4. Atomowa mutacja obu struktur
        occupiedSlots.removeAll(currentOccupied);
        slots.remove(fromIndex);
        slots.put(toIndex, item);
        occupiedSlots.addAll(newOccupied);

        return new InventoryResult.Success(fromIndex, toIndex);
    }

    /**
     * Techniczne Edge Cases: obsługa braku ruchu oraz walidacja ujemnych indeksów.
     */
    public InventoryResult processMove(int fromIndex, int toIndex) {
        if (fromIndex == toIndex) return new InventoryResult.Success(fromIndex, toIndex);
        if (fromIndex < 0 || toIndex < 0) return new InventoryResult.OutOfBounds(toIndex, 0, 0);
        return moveItem(fromIndex, toIndex);
    }

    /**
     * Walidacja wrap-around: każdy rząd zajmowany przez przedmiot nie może
     * wychodzić poza prawą krawędź siatki.
     */
    private InventoryResult validateBounds(int topLeft, ItemStack item) {
        int col = topLeft % width;
        int row = topLeft / width;

        if (col + item.getWidth() > width) {
            return new InventoryResult.OutOfBounds(topLeft, item.getWidth(), item.getHeight());
        }
        if (row + item.getHeight() > height) {
            return new InventoryResult.OutOfBounds(topLeft, item.getWidth(), item.getHeight());
        }
        return new InventoryResult.Success(topLeft, topLeft);
    }

    /**
     * Wylicza wszystkie gridIndex zajmowane przez item zaczynający od topLeft.
     */
    private Set<Integer> calculateOccupied(int topLeft, ItemStack item) {
        Set<Integer> occupied = new HashSet<>();
        int col = topLeft % width;
        int row = topLeft / width;
        for (int r = 0; r < item.getHeight(); r++) {
            for (int c = 0; c < item.getWidth(); c++) {
                occupied.add((row + r) * width + (col + c));
            }
        }
        return occupied;
    }
}
```

---

## 3. Zastosowanie DDD — Application Service

Pakiet: `com.agentgierka.mmo.inventory.application`

### Warstwa Aplikacyjna i Konfiguracja Resilience (Spring Boot 4 Native)

#### [NEW] [RetryConfig.java](file:///c:/AgentGierka/src/main/java/com/agentgierka/mmo/config/RetryConfig.java)
- Włączenie natywnego wsparcia dla odporności za pomocą `@EnableResilientMethods`.
- Rezygnacja z zewnętrznych bibliotek `spring-retry`.

#### [MODIFY] [InventoryApplicationService.java](file:///c:/AgentGierka/src/main/java/com/agentgierka/mmo/inventory/application/InventoryApplicationService.java)
- Zmiana importów na natywne `org.springframework.resilience.annotation.Retryable`.
- Konfiguracja ponowień bez konieczności dodawania starterów AOP (automatycznie obsługiwane przez framework).

```java
// Bean ZEWNĘTRZNY — obsługuje retry, nie ma @Transactional
@Service
@RequiredArgsConstructor
public class InventoryApplicationService {

    private final InventoryTransactionService transactionService;

    @Retryable(
        retryFor = OptimisticLockingFailureException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 100)
    )
    public InventoryResult switchItemPosition(UUID characterId, int indexFrom, int indexTo) {
        // Deleguje do wewnętrznego beana, który zarządza transakcją.
        // Gdy transakcja się commituje i rzuca OptimisticLockingFailureException,
        // wyjątek propaguje się tutaj i @Retryable może go złapać.
        return transactionService.executeInTransaction(characterId, indexFrom, indexTo);
    }
}

// Bean WEWNĘTRZNY — zarządza transakcją, brak @Retryable
@Service
@Transactional
@RequiredArgsConstructor
public class InventoryTransactionService {

    private final InventoryRepository inventoryRepo; // Port architektury heksagonalnej

    public InventoryResult executeInTransaction(UUID characterId, int indexFrom, int indexTo) {
        // Pobranie całego ekwipunku bez ryzyka N+1 dzięki JOIN FETCH
        Inventory inv = inventoryRepo.findByCharacterIdWithItems(characterId)
            .orElseThrow(() -> new InventoryNotFoundException(characterId));

        InventoryResult result = inv.moveItem(indexFrom, indexTo); // DDD na czysto

        if (result instanceof InventoryResult.Success) {
            inventoryRepo.save(inv);
        }

        return result; // Ścisła struktura odpowiedzi do Controller / WebSocket handlera
    }
}
```

---

## 4. Adaptacja do bazy danych i wzorzec Repository (Adapters / DB)

Aby powiązać wiedzę między tabelą pozbawioną "Mózgu" a mądrą logiką Domeny, dodajemy mapper: `com.agentgierka.mmo.inventory.infrastructure.db`

### `ItemDefinitionDictionary` — cykl życia i thread-safety

> **Ważne:** Słownik definicji itemów to dane read-only ładowane raz przy starcie aplikacji. Musi być jawnie zdefiniowany jako singleton z thread-safe strukturą danych.

```java
@Component
@RequiredArgsConstructor
public class ItemDefinitionDictionary {

    private final ItemDefinitionJpaRepository repo;

    // Niemodyfikowalna mapa ładowana raz przy starcie — thread-safe bez synchronizacji
    private Map<String, ItemDefinition> cache;

    @PostConstruct
    public void load() {
        this.cache = repo.findAll().stream()
            .map(ItemDefinitionMapper::toDomain)
            .collect(Collectors.toUnmodifiableMap(ItemDefinition::id, d -> d));
    }

    public ItemDefinition getById(String id) {
        ItemDefinition def = cache.get(id);
        if (def == null) throw new ItemDefinitionNotFoundException(id);
        return def;
    }

    /**
     * Wywoływane przy hot-reload konfiguracji (deploy bez restartu, GM tools).
     * Nowe instancje są konstruowane i atomowo podmieniane jako całość.
     */
    public void reload() {
        load();
    }
}
```

### `InventoryPersistenceAdapter` + Mappery

Implementacja Portu. Nakarmia agregat `Inventory` przy budowaniu referencjami z globalnego słownika definicji itemów!

```java
@Component
@RequiredArgsConstructor
public class InventoryPersistenceAdapter implements InventoryRepository {

    private final InventoryJpaRepository jpaRepo;
    private final ItemDefinitionDictionary dictionary;

    @Override
    public Optional<Inventory> findByCharacterIdWithItems(UUID id) {
        return jpaRepo.findByCharacterIdWithItems(id)
            .map(entity -> InventoryMapper.toDomain(entity, dictionary));
        // TUTAJ POJAWIA SIĘ MAGIA — mapujemy String templateId na fizyczny obiekt
        // ItemDefinition z cache, by Domena nie musiała nic szukać samodzielnie.
    }
}
```

### Encje Bazodanowe (JPA)

Zaktualizowane adnotacje `@UuidGenerator` zapobiegające fragmentacji stron Postgres (UUIDv7 generowane po stronie serwera).

```java
@Entity
@Table(name = "inventory_items")
public class ItemStackEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @Column(name = "item_definition_id", nullable = false)
    private String itemDefinitionId; // referencja do słownika (String, nie FK do tabeli)

    @Version
    private Long version; // Optimistic Locking — wymagane dla mechanizmu retry

    // gridIndex = top-left anchor pozycji w ekwipunku
    @Column(name = "grid_index", nullable = false)
    private int gridIndex;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    // bonusy, enchanty itp. np. jako @Type JSONB
}
```

---

## Open Questions

1. **Wymiary siatki:** Jakie docelowo powinny być wymiary siatki głównego ekwipunku gracza (X na Y rzędów, np. 5×9)?
2. **Kształty:** Czy uwzględniamy tylko regularne wymiary prostokątne: `1×1`, `1×2`, `1×3`, `2×2`, `2×3`?

---

## Verification Plan

1. **Test Przeliczania Pól Siatki (`InventoryTest.java` — TDD):**

   Najważniejszy element systemu. Przypadki testowe:
   - Przedmiot 2×2 zaczynający od indeksu `4` przy szerokości `5` — oczekiwany wynik: `OUT_OF_BOUNDS` (wrap-around przez prawą krawędź).
   - Przedmiot 2×2 zaczynający od indeksu `3` przy szerokości `5` — oczekiwany wynik: `Success`, zajęte pola `{3, 4, 8, 9}`.
   - Kolizja: dwa przedmioty nakładające się na pole `7` — oczekiwany wynik: `Collision` z `collidingSlots = {7}`.
   - Przedmiot wychodzący poza dolną krawędź siatki — oczekiwany wynik: `OUT_OF_BOUNDS`.
   - Przesunięcie przedmiotu na jego własną obecną pozycję (fromIndex == toIndex) — oczekiwany wynik: `Success` bez zmian stanu.
   - **Stackowanie przedmiotów:** Przesunięcie stackowalnego przedmiotu na inny o tym samym ID — oczekiwany wynik: `Success` z aktualizacją `quantity`.

2. **Zatwierdzenie Architektury Kontrolera i Transakcji (testy integracyjne):**

   - Symulacja podwójnego równoczesnego żądania przesunięcia tego samego itemu — warstwa aplikacyjna musi zapewnić, że gracz dostanie przezroczyste ponowienie po `OptimisticLockingFailureException` (weryfikacja separacji beanów `@Retryable` + `@Transactional`).
   - Weryfikacja odpowiedzi WebSocket: `ITEM_COLLISION` musi zwracać `collidingSlots` do podświetlenia w UI.
   - Weryfikacja, że `occupiedSlots` i `slots` są zawsze spójne po operacjach (niezmiennik agregatu).
