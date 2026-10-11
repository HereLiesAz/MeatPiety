# Meat Piety

A calculator for vegans and vegetarians: enter how many days you've lived
that way and it works out what that did to each animal's market and
population — meat not bought, the price drop it causes, how much of the slack
cheaper prices tempt someone else into eating, the animals actually never
raised, the breeders retired and births never needed. It runs the same model
for the people you've influenced (only once you link a social account) and
for the world's vegans and vegetarians, and keeps a backward line for buffalo,
whose numbers survive on ranching demand rather than despite it.

The calculator itself — `shared/src/commonMain` — is Kotlin Multiplatform
Compose, built once and run on both Android and web.

## Model

`shared/src/commonMain`:

- `Species.kt` — per-animal data: retail yield, U.S. and world per-capita
  consumption, price, demand/supply elasticities, producer margin, and
  reproduction (birthing size, offspring per breeder, gestation, slaughter age).
- `Economics.kt` — a causal chain in time: skipped purchases of animals already
  bred → short-run price drop (supply fixed) → others buy them → breeding cut →
  births fall after gestation → slaughter falls after gestation + slaughter age →
  long-run price. One global constant-elasticity market per species. A layer
  removes demand; the new equilibrium gives the price change, rebound
  consumption, net animals not raised, breeders not kept, birthings not
  needed, producer revenue lost, and animals still alive: those already born
  for that demand whose slaughter is postponed while breeding cuts work
  through (a day's share per elapsed day, capped at slaughter age).
- `projections()` (Economics.kt) — your ledger 1, 5, 10 and 25 years on, and
  one year of world abstention at that horizon's headcount, assuming 3% yearly
  growth (`ABSTAINER_GROWTH`, a stated guess).
- `Footprint.kt` — CO₂e, land and water per kg (Poore & Nemecek 2018),
  applied to net production cut, so it follows the same chain.
- `Influence.kt` — linked handles → follower counts → reached (organic reach
  per platform, overlap-discounted) → persuaded → still abstaining (16%).
  No linked account with a readable count, no influence chapter.
- `Money.kt` — prices are U.S. figures in USD, converted to the user's
  national currency (device locale on Android, visitor country on web).
- `Milestones.kt` — thresholds the ledger crosses. `Notifier` only notifies
  while the user is away: in the app it saves the ledger and marks every crossed milestone as known; a daily WorkManager job
  advances the day count and notifies for milestones crossed since. Opening
  the app or changing values never notifies. The web build doesn't notify.
- `Storage.kt` — the start date the user picks (Material date picker); days
  are derived from it, so the count advances daily. Diet is remembered too.
- `LiveData.kt` — client for the worker; everything falls back to the
  built-in figures offline.

## Worker

`worker/` is a Cloudflare Worker at `meatpiety.hereliesaz.workers.dev`,
caching each answer for a day:

- `/market` — BLS average retail prices (USD).
- `/fx` — USD exchange rate (open.er-api.com).
- `/followers?platform=&handle=` — Bluesky and Mastodon via their public
  APIs, YouTube via the Data API (`YOUTUBE_KEY` secret), TikTok, Instagram
  and Facebook by reading public profile pages. Scraped platforms can block
  the worker or hide counts; the app then leaves that account out. X is not
  supported.

Pushes to `main` touching `worker/` deploy it through `cloudflare-worker-deploy`
in `HereLiesAz/workflows` (trigger contract `.github/workflows/worker-deploy.yml`).
Locally:

~~~
cd worker && npm test
~~~

## Modules

- `app` — Android entry point (`MainActivity`), depends on `shared`.
- `shared` — the calculator and UI (`commonMain`), plus a `wasmJs` target
  that renders it in the browser via Compose Multiplatform for Web.

## Stack

- Android Gradle Plugin 9.4.1
- Kotlin Multiplatform 2.4.20, Compose Multiplatform 1.12.1, Gradle 9.6.1
- Jetpack Compose / Material 3
- compileSdk / targetSdk 37
- minSdk 28
- JDK 21 (matches the release workflow)

## Design

Scrollytelling chapters ending on a bento recap: parallax field hero with an
interactive variable-type title (drag to sculpt weight, width and slab serif),
counting headlines, log-scaled bars, a pictogram, ripple and timeline figures.
Earth palette in `Theme.kt`; type is **Azrienoch** (variable: `wght`, `wdth`,
`SERF`, `GRAD`; SIL OFL 1.1, `shared/OFL-azrienoch.txt`) in `Type.kt`; motion
primitives in `Motion.kt`, figures in `Visuals.kt`. All motion is disabled when
the platform requests reduced motion.

Distressed editorial treatment is decorative only: `Visuals.kt` provides
`Modifier.inkPatina()` with deterministic, size-cached ink erosion painted
behind content. The field hero adds antique-gold halo/registration lines;
the hero, buffalo eulogy, and bento surfaces carry restrained weathering.
Gold denotes ornament, clay denotes loss, and greens retain their ledger
meaning. Inputs, labels, animated counts, and graph values remain crisp.

### Android launcher icon

The Android launcher uses the full-bleed cow artwork in
`app/src/main/res/drawable-nodpi/ic_launcher_art.webp` (512 × 512).
`mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml` use
that image as the adaptive background and a transparent foreground.
No rounded corners are baked into the asset; the device launcher applies
its own adaptive mask and can crop the image's outermost details.
`drawable/ic_stat_meatpiety.xml` is a single-colour cow head used as the
notification status-bar icon and, inset, as the adaptive icon's
`<monochrome>` layer for themed launchers.
The manifest declares both `android:icon` and `android:roundIcon`.

## Running the web build

```
./gradlew :shared:wasmJsBrowserDevelopmentRun
```

## Tests

~~~
./gradlew :shared:testAndroidHostTest
~~~

Calculator logic lives in `shared/src/commonTest`.

## Workflows

Do **not** invent repository-local workflow implementations. Choose existing automation or describe a new generalized capability in `.github/workflow-request.yml`. New implementations belong in `HereLiesAz/workflows`.

### Release

Android builds publish through `android-play-release` (Google Play) and
`android-github-release` (GitHub Releases) in `HereLiesAz/workflows`; the trigger contracts are
in `.github/workflows/` and the controller replaces them with trackers. CI passes `-PversionCode` /
`-PversionName` and the upload key as `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`,
`KEY_PASSWORD`; local builds read `version.properties`.

`.version-state.json` is written by the central controller (`HereLiesAz/workflows`
version contract sync); it records the last synced version and is not edited by hand.
