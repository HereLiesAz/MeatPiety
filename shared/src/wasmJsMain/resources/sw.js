// Meat Piety service worker.
// - Offline: caches the app shell (network-first, so new builds win).
// - Notifications while away: the page writes an upcoming-milestone schedule
//   to Cache Storage; a daily Periodic Background Sync (installed PWAs in
//   Chromium) shows the ones whose day has come. The page never notifies.

const SHELL = "meatpiety-shell-v1";
const DATA = "meatpiety-data";
const SCHEDULE = "/__schedule.json";

self.addEventListener("install", (e) => {
  e.waitUntil(caches.open(SHELL).then((c) => c.addAll(["./", "index.html", "manifest.webmanifest"])).then(() => self.skipWaiting()));
});

self.addEventListener("activate", (e) => {
  e.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((k) => k.startsWith("meatpiety-shell-") && k !== SHELL).map((k) => caches.delete(k))))
      .then(() => self.clients.claim()),
  );
});

self.addEventListener("fetch", (e) => {
  const url = new URL(e.request.url);
  if (e.request.method !== "GET" || url.origin !== location.origin) return;
  e.respondWith(
    fetch(e.request)
      .then((res) => {
        if (res.ok) caches.open(SHELL).then((c) => c.put(e.request, res.clone()));
        return res;
      })
      .catch(() => caches.match(e.request)),
  );
});

function localEpochDay() {
  const d = new Date();
  return Math.floor((d.getTime() - d.getTimezoneOffset() * 60000) / 86400000);
}

async function showDue() {
  const cache = await caches.open(DATA);
  const res = await cache.match(SCHEDULE);
  if (!res) return;
  const schedule = await res.json(); // { seen: [ids], upcoming: [{day, id, title, body}] }
  const today = localEpochDay();
  const seen = new Set(schedule.seen);
  // Skip while the app is open; the page marks these as seen silently.
  const open = await self.clients.matchAll({ type: "window" });
  if (open.some((c) => c.visibilityState === "visible")) return;
  const due = schedule.upcoming.filter((n) => n.day <= today && !seen.has(n.id));
  for (const n of due) {
    seen.add(n.id);
    await self.registration.showNotification(n.title, { body: n.body, tag: n.id, icon: "icons/icon-192.png", badge: "icons/badge-96.png" });
  }
  schedule.seen = [...seen];
  await cache.put(SCHEDULE, new Response(JSON.stringify(schedule), { headers: { "content-type": "application/json" } }));
}

self.addEventListener("periodicsync", (e) => {
  if (e.tag === "milestones") e.waitUntil(showDue());
});

self.addEventListener("notificationclick", (e) => {
  e.notification.close();
  e.waitUntil(
    self.clients.matchAll({ type: "window" }).then((cs) => (cs.length ? cs[0].focus() : self.clients.openWindow("./"))),
  );
});
