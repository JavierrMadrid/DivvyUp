# AGENTS.md — DivvyUp

## Project Overview

DivvyUp is a **Kotlin Multiplatform (KMP)** expense-splitting app using **Compose Multiplatform** for shared UI and **Supabase** (Postgres) as backend. Users create groups, add participants, categorize and track shared expenses, and settle balances. The UI language is **Spanish**.

## Agent Routing (orchestrator → subagents)

The default agent is `orchestrator` (`.opencode/agent/orchestrator.md`). It classifies each request and delegates to one specialist subagent; it does not implement code itself.

| Work | Delegate to | Skills loaded by default |
|---|---|---|
| `domain/`, `application/`, `integration/supabase/`, `integration/cache/`, DTOs, SQL migrations, repositories, services, ViewModels, DI wiring | `kotlin-backend` | `kotlin-multiplatform`, `android-clean-architecture`, `android-kotlin`, `invite-link-postgres-best-practices` |
| `integration/ui/` screens, theme, components, navigation, motion, tokens, accessibility, Spanish copy | `ux-design` | `mobile-android-design`, `mobile-app-ui-design`, `android-jetpack-compose`, `compose-component-design`, `compose-state-and-effects`, `compose-animations` |

Cross-cutting feature → split by layer: `kotlin-backend` implements data/logic first (domain → repository → service → ViewModel), then `ux-design` builds the screen against the resulting `UiState`. Verify with `.\gradlew.bat test` and `.\gradlew.bat assembleDebug`.

## Architecture — Clean Architecture / Hexagonal (KMP)

Three-layer hexagonal architecture under `app/src/commonMain/kotlin/com/example/divvyup/`:

| Layer | Path | Responsibility |
|---|---|---|
| **domain** | `domain/model/`, `domain/repository/` | Pure Kotlin data classes and repository interfaces. **No framework imports.** Lives in `commonMain`. |
| **application** | `application/` | Services orchestrate domain logic: `GroupService`, `SpendService`, `SettlementService`, `CategoryService`. Receive repositories via constructor injection. Lives in `commonMain`. |
| **integration** | `integration/supabase/`, `integration/cache/`, `integration/ui/` | Platform adapters: `Supabase*Repository` implementations, in-memory cache decorators (`Cached*Repository`), and Compose UI/navigation/ViewModels. |

**Dependency rules** (strict — enforced by module boundaries):
- `domain` → **nothing** (pure Kotlin only)
- `application` → `domain` only
- `integration` → `application` + `domain`
- Never import Android framework classes in `domain` or `application`

## Kotlin Multiplatform — Source Set Structure

```
commonMain/          → Domain models, repository interfaces, business logic, UseCases,
                       Supabase repositories, cache decorators, ViewModels (StateFlow),
                       Compose screens, theme, navigation, UI components.
                       Dependencies: kotlin-stdlib, kotlinx-coroutines, kotlinx-serialization,
                       kotlinx-datetime, compose-multiplatform,
                       org.jetbrains.androidx.navigation:navigation-compose,
                       lifecycle-viewmodel-compose, Supabase Postgrest
androidMain/         → ONLY strictly Android-specific: MainActivity, AndroidManifest,
                       BuildConfig/local.properties wiring, Ktor Android engine.
iosMain/             → iOS-specific entry point (`MainViewController.kt`), DI wiring,
                       Ktor Darwin engine.
commonTest/          → Pure Kotlin tests for application logic (e.g. `SettlementServiceTest`).
```

### KMP Decision Rules
- **Pure Kotlin logic** (models, validation, UseCases) → `commonMain`
- **ViewModels** (StateFlow + business logic) → `commonMain` (StateFlow is KMP-compatible)
- **Compose screens, theme, navigation** → `commonMain` (Compose Multiplatform runs on all targets)
- **Only go to `androidMain`** when the code literally cannot compile without Android SDK:
  `Activity`, `Context`, `AndroidManifest`, platform Supabase client init, google-services.
- **Supabase client initialization** → platform entry points (`MainActivity` on Android, `MainViewController` on iOS); repository interfaces and implementations stay in `commonMain`
- Use `expect`/`actual` **only** when code is needed by 2+ platforms and varies by platform
- Prefer `kotlinx.*` libraries over platform-specific ones:
  - `kotlinx.serialization` (not Jackson/Gson)
  - `kotlinx.datetime` (not `java.util.Date`)
  - `ktor` (not OkHttp)
