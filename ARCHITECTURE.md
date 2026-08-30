# ARCHITECTURE.md — Second Brain (Phase 0)

No reference UI repo was supplied for this build (see `CLAUDE.md` → Build notes). Package structure below follows CLAUDE.md's rules directly: `ui → domain → data`, package-by-feature-then-layer, single Gradle module.

**Revision note:** the kickoff now names the ambient overlay (bubble + panel) as the flagship surface, reached through four sanctioned entry points ("doors"), not an excluded feature. This revises the Phase 0 docs written earlier this session, which had explicitly ruled the floating overlay out — see the "four doors" section below and `SCOPE.md`.

## Decisions

Settled calls, recorded so they don't get silently re-litigated:

| Decision | Call | Rationale |
|---|---|---|
| AGP 9 migration | **Deferred** to between Phase 1 and Phase 2 | Requires dropping `org.jetbrains.kotlin.android` for AGP's built-in Kotlin support — a build-foundation rewrite Hilt and KSP both sit on top of. Not worth debugging before a single screen exists; Phase 1 screens ship with full `@Preview` coverage per state regardless, so the eventual migration to Compose Preview Screenshot Testing is additive, not a redo. |
| `minSdk` | **26**, keep | Forced by `TYPE_APPLICATION_OVERLAY`, which Door 3's bubble needs and which itself requires API 26 — not an arbitrary floor. |
| `android:allowBackup` | **`false`** | The Room DB is a private personal archive; free device-swap continuity isn't worth it riding along in the user's Google account backup by default. Revisit only if the app ships an explicit, encrypted export path. |
| Backend | **Supabase**, keep | Managed Postgres+pgvector+Auth+Storage in one, matches "boring managed infra" for a solo nights-and-weekends build. **Re-evaluate if any of:** (a) a Kotlin backend becomes wanted — Edge Functions are Deno/TypeScript, not Kotlin, a real seam if that ever matters; (b) the free-tier 7-day inactivity pause starts costing real dev time (confirmed: Free plan projects pause after 7 days of low DB activity, restorable up to 90 days — Pro removes this); (c) pgvector recall/latency visibly degrades as captured-item count grows (ANN recall on an untuned ivfflat/hnsw index degrades at scale — watch this once Phase 3's Ask is real, not before). |
| Token refresh | **Implemented** (2026-08-30) | `AuthSessionManagerImpl` decodes the stored access token's `exp` claim (`JwtExpiry`, pure JVM — deliberately not `android.util.Base64`, which is stubbed in unit tests) and refreshes proactively with a 60s buffer; `ItemRepositoryImpl.withAuthRetry` catches a reactive 401 (clock skew, out-of-band revocation) and retries once via `invalidateAndRefresh()`. Same pass also fixed a real latent bug found while building this: `insertItem`/`upload` return `Response<Unit>`, which Retrofit does not throw on for non-2xx — the old code never checked `.isSuccessful`, so any real server error was silently recorded as `SYNCED`. |
| Door 3 (bubble) priority | **Timeboxed spike stays; full feature is the first cut under pressure** | Door 1 + Door 2 alone satisfy the literal north-star test (capture → forget → ask → cited answer) with zero special permission. Door 3 adds persistent cross-app presence and frictionless follow-up on the just-captured item — real, but continuity/engagement value, not core-loop necessity. The Phase 3.5 gate's own framing ("that video is also the first ad creative") already names the demo/marketing payoff directly. The spike (platform-risk discovery, `SYSTEM_ALERT_WINDOW` behavior under OEM battery management) stays because it's cheap and answers a real unknown either way. |
| Silent identity-orphan on refresh-token rejection | **Known gap, made detectable (2026-08-30), not fixed** | `AuthSessionManagerImpl.ensureSession()` falls back to a fresh `signInAnonymously()` when a stored refresh token is rejected (revoked/expired) — that mints a **new** user id. Every item this install previously synced belongs to the old id; RLS hides it from the new session, and nothing looked wrong locally (Room still shows the local rows). This is the account-recovery hole the anonymous-auth decision always implied, surfacing here as a silent failure path rather than the expected uninstall/reinstall one. Not fixed — recovery is real UI scope, belongs to the account-upgrade flow below (#2). Made detectable instead: `AuthSessionManagerImpl.persist()` now logs loudly (`Log.e`) on any user-id change and keeps the orphaned id(s) in SharedPreferences (`orphaned_user_ids`) for whenever recovery is built. |

**Open, pending explicit approval — not decided, recorded here so they're not lost:**

1. **Local-first, narrowed (2026-08-30):** not a full reshape (that needed on-device extraction, confirmed not viable at minSdk 26 — AICore/Gemini Nano gates on a current-flagship hardware allowlist, not an OS version). Narrower version: stop persisting `raw_text` in Postgres and delete capture files from Storage after extraction returns; keep `embeddings` persisted (retrieval is unaffected — the vector is computed from full text before deletion). The real cost is Ask's *answer-composition* stage if it's grounded server-side against stored content — proposed mitigation: the app resubmits the relevant items' full Room-local text ephemerally alongside the question at Ask-time (never written to a table), preserving same-device answer quality at the cost of a more complex Ask request shape than "everything's already server-side." Cross-device/reinstall loss of raw text is real and NOT mitigated by that trick — even after the account-upgrade path below restores metadata/briefs/embeddings on a new device, pre-migration items stay summary-only permanently. Proposal only — not decided, not built.
2. **Anonymous→permanent account upgrade flow** (new email+OTP UI, real Phase-4-sized scope). Trigger revised 2026-08-30: **first successful Ask that cites 3+ items, OR 25 items captured**, whichever comes first — the moment the archive is demonstrably worth protecting, not an arbitrary day-count. Non-blocking Home banner, never at onboarding. Not built.

## Package structure

```
com.zackwhye.secondbrain
├── SecondBrainApp.kt                 # @HiltAndroidApp
├── MainActivity.kt                   # hosts NavHost; also Door 1's ACTION_SEND / ACTION_SEND_MULTIPLE target
│
├── core/
│   ├── designsystem/                 # tokens from DESIGN.md: Color.kt, Type.kt, Shape.kt, Motion.kt, Theme.kt, components/ (Card, Chip, SectionHeader, BriefBlock, ChatBubble, Bubble, Panel, NavBar)
│   ├── database/                     # Room only: AppDatabase.kt, entity/ — no business logic. dao/ lands in Phase 1 alongside the screens/repositories that need it
│   ├── network/                      # Supabase client (Postgrest/Storage/Realtime), DTOs, mappers entity<->dto
│   ├── data/                         # repositories: single source of truth, composes database + network
│   │   ├── ItemRepository.kt
│   │   ├── BriefRepository.kt
│   │   ├── ProjectRepository.kt      # schema-only in this build — see Scope
│   │   ├── PersonRepository.kt       # written by sync, not read/displayed in this build
│   │   └── DecisionRepository.kt     # written by sync, not read/displayed in this build
│   ├── model/                        # pure Kotlin domain types: Item, Brief, BriefState, Project, Person, Decision, ItemLink, CapturedContext, SourceDoor
│   └── di/                           # Hilt modules: DatabaseModule, NetworkModule
│
├── feature/
│   ├── capture/
│   │   ├── ui/                       # CaptureConfirmationRoute/Screen (bottom sheet), CaptureViewModel
│   │   └── domain/                   # SaveCapturedItemUseCase(CapturedContext) — the one funnel every door writes through
│   ├── home/
│   │   └── ui/                       # HomeRoute, HomeScreen, HomeUiState, HomeViewModel — no domain layer, passthrough to ItemRepository
│   ├── itemdetail/
│   │   └── ui/                       # ItemDetailRoute, ItemDetailScreen, ItemDetailUiState, ItemDetailViewModel
│   ├── ask/
│   │   └── ui/                       # AskRoute, AskScreen (Phase 1: stub only), AskUiState
│   ├── overlay/                      # Door 3 — planned, not created in Phase 0/1-spike scope goes no further than the feasibility spike
│   │   ├── ui/                       # BubbleService (foreground service), BubblePanelRoute/Screen, BubbleViewModel
│   │   └── domain/                   # BubbleUiState machine: Hidden / Idle(itemId) / Expanded(itemId) / PermissionDenied
│   ├── processtext/                  # Door 2 — Phase 3.5, not created yet
│   │   └── ui/                       # ProcessTextActivity (ACTION_PROCESS_TEXT target), "keep in memory" vs "just answer" prompt
│   └── assist/                       # Door 4 — Phase 3.5, not created yet
│       └── ui/                       # AssistSettingsRoute/Screen (RoleManager request + trade-off copy), SecondBrainVoiceInteractionSessionService
│
└── navigation/                       # SecondBrainNavHost.kt, Destination.kt
```

`feature/home` and `feature/itemdetail` skip a `domain/` package: CLAUDE.md is explicit that a passthrough use case is noise. `feature/capture` gets one because normalizing four different MIME types (and now four *doors*) before persistence is non-trivial and reused. `feature/overlay`, `feature/processtext`, `feature/assist` are named here for the coherence contract below but stay unimplemented until Phase 1 (spike only) and Phase 3.5 (full) — Phase 0 ships no feature code.

## The four doors → `CapturedContext`

One mental model ("my assistant is here"), four sanctioned Android entry points, one funnel. Each door is a *window*, never a sensor — none of them may read another app's screen, notifications, or audio; every chain starts from a user-initiated handoff.

| Door | Entry point | Phase |
|---|---|---|
| 1. Share sheet | `MainActivity` handling `ACTION_SEND` / `ACTION_SEND_MULTIPLE` | 1 |
| 2. Text selection | `ProcessTextActivity` handling `ACTION_PROCESS_TEXT` | 3.5 |
| 3. Bubble | `BubbleService` (`SYSTEM_ALERT_WINDOW` foreground service) | 1 spike / 3.5 feature |
| 4. Assist role | `SecondBrainVoiceInteractionSessionService.onHandleAssist()` / `onHandleScreenshot()` | 3.5, opt-in |

**The coherence contract:** every door's entry point does nothing but construct a `CapturedContext` (`core/model`) and hand it to the same `SaveCapturedItemUseCase` — the one funnel all four doors write through, so the pipeline behind it (Room write → sync → brief → embedding) never needs to know which door an item came from.

```kotlin
enum class SourceDoor { SHARE, PROCESS_TEXT, ASSIST, MANUAL }

data class CapturedContext(
    val door: SourceDoor,
    val sourceType: ItemSourceType,   // url / text / image / pdf
    val sourceUri: String?,
    val rawText: String?,
    val capturedAt: Instant,
)
```

`items.source_door` (schema below) persists which door produced a row — display-only metadata in this build, not branching logic; the pipeline treats every `CapturedContext` identically regardless of door. Door 3's bubble is additionally *fed by* successful captures from doors 1/2 (it appears holding whatever was just captured) — that's a UI-layer concern in `feature/overlay`, not a second funnel.

## Where `LlmGateway` actually sits

**Not in the Android app.** Phase 2 runs extraction from an Edge Function ("new item → `LlmGateway` extraction"), not from the phone. So `LlmGateway` is a **backend-side interface in the Supabase Edge Function runtime** (TypeScript/Deno), swappable there. The Kotlin app never calls an LLM directly — it only ever reads `briefs` rows that sync down through Room, and displays whatever `status` those rows carry. This keeps the app thin (matches the "URL extraction stays server-side" rule) and means there's no `core/llm` Kotlin package in this build.

```
supabase/functions/extract-brief/
├── index.ts            # trigger entrypoint: new item → orchestration
├── llm_gateway.ts       # interface: extract(content: NormalizedContent): Promise<BriefJson>
└── providers/           # one file per provider; Phase 0/1 ships zero providers (deferred, see CLAUDE.md Build notes)
```

## Data model (Room + Postgres, schema rule)

Both schemas carry the same seven tables so no later feature needs a migration. Interpretation note on "created_at/captured_at everywhere": every table gets `created_at` (and `updated_at` where the row is ever mutated); `captured_at` is added only where it's semantically distinct from `created_at` (i.e. on `items`, where the user captured the source at a moment that matters even if the sync `created_at` differs). Flagging this rather than stamping a meaningless `captured_at` on e.g. `people`.

### `items`
| Column | Type | Notes |
|---|---|---|
| id | uuid PK | |
| user_id | uuid | Postgres: FK → auth.users. Room: stored, not enforced |
| source_type | enum(url, text, image, pdf) | |
| source_door | enum(share, process_text, assist, manual) | which of the four doors produced this row — display-only metadata, not branching logic |
| source_uri | text, nullable | original URL, or Storage object path for image/pdf |
| raw_text | text, nullable | shared text, or server-extracted readable text for URLs |
| title | text, nullable | filled once available |
| project_id | uuid, nullable FK → projects | schema-only; nothing writes it in this build |
| sync_state | enum(pending, synced, failed) | **Room-only** — meaningless server-side |
| captured_at | timestamptz | when the user shared it |
| created_at | timestamptz | row-creation time (may lag `captured_at` if sync was offline) |
| updated_at | timestamptz | |

### `briefs`
| Column | Type | Notes |
|---|---|---|
| id | uuid PK | |
| item_id | uuid, unique FK → items | 1:1 |
| status | enum(pending, ready, failed) | maps to `BriefUiState` — never silently absent |
| summary | text, nullable | |
| entities | jsonb / Room: `List<String>` via TypeConverter | |
| topics | jsonb / `List<String>` | |
| tasks | jsonb / `List<String>` | |
| importance | int 1–5, nullable | |
| failure_reason | text, nullable | set when status = failed |
| created_at | timestamptz | |
| updated_at | timestamptz | |

### `embeddings`
| Column | Type | Notes |
|---|---|---|
| id | uuid PK | |
| item_id | uuid FK → items | |
| chunk_index | int, default 0 | MVP: one chunk per item |
| vector | `vector(1536)` (pgvector) | **Postgres-only.** Room entity keeps id/item_id/chunk_index/model/created_at but never stores the vector — no on-device similarity search in this build |
| model | text | embedding model identifier, for future re-embedding |
| created_at | timestamptz | |

### `projects`
| Column | Type | Notes |
|---|---|---|
| id | uuid PK | |
| user_id | uuid | |
| name | text | |
| created_at / updated_at | timestamptz | |

Schema-only: no UI creates, assigns, or displays a project in this build ("auto project assignment" is explicitly out; manual assignment isn't in scope either).

### `people`
| Column | Type | Notes |
|---|---|---|
| id | uuid PK | |
| user_id | uuid | |
| item_id | uuid FK → items | which extraction produced this row |
| name | text | as extracted, no dedup |
| created_at | timestamptz | |

Extraction writes rows here; no screen reads this table in this build.

### `decisions`
| Column | Type | Notes |
|---|---|---|
| id | uuid PK | |
| user_id | uuid | |
| item_id | uuid FK → items | |
| description | text | |
| created_at | timestamptz | |

Same as `people`: written, not surfaced.

### `item_links`
| Column | Type | Notes |
|---|---|---|
| id | uuid PK | |
| user_id | uuid | |
| from_item_id | uuid FK → items | |
| to_item_id | uuid FK → items | |
| link_type | enum(relates_to, supports, contradicts) | |
| created_at | timestamptz | |

Written by nobody in this build. Exists so a future linker feature is additive, not a migration.

## Capture flow

1. A door's entry point resolves into a `CapturedContext` (Phase 1: Door 1 only — `MainActivity` handling `ACTION_SEND`/`ACTION_SEND_MULTIPLE`, MIME-sniffed into `source_type` + `source_uri`/`raw_text`; Doors 2–4 do the same from their own entry points starting Phase 3.5). `SaveCapturedItemUseCase` takes that `CapturedContext` and writes an `Item` draft (`source_type`, `source_door`, `source_uri` or `raw_text`, `captured_at = now`) — one funnel regardless of door.
2. Item is written to Room immediately (`sync_state = pending`) — capture must feel instant and work offline. `feature/capture` shows the confirmation bottom sheet from `DESIGN.md` right away, `BriefUiState = Pending`.
3. `ItemRepository` uploads the row (and, for image/pdf, the binary to Supabase Storage first) in the background; `sync_state` becomes `synced` or `failed` (retryable).
4. A Postgres trigger (or Storage webhook, for binary uploads) invokes the `extract-brief` Edge Function: for `source_type = url` it first runs server-side readable-text extraction, then calls `LlmGateway.extract(...)`, writes the `briefs` row (`status = ready` or `failed` with `failure_reason`), and computes the embedding into `embeddings`.
5. The app syncs `briefs` down (Supabase Realtime channel on `briefs`, or a poll fallback) into Room; Home cards and Item detail re-render once `status` flips from `pending`.

## Extraction JSON schema (Edge Function ⇄ `LlmGateway` contract)

```json
{
  "summary": "string, 1-3 sentences",
  "entities": ["string", "..."],
  "people": ["string", "..."],
  "topics": ["string", "..."],
  "tasks": ["string", "..."],
  "decisions": ["string", "..."],
  "importance": 1
}
```

`importance` is an integer 1–5. `people` rows become `people` table inserts, `decisions` become `decisions` table inserts; everything else lands in the `briefs` row's own columns. The Edge Function validates this shape before writing — malformed output is a `failed` brief with a `failure_reason`, never a partially-written row.
