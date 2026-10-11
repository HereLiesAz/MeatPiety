import { test } from "node:test";
import assert from "node:assert/strict";
import { cleanHandle, parseCount } from "../src/index.js";

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