- **Do NOT move screens/viewmodels/theme to `androidMain`** just because the IDE shows warnings in `commonMain` — those are false positives from analyzing KMP source sets without full Android context. The code compiles and runs correctly.
- **"Unresolved reference 'navigation'"** en `commonMain` es un falso positivo del IDE. `org.jetbrains.androidx.navigation:navigation-compose` resuelve sus artefactos KMP vía Gradle metadata en tiempo de compilación. `assembleDebug` compila sin errores.

## Dependency Wiring (Manual DI)

**No DI framework** (no Hilt/Dagger/Koin). Manual wiring lives in platform entry points: `MainActivity.onCreate()` on Android and `MainViewController()` on iOS.
```kotlin
val supabaseClient = createSupabaseClient("URL", "ANON_KEY") { install(Postgrest) }
val postgrest = supabaseClient.postgrest
val groupRepository = CachedGroupRepository(SupabaseGroupRepository(postgrest)) // Android
val spendRepository = CachedSpendRepository(SupabaseSpendRepository(postgrest))
val groupService = GroupService(groupRepository)
val spendService = SpendService(spendRepository, participantRepository)
```
On iOS, `MainViewController()` currently wires the same services with direct `Supabase*Repository` instances (without cache decorators). When adding new services or repositories, mirror the wiring in both platform entry points.

## Data Layer — Supabase / Postgres

**Source of truth (read, do not re-derive):** `docs/DATABASE_MODEL.md` (ER diagram + full current schema) and applied migrations in `docs/sql/V0xx__*.sql` (currently V001–V020). Never paste or guess the full DDL — read those files. Never edit an applied migration; add a new `V0xx__*.sql`.

Core normalized tables (no embedded arrays): `groups`, `participants`, `categories` (NULL `group_id` = global default), `spends`, `spend_shares`, `settlements`; plus `activity_log`, `user_profiles`, `recurring_spends` added later. See docs for the authoritative, evolved schema.

### Schema Design Rules (Supabase best practices)
- **Primary keys**: `bigint generated always as identity`; UUIDv7 for distributed IDs. Avoid random UUIDv4 on large tables (index fragmentation).
- **Data types**: `text` (not `varchar(n)`), `timestamptz` (not `timestamp`), `numeric` for money (not `float`), `boolean` (not string).
- **Foreign keys**: always create an index on FK columns — Postgres does NOT auto-index them.
- **RLS**: enable on every table with user data; use `auth.uid()` in policies.

### Repository & Query Rules
- All repository methods are `suspend` functions.
- Interfaces in `domain/repository/`; Supabase impls in `integration/supabase/` (+ `dto/` with `@Serializable` DTOs + mapper extensions); cache decorators in `integration/cache/`.
- `Supabase*Repository` receive a shared `Postgrest` instance, not the full Supabase client.
- Wrap Supabase exceptions into domain `Exception` with Spanish messages.
- Always index WHERE/JOIN columns; keep transactions short.
- Mirrored settlement linkage: `SettlementService` writes notes `__settlement_id:<id>`; `GroupDetailViewModel` relies on that prefix.
- Balances/analytics source of truth: `SettlementService.getBalances` + SQL views in `docs/sql/`.

## UI Patterns

- **Navigation**: `NavHost` with type-safe `@Serializable` sealed routes in `Screen.kt`. Main flow: `GroupList` → `CreateGroup` → `AddParticipants` → `GroupDetail`, with additional routes `AddSpend`, `AddParticipantInGroup`, and `GroupSettings`.
- **App shell**: `AppShell.kt` wraps the `NavHost` with a floating pill bottom bar (Grupos / Actividad / Perfil) shown only on top-level destinations. The top-level Activity feed lives in `ActivityFeedScreen.kt` + `ActivityFeedViewModel`. `NavHost` uses shared-axis + fade transitions (`DivvyUpMotion`).
- **System bars**: use `SetSystemBarAppearance(useDarkIcons)` per screen (gradient headers → `false`) and `ThemedSystemBarAppearance()` for light top bars. Navigation bar follows the resolved theme via `DivvyUpTheme`. See `integration/ui/SystemBars.kt`.
- **State management**: `GroupListViewModel`, `GroupDetailViewModel`, and `AddParticipantsViewModel` expose `StateFlow<*UiState>` from `commonMain`. Navigation side effects are modeled with flags such as `createdGroupId`, `navigateToSpendScreen`, and `spendSaved`.
- **Composable guidelines**:
  - Accept `Modifier` as first optional parameter.
  - Use `key` in `LazyColumn` items for efficient recomposition.
  - Use `LaunchedEffect` for side effects — never call ViewModel methods directly in composition.
  - Use `derivedStateOf` for expensive computations, not inline in composable body.
  - Use `rememberSaveable` for state surviving configuration changes.
  - In `commonMain` screens/navigation, collect flows with `collectAsState()` (see `GroupListScreen`, `GroupDetailScreen`, `AppNavigation`).
