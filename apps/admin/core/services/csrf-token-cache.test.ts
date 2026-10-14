import { describe, expect, it } from "vitest";
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
    expect(loads).toBe(1);
    cache.invalidate();
    const session = await cache.get(async () => ({
      csrfToken: "session",
      scope: "SESSION",
      expiresAt: "2026-08-28T12:00:00Z",
    }));
    expect(session.scope).toBe("SESSION");
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

    expect(replacements).toBe(1);
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
    if (!resolveStale) {
      throw new Error("Expected stale resolver");
    }

    cache.invalidate();
    const current = cache.get(async () => ({
      csrfToken: "session",
      scope: "SESSION",
      expiresAt: "2026-08-28T12:00:00Z",
    }));
    resolveStale({ csrfToken: "preauth", scope: "PREAUTH", expiresAt: "2026-08-27T12:30:00Z" });

    await stale;
    expect((await current).scope).toBe("SESSION");
  });
});

describe("admin CSRF lifecycle and recovery policy", () => {
  it("bounds exact recovery to one retry", () => {
    expect(shouldRetryInvalidCsrf(403, { error: "INVALID_CSRF_TOKEN" }, 0)).toBe(true);
    expect(shouldRetryInvalidCsrf(403, { key: "INVALID_CSRF_TOKEN" }, 0)).toBe(true);
    expect(shouldRetryInvalidCsrf(403, { error: "INVALID_CSRF_TOKEN" }, 1)).toBe(false);
  });

  it("does not retry arbitrary 403, 401, or 5xx responses", () => {
    expect(shouldRetryInvalidCsrf(403, { error: "FORBIDDEN" }, 0)).toBe(false);
    expect(shouldRetryInvalidCsrf(401, { error: "INVALID_CSRF_TOKEN" }, 0)).toBe(false);
    expect(shouldRetryInvalidCsrf(500, { error: "INVALID_CSRF_TOKEN" }, 0)).toBe(false);
  });

  it("invalidates only current-session and all-session revocation outcomes", () => {
    expect(revokedCurrentSession({ currentSessionRevoked: true })).toBe(true);
    expect(revokedCurrentSession({ currentSessionRevoked: true, scope: "ALL" })).toBe(true);
    expect(revokedCurrentSession({ currentSessionRevoked: false })).toBe(false);
    expect(revokedCurrentSession({ currentSessionRevoked: false, scope: "OTHERS" })).toBe(false);
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
    expect(restored.csrfToken).toBe("stored-session");
    expect(loads).toBe(0);
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
    expect(storage.writes).toBe(writesBeforeRestore);
    expect(storage.getItem(SESSION_CSRF_STORAGE_KEY)).not.toBe(null);
  });

  it("persists only the exact public SESSION response shape", async () => {
    const storage = new MemorySessionStorage();
    const cache = new CsrfTokenCache(
      () => NOW,
      () => storage
    );
    await cache.get(async () => storedSession());
    const serialized = storage.getItem(SESSION_CSRF_STORAGE_KEY);
    expect(serialized).not.toBe(null);
    expect(Object.keys(JSON.parse(serialized as string)).sort()).toEqual(["csrfToken", "expiresAt", "scope"]);
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
    expect(storage.getItem(SESSION_CSRF_STORAGE_KEY)).toBe(null);
    expect(storage.writes).toBe(0);
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
    expect(loads).toBe(1);
    expect(storage.getItem(SESSION_CSRF_STORAGE_KEY)).toBe(null);
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
    expect(loads).toBe(1);
    expect(storage.getItem(SESSION_CSRF_STORAGE_KEY)).toBe(null);
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
      expect(loads).toBe(1);
      expect(storage.getItem(SESSION_CSRF_STORAGE_KEY)).toBe(null);
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
      expect(loads).toBe(1);
      expect(storage.getItem(SESSION_CSRF_STORAGE_KEY)).toBe(null);
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
    expect(loads).toBe(1);
    expect(storage.getItem(SESSION_CSRF_STORAGE_KEY)).toBe(null);
  });

  it("clears memory and persisted SESSION state on invalidation", async () => {
    const storage = new MemorySessionStorage();
    const cache = new CsrfTokenCache(
      () => NOW,
      () => storage
    );
    await cache.get(async () => storedSession());
    cache.invalidate();
    expect(storage.getItem(SESSION_CSRF_STORAGE_KEY)).toBe(null);
    expect(storage.removals >= 1).toBeTruthy();
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
    expect(storage.getItem(SESSION_CSRF_STORAGE_KEY)).toBe(null);
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
    if (!resolveStale) {
      throw new Error("Expected stale resolver");
    }
    cache.invalidate();
    resolveStale(storedSession());
    await stale;
    expect(storage.getItem(SESSION_CSRF_STORAGE_KEY)).toBe(null);
  });

  it("does not clear restored SESSION state for an access-bootstrap 401", async () => {
    const storage = new MemorySessionStorage();
    storage.setItem(SESSION_CSRF_STORAGE_KEY, JSON.stringify(storedSession()));
    const cache = new CsrfTokenCache(
      () => NOW,
      () => storage
    );
    expect(shouldRetryInvalidCsrf(401, { error: "UNAUTHORIZED" }, 0)).toBe(false);
    const restored = await cache.get(async () => storedSession());
    expect(restored.csrfToken).toBe("stored-session");
    expect(storage.getItem(SESSION_CSRF_STORAGE_KEY)).not.toBe(null);
  });

  it("continues with the in-memory cache when sessionStorage is unavailable", async () => {
    const cache = new CsrfTokenCache(
      () => NOW,
      () => {
        throw new Error("unavailable");
      }
    );
    const value = await cache.get(async () => storedSession());
    expect(value.scope).toBe("SESSION");
  });
});
