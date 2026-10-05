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
- Kotlin Multiplatform 2.4.20, Compose Multiplatform 1.9.0
- Jetpack Compose / Material 3
- compileSdk / targetSdk 37
- minSdk 28
- JDK 21 (matches the release workflow)

## Running the web build

```
./gradlew :shared:wasmJsBrowserDevelopmentRun
```

## Tests

~~~
./gradlew :shared:allTests
~~~

Calculator logic lives in `shared/src/commonTest`.

## Workflows

Do **not** invent repository-local workflow implementations. Choose existing automation or describe a new generalized capability in `.github/workflow-request.yml`. New implementations belong in `HereLiesAz/workflows`.

### Release

Android builds publish through `android-release` in `HereLiesAz/workflows` (Google Play +
GitHub Releases), requested in `.github/workflow-request.yml`. CI passes `-PversionCode` /
`-PversionName` and the upload key as `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`,
`KEY_PASSWORD`; local builds read `version.properties`.

`.version-state.json` is written by the central controller (`HereLiesAz/workflows`
version contract sync); it records the last synced version and is not edited by hand.
