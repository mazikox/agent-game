# 🎮 Analiza Frontend UI/UX — AgentGierka

**Projekt:** C:\AgentGierka (React Native / Expo)  
**Data analizy:** 2026-04-14  
**Cel:** Ocena elastyczności rozmieszczania elementów na stronie — jak łatwo przesuwać, dodawać i modyfikować komponenty UI.

---

## 📐 Jak jest zbudowany layout

Projekt to **React Native (Expo)** — działa jako mobile app + web. Brak dedykowanego webowego frontendu (foldery `static/` i `templates/` są puste). Cały UI żyje w `mobile/`.

### Hierarchia komponentów (App.js → GameContent)

```
App
├── LoginScreen                    (jeśli nie zalogowany)
└── SocketProvider
    └── GameContent
        ├── MapView               ← tło, pełny ekran (absoluteFill, zIndex: -1)
        └── hudOverlay            ← absoluteFill, pointerEvents: box-none
            └── SafeAreaView
                ├── TopBar        ← flexDirection: row (góra, w flow)
                └── contentContainer  ← flex: 1, padding: 15
                    ├── AgentProfile     ← brak pozycjonowania (w flow)
                    ├── SideMenu         ← position: absolute, right: 15, top: 25%
                    └── bottomLeftContainer ← position: absolute, bottom: 20, left: 20
                        └── AgentConsole
```

---

## 🔴 Problemy krytyczne — brak elastyczności rozmieszczania

### 1. MapView zamrożony na warstwie tła (zIndex: -1)

**Plik:** `mobile/src/features/world/MapView.js`

```js
container: {
    ...StyleSheet.absoluteFillObject,
    zIndex: -1,
}
```

Mapa jest **zawsze pod całym HUD-em**. Nie da się jej przesunąć, zminimalizować, ani umieścić komponentu "pod mapą". Jest trwale przyklejona do pełnego ekranu. HUD jest na niej nakładką z `pointerEvents: 'box-none'`.

**Wpływ:** Jeśli chcesz dodać element pod mapą, dodać minimapę na mapie, albo zamienić mapę z innym komponentem — wymaga to gruntownej zmiany architektury.

---

### 2. AgentProfile nie ma pozycjonowania — leci w normalnym flow

**Plik:** `mobile/src/features/agent/AgentProfile.js`  
**Plik:** `mobile/App.js` (rendering)

W `contentContainer` (flex: 1), AgentProfile po prostu leci od góry. Nie ma `position: 'absolute'`. Oznacza to:

- Przesuwa się w dół/scrollowalnie jeśli coś nad nim rośnie
- Nie da się go przypiąć do konkretnej pozycji na ekranie
- Zmiana rozmiaru contentu wpływa na jego pozycję

**Wpływ:** Niezamierzone przesunięcia przy dodawaniu nowych elementów do HUD.

---

### 3. SideMenu — hardcoded position: absolute z magicznymi wartościami

**Plik:** `mobile/src/features/hud/SideMenu.js`

```js
container: {
    position: 'absolute',
    right: 15,
    top: '25%',      // ← hardcoded procent!
    width: 160,
}
```

Nie da się go łatwo przenieść w inne miejsce — pozycja jest "na sztywno". Brak konfiguracyjnego systemu layoutu. Jeśli dodasz nowy element obok SideMenu, musisz ręcznie edytować jego style.

**Wpływ:** Każda zmiana pozycji wymaga edycji kodu wewnątrz komponentu.

---

### 4. AgentConsole przypięty do bottom-left na sztywno

**Plik:** `mobile/App.js`

```js
bottomLeftContainer: {
    position: 'absolute',
    bottom: 20,
    left: 20,
}
```

Dokładnie tak samo — nie da się go przemieścić bez edycji App.js.

---

### 5. TopBar w flow, nie w absolute

TopBar jest pierwszym dzieckiem w SafeAreaView, więc zawsze na górze. Nie da się go zamienić miejscami z innym elementem bez zmiany struktury renderowania.

---

### 6. Brak layout engine / grid systemu

Cały layout opiera się na **mieszance flex + absolute positioning** bez spójnego systemu. Poszczególne komponenty samodzielnie decydują jak się pozycjonują. Nie ma:

- Grid systemu (CSS Grid, Yoga Grid)
- Konfiguracyjnego systemu pozycji (np. layout JSON)
- Draggable / dockable mechanizmów

**Wpływ:** Dodanie nowego elementu wymaga:
1. Dodania go do JSX w App.js
2. Stworzenia containera z position: absolute
3. Ręcznego dobrania top/left/right/bottom
4. Testowania czy nie koliduje z innymi elementami

---

## 🟡 Problemy średnie

### 7. Hardcoded wymiary w wielu miejscach

| Komponent | Plik | Hardcoded wartości |
|-----------|------|-------------------|
| AgentProfile | AgentProfile.js | `padding: 20`, `minWidth: 300`, portrait `SIZE: 115`, `BORDER: 5` |
| AgentConsole | AgentConsole.js | `width: 420`, `height: 120` (sekcja logów) |
| SideMenu | SideMenu.js | `width: 160`, gridItem `70x70` |
| GothicStatBar | AgentProfile.js | `HEIGHT: 26`, `ICON_SCALE: 2.2`, `SOCKET_PADDING: 32` |
| MapView | MapView.js | viewport `width: 92%`, `maxWidth: 1100`, `height: 82%` |

**Wpływ:** Zmiana wymaga edycji kodu źródłowego konkretnego komponentu. Brak jednego miejsca do globalnego skalowania.

---

### 8. Brak responsywności — fixed pixels

AgentConsole ma `width: 420` — na małym ekranie (mobile telefon) będzie się ucinać. Brak `maxWidth`, brak warunków ekranowych (breakpoints), brak obsługi orientacji landscape vs portrait.

**Wpływ:** UI wygląda dobrze na desktop/web, ale na mniejszych ekranach elementy mogą się nakładać lub ucinać.

---

### 9. Brak systemu zarządzania warstwami (z-index)

Tylko MapView ma `zIndex: -1`. Reszta używa domyślnego stacking contextu. Jeśli chcesz umieścić nowy element nad/pod innym — nie ma spójnego systemu zarządzania warstwami.

**Wpływ:** Np. modal, tooltip, czy popup nad HUD-em może być problematyczny.

---

### 10. Informacje o agentzie rozrzucone po dwóch miejscach

`gold` i `gems` są przekazywane do `TopBar` jako **hardcoded values** (1575, 28) w App.js. Nie pochodzą z API. `hp`, `maxHp`, `stamina` idą do `AgentProfile` z hooka. Nie ma spójnego obiektu "agent state" który trzyma wszystkie dane UI.

```js
// App.js — hardcoded!
<TopBar gold={1575} gems={28} ... />
```

**Wpływ:** Przy dodaniu nowych statystyk (np. XP, reputation) trzeba szukać gdzie je podpiąć.

---

## 🟢 Co jest zrobione dobrze

| Aspekt | Detal |
|--------|-------|
| **pointerEvents: 'box-none'** | HUD overlay nie blokuje kliknięć na mapę |
| **Animated marker** | Pin agenta animuje się płynnie przy zmianie pozycji |
| **Map switching** | Fade-in/fade-out przy zmianie lokacji z `Animated.timing` |
| **Best-fit algorithm** | Mapa skaluje się zachowując aspect ratio |
| **Theme system** | Centralny `theme.js` z kolorami, spacingiem, typografią |
| **WebBlendedImage** | Workaround dla `mixBlendMode` na web vs native |
| **Corner decorations** | Reużywalny komponent `<Corner>` |
| **PulseIcon** | Reużywalny komponent animacji |
| **Socket real-time** | WebSocket z STOMP + SockJS, subskrypcje per agent |
| **Struktura plików** | Czysta organizacja: features/agent, features/hud, features/world |
| **Custom hook** | `useAgentState` ładnie izoluje logikę od UI |

---

## 🛠️ Rekomendacje — jak zrobić to elastyczne

### Opcja A: Konfiguracyjny Layout Engine (JSON-driven)

Stwórz plik konfiguracyjny, który definiuje gdzie jest każdy element:

