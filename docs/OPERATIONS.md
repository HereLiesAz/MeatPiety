# Operations

## Build and run

~~~
./gradlew :shared:testAndroidHostTest                  # model tests (JVM)
./gradlew :app:assembleDebug                           # Android debug APK
./gradlew :app:bundleRelease                           # signed AAB when keystore env vars are set
./gradlew :shared:wasmJsBrowserDevelopmentRun          # web build, dev server
./gradlew :shared:wasmJsBrowserDistribution            # web build, static files
~~~

Requires JDK 21. The version comes from `-PversionCode` / `-PversionName`
in CI, or `version.properties` locally.

## Release

Android builds publish through `HereLiesAz/workflows`:
- `android-play-release` publishes to Google Play;
- `android-github-release` publishes to GitHub Releases.

The trigger contracts in `.github/workflows/` are replaced by trackers once
the controller registers them. Do not add repository-local workflow
implementations: new automation goes into `HereLiesAz/workflows`. Describe
new needs in `.github/workflow-request.yml`.

- **Signing:** CI passes `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`
  and `KEY_PASSWORD`. Release builds are minified with R8, and Play needs
  the `mapping.txt` this produces.
- **Version state:** `.version-state.json` is written by the controller.
  Don't edit it by hand.
- **Play Console:** the app `com.hereliesaz.meatpiety` must exist there, with
  the service account granted release access, before API uploads can
  succeed.

## Worker

`worker/` deploys through `cloudflare-worker-deploy` in `HereLiesAz/workflows`
whenever a push to `main` touches `worker/`. The trigger contract is
`.github/workflows/worker-deploy.yml`.

To deploy without a worker change, dispatch `gateway.yml` in
`HereLiesAz/workflows` with:
- `repository`: `HereLiesAz/MeatPiety`
- `source_workflow_path`: `.github/workflows/worker-deploy.yml`

Secrets are held centrally and set on the worker by the deploy profile
(`worker_secrets`):

| Secret | Use |
|---|---|
| `BLS_KEY` | BLS v2 API, so live prices aren't limited by the shared keyless quota |
| `YOUTUBE_KEY` | YouTube subscriber counts (optional) |

## Notifications

- **Android:** a WorkManager periodic job named `milestones` runs about
  daily. Users must grant `POST_NOTIFICATIONS` on Android 13+.
- **Web:** only an installed PWA in a Chromium browser receives
  notifications, through Periodic Background Sync. The schedule covers 60
  days from the last open.
