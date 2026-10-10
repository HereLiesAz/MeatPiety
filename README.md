# Meat Piety

A calculator for vegans and vegetarians: enter how many days you've lived
that way and it tallies the animals a standard U.S. diet would otherwise
have put through the meat, dairy, and egg industries. It also projects that
math onto your Facebook friends and the rest of the world's vegans and
vegetarians — and it keeps a separate, backward line for buffalo, whose
numbers survive on ranching demand rather than despite it.

The calculator itself — `shared/src/commonMain` — is Kotlin Multiplatform
Compose, built once and run on both Android and web.

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
