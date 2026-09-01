import type { ICsrfTokenData } from "@syncturtle/types";

const EXPIRY_SKEW_MS = 5_000;
export const SESSION_CSRF_STORAGE_KEY = "syncturtle.web.csrf.session.v1";

type TSessionStorage = Pick<Storage, "getItem" | "setItem" | "removeItem">;
type TSessionStorageProvider = () => TSessionStorage | null;

const browserSessionStorage = (): TSessionStorage | null => {
  if (typeof window === "undefined") return null;

  try {
    return window.sessionStorage;
  } catch {
    return null;
  }
};

export class CsrfTokenCache {
  private cached: ICsrfTokenData | null = null;
  private pending: Promise<ICsrfTokenData> | null = null;
  private generation = 0;
  private restoreAttempted = false;
  private readonly now: () => number;
  private readonly sessionStorageProvider: TSessionStorageProvider;

  constructor(now: () => number = Date.now, sessionStorageProvider: TSessionStorageProvider = browserSessionStorage) {
    this.now = now;
    this.sessionStorageProvider = sessionStorageProvider;
  }

  get(load: () => Promise<ICsrfTokenData>): Promise<ICsrfTokenData> {
    this.restoreSession();

    const cached = this.cached;
    if (cached && this.isUsable(cached)) return Promise.resolve(cached);

    if (cached?.scope === "SESSION") this.removePersistedSession();
    this.cached = null;

    if (this.pending) return this.pending;

    const loadGeneration = this.generation;
    const request = Promise.resolve()
      .then(load)
      .then((value) => {
        if (!this.isUsable(value)) throw new Error("Invalid CSRF token response");
        if (this.generation === loadGeneration) {
          this.cached = value;
          if (value.scope === "SESSION") this.persistSession(value);
        }
        return value;
      })
      .finally(() => {
        if (this.pending === request) {
          this.pending = null;
        }
      });
    this.pending = request;
    return request;
  }

  invalidate(): void {
    this.generation += 1;
    this.cached = null;
    this.pending = null;
    this.restoreAttempted = true;
    this.removePersistedSession();
  }

  private isUsable(value: ICsrfTokenData | null): boolean {
    if (
      !value ||
      typeof value.csrfToken !== "string" ||
      !value.csrfToken.trim() ||
      typeof value.expiresAt !== "string" ||
      (value.scope !== "PREAUTH" && value.scope !== "SESSION")
    ) {
      return false;
    }
    const expiresAt = Date.parse(value.expiresAt);
    return Number.isFinite(expiresAt) && expiresAt > this.now() + EXPIRY_SKEW_MS;
  }

  private restoreSession(): void {
    if (this.restoreAttempted) return;
    this.restoreAttempted = true;

    const storage = this.getSessionStorage();
    if (!storage) return;

    let serialized: string | null;
    try {
      serialized = storage.getItem(SESSION_CSRF_STORAGE_KEY);
    } catch {
      return;
    }

    if (serialized === null) return;

    let parsed: unknown;
    try {
      parsed = JSON.parse(serialized);
    } catch {
      this.removePersistedSession();
      return;
    }

    const restored = this.parseStoredSession(parsed);
    if (!restored) {
      this.removePersistedSession();
      return;
    }

    this.cached = restored;
  }

  private parseStoredSession(value: unknown): ICsrfTokenData | null {
    if (!value || typeof value !== "object" || Array.isArray(value)) return null;

    const keys = Object.keys(value).sort();
    if (keys.length !== 3 || keys[0] !== "csrfToken" || keys[1] !== "expiresAt" || keys[2] !== "scope") return null;

    const candidate = value as { csrfToken?: unknown; scope?: unknown; expiresAt?: unknown };
    if (candidate.scope !== "SESSION") return null;
    if (typeof candidate.csrfToken !== "string" || !candidate.csrfToken.trim()) return null;
    if (typeof candidate.expiresAt !== "string") return null;

    const restored: ICsrfTokenData = {
      csrfToken: candidate.csrfToken,
      scope: "SESSION",
      expiresAt: candidate.expiresAt,
    };
    return this.isUsable(restored) ? restored : null;
  }

  private persistSession(value: ICsrfTokenData): void {
    const storage = this.getSessionStorage();
    if (!storage) return;

    const persisted = {
      csrfToken: value.csrfToken,
      scope: "SESSION" as const,
      expiresAt: value.expiresAt,
    };

    try {
      storage.setItem(SESSION_CSRF_STORAGE_KEY, JSON.stringify(persisted));
    } catch {
      // The in-memory cache remains authoritative when browser storage is unavailable.
    }
  }

  private removePersistedSession(): void {
    const storage = this.getSessionStorage();
    if (!storage) return;

    try {
      storage.removeItem(SESSION_CSRF_STORAGE_KEY);
    } catch {
      // Browser storage failure must not break the canonical in-memory cache.
    }
  }

  private getSessionStorage(): TSessionStorage | null {
    try {
      return this.sessionStorageProvider();
    } catch {
      return null;
    }
  }
}

export function shouldRetryInvalidCsrf(status: number, body: unknown, attempt: number): boolean {
  if (status !== 403 || attempt >= 1 || !body || typeof body !== "object") return false;
  const payload = body as { key?: unknown; error?: unknown };
  return payload.key === "INVALID_CSRF_TOKEN" || payload.error === "INVALID_CSRF_TOKEN";
}

export function revokedCurrentSession(body: unknown): boolean {
  return Boolean(
    body && typeof body === "object" && (body as { currentSessionRevoked?: unknown }).currentSessionRevoked === true
  );
}
