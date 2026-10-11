// MeatPiety worker. Every response is JSON, cached for a day, and CORS-open
// for the web build. Failures return {error} with a 4xx/5xx status; the app
// then leaves that figure out rather than inventing one.

const DAY = 86_400;
const UA =
  "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0 Safari/537.36";

// BLS CPI average retail prices (U.S. city average), keyed by the app's species names.
export const BLS_SERIES = {
  Chickens: "APU0000706111", // whole chicken, per lb
  Turkeys: "APU0000706311", // whole frozen turkey, per lb
  Cows: "APU0000703112", // ground beef, 100% beef, per lb
  Pigs: "APU0000FD3101", // pork chops, all, per lb
  "Egg-Laying Hens": "APU0000708111", // eggs, grade A large, per dozen
  "Dairy Cows": "APU0000709112", // milk, whole, per gallon
};

// Country → currency for visitors whose device doesn't say (the web build).
// Unlisted countries fall back to USD.
const EURO = "AT BE CY DE EE ES FI FR GR HR IE IT LT LU LV MT NL PT SI SK".split(" ");
const COUNTRY_CURRENCY = {
  ...Object.fromEntries(EURO.map((c) => [c, "EUR"])),
  US: "USD", GB: "GBP", CA: "CAD", AU: "AUD", NZ: "NZD", JP: "JPY", CN: "CNY", IN: "INR",
  KR: "KRW", BR: "BRL", MX: "MXN", AR: "ARS", CL: "CLP", CO: "COP", PE: "PEN", ZA: "ZAR",
  NG: "NGN", KE: "KES", EG: "EGP", IL: "ILS", TR: "TRY", RU: "RUB", UA: "UAH", PL: "PLN",
  CZ: "CZK", HU: "HUF", RO: "RON", BG: "BGN", SE: "SEK", NO: "NOK", DK: "DKK", IS: "ISK",
  CH: "CHF", TH: "THB", VN: "VND", ID: "IDR", MY: "MYR", SG: "SGD", PH: "PHP", PK: "PKR",
  BD: "BDT", LK: "LKR", AE: "AED", SA: "SAR", HK: "HKD", TW: "TWD",
};

const json = (body, status = 200, maxAge = DAY) =>
  new Response(JSON.stringify(body), {
    status,
    headers: {
      "content-type": "application/json",
      "access-control-allow-origin": "*",
      "cache-control": status === 200 ? `public, max-age=${maxAge}` : "no-store",
    },
  });

class Fail extends Error {
  constructor(message, status = 404) {
    super(message);
    this.status = status;
  }
}

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    // The country decides /fx's default currency, so it is part of the cache key.
    const cacheKey = new Request(
      url.pathname === "/fx" && !url.searchParams.get("currency")
        ? `${url.origin}/fx?country=${request.cf?.country ?? "US"}`
        : url.toString(),
    );
    const cache = caches.default;
    const hit = await cache.match(cacheKey);
    if (hit) return hit;

    let response;
    try {
      response = json(await route(url, request, env));
    } catch (e) {
      response = json({ error: e instanceof Fail ? e.message : "upstream unavailable" }, e.status ?? 502);
    }
    if (response.status === 200) ctx.waitUntil(cache.put(cacheKey, response.clone()));
    return response;
  },
};

async function route(url, request, env) {
  switch (url.pathname) {
    case "/market":
      return { prices: await blsPrices(env) };
    case "/fx":
      return fx(url.searchParams.get("currency") ?? COUNTRY_CURRENCY[request.cf?.country] ?? "USD");
    case "/followers":
      return {
        followers: await followers(url.searchParams.get("platform") ?? "", url.searchParams.get("handle") ?? "", env),
      };
    default:
      throw new Fail("not found");
  }
}

async function blsPrices(env) {
  const ids = Object.values(BLS_SERIES);
  const res = await fetch(`https://api.bls.gov/publicAPI/${env.BLS_KEY ? "v2" : "v1"}/timeseries/data/`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ seriesid: ids, ...(env.BLS_KEY ? { registrationkey: env.BLS_KEY } : {}) }),
  });
  const body = await res.json();
  if (body.status !== "REQUEST_SUCCEEDED") throw new Fail("price source unavailable", 502);
  const byId = Object.fromEntries(
    body.Results.series.map((s) => [s.seriesID, parseFloat(s.data?.[0]?.value)]),
  );
  return Object.fromEntries(
    Object.entries(BLS_SERIES)
      .map(([name, id]) => [name, byId[id]])
      .filter(([, v]) => Number.isFinite(v) && v > 0),
  );
}

