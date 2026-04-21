# Projekt Systemu Ekwipunku i Zapasów (Inventory System)

Ten dokument stanowi kompleksowy plan implementacji systemu zarządzania ekwipunkiem gracza, opartego o zasady Domain-Driven Design (DDD) i Architekturę Heksagonalną. Docelowy system ma naśladować rozwiązania chociażby z gry Metin 2 (siatka przedmiotów o różnych rozmiarach w ekwipunku, system ulepszeń, unikalne id przedmiotów). Zgodnie z założeniami, system **nie będzie posiadał mechaniki "Auto-Sort"**, skupiając się w zamian na poprawności operacyjnej i wydajności bazodanowej.

## User Review Required

> [!IMPORTANT]
> Proszę o zapoznanie się z zaktualizowanym o optymalizacje planem przed rozpoczęciem implementacji (Krok 1 to obiekty Domeny).

## 1. Architektura i Założenia (Aktualizacja)

Stosujemy **Rich Domain Model** i czystą abstrakcję przez **Architekturę Heksagonalną**. 

**Mechanizmy obronne i optymalizacyjne:**
1. **Optimistic Locking (@Version) + Retry:** Równoległe manipulacje ekwipunkiem zakończą się konfliktem wersji w bazie. By gracz nie odczuł "błędu bazy" podczas nieszczęśliwego timing'u, warstwa aplikacyjna automatycznie ponowi próbę za pomocą `@Retryable` w ułamku sekundy, rozwiązując to transparentnie.
2. **JOIN FETCH:** Potężna ochrona przed efektem N+1 w samej technologii Hibernate dla Ekwipunku.
3. **Pamięć wymiarów:** Mapper DB tworzący obiekty domeny od razu podpina dla nich referencje do wymiarów. Metody domenowe `Inventory` nie są zaśmiecane nadmiernym przepychaniem parametrów słownikowych przez kontrolery z góry.
4. **UUID TIME:** Baza będzie korzystała z sekwencyjnego UUID (v7). Chroni to indeksy drzew B-Tree w bazach typu Postgres przed morderczą fragmentacją przy rozroście bazy MMO.
5. **Kalkulacja Siatki:** Z racji na zapis w postaci `int gridIndex` - stanowi on punkt zakotwiczenia i jest zawsze *lewym górnym rogiem* (top-left) pudełka. Klasa `Inventory` musi wyliczyć pozostałe miejsca blokujące siatkę na podstawie `width` oraz `height`. 

---

## 2. Model Domenowy (Core Domain)

Proponowana struktura pakietów dla czystej Javy z pełną separacją od frameworka: `com.agentgierka.mmo.inventory.domain`

### [NEW] `ItemTemplate` (Value Object)
Baza referencyjna właściwości.
```java
public record ItemTemplate(
    String id,
    String name,
    int width,
    int height,
    int maxStack
) {}
```

### [NEW] `ItemStack` (Entity)
Klasa nafaszerowana wiedzą. Sama trzyma referencję (szablon)! Odciąża to API biznesowe od pytania o obiekty pomocnicze przy każdym ruchu.
```java
public class ItemStack {
    // ... id, quantity, bonuses itp ...
    
    // Obiekt domeny przechowuje wewnątrz szablon przekazany przy generowaniu przez nasz DB Mapper.
    private final ItemTemplate template; 
    
    public int getWidth() { return template.width(); }
    public int getHeight() { return template.height(); }
}
```

### [NEW] `Inventory` (Aggregate Root)
Zmieniona koncepcja zwracanych wartości dla logiki transakcyjnej z jasnym mapowaniem punktów kotwiczących na zbiory wektorów.
```java
public class Inventory {
    private int width; // szerokość EQ np 5
    // slots key - to wyłącznie TopLeft krańce posiadanych przedmiotów.
    private final Map<Integer, ItemStack> slots;

    // Przesuwanie nie rzuca wulgarnych Wyjątków Javy, lecz zwraca stan operacji by obsłużył to np. WebSocket (brak wymuszania wyszukiwań szablonu w Serwisie!)
    public InventoryResult moveItem(int fromIndex, int toIndex);
}
```

