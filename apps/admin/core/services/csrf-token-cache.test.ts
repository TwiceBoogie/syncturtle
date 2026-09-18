import assert from "node:assert/strict";
import { describe, it } from "node:test";
import {
  CsrfTokenCache,
  revokedCurrentSession,
  SESSION_CSRF_STORAGE_KEY,
  shouldRetryInvalidCsrf,
} from "./csrf-token-cache";

const NOW = Date.parse("2026-08-27T12:00:00Z");

class MemorySessionStorage {
  private readonly values = new Map<string, string>();
  writes = 0;
  removals = 0;

  getItem(key: string): string | null {
    return this.values.get(key) ?? null;
  }

  setItem(key: string, value: string): void {
    this.writes += 1;
    this.values.set(key, value);
  }

  removeItem(key: string): void {
    this.removals += 1;
    this.values.delete(key);
  }
}

const storedSession = (expiresAt = "2026-08-28T12:00:00Z") => ({
  csrfToken: "stored-session",
  scope: "SESSION" as const,
  expiresAt,
});

describe("admin CsrfTokenCache", () => {
  it("is typed, expiry-aware, single-flight, and invalidatable", async () => {
    const cache = new CsrfTokenCache(() => NOW);
    let loads = 0;
    const load = async () => {
      loads += 1;
      return { csrfToken: "preauth", scope: "PREAUTH" as const, expiresAt: "2026-08-27T12:30:00Z" };
    };

    await Promise.all([cache.get(load), cache.get(load)]);
    assert.equal(loads, 1);
    cache.invalidate();
    const session = await cache.get(async () => ({
      csrfToken: "session",
      scope: "SESSION",
      expiresAt: "2026-08-28T12:00:00Z",
    }));
    assert.equal(session.scope, "SESSION");
  });

  it("replaces an expired SESSION value and then preserves the replacement", async () => {
    let now = NOW;
    const cache = new CsrfTokenCache(() => now);
    await cache.get(async () => ({
      csrfToken: "expired",
      scope: "SESSION",
      expiresAt: "2026-08-27T12:00:06Z",
    }));

    now += 1_001;
    let replacements = 0;
    const loadReplacement = async () => {
      replacements += 1;
      return { csrfToken: "current", scope: "SESSION" as const, expiresAt: "2026-08-28T12:00:00Z" };
    };
    await cache.get(loadReplacement);
    await cache.get(loadReplacement);

    assert.equal(replacements, 1);
  });

  it("starts a new issuance after invalidating an in-flight load", async () => {
    const cache = new CsrfTokenCache(() => NOW);
    let resolveStale: ((value: { csrfToken: string; scope: "PREAUTH"; expiresAt: string }) => void) | undefined;
    const stale = cache.get(
      () =>
        new Promise((resolve) => {
          resolveStale = resolve;
        })
    );
    await Promise.resolve();
    assert.ok(resolveStale);

    cache.invalidate();
    const current = cache.get(async () => ({
      csrfToken: "session",
      scope: "SESSION",
      expiresAt: "2026-08-28T12:00:00Z",
    }));
    resolveStale({ csrfToken: "preauth", scope: "PREAUTH", expiresAt: "2026-08-27T12:30:00Z" });

    await stale;
    assert.equal((await current).scope, "SESSION");
  });
});

describe("admin CSRF lifecycle and recovery policy", () => {
  it("bounds exact recovery to one retry", () => {
    assert.equal(shouldRetryInvalidCsrf(403, { error: "INVALID_CSRF_TOKEN" }, 0), true);
    assert.equal(shouldRetryInvalidCsrf(403, { key: "INVALID_CSRF_TOKEN" }, 0), true);
    assert.equal(shouldRetryInvalidCsrf(403, { error: "INVALID_CSRF_TOKEN" }, 1), false);
  });

  it("does not retry arbitrary 403, 401, or 5xx responses", () => {
    assert.equal(shouldRetryInvalidCsrf(403, { error: "FORBIDDEN" }, 0), false);
    assert.equal(shouldRetryInvalidCsrf(401, { error: "INVALID_CSRF_TOKEN" }, 0), false);
    assert.equal(shouldRetryInvalidCsrf(500, { error: "INVALID_CSRF_TOKEN" }, 0), false);
  });

  it("invalidates only current-session and all-session revocation outcomes", () => {
    assert.equal(revokedCurrentSession({ currentSessionRevoked: true }), true);
    assert.equal(revokedCurrentSession({ currentSessionRevoked: true, scope: "ALL" }), true);
    assert.equal(revokedCurrentSession({ currentSessionRevoked: false }), false);
    assert.equal(revokedCurrentSession({ currentSessionRevoked: false, scope: "OTHERS" }), false);
  });
});

