# CLAUDE.md — Second Brain (Android native)

Read this fully before any task. If a request conflicts with this file, say so instead of silently deviating.

## Product

An intelligence layer that understands what the user encounters, remembers what matters, connects it to what they already know, and helps them act. Positioning: **a second brain for your phone** — never "an AI note taker."

**North-star test:** capture something from another app, forget it, ask a question days later whose answer needs that item plus accumulated context, get a correct **cited** answer.

**Branding:** no mascot, no character, no persona, no assistant name. No anthropomorphic voice ("I noticed…", "I've been thinking…"). The product speaks as a tool. Display name lives in one constants file; package id is `com.zackwhye.secondbrain`.

## Stack (decided — do not relitigate, do not substitute)

Kotlin · Jetpack Compose · Material 3 · Hilt · Room · Retrofit+OkHttp (or Ktor if already present) · Coroutines/Flow · KSP (never KAPT) · Gradle version catalog (`libs.versions.toml`) with Kotlin DSL. Backend: Supabase (Postgres + pgvector + Auth + Storage + Edge Functions).

Android-only. No Flutter, no KMP, no iOS code. Do not add a dependency without asking first — one line on why, and what it replaces.

## Architecture

Three layers, dependencies point inward: **ui → domain → data**. Nothing points outward.

- **UI layer** — Compose + ViewModel as state holder. The ViewModel exposes exactly one `StateFlow<XUiState>`; the screen collects with `collectAsStateWithLifecycle()`. No business logic in composables.
- **Domain layer** — pure Kotlin use cases, no Android imports. Only add a use case when logic is reused or non-trivial; a passthrough use case is noise, skip it and let the ViewModel call the repository.
- **Data layer** — repositories own their data and are the single source of truth. Room is the local SSOT; network writes into Room, UI reads from Room. Never let the UI call Retrofit or Room directly.

**Unidirectional data flow, strictly.** State flows down, events flow up. All state that crosses a layer boundary is immutable (`data class` with `val`, `List` not `MutableList`, `StateFlow` not `MutableStateFlow` in public API).

**Screen state is one sealed hierarchy per screen**, not scattered booleans:

```kotlin
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Error(val message: String, val retryable: Boolean) : HomeUiState
    data class Ready(val items: List<CaptureCard>, val isRefreshing: Boolean) : HomeUiState
}
```
Never `isLoading && !isError && data != null` combinations — they permit impossible states.

**One-off events** (navigation, snackbars) go through a `Channel`/`SharedFlow`, never a nullable field in the state that the UI must nulls-out. Errors are modelled states, never silent.

**MVVM as the base; MVI-style reducers only where a screen genuinely earns it** (multi-step, high-interaction). Don't build an event/reducer framework for a list screen.

**Package by feature, then layer:** `feature/capture/{ui,domain,data}`, `feature/ask/…`, plus `core/{designsystem,database,network,model}`. Single Gradle module until build time actually hurts — premature modularization costs a solo developer more than it returns. When you do split, split along these existing package seams.

## UI practices

- **Design tokens first.** Colors, type scale, spacing, shape, elevation live in `core/designsystem`. No hardcoded hex, `dp`, or `sp` in feature code — if a token is missing, add it to the design system, don't inline a literal.
- **Stateless composables + state hoisting.** Screen-level composable takes `uiState` and lambdas; it does not take a ViewModel. A thin `XRoute` composable wires the ViewModel to the stateless `XScreen`. This is what makes previews and screenshot tests possible.
- **`@Preview` for every state**, not just the happy path: Loading, Error, Empty, Ready, long-content overflow, dark theme. These previews are the screenshot test corpus — treat them as test fixtures, not decoration.
- **Lists:** `LazyColumn` with stable `key =`, and `contentType` when rows differ.
- **Accessibility is not optional:** `contentDescription` on meaningful icons (`null` on decorative), minimum 48dp touch targets, support dynamic font scale (test at 1.3x), never convey meaning by color alone.
- **Compose performance:** keep composable parameters stable; prefer immutable collections; defer state reads into lambdas (`Modifier.offset { }`) for frequently-changing values; don't optimize before a measured problem.
- Dark and light theme both supported from day one.

## Testing

Test the state machine, not the framework.

- **ViewModel tests are mandatory** for every screen: each state transition, error path, and retry. Use **Turbine** for Flow assertions and a `MainDispatcherRule` with `StandardTestDispatcher`.
- **Prefer fakes over mocks.** Write `FakeItemRepository` implementing the real interface. Reach for MockK only for awkward third-party boundaries. A test that mocks the thing it's testing proves nothing.
- **Repository tests** with an in-memory Room database and a fake network source.
- **Screenshot tests** for UI regressions — see the workflow file for the chosen library. Record goldens deliberately, review diffs by eye, never blind-update goldens to make a build green.
- **Instrumented tests only for what can't be tested on the JVM**: share-intent handling, Room migrations, permission flows. They're slow; keep them few and meaningful.
- **Coverage:** no percentage target. Required: every ViewModel, every repository, every non-trivial domain function. Not required: composable layout, generated code, DI wiring. A high number achieved by testing getters is worse than an honest lower one.
- Every bug fix ships with a test that fails without the fix. State that in the commit.

## Working rules for Claude Code

1. **Verify before you write.** Check current API signatures for Room, Compose, Hilt, Supabase, and any provider SDK against real docs — never from memory. Version-catalog versions are the source of truth for what's available.
2. **Never claim something works that you haven't run.** In every report, state explicitly: *compiles* / *unit tests pass* / *ran on emulator* / *ran on physical device*. These are four different claims and only the last one counts for capture flows.
3. **Run `./gradlew :app:assembleDebug`, `./gradlew test`, and `./gradlew lint`** before declaring a task done. Paste the relevant output — not a summary of it.
4. **Small commits, one concern each.** Conventional commit messages. No mixed refactor-plus-feature commits.
5. **Stop and ask** when: a new dependency is needed, a documented decision here seems wrong, an API differs from expectation, or a task looks like it needs more than ~300 lines of new code. Propose options; don't pick silently.
6. **No TODOs, no stubs, no placeholder implementations** unless I asked for a scaffold. An unimplemented function must throw, not return fake data.
7. **Don't build ahead.** If it's not in the current phase's scope, don't scaffold "for later" — except where the phase plan explicitly says schema-only.
8. Secrets never in source or version control. `local.properties` / env, and say so when adding one.

## Build notes (this repo)

- No reference architecture repo was provided — conventions below follow this file's rules directly, no external inventory step.
- `second_brain_strategy.md` was not supplied — schema and scope follow the rules in this file and in `SCOPE.md` only.
- `LlmGateway` provider/key source deferred to Phase 2; interface is provider-agnostic in Phase 0/1.
