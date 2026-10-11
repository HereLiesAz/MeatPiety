# Architecture

## Modules

| Module | Role |
|---|---|
| `app` | Android entry point: `MainActivity` initialises storage and notifications, asks for notification permission, and hosts `MeatPietyApp()`. |
| `shared` | The calculator and UI. Compose Multiplatform for Android and Wasm (web). |
| `worker` | Cloudflare Worker: prices, exchange rates, follower counts. See [worker/README.md](../worker/README.md). |

## `shared/src/commonMain`

| File | What it holds |
|---|---|
| `Species.kt` | Species data, reproduction (`Biology`), diet filter, abstainer counts |
| `Population.kt` | UN population curve, `marketFactor`, `pastMarketFactor`, `currentYear` |
| `Economics.kt` | `impact`, `Layer`, `layerOf` (dairy–beef), the three layers, `personalRange`, `projections`, growth scenarios |
| `Consolidation.kt` | Factory-farm shares and the margin-squeeze shift |
| `Footprint.kt` | Per-kg footprints, replacement food, net environment |
| `Outlook.kt` | Global herds, reservoirs, `herdOutlook`, soy and tofu |
| `Influence.kt` | Platforms, follower lookups, persuasion funnel |
| `Milestones.kt` | Threshold notices |
| `Notifier.kt` | Background-only notification contract; `upcomingNotices` for the web |
| `Storage.kt` | Key-value store, start date, date labels |
| `LiveData.kt` | Worker client (Ktor); falls back to built-ins offline |
| `Money.kt` | Currency formatting and conversion |
| `Format.kt` | Number formatting, whole-animal phrasing |
| `MeatPietyApp.kt` | The screen: chapters, rows, pickers, recap |
| `Theme.kt`, `Type.kt`, `Motion.kt`, `Visuals.kt` | Palette, Azrienoch variable type, motion, figures |
| `Precedents.kt` | The historical timeline |

## Platform code

| Concern | Android (`androidMain`) | Web (`wasmJsMain`) |
|---|---|---|
| Storage | SharedPreferences (`AppContext`) | `localStorage` |
| Currency | Device locale | Worker infers it from the visitor's country |
| Notifications | WorkManager daily job + NotificationManager | Service worker + Periodic Background Sync |
| Reduced motion | System animator scale | `prefers-reduced-motion` |

## Data flow

1. The start date and diet are loaded from storage, and `days` is derived.
2. `LiveData` fetches prices and the exchange rate; built-ins are used until
   they arrive.
3. Linked handles are looked up through the worker, and
   `influenceFunnel` → `influenceLayer` runs only if a count returns.
4. `personalLayer`, `worldLayer`, `projections` and `herdOutlook` compute
   everything shown.
5. `milestones()` produces notices. `Notifier.foreground` records them as
   known, and the web build also schedules upcoming ones for its service
   worker.

## Screen order

1. Hero.
2. Your terms: diet and start date.
3. Your reach: handles.
4. Chapters:
   - 01 Your ledger;
   - 02 Your circle (only with a linked account);
   - Meanwhile, out there.
5. If you keep going: the growth scenario picker.
6. How long they last.
7. The feed was the soy.
8. The bison eulogy.
9. What happens to the price.
10. What happens to the animal.
11. This has happened before.
12. What this doesn't count.
13. Recap.

## PWA

`shared/src/wasmJsMain/resources/` holds:
- `index.html` (registers `sw.js`);
- `manifest.webmanifest`;
- `sw.js`: an offline shell cache, plus milestone notifications from the
  cached schedule;
- `icons/`: any-purpose, maskable and a monochrome badge.

## Tests

`shared/src/commonTest` runs on the JVM:

~~~
./gradlew :shared:testAndroidHostTest
~~~

It covers the market chain, lifetimes and males, cull waves, seafood,
dairy–beef, ranges, footprint, soy, the herd outlook, population,
influence, milestones and scheduling.
