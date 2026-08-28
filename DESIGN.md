# DESIGN.md — Second Brain design tokens

Source: tokens picked via `ui-ux-pro-max` design-system search (query: "productivity knowledge tool SaaS dark mode minimal professional trustworthy"), adapted to Material 3 roles. `second_brain_mvp_prototype.html` used only as layout/screen-inventory reference for Home / capture-confirmation / item detail / Ask (tokens extracted, markup not copied, per CLAUDE.md's UI workflow).

**Revision note:** an earlier pass of this doc ruled the floating overlay out of scope and treated the prototype's ambient-capture screen as rejected. The kickoff has since named the bubble the flagship surface (see `ARCHITECTURE.md` → "The four doors"). Two additional references were supplied for it — `reference_door2_text_selection.webp` and `reference_door3_bubble_card.webp` — and drive the Bubble/Panel spec below in place of the old prototype's always-on overlay screen.

## Visual references

| File | What it shows | Informs |
|---|---|---|
| `second_brain_mvp_prototype.html` | Home, item detail, Ask layouts | Card, chip, section header, chat bubble tokens |
| `reference_door2_text_selection.webp` | A custom action icon inline in the OS text-selection toolbar, alongside copy/select-all | Door 2 — confirms the entry point is the *existing* selection toolbar, not a new UI surface; nothing to design here beyond the icon itself |
| `reference_door3_bubble_card.webp` | A bottom-anchored card over the source app: source chip (app icon + label) top-left, dismiss `✕` top-right, a suggested prompt row with mic/send below | Panel shape, source-chip pattern, and dismiss affordance in the Bubble/Panel spec below |

## Rationale

Rejected the skill's literal "calm" match (teal/Lora/Raleway — wellness-app mood, wrong for an intelligence tool). Picked the Indigo "Micro SaaS" palette + Inter ("Minimal Swiss") pairing: indigo reads as focused/tech without the coldness of pure grey-blue enterprise tone, Inter is neutral and renders cleanly at Android's default text scale, and both hold contrast in a Material 3 dynamic light/dark scheme.

## Color roles (Material 3 mapping)

| Material 3 role | Light | Dark |
|---|---|---|
| primary | `#6366F1` | `#B4B8FF` |
| onPrimary | `#FFFFFF` | `#1E1B4B` |
| primaryContainer | `#E0E7FF` | `#3730A3` |
| onPrimaryContainer | `#1E1B4B` | `#E0E7FF` |
| secondary (accent/CTA) | `#059669` | `#4ADE80` |
| onSecondary | `#FFFFFF` | `#0B2A1C` |
| background | `#FAFAFC` | `#131320` |
| onBackground | `#1E1B4B` | `#E7E7F5` |
| surface | `#FFFFFF` | `#1C1B2E` |
| onSurface | `#1E1B4B` | `#E7E7F5` |
| surfaceVariant / muted | `#EBEFF9` | `#272538` |
| onSurfaceVariant (muted text) | `#64748B` | `#9A99B8` |
| outline / border | `#E0E7FF` | `#38364F` |
| error | `#DC2626` | `#F87171` |
| onError | `#FFFFFF` | `#450A0A` |

Brief-importance accent (non-semantic, used only for chips/badges, never as the sole signal — always paired with text/icon): amber `#D97706` / dark `#FBBF24`.

## Typography

Single family: **Inter** (400/500/600/700). No heading/body pairing split — one font, weight carries hierarchy, matches Android's own type-role approach and avoids a second web-font fetch on-device (Inter ships as a bundled variable font resource).

Type scale (Material 3 roles, sp):

| Role | Size | Weight | Line height |
|---|---|---|---|
| displaySmall (item title, big) | 32 | 700 | 40 |
| titleLarge (screen title) | 24 | 700 | 32 |
| titleMedium (card title) | 16 | 600 | 24 |
| bodyLarge | 16 | 400 | 24 |
| bodyMedium (card summary) | 14 | 400 | 20 |
| labelLarge (buttons) | 14 | 600 | 20 |
| labelSmall (chips, meta, section headers) | 12 | 500 | 16 |

## Spacing

4dp base unit, used in 4/8/12/16/24/32 increments. Card internal padding 16dp. Card-to-card gap 12dp. Screen horizontal margin 16dp.

## Shape

| Element | Radius |
|---|---|
| card | 20dp |
| chip | 999dp (pill) |
| button | 14dp |
| bottom sheet (capture confirmation) | 24dp top corners |
| text input / search bar | 16dp |

## Elevation

Material 3 tonal elevation, 3 tiers only:
- Level 0 — screen background, list rows resting state
- Level 1 — resting cards (`surface` + 1dp tonal overlay, no drop shadow in dark theme)
- Level 3 — bottom sheet / modal (scrim `#000000` at 48% light / 60% dark, per ui-ux-pro-max scrim guidance)

## Motion

150–250ms, ease-out on enter / ease-in on exit (Material 3 defaults). Card press: scale 0.98. Bottom sheet: slide up + fade, exit at ~65% of enter duration. Respect `Configuration` reduced-motion (system animator scale) — no required-motion affordances.

## Iconography

Material Symbols (outlined, 24dp, 1.5–2px optical stroke — ship as a single consistent set, not mixed with any emoji). Filled variant only for the active bottom-nav item.

## Component inventory (from prototype's 4-screen set, remapped to this build's scope)

1. **Capture card** (Home list row) — leading icon-in-dot (type glyph: link/image/pdf/text), title, one-line summary, topic chip row, relative timestamp.
2. **Section header** — label-small, uppercase-tracked, muted.
3. **Search bar** — pill, muted-fill, leading search icon, tappable → Ask.
4. **Capture confirmation sheet** (replaces prototype's floating overlay — presented as a standard bottom sheet after a share-intent completes, not an always-on overlay) — mini eyebrow label, title, 1–2 line pending/summary state, action row (view / dismiss).
5. **Brief block** (Item detail) — tinted `primaryContainer` panel, eyebrow label, insight text; states Pending/Ready/Failed(retryable) render as three distinct visual variants of this block, never a blank space.
6. **Evidence/source card** — same shape as capture card, used for citation targets.
7. **Chat bubble** (Ask) — `ai` bubble on `surfaceVariant`, `me` bubble on `primary`-tinted dark fill; citation chip embedded inline, tappable, opens source item.
8. **Bottom nav** — 3 items only for this build's scope (Home / Ask / Projects placeholder-empty per schema rule); Material 3 `NavigationBar`.
9. **Bubble** (Door 3, Phase 1 spike / Phase 3.5 feature) — 56dp circle, `primary` fill, elevation level 3, a single neutral glyph (not a face — per CLAUDE.md branding rule), draggable, docks to screen edge when idle. States map 1:1 to `BubbleUiState`: `Hidden` (not rendered), `Idle(itemId)` (plain glyph), `Expanded(itemId)` (glyph replaced by the Panel below), `PermissionDenied` (glyph shown with a small warning dot — never just absent, so the user can see why nothing happened). State is never color-only: each state also changes the glyph/dot, not just a tint.
10. **Panel** (Door 3's expanded state) — bottom-anchored card, same 24dp-top-corner sheet shape as the capture-confirmation sheet, anchored near the bubble rather than full-width. Per `reference_door3_bubble_card.webp`: a **source chip** top-left (small app-type icon + one-line label — "YouTube video", "Shared text" — reusing the capture card's `.dot` glyph pattern), a dismiss `✕` top-right, then the brief/ask content, then a prompt row (text field + mic + send) at the bottom. This is the same `Ask` input bar and `Brief block` components from screens 3–4, re-hosted in a floating panel — no parallel UI stack.

## Dark mode

Designed alongside light, not inverted: dark surfaces use desaturated indigo tints (`#1C1B2E`, `#272538`) rather than pure black or a hue-flipped primary; primary shifts to a lighter tint (`#B4B8FF`) to hold 4.5:1 against dark surfaces per WCAG AA.
