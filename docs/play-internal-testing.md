# Play Console — internal testing setup

Status as of 2026-09-01. Repo side is done; the Play Console side needs the account owner.
Verified against current Play requirements: new apps submitted after 2026-08-31 must target
API 36 — this app targets 36.

## Already done in the repo

- `app/build.gradle.kts`: `applicationId com.zackwhye.secondbrain` (permanent once uploaded),
  `targetSdk 36`, `versionCode 1`, `versionName "0.1.0"`.
- Signed release AAB: `./gradlew :app:bundleRelease` →
  `app/build/outputs/bundle/release/app-release.aab`, signed with the keystore referenced from
  `local.properties` (never committed). On first upload Google Play enrolls the app in **Play App
  Signing**: this keystore becomes the *upload key*, Google holds the app signing key. Losing the
  upload key is recoverable (key reset request); treat `~/.android/secondbrain-release.jks` and its
  passwords as the thing to back up anyway.
- Version bumping: every new upload needs a strictly higher `versionCode` (2, 3, …).
  `versionName` is free-form for humans.
- Data safety declaration draft: below. Privacy policy draft: `docs/privacy-policy.md`.

## Zack does this (in order)

1. **Developer account** — play.google.com/console, one-time US$25, identity verification
   (takes up to a couple of days). Note: a *personal* account created after 2023-11-13 must later
   pass a closed test with **12 testers opted in for 14 consecutive days** before it can apply for
   *production* access. Internal testing is not gated by this and supports up to 100 testers.
2. **Create app** — name "Second Brain", default language, App (not game), Free.
3. **Internal testing track** — Testing → Internal testing → create release → upload
   `app-release.aab` → accept Play App Signing enrollment. No review cycle; live for testers in
   minutes.
4. **Testers** — create an email list (up to 100 Google accounts), share the opt-in link Play
   generates. Testers tap the link once, then install from the Play Store like any app — no MIUI
   sideload dialog.
5. **Store listing minimums** (internal testing tolerates a draft listing, but fill these once —
   they are required before closed testing):
   - App name, short description (≤80 chars), full description (≤4000 chars).
   - App icon 512×512 PNG. Current launcher icon is the default placeholder — export it or any
     interim 512px asset; the good icon stays on the stranger list.
   - Feature graphic 1024×500, at least 2 phone screenshots (use the pilot screenshots).
   - Category (Productivity), contact email.
   - Privacy policy URL — host `docs/privacy-policy.md` anywhere public (GitHub Pages is fine)
     and paste the URL.
6. **App content section** — ads declaration (none), content rating questionnaire, target
   audience (18+, not child-directed), Data safety form (**exempt while the app is only on the
   internal track; mandatory before closed/open/production** — answers below).
7. **Crash reporting** — nothing to set up: Android vitals collects crashes/ANRs automatically
   for Play-installed builds. Quality → Android vitals in the console.

## Data safety form — what we declare (accurate, not minimised)

The honest model of this app: **everything a user shares into it leaves the device.** Captured
content is stored in Supabase (Postgres + Storage) under an anonymous per-install identity, and
is sent to two AI providers for processing: Anthropic (Claude API — summarisation, fact
extraction, answer generation) and OpenAI (embeddings API — semantic search vectors). Both are
service providers processing on our behalf; under Play's definitions this is *collection* (data
leaves the device) with processing by service providers, not "sharing" in the sell/ad sense. No
ads, no analytics SDKs, nothing sold.

Declare **collected** (all: purpose = App functionality, encrypted in transit = yes,
optional = yes — the user chooses every share):

| Play category | What it actually is |
|---|---|
| Personal info → Name | Names of people appearing in shared content; the app deliberately extracts person-scoped facts about them |
| Personal info → User IDs | Anonymous per-install account ID (Supabase anonymous auth) |
| Photos and videos → Photos | Screenshots/images the user shares in |
| Files and docs | PDFs the user shares in |
| Other → Other user-generated content | Shared text and links; AI-generated briefs and facts derived from them |
| App activity → In-app search history | Questions typed into Ask (sent to the backend, embedded via OpenAI, answered via Anthropic) |

Handling answers:
- **Encrypted in transit:** yes (TLS everywhere).
- **Ephemeral processing:** no — content is stored server-side.
- **Data deletion:** partial. Deleting an item in-app deletes it and its derived data
  server-side (implemented this release). There is **no account-level deletion flow** — declare
  "no" for user-initiated account data deletion, or ship account wipe before closed testing.
- **Third parties (name them in the privacy policy):** Supabase (hosting), Anthropic (Claude
  API; API inputs/outputs not used for model training per their commercial terms), OpenAI
  (embeddings API; API data not used for training by default).

Two honesty flags, decide before closed testing (both out of scope now, capability frozen):
1. Play's **account deletion requirement** applies to apps that let users create accounts. Our
   anonymous auto-identity is a grey area, but the safe reading says we need an in-app
   "delete everything" + a web deletion-request link before production.
2. The first-run line says uninstalling "permanently deletes" your data. From the user's
   standpoint it's unrecoverable (the anonymous identity is lost), but the rows are orphaned in
   Supabase, not erased. The privacy policy states this accurately; consider rewording the
   first-run line or adding an orphan-purge job later.
