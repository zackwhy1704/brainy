# ARCHITECTURE.md — Second Brain (Phase 0)

No reference UI repo was supplied for this build (see `CLAUDE.md` → Build notes). Package structure below follows CLAUDE.md's rules directly: `ui → domain → data`, package-by-feature-then-layer, single Gradle module.

**Revision note:** the kickoff now names the ambient overlay (bubble + panel) as the flagship surface, reached through four sanctioned entry points ("doors"), not an excluded feature. This revises the Phase 0 docs written earlier this session, which had explicitly ruled the floating overlay out — see the "four doors" section below and `SCOPE.md`.

## Package structure

```
com.zackwhye.secondbrain
├── SecondBrainApp.kt                 # @HiltAndroidApp
├── MainActivity.kt                   # hosts NavHost; also Door 1's ACTION_SEND / ACTION_SEND_MULTIPLE target
│
├── core/
│   ├── designsystem/                 # tokens from DESIGN.md: Color.kt, Type.kt, Shape.kt, Motion.kt, Theme.kt, components/ (Card, Chip, SectionHeader, BriefBlock, ChatBubble, Bubble, Panel, NavBar)
│   ├── database/                     # Room only: AppDatabase.kt, entity/, dao/ — no business logic
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