async function fx(currency) {
  if (!/^[A-Z]{3}$/.test(currency)) throw new Fail("bad currency", 400);
  const body = await (await fetch("https://open.er-api.com/v6/latest/USD")).json();
  const rate = body?.rates?.[currency];
  if (!rate) return { currency: "USD", perUsd: 1 };
  return { currency, perUsd: rate };
}

/** Strips URLs and @ so "https://instagram.com/name/" and "@name" both become "name". */
export function cleanHandle(raw) {
  let h = raw.trim();
  try {
    if (/^https?:\/\//i.test(h)) h = new URL(h).pathname.split("/").filter(Boolean).pop() ?? "";
  } catch {
    // Not a URL after all.
  }
  return h.replace(/^@/, "");
}

/** "1.2K followers" → 1200. */
export function parseCount(text) {
  const m = /^([\d.,]+)\s*([KkMmBb]?)$/.exec(text.trim());
  if (!m) return null;
  const n = parseFloat(m[2] ? m[1].replace(/,/g, "") : m[1].replace(/[.,]/g, ""));
  return Math.round(n * ({ k: 1e3, m: 1e6, b: 1e9 }[m[2].toLowerCase()] ?? 1));
}

const SAFE = /^[A-Za-z0-9._-]{1,100}$/;

async function followers(platform, raw, env) {
  const handle = cleanHandle(raw);
  switch (platform) {
    case "bluesky": {
      if (!SAFE.test(handle)) throw new Fail("not a Bluesky handle", 400);
      const res = await fetch(
        `https://public.api.bsky.app/xrpc/app.bsky.actor.getProfile?actor=${encodeURIComponent(handle)}`,
      );
      if (!res.ok) throw new Fail("no such Bluesky account");
      return (await res.json()).followersCount;
    }
    case "mastodon": {
      // @name@instance. The instance is user-supplied, so only public DNS names are fetched.
      const [name, host] = handle.split("@");
      if (!SAFE.test(name ?? "") || !/^(?!\d+\.\d+\.\d+\.\d+$)[a-z0-9-]+(\.[a-z0-9-]+)+$/i.test(host ?? "")) {
        throw new Fail("use @name@instance", 400);
      }
      const res = await fetch(`https://${host}/api/v1/accounts/lookup?acct=${encodeURIComponent(name)}`);
      if (!res.ok) throw new Fail("no such Mastodon account");
      return (await res.json()).followers_count;
    }
    case "youtube": {
      if (!SAFE.test(handle)) throw new Fail("not a YouTube handle", 400);
      if (!env.YOUTUBE_KEY) throw new Fail("YouTube lookups aren't configured yet", 503);
      const res = await fetch(
        `https://www.googleapis.com/youtube/v3/channels?part=statistics&forHandle=@${encodeURIComponent(handle)}&key=${env.YOUTUBE_KEY}`,
      );
      const count = (await res.json()).items?.[0]?.statistics?.subscriberCount;
      if (count == null) throw new Fail("no such YouTube channel");
      return Number(count);
    }
    case "tiktok": {
      if (!SAFE.test(handle)) throw new Fail("not a TikTok username", 400);
      const html = await page(`https://www.tiktok.com/@${handle}`);
      const m = /"followerCount":(\d+)/.exec(html);
      if (!m) throw new Fail("TikTok didn't show a follower count");
      return Number(m[1]);
    }
    case "instagram": {
      if (!SAFE.test(handle)) throw new Fail("not an Instagram username", 400);
      const res = await fetch(
        `https://i.instagram.com/api/v1/users/web_profile_info/?username=${encodeURIComponent(handle)}`,
        { headers: { "user-agent": UA, "x-ig-app-id": "936619743392459" } },
      );
      const count = res.ok ? (await res.json())?.data?.user?.edge_followed_by?.count : null;
      if (count == null) throw new Fail("Instagram didn't show a follower count (private, or blocked)");
      return count;
    }
    case "facebook": {
      if (!SAFE.test(handle)) throw new Fail("not a Facebook username", 400);
      const html = await page(`https://www.facebook.com/${handle}`);
      // Pages and public profiles expose "N followers" or "N friends"; most
      // personal profiles hide both from logged-out visitors.
      const m = /([\d.,]+\s*[KkMmBb]?)\s+(followers|friends)/.exec(html);
      const count = m && parseCount(m[1]);
      if (count == null) throw new Fail("Facebook hides this profile's count");
      return count;
    }
    default:
      throw new Fail("unknown platform", 400);
  }
}

async function page(url) {
  const res = await fetch(url, { headers: { "user-agent": UA, "accept-language": "en-US,en" } });
  if (!res.ok) throw new Fail("account not found or blocked");
  return res.text();
}
