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

### `ItemDefinition` (Entity referencyjna, read-only) [ZROBIONE]

### `ItemStack` (Entity) [ZROBIONE]

### `InventoryResult` (Sealed Hierarchy) [ZROBIONE]
- Dodano `EmptySlot` oraz `NoSpace`.

### `Inventory` (Aggregate Root) [ZROBIONE]
- Pełna obsługa `processMove` (Swap, Stack, Collision).
- Dodano `addItem` (Looting/Auto-placement) z algorytmem szukania wolnego miejsca i auto-stackowaniem.

---

## 3. Zastosowanie DDD — Application Service

Pakiet: `com.agentgierka.mmo.inventory.application`

### Warstwa Aplikacyjna i Konfiguracja Resilience (Spring Boot 4 Native) [ZROBIONE]

#### [RetryConfig.java](file:///e:/agentgierka/src/main/java/com/agentgierka/mmo/config/RetryConfig.java)
- Włączenie natywnego wsparcia dla odporności za pomocą `@EnableResilientMethods`.

#### [InventoryApplicationService.java](file:///e:/agentgierka/src/main/java/com/agentgierka/mmo/inventory/application/InventoryApplicationService.java)
- Natywne `org.springframework.resilience.annotation.Retryable`.
- `delay` jako `long` (milisekundy), `includes` zamiast `retryFor`, `maxRetries` zamiast `maxAttempts`.

#### [InventoryTransactionService.java](file:///e:/agentgierka/src/main/java/com/agentgierka/mmo/inventory/application/InventoryTransactionService.java)
- Bean wewnętrzny z `@Transactional`. Korzysta z `InventoryRepository` (klasa w `infrastructure.db`).
- Musi zostać zaktualizowany o obsługę `InventoryResult`.

---

## 4. Adaptacja do bazy danych (Infrastructure) [ZROBIONE]

### `InventoryRepository` [ZROBIONE]
- Główna klasa odpowiedzialna za trwałość (Persistence).
- Deleguje mapowanie do `InventoryMapper`.

### `InventoryMapper` [ZROBIONE]
- Mapowanie stanu agregatu `Inventory` na encje JPA `InventoryEntity` i `ItemStackEntity`.
- **Wyzwanie**: Poprawna obsługa usuwania przedmiotów (Orphan Removal) przy mutacjach siatki.

---

## 5. Warstwa Web (REST API) [ZROBIONE]
- Implementacja `InventoryController` z obsługą Get, Move, Remove.
- DTO (Requests/Responses) oraz Mapper (MapStruct).

---

## Verification Plan (TDD Matrix - PoE Style)

Będziemy wdrażać logikę w oparciu o poniższe testy w `InventoryTest.java`:

### 1. Podstawowe ruchy (Basic Movement)
- [x] **Move to Empty**: Przesunięcie przedmiotu na całkowicie wolne pola.
- [x] **Move to Same**: Przesunięcie na tę samą pozycję (No-op).
- [x] **Out of Bounds**: Próba wyjścia poza krawędź (prawa/lewa/dół).

### 2. Inteligentna Zamiana (Smart Swap)
- [x] **1x1 Swap**: Przesunięcie 1x1 na inny 1x1 (zamiana pozycji).
- [x] **Multi-slot Swap**: Przesunięcie dużego przedmiotu (np. 2x3) na mały (1x1).
- [x] **Failed Multi-Swap**: Próba przesunięcia dużego przedmiotu na pola zajęte przez **dwa lub więcej** innych przedmiotów -> Oczekiwany `Collision`.

### 3. Stackowanie (Stacking Logic)
- [x] **Full Merge**: Połączenie dwóch stackowalnych przedmiotów.
- [x] **Partial Merge**: Połączenie, gdy suma przekracza limit.
- [x] **Incompatible Stack**: Próba stackowania przedmiotów o różnych ID -> Oczekiwany `Swap`.

### 4. Looting (Auto-placement)
- [x] **Auto-stack on Pickup**: Automatyczne dopełnianie istniejących stosów przy dodawaniu.
- [x] **First Empty Search**: Znajdowanie pierwszego wolnego miejsca o odpowiednich wymiarach.
- [x] **Inventory Full**: Obsługa braku miejsca (`NoSpace`).

### 5. Spójność Agregatu (Invariants)
- [x] Weryfikacja, czy po każdym Swapie/Stacku `occupiedSlots` idealnie pokrywa się z faktycznym rozmieszczeniem przedmiotów.
- [x] Weryfikacja, czy przedmioty "widmo" nie zostają w siatce po nieudanych operacjach.
