# Meat Piety

A ledger of the dead who stayed that way, on your account.

Meat Piety works out what not eating animals has actually done. Your
abstention was not a moral feeling; it was a demand shock. Pick the day you
stopped, and it follows the shock all the way down:

- **The market.** Meat you didn't buy, the price drop it caused, and the
  cheaper meat someone else ate because of it.
- **The animals.** The ones never bred. That includes whole litters and
  clutches, the replacement breeders who were never born, the male chicks
  who were never hatched to be killed, and the calves never sent to veal.
- **The farms.** The squeeze that closes small farms first, which leaves the
  animals still bred in factory barns.
- **The land.** Emissions, cleared land and water spared, net of the tofu
  eaten instead.
- **The future.** Herds projected against UN population curves, and what
  would outlast farming if it stopped.

The same model runs for the people you've influenced, once you link a social
account, and for every vegan and vegetarian alive. Bison keep a backward
line: they survive because people eat them.

Every number is sourced, ranged where it's uncertain, and honest about what
it can't count. Live U.S. retail prices are converted to your currency.
Notifications arrive only while you're away. It runs on Android and as an
installable web app.

The calculator itself — `shared/src/commonMain` — is Kotlin Multiplatform
Compose, built once and run on both Android and web.

## Documentation

| Document | Covers |
|---|---|
| [docs/MODEL.md](docs/MODEL.md) | How every number is calculated: layers, baselines, the market chain, herd outlook, footprint, soy, influence, notifications, what isn't counted |
| [docs/SOURCES.md](docs/SOURCES.md) | Every input with its value, source and confidence; what is still unsourced |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Modules, files, platform code, data flow, screen order, PWA, tests |
| [docs/OPERATIONS.md](docs/OPERATIONS.md) | Building, releasing, deploying the worker, secrets, notifications |
| [worker/README.md](worker/README.md) | Worker endpoints, platforms, BLS series |
| [docs/PLAY_LISTING.md](docs/PLAY_LISTING.md) | Google Play title, short and full descriptions |
| [PRIVACY.md](PRIVACY.md) | Privacy policy (also served by the web build as `privacy.html`) |

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

~~~
./gradlew :shared:wasmJsBrowserDevelopmentRun
~~~

The web build is an installable PWA; see [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#pwa).

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
