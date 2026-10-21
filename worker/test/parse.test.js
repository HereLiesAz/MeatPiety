import { test } from "node:test";
import assert from "node:assert/strict";
import { cacheKeyFor, cleanHandle, isPublicHost, parseCount } from "../src/index.js";

test("cleanHandle strips URLs and @", () => {
  assert.equal(cleanHandle("@name"), "name");
  assert.equal(cleanHandle("https://www.instagram.com/name/"), "name");
  assert.equal(cleanHandle("  name.bsky.social "), "name.bsky.social");
});

test("parseCount reads abbreviations and separators", () => {
  assert.equal(parseCount("1.2K"), 1200);
  assert.equal(parseCount("3M"), 3_000_000);
  assert.equal(parseCount("1,234"), 1234);
  assert.equal(parseCount("lots"), null);
});

test("parseCount handles decimal commas and bare decimals", () => {
  assert.equal(parseCount("1,5M"), 1_500_000);
  assert.equal(parseCount("1.5M"), 1_500_000);
  assert.equal(parseCount("1.234.567"), 1234567);
  assert.equal(parseCount("12,345"), 12345);
  assert.equal(parseCount("1.5"), 2);
  assert.equal(parseCount("1,234.5K"), 1_234_500);
  assert.equal(parseCount("1.2.3"), null);
});

test("cacheKeyFor ignores unknown query params", () => {
  const key = (u, c) => cacheKeyFor(new URL(u), c);
  const base = "https://w.example";
  assert.equal(key(`${base}/market?x=1`), key(`${base}/market`));
  assert.equal(key(`${base}/followers?handle=a&platform=bluesky&x=1`), key(`${base}/followers?platform=bluesky&handle=a`));
  assert.equal(key(`${base}/fx`, "DE"), `${base}/fx?country=DE`);
  assert.equal(key(`${base}/fx`), `${base}/fx?country=US`);
  assert.equal(key(`${base}/fx?currency=EUR&x=1`), `${base}/fx?currency=EUR`);
});

test("isPublicHost rejects IPs and local names", () => {
  assert.equal(isPublicHost("mastodon.social"), true);
  assert.equal(isPublicHost("127.0.0.1"), false);
  assert.equal(isPublicHost("0x7f.1"), false);
  assert.equal(isPublicHost("foo.internal"), false);
  assert.equal(isPublicHost("localhost"), false);
});