```js
// hudLayoutConfig.js
export const HUD_LAYOUT = {
  TopBar:     { position: 'absolute', top: 0, left: 0, right: 0, zIndex: 100 },
  Profile:    { position: 'absolute', top: 60, left: 15, zIndex: 90 },
  SideMenu:   { position: 'absolute', right: 15, top: '25%', zIndex: 80 },
  Console:    { position: 'absolute', bottom: 20, left: 15, zIndex: 90 },
  MiniMap:    { position: 'absolute', bottom: 20, right: 15, zIndex: 80, width: 200, height: 200 },
  // nowy element? po prostu dodaj wpis:
  // QuestTracker: { position: 'absolute', top: 120, right: 200, zIndex: 85 },
};
```

Wtedy w `GameContent` renderujesz wszystko przez wrapper:

```jsx
{Object.entries(HUD_LAYOUT).map(([key, layout]) => (
  <View key={key} style={layout}>
    {components[key]}
  </View>
))}
```

**Plusy:** Dodanie/przesunięcie elementu = edycja jednego pliku.  
**Minusy:** Trzeba refaktorować istniejący kod.

---

### Opcja B: Draggable HUD (runtime)

Użyj `react-native-gesture-handler` + `react-native-reanimated` żeby każdy element HUD był przeciągalny palcem (lub myszką na web). Pozycje zapisuj do `AsyncStorage` / `localStorage`.

**Plusy:** Ultimate elastyczność — użytkownik sam układa UI.  
**Minusy:** Większy narzut kodowy, komplikacja testów.

---

### Opcja C: CSS Grid / Yoga Layout

Przejdź na grid-based layout zdefiniowany na jednym poziomie:

```jsx
<View style={styles.hudGrid}>
  {/* grid-template-areas:
     "top    top    top"
     "prof   map    menu"
     "cons   cons   cons" */}
  <TopBar style={gridArea('top')} />
  <Profile style={gridArea('profile')} />
  <MapView style={gridArea('map')} />
  <SideMenu style={gridArea('menu')} />
  <Console style={gridArea('console')} />
</View>
```

**Plusy:** Spójny system, responsywność z automatu.  
**Minusy:** React Native web wspiera CSS Grid tylko częściowo.

---

### Opcja D (rekomendowana): Ujednolicenie + Wrapper pozycjonujący

1. **Wszystkie elementy HUD jako `position: absolute`** w jednym wspólnym wrapperze
2. **Pozycje zdefiniowane w jednym pliku** (nie rozsiane po komponentach)
3. **Z-indexy z konfiguracji**
4. **Responsive variant** — inna konfiguracja dla mobile vs web vs tablet
5. **Komponenty nie znają swojej pozycji** — są "czyste", pozycjonowanie jest na zewnątrz

---

## 📊 Podsumowanie — ocena elastyczności layoutu

| Kryterium | Ocena | Notatka |
|-----------|-------|---------|
| Przesuwanie komponentów | 🔴 1/5 | Hardcoded position w każdym komponencie |
| Dodawanie nowych elementów | 🟡 2/5 | Trzeba ręcznie edytować App.js + style |
| Responsywność | 🔴 1/5 | Fixed pixels, brak breakpoints |
| Zmiana warstw (z-order) | 🔴 1/5 | Brak systemu z-index |
| Spójność layoutu | 🟡 2/5 | Mieszanka flex + absolute bez planu |
| Czytelność kodu | 🟢 4/5 | Czysta struktura, dobre nazewnictwo |
| Theme / design system | 🟢 4/5 | Centralny theme.js |
| Animacje | 🟢 4/5 | Animated API dobrze użyte |

---

## Wniosek

Projekt wygląda ładnie i jest dobrze zakodowany pod kątem wizualnym, ale **layout jest bardzo sztywny** — pozycje elementów są rozsiane po plikach, wszystko jest hardcoded. Jeśli chcesz łatwo przesuwać komponenty, dodawać nowe, zmieniać warstwy — **najlepszy ruch to ujednolicić pozycjonowanie w jednym miejscu** (konfiguracja JSON) i uczynić wszystkie elementy HUD `position: absolute` zarządzanym z poziomu jednego wrapper komponentu.