- **Dialogs** colocated in the same file as their parent screen.
- **Group detail composition**: `GroupDetailScreen` switches tabs via `GroupDetailTab` and delegates content to `SpendTabScreen`, `BalanceTabScreen`, and `AnalyticsTabScreen`.
- **Material 3**: Access colors via `MaterialTheme.colorScheme`, support dynamic color on Android 12+.

### Design System — "Soft Jungle" (soft / rounded friendly)

The visual language is warm, rounded and friendly. Its single source of truth is `integration/ui/theme/`:

- **Colors** (`Color.kt`): green brand anchor (`JungleGreen`), coral secondary (`Coral*`), lavender tertiary (`Lavender*`), soft mint background. Light/dark schemes only.
- **Typography** (`Type.kt`): **Plus Jakarta Sans** (bundle in `commonMain/composeResources/font/`). Never set `fontFamily` inline — always inherit from `MaterialTheme.typography`. Use `AmountText` for tabular figures.
- **Shapes** (`Theme.kt`): soft-rounded — controls 16 dp, cards 28 dp, dialogs 32 dp, pills 50 dp. Use `DivvyUpTokens.Radius*`.
- **Motion** (`Motion.kt`): use `DivvyUpMotion.Short/Medium/Long/ExtraLong` and `Standard/Emphasized*` easings. Never hardcode `tween(300)`.
- **Elevation**: use `DivvyUpTokens.Elevation*`; shadows must be soft and tinted, never pure grey/black.

### Centralized UI Style — Rules (enforce always)

All screens **must** follow the centralized style. Never hardcode raw color values or sizes; always use design tokens and theme color roles:

| Element | Rule |
|---|---|
| **Colors** | Always use `MaterialTheme.colorScheme.*` roles. Direct token constants (`JungleGreen`, `Coral`, `Lavender`, etc.) are only allowed for the primary CTA fill (`containerColor = JungleGreen, contentColor = Color.White`) and avatar backgrounds. Never use `.copy(alpha = …)` on brand colors as Surface backgrounds — it breaks contrast in dark mode. |
| **Card / Surface** | Use `Card` with `CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)` + `CardDefaults.cardElevation(defaultElevation = DivvyUpTokens.ElevationCard)` + `.shadow(DivvyUpTokens.ElevationCard, RoundedCornerShape(DivvyUpTokens.RadiusCard), …)`. **Never use bare `Surface` without elevation for content cards** — they blend into the background. |
| **Button shapes** | Primary CTA: `RoundedCornerShape(DivvyUpTokens.RadiusPill)` + `height = DivvyUpTokens.PrimaryButtonHeight`. Secondary/cancel inline buttons: `RoundedCornerShape(DivvyUpTokens.RadiusPill)` + `height = DivvyUpTokens.ControlHeight`. Never mix `RadiusControl` for CTA buttons. |
| **Button colors** | Primary fill: `containerColor = JungleGreen, contentColor = Color.White`. Secondary tonal: `containerColor = primaryContainer, contentColor = onPrimaryContainer`. Destructive outlined: `border = 1.5.dp error.copy(0.7f), contentColor = error`. Neutral outlined: `border = outline, contentColor = onSurface`. |
| **Section headers inside cards** | `style = labelLarge, fontWeight = SemiBold, color = primary` (green). Never `onSurfaceVariant` for section labels — too low contrast against card background. |
| **Icon sizes** | Use `DivvyUpTokens.IconSm` (18 dp) / `IconMd` (20 dp) / `IconLg` (24 dp). Never hardcode icon dp values inline. |
| **Spacing** | Use `DivvyUpTokens.GapSm/GapMd/GapLg` for `Arrangement.spacedBy(…)` between items. Screen horizontal padding: `DivvyUpTokens.ScreenPaddingHLg` (20 dp). |
| **Info/guest banners** | Use `tertiaryContainer / onTertiaryContainer` — guaranteed contrast in both themes. Never `JungleGreen.copy(alpha = 0.15f)` as a background. |
| **Avatar circles** | Icon avatars (no photo): `primaryContainer` background + `onPrimaryContainer` tint. Photo/initial avatars: `JungleGreen` background + `Color.White` text. Always add `.shadow(…, CircleShape)`. |
| **Dividers inside cards** | `HorizontalDivider(color = outlineVariant.copy(alpha = 0.5f))` — never full-opacity dividers. |

