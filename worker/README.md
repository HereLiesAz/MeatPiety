# Meat Piety worker

A Cloudflare Worker at `https://meatpiety.hereliesaz.workers.dev`.
- Every response is JSON with open CORS.
- Successful responses are cached for a day.
- Failures return `{ "error": "…" }` with a 4xx or 5xx status, and the app
  then leaves that figure out.

## Endpoints

| Path | Returns | Source |
|---|---|---|
| `GET /market` | `{ "prices": { "Chickens": 2.01, … } }`, USD per species unit | BLS CPI average prices (v2 with `BLS_KEY`, v1 otherwise) |
| `GET /fx?currency=EUR` | `{ "currency": "EUR", "perUsd": 0.89 }` | open.er-api.com. Without `currency`, the visitor's country picks it. |
| `GET /followers?platform=…&handle=…` | `{ "followers": 1234 }` | See below |

Platforms for `/followers`:

| Platform | Method | Notes |
|---|---|---|
| `bluesky` | Public API (`app.bsky.actor.getProfile`) | Reliable |
| `mastodon` | `/api/v1/accounts/lookup` on the user's instance | The handle is `name@instance`. Only public DNS names are fetched. |
| `youtube` | YouTube Data API | Needs `YOUTUBE_KEY` |
| `tiktok` | Public profile page | Breaks when TikTok changes its markup |
| `instagram` | Public profile endpoint | Often blocked from Cloudflare |
| `facebook` | Public page text | Most personal profiles hide the count |

X is not supported.

## BLS series

| Species | Series |
|---|---|
| Chickens | APU0000706111 (whole chicken) |
| Turkeys | APU0000706311 (currently returns no data; the app keeps its built-in price) |
| Cows | APU0000703112 (ground beef) |
| Pigs | APU0000FD3101 (pork chops) |
| Egg-Laying Hens | APU0000708111 (eggs, dozen) |
| Dairy Cows | APU0000709112 (whole milk, gallon) |

## Develop

~~~
npm test             # handle and count parsing
npx wrangler dev     # local
~~~

Deploys go through the central workflow; see [docs/OPERATIONS.md](../docs/OPERATIONS.md).