describe("admin per-tab SESSION persistence", () => {
  it("restores an unexpired SESSION and returns it without issuance", async () => {
    const storage = new MemorySessionStorage();
    storage.setItem(SESSION_CSRF_STORAGE_KEY, JSON.stringify(storedSession()));
    const cache = new CsrfTokenCache(
      () => NOW,
      () => storage
    );
    let loads = 0;
    const restored = await cache.get(async () => {
      loads += 1;
      return { csrfToken: "unexpected", scope: "SESSION", expiresAt: "2026-08-28T12:00:00Z" };
    });
    assert.equal(restored.csrfToken, "stored-session");
    assert.equal(loads, 0);
  });

  it("preserves restored SESSION state for refresh without rewriting storage", async () => {
    const storage = new MemorySessionStorage();
    storage.setItem(SESSION_CSRF_STORAGE_KEY, JSON.stringify(storedSession()));
    const writesBeforeRestore = storage.writes;
    const cache = new CsrfTokenCache(
      () => NOW,
      () => storage
    );
    await cache.get(async () => storedSession());
    await cache.get(async () => storedSession());
    assert.equal(storage.writes, writesBeforeRestore);
    assert.notEqual(storage.getItem(SESSION_CSRF_STORAGE_KEY), null);
  });

  it("persists only the exact public SESSION response shape", async () => {
    const storage = new MemorySessionStorage();
    const cache = new CsrfTokenCache(
      () => NOW,
      () => storage
    );
    await cache.get(async () => storedSession());
    const serialized = storage.getItem(SESSION_CSRF_STORAGE_KEY);
    assert.notEqual(serialized, null);
    assert.deepEqual(Object.keys(JSON.parse(serialized as string)).sort(), ["csrfToken", "expiresAt", "scope"]);
  });

  it("never persists PREAUTH", async () => {
    const storage = new MemorySessionStorage();
    const cache = new CsrfTokenCache(
      () => NOW,
      () => storage
    );
    await cache.get(async () => ({
      csrfToken: "preauth",
      scope: "PREAUTH",
      expiresAt: "2026-08-27T12:30:00Z",
    }));
    assert.equal(storage.getItem(SESSION_CSRF_STORAGE_KEY), null);
    assert.equal(storage.writes, 0);
  });

  it("deletes stored PREAUTH and loads a fresh value", async () => {
    const storage = new MemorySessionStorage();
    storage.setItem(
      SESSION_CSRF_STORAGE_KEY,
      JSON.stringify({ csrfToken: "preauth", scope: "PREAUTH", expiresAt: "2026-08-27T12:30:00Z" })
    );
    const cache = new CsrfTokenCache(
      () => NOW,
      () => storage
    );
    let loads = 0;
    await cache.get(async () => {
      loads += 1;
      return { csrfToken: "fresh", scope: "PREAUTH", expiresAt: "2026-08-27T12:30:00Z" };
    });
    assert.equal(loads, 1);
    assert.equal(storage.getItem(SESSION_CSRF_STORAGE_KEY), null);
  });

  it("deletes malformed JSON without sending it", async () => {
    const storage = new MemorySessionStorage();
    storage.setItem(SESSION_CSRF_STORAGE_KEY, "not-json");
    const cache = new CsrfTokenCache(
      () => NOW,
      () => storage
    );
    let loads = 0;
    await cache.get(async () => {
      loads += 1;
      return { csrfToken: "fresh", scope: "PREAUTH", expiresAt: "2026-08-27T12:30:00Z" };
    });
    assert.equal(loads, 1);
    assert.equal(storage.getItem(SESSION_CSRF_STORAGE_KEY), null);
  });

  it("rejects stored values with missing or extra fields", async () => {
    for (const value of [
      { scope: "SESSION", expiresAt: "2026-08-28T12:00:00Z" },
      { ...storedSession(), sid: "must-not-be-restored" },
    ]) {
      const storage = new MemorySessionStorage();
      storage.setItem(SESSION_CSRF_STORAGE_KEY, JSON.stringify(value));
      const cache = new CsrfTokenCache(
        () => NOW,
        () => storage
      );
      let loads = 0;
      await cache.get(async () => {
        loads += 1;
        return { csrfToken: "fresh", scope: "PREAUTH", expiresAt: "2026-08-27T12:30:00Z" };
      });
      assert.equal(loads, 1);
      assert.equal(storage.getItem(SESSION_CSRF_STORAGE_KEY), null);
    }
  });

  it("rejects blank tokens and invalid expiry values", async () => {
    for (const value of [storedSession("invalid"), { ...storedSession(), csrfToken: "   " }]) {
      const storage = new MemorySessionStorage();
      storage.setItem(SESSION_CSRF_STORAGE_KEY, JSON.stringify(value));
      const cache = new CsrfTokenCache(
        () => NOW,
        () => storage
      );
      let loads = 0;
      await cache.get(async () => {
        loads += 1;
        return { csrfToken: "fresh", scope: "PREAUTH", expiresAt: "2026-08-27T12:30:00Z" };
      });
      assert.equal(loads, 1);
      assert.equal(storage.getItem(SESSION_CSRF_STORAGE_KEY), null);
    }
  });

  it("deletes an expired stored SESSION", async () => {
    const storage = new MemorySessionStorage();
    storage.setItem(SESSION_CSRF_STORAGE_KEY, JSON.stringify(storedSession("2026-08-27T12:00:00Z")));
    const cache = new CsrfTokenCache(
      () => NOW,
      () => storage
    );
    let loads = 0;
    await cache.get(async () => {
      loads += 1;
      return { csrfToken: "fresh", scope: "PREAUTH", expiresAt: "2026-08-27T12:30:00Z" };
    });
    assert.equal(loads, 1);
    assert.equal(storage.getItem(SESSION_CSRF_STORAGE_KEY), null);
  });

  it("clears memory and persisted SESSION state on invalidation", async () => {
    const storage = new MemorySessionStorage();
    const cache = new CsrfTokenCache(
      () => NOW,
      () => storage
    );
    await cache.get(async () => storedSession());
    cache.invalidate();
    assert.equal(storage.getItem(SESSION_CSRF_STORAGE_KEY), null);
    assert.ok(storage.removals >= 1);
  });

  it("removes persisted SESSION state when the canonical value expires", async () => {
    let now = NOW;
    const storage = new MemorySessionStorage();
    const cache = new CsrfTokenCache(
      () => now,
      () => storage
    );
    await cache.get(async () => storedSession("2026-08-27T12:00:06Z"));
    now += 1_001;
    await cache.get(async () => ({ csrfToken: "fresh", scope: "PREAUTH", expiresAt: "2026-08-27T12:30:00Z" }));
    assert.equal(storage.getItem(SESSION_CSRF_STORAGE_KEY), null);
  });

  it("does not let invalidated in-flight SESSION issuance repopulate storage", async () => {
    const storage = new MemorySessionStorage();
    const cache = new CsrfTokenCache(
      () => NOW,
      () => storage
    );
    let resolveStale: ((value: ReturnType<typeof storedSession>) => void) | undefined;
    const stale = cache.get(
      () =>
        new Promise((resolve) => {
          resolveStale = resolve;
        })
    );
    await Promise.resolve();
    assert.ok(resolveStale);
    cache.invalidate();
    resolveStale(storedSession());
    await stale;
    assert.equal(storage.getItem(SESSION_CSRF_STORAGE_KEY), null);
  });

  it("does not clear restored SESSION state for an access-bootstrap 401", async () => {
    const storage = new MemorySessionStorage();
    storage.setItem(SESSION_CSRF_STORAGE_KEY, JSON.stringify(storedSession()));
    const cache = new CsrfTokenCache(
      () => NOW,
      () => storage
    );
    assert.equal(shouldRetryInvalidCsrf(401, { error: "UNAUTHORIZED" }, 0), false);
    const restored = await cache.get(async () => storedSession());
    assert.equal(restored.csrfToken, "stored-session");
    assert.notEqual(storage.getItem(SESSION_CSRF_STORAGE_KEY), null);
  });

  it("continues with the in-memory cache when sessionStorage is unavailable", async () => {
    const cache = new CsrfTokenCache(
      () => NOW,
      () => {
        throw new Error("unavailable");
      }
    );
    const value = await cache.get(async () => storedSession());
    assert.equal(value.scope, "SESSION");
  });
});