---

## 3. Zastosowanie DDD - (Application Service)

Pakiet: `com.agentgierka.mmo.inventory.application`

### [NEW] `InventoryApplicationService`
Usługa aplikacyjna koordynuje transakcję dbając o ponowny zaciąg w przypadu blokad relacyjnych bazy - ratuje User Experience.
```java
@Service
@Transactional
public class InventoryApplicationService {
    private final InventoryRepository inventoryRepo; // Wywołuje Port na architekturze Hex.

    // Automatyczna przezroczysta próba ponowienia wywołania wyścigu np. odczekując 100 milisekund
    @Retryable(
      retryFor = OptimisticLockingFailureException.class, 
      maxAttempts = 3, 
      backoff = @Backoff(delay = 100)
    )
    public InventoryResult switchItemPosition(UUID characterId, int indexFrom, int indexTo) {
         // Pobranie całego ekwipunku bez ryzyka N+1
        Inventory inv = inventoryRepo.findByCharacterIdWithItems(characterId).orElseThrow();
        
        InventoryResult result = inv.moveItem(indexFrom, indexTo); // DDD na czysto
        
        inventoryRepo.save(inv);
        return result; // Ścisła struktura odpowiedzi do zwrotu z Controller / handlera Socketów.
    }
}
```

---

## 4. Adaptacja do bazy danych i wzorzec Repository (Adapters / DB)

Aby powiązać wiedzę między tabelą pozbawioną "Mózgu" a mądrą logiką Domeny, dodajemy mapper: `com.agentgierka.mmo.inventory.infrastructure.db`

### [NEW] `InventoryPersistenceAdapter` + Mappery
Implementacja Portu. Nakarmia encję `Inventory` przy wybudowaniu referencjami z globalnego słownika itemów!
```java
@Component
public class InventoryPersistenceAdapter implements InventoryRepository {
    private final InventoryJpaRepository jpaRepo;
    private final ItemTemplateDictionary dictionary;

    @Override
    public Optional<Inventory> findByCharacterIdWithItems(UUID id) {
        return jpaRepo.findByCharacterIdWithItems(id).map(entity -> {
            // 💡 TUTAJ POJAWIA SIĘ MAGIA - MAPOWIEMY TYP (String) NA ZWROT FIZYCZNEGO SZABLONU dla DOMENY
            return InventoryMapper.toDomain(entity, dictionary);
        });
    }
}
```

### [NEW] Encje Bazodanowe (JPA)
Zaktualizowane adnotacje `@UuidGenerator` zapobiegające awariom wydajnościowym pod wpływem ułamkowania stron baz typu Postgres SQL (tzw. UUIDv7 generowane na serwerze).
```java
@Entity
@Table(name = "inventory_items")
public class ItemStackEntity {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;
    // ... relacje JSONB itp
}
```

---

## Open Questions

1. **Wymiary siatki:** Jakie docelowo powinny być wymiary siatki głównego ekwipunku gracza (X na Y rzędów np. 5 na 9)? 
2. **Kształty:** Czy uwzględniamy tylko regularne wymiary przedmiotów prostokątno-podłużnych np. `1x1`, `1x2`, `1x3`, `2x2`, `2x3`?

## Verification Plan

1. **Test Przeliczania Pól Siatki (Domain / `InventoryTest.java`):** Najważniejszy element dla systemu. Kod wyliczający zajmowany obszar, startujący np. od `0` i analizujący przemieszczenia szerokości oblewająco, dla przedmiotu 2x2, na podstawie rzędów np. po 5 kratek długości. Udowadnianie metod w TDD takich jak kolizja i wbijanie się "pod mapę".
2. **Zatwierdzenie Architektury Kontrolera i Transakcji:** Przy próbach zniszczeń integracyjnych i podwójnego żądania nadpisywania wymusi na warstwie upewnienie się, że gracz dostanie ponowienie po `OptimisticLockingFailureException`. Powinny nastąpić symulacje `InventoryResult` z np. wynikiem `ITEM_COLLISION` w przypadku niemożliwości ułożenia elementu w danym skrawku przez logikę. 
