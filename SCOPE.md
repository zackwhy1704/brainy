# SCOPE.md — Second Brain

**Revision note:** the flagship surface is now the ambient overlay reached through four doors (Share sheet, Text selection, Bubble, Assist role) — see `ARCHITECTURE.md` → "The four doors". This replaces an earlier pass of this doc, which excluded the floating overlay entirely. Phase 1 gains an overlay feasibility spike; a new Phase 3.5 wires up the remaining three doors.

## In scope: Phases 1–3.5

**Phase 1 — Design system, capture, and the overlay spike**
- `core/designsystem`: tokens from `DESIGN.md`, typography, shapes, light/dark themes.
- Home screen (list, newest first), Item detail (raw content), stub Ask tab. Stateless composables + `Route` wrappers, previews for Loading/Error/Empty/Ready/dark.
- **Door 1:** Android share `intent-filter` for text, URL, image, PDF → item persists to Room → syncs to Supabase. Auth: simplest viable (magic link / anonymous, decided at Phase 1 kickoff).
- URL items: server-side readable-text extraction via Edge Function; the app stays thin.
- Screenshot tests recorded for every preview state.
- **Overlay feasibility spike** (timeboxed to one session, no product logic): foreground service, draggable neutral glyph via `SYSTEM_ALERT_WINDOW`, tap expands a static Compose panel, survives app-switch and rotation, graceful path when permission is denied. Document behavior after device sleep and under OEM battery optimization — this is platform-risk discovery, not the Door 3 feature (that's Phase 3.5).
- Gate: on a physical device, share a URL from Chrome and a screenshot from Photos — both appear, synced; AND the spike bubble persists across app switches, with a written note on every OEM/battery issue observed. Report separates "emulator" from "device".

**Phase 2 — The brief**
- Edge Function: new item → `LlmGateway` extraction → store brief → embedding → pgvector.
- Item detail renders the brief; Home cards show title, one-line summary, topic chips.
- Brief states are explicit: `Pending`, `Ready`, `Failed(retryable)`. Never silently absent.
- ViewModel tests for all three states plus retry.
- Gate: 10 real mixed-type items produce sane briefs. Paste 3 input→output JSON examples.

**Phase 3 — Ask with citations**
- Question → embed → pgvector top-k → answer constrained to retrieved items → tappable citations opening the source item.
- No relevant retrieval → say so explicitly. Never answer from model knowledge without a citation.
- Gate: the north-star test, run by the user, on their own captured data.

**Phase 3.5 — The ambient assistant (flagship)**

Wires the remaining doors into the loop built in Phases 1–3. Build order: Door 2, then Door 3, then Door 4.

- **Door 2 — `ACTION_PROCESS_TEXT`:** selected text in any app → our activity → capture + brief, with an "ask about this" input. Includes a "keep in memory" vs "just answer" choice — not every selection deserves permanent storage.
- **Door 3 — bubble as product:** after any successful capture the bubble appears holding that item as context. Tap → panel showing the brief plus an ask input running the Phase 3 stack scoped to that item, with citations. Quick actions: save to existing project label, dismiss. Bubble state machine explicit and tested: `Hidden`, `Idle(itemId)`, `Expanded(itemId)`, `PermissionDenied`.
- **Door 4 — assist role, opt-in:** a settings screen explaining the trade-off plainly ("this replaces Gemini as your assistant"), requesting the role via `RoleManager`. On invoke, `onHandleAssist()` context becomes a `CapturedContext` and flows through the same pipeline. Detects and surfaces role loss after app updates (the role is known to clear on reinstall) rather than failing silently.
- Permission requests are contextual, never at onboarding: overlay permission after the first successful share; assist role only from settings. The app must be fully functional with every optional permission denied.
- Overlay UI reuses the app's stateless composables and ViewModels — no parallel UI stack.
- Gate: filmed in one continuous take on device — highlight a message in a chat app → Process Text → bubble appears → ask about it → cited answer.

## Explicitly NOT in this build

Copied verbatim from the kickoff:

> "What Changed?" digest · "Connect This" / contradiction detection · auto project assignment · calendar/reminder/task actions · widgets · voice capture · WhatsApp/Telegram/Slack/any messenger API ingestion · on-device model bridges · MCP/tool bus · knowledge-graph UI · overlay answers drawing on the whole archive ("what does this mean for my project X" — Phase 4; it needs an accumulated archive to be honest).

**Also explicitly ruled out, regardless of phase:** the accessibility-service route to any of this. Reading another app's screen via `AccessibilityService` for data collection violates Play policy and is grounds for removal — if any task seems to need it, stop and ask.

The one exception is the schema rule (`ARCHITECTURE.md` → Data model): `items` (incl. `source_door`), `briefs`, `embeddings`, `projects`, `people`, `decisions`, `item_links` and `created_at`/`captured_at` timestamps exist now as empty structure. Extraction stores `people` and `decisions` rows. Nothing links, dedupes, or displays them in this build.

## Phase 4 preview (not built, on the record)

- Surface `item_links` in the UI: a "related items" section on Item detail, populated by whatever future linker logic decides to write rows there.
- A digest/"what changed" view over accumulated `briefs` + `item_links`, once there's enough real captured history to make one meaningful.
- Overlay answers that draw on the whole archive, not just the item that opened the panel — needs an accumulated archive to be honest, hence deferred past 3.5.
- Manual project assignment UI (the `projects` table already exists; auto-assignment stays out).
- On-device model routing behind `LlmGateway`, if a provider is added there later — the interface boundary from Phase 0 exists specifically so this is additive.