## Build & Run

```powershell
# Build debug APK
.\gradlew.bat assembleDebug

# Run unit tests
.\gradlew.bat test

# Run instrumented tests (requires emulator/device)
.\gradlew.bat connectedAndroidTest
```

- **Compile SDK**: 36 | **Min SDK**: 26 | **Target SDK**: 36
- **Kotlin**: 2.3.20 | **Compose Multiplatform**: 1.10.3 | **Compose BOM**: 2026.03.01 | **AGP**: 9.1.0
- **Java / JVM target**: 21
- Version catalog at `gradle/libs.versions.toml` — always add new dependencies there.
- Android reads `SUPABASE_URL` and `SUPABASE_ANON_KEY` from `local.properties` into `BuildConfig`; iOS receives them as parameters in `MainViewController()`.

## Conventions

- **Language**: UI strings in Spanish (hardcoded in composables, not `strings.xml`).
- **IDs**: Domain and navigation IDs are `Long`, backed by Postgres `bigint generated always as identity`. New unsaved domain objects use `id = 0` until persisted.
- **Error handling**: Repository wraps Supabase exceptions into `Exception` with Spanish messages. ViewModel catches and stores in `UiState.error`.
- **Serialization**: Use `kotlinx.serialization` for all DTOs. Domain models are plain data classes (no annotations).
- **Dates**: Persisted timestamps use `kotlin.time.Instant` in domain/application code; analytics filters use `kotlinx.datetime.LocalDate` / `Month`. Avoid `java.util.Date`.
- **Debug logging**: `println("DEBUG ClassName: ...")` pattern.
- **No DI framework**, no Room/local DB, no authentication yet — Supabase is the sole data source.

## Anti-Patterns to Avoid

- ❌ Android framework imports in `domain` or `application` — keep pure Kotlin
- ❌ Exposing database DTOs to UI — always map to domain models
- ❌ `GlobalScope` or `runBlocking` on main thread — use `viewModelScope`
- ❌ Exposing `MutableStateFlow` — expose only `StateFlow`
- ❌ Side effects in composable body — use `LaunchedEffect`/`SideEffect`
- ❌ Introducing `String`/UUID IDs in domain models or navigation — the current app uses `Long` IDs end-to-end
- ❌ Missing indexes on FK columns — always create them
- ❌ `timestamp` without timezone — always use `timestamptz`
- ❌ Platform-specific libraries in `commonMain` — use `kotlinx.*` alternatives
- ❌ Raw `Surface` without elevation for content cards — use `Card` with `cardElevation` + `.shadow()`
- ❌ `JungleGreen.copy(alpha = …)` as a Surface/Card background — breaks dark-mode contrast; use `tertiaryContainer` or `primaryContainer`
- ❌ Hardcoded dp values for icons, heights, radii or spacing — always use `DivvyUpTokens.*`
- ❌ `onSurfaceVariant` color for section header labels inside cards — use `primary` (green) for visibility
- ❌ `RadiusControl` shape for primary CTA buttons — use `RadiusPill`

## Adding a New Feature Checklist

1. **Domain model** → `domain/model/` (data class with default values, pure Kotlin, KMP-compatible types)
2. **Repository interface** → relevant file in `domain/repository/` (suspend functions)
3. **Supabase implementation** → `integration/supabase/` + `integration/supabase/dto/` (DTO with `@Serializable`, mapper extensions, indexed FK in SQL)
4. **Cache decorator** → if the repository feeds list/detail screens repeatedly, mirror the existing `Cached*Repository` pattern in `integration/cache/`
5. **Service/UseCase** → `application/` (orchestrates domain logic; see `SpendService` and `SettlementService` for validation/calculation patterns)
6. **ViewModel + UiState** → `integration/ui/viewmodel/` (StateFlow, viewModelScope, navigation flags in state when needed)
7. **Screen composable** → `integration/ui/screens/` (stateless where possible, Modifier param, key in lists)
8. **Route** (if new screen) → `Screen.kt` + `AppNavigation.kt`
9. **Wire in platform entry points** → update both `MainActivity` and `MainViewController` if new service/repository introduced
10. **SQL migration** → add/update `docs/sql/V00x__*.sql`, keep FK indexes/RLS, and sync `docs/DATABASE_MODEL.md`
