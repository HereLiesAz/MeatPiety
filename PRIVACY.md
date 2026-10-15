# Privacy Policy — Meat Piety

Effective 11 October 2026. Applies to the Meat Piety Android app
(`com.hereliesaz.meatpiety`) and the Meat Piety web app.

Meat Piety has no accounts, no ads, no analytics and no tracking. It does not
sell, rent or share personal data.

## What stays on your device

These are stored only on your device, in Android app storage or browser
local storage:

- the date you started and your diet (vegan or vegetarian);
- for background notifications:
  - which milestones you've already reached;
  - the number of people your linked accounts are estimated to influence;
  - your currency and exchange rate.

On the web, a cached schedule of upcoming milestones is also kept.

Uninstalling the app, or clearing the site's data, deletes all of it. None of
it is sent anywhere.

## What leaves your device

The app asks Meat Piety's data service (`meatpiety.hereliesaz.workers.dev`,
run on Cloudflare) for three things:

| Request | What it sends | Why |
|---|---|---|
| Retail prices | nothing about you | to price the ledger |
| Exchange rate | your currency code, or nothing (the service then guesses your country from your connection) | to show money in your currency |
| Follower count | the platform and the handle you typed | to estimate your reach |

- **Handles:** they are only looked up if you type them in. The app keeps
  them in memory while it's open and does not save them.
- **Third parties:** to answer a follower lookup, the data service asks that
  platform (Bluesky, Mastodon, YouTube, TikTok, Instagram or Facebook) for the
  public profile of the handle you entered. Prices come from the U.S. Bureau
  of Labor Statistics, and exchange rates from open.er-api.com. Those
  services see a request from the data service, not from you.
- **Caching:** answers are cached for up to 24 hours so repeated lookups
  don't hit the platforms again. The cache holds the public follower count
  for a handle, not who asked for it.
- **Cloudflare:** like any web service, it processes your IP address to
  deliver the response. Meat Piety does not log it or keep it.

## Notifications

Milestone notifications are created on your device: by Android's background
scheduler, or by the browser's service worker for an installed web app. No
push service or server is involved. You can turn them off in your device or
browser settings.

## Permissions (Android)

- **Internet:** to fetch prices, exchange rates and follower counts.
- **Notifications:** to tell you about milestones while you're away. This is
  optional.

## Children

Meat Piety is not directed at children under 13 and collects no personal
information from anyone.

## Changes

Changes to this policy are published here, with a new effective date.

## Contact

Questions: open an issue at https://github.com/HereLiesAz/MeatPiety/issues.
