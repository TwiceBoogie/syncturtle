import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { BrowserSessionEvent } from "@syncturtle/utils";
import type { IRequestConfig } from "./api.service";

vi.mock("@syncturtle/constants", () => ({
  API_BASE_URL: "https://admin.test",
  ADMIN_BASE_URL: "https://admin.test",
  ADMIN_BASE_PATH: "/god-mode",
}));
// Use the current source primitive, not a mock of its leadership algorithm.
vi.mock("@syncturtle/utils", () => import("../../../../packages/utils/src/browser-session-coordinator"));

const STATE = "syncturtle.auth.refresh-generation.v1";
const runtimeId = "00000000-0000-4000-8000-000000000001";
const remoteId = "00000000-0000-4000-8000-000000000002";
const csrf = { csrfToken: "fictional-csrf", scope: "SESSION", expiresAt: "2099-01-01T00:00:00Z" };
const invalidCsrf = { key: "INVALID_CSRF_TOKEN" };

function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((done) => {
    resolve = done;
  });
  return { promise, resolve };
}

function response(status = 200, data: unknown = {}) {
  return new Response(status === 204 ? null : JSON.stringify(data), { status });
}

function storage() {
  const entries = new Map<string, string>();
  return {
    getItem: vi.fn((key: string) => entries.get(key) ?? null),
    setItem: vi.fn((key: string, value: string) => {
      entries.set(key, value);
    }),
    removeItem: vi.fn((key: string) => {
      entries.delete(key);
    }),
  };
}

let shared: ReturnType<typeof storage>;
let session: ReturnType<typeof storage>;
let posted: BrowserSessionEvent[];
let receive: ((event: { data: unknown }) => void) | null;
let pagehide: (event: { persisted: boolean }) => void;
let closed: ReturnType<typeof vi.fn<() => void>>;
type LockRequest = (
  name: string,
  options: { mode: "exclusive"; signal: AbortSignal },
  action: () => Promise<void>
) => Promise<void>;
let lock: ReturnType<typeof vi.fn<LockRequest>>;
let fetchMock: ReturnType<typeof vi.fn<typeof fetch>>;
let browser: {
  crypto: { randomUUID: () => string };
  localStorage: ReturnType<typeof storage>;
  sessionStorage: ReturnType<typeof storage>;
  navigator: { locks?: { request: ReturnType<typeof vi.fn<LockRequest>> } };
  location: { origin: string };
  setTimeout: typeof setTimeout;
  clearTimeout: typeof clearTimeout;
  addEventListener: ReturnType<typeof vi.fn<(name: string, listener: (event: { persisted: boolean }) => void) => void>>;
};

async function service() {
  const { APIService } = await import("./api.service");
  class TestService extends APIService {
    constructor() {
      super("https://admin.test");
    }
    read(config: IRequestConfig = { authRefresh: true }) {
      return this.get("/protected", config);
    }
    write(config: IRequestConfig = {}) {
      return this.post("/unsafe", {}, config);
    }
    token() {
      return this.requestCsrfToken();
    }
    confirm() {
      return this.confirmAuthentication(false);
    }
    refreshDirect() {
      return this.post("/auth/refresh", undefined, { authRefresh: true });
    }
  }
  return new TestService();
}

function notice(kind: BrowserSessionEvent["kind"], generation = 0) {
  receive?.({ data: { version: 1, generation, kind, runtimeId: remoteId, timestamp: 1 } });
}

function calls(path: string) {
  return fetchMock.mock.calls.filter(([url]) => new URL(String(url)).pathname === path);
}

beforeEach(() => {
  vi.resetModules();
  shared = storage();
  session = storage();
  posted = [];
  receive = null;
  closed = vi.fn<() => void>();
  lock = vi.fn<LockRequest>(async (_name, _options, action) => {
    await action();
  });
  browser = {
    crypto: { randomUUID: () => runtimeId },
    localStorage: shared,
    sessionStorage: session,
    navigator: { locks: { request: lock } },
    location: { origin: "https://admin.test" },
    setTimeout,
    clearTimeout,
    addEventListener: vi.fn((_name, listener) => {
      pagehide = listener;
    }),
  };
  vi.stubGlobal("window", browser);
  vi.stubGlobal(
    "BroadcastChannel",
    class {
      set onmessage(listener: typeof receive) {
        receive = listener;
      }
      postMessage(event: BrowserSessionEvent) {
        posted.push(event);
      }
      close() {
        closed();
      }
    }
  );
  fetchMock = vi.fn<typeof fetch>();
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe("admin bounded refresh integration", () => {
  it("leaves a successful protected request unchanged", async () => {
    const api = await service();
    fetchMock.mockResolvedValueOnce(response(200, { ok: true }));
    const result = await api.read();
    expect(result.data).toEqual({ ok: true });
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(lock).not.toHaveBeenCalled();
  });

  it("elects one leader, advances generation and retries once", async () => {
    const api = await service();
    fetchMock
      .mockResolvedValueOnce(response(401))
      .mockResolvedValueOnce(response(200, csrf))
      .mockResolvedValueOnce(response(204))
      .mockResolvedValueOnce(response());
    await api.read();
    expect(calls("/protected")).toHaveLength(2);
    expect(calls("/auth/refresh")).toHaveLength(1);
    expect(lock).toHaveBeenCalledTimes(1);
    expect(shared.getItem(STATE)).toBe("1");
    expect(lock.mock.calls[0]?.[0]).toBe("syncturtle.auth.refresh.v1");
    expect(lock.mock.calls[0]?.[1]).toMatchObject({ mode: "exclusive", signal: expect.any(AbortSignal) });
    expect(posted.map((event) => event.kind)).toEqual(["CSRF_REPLACED", "REFRESH_SUCCEEDED"]);
  });

  it("shares one pending refresh across service instances", async () => {
    const api = await service();
    const other = await service();
    const refresh = deferred<Response>();
    const started = deferred<void>();
    let protectedCalls = 0;
    fetchMock.mockImplementation(async (url) => {
      const path = new URL(String(url)).pathname;
      if (path === "/protected") return response(++protectedCalls <= 2 ? 401 : 200);
      if (path === "/api/get-csrf-token") return response(200, csrf);
      started.resolve();
      return refresh.promise;
    });
    const first = api.read();
    const second = other.read();
    await started.promise;
    refresh.resolve(response(204));
    await Promise.all([first, second]);
    expect(lock).toHaveBeenCalledTimes(1);
    expect(calls("/auth/refresh")).toHaveLength(1);
    expect(calls("/protected")).toHaveLength(4);
  });

  it("coordinates two independent API runtimes through one shared lock", async () => {
    let tail = Promise.resolve();
    const queued = deferred<void>();
    lock.mockImplementation((_name, _options, action) => {
      const entered = tail.then(action);
      tail = entered;
      if (lock.mock.calls.length === 2) queued.resolve();
      return entered;
    });
    const firstRuntime = await service();
    const refresh = deferred<Response>();
    const started = deferred<void>();
    let protectedCalls = 0;
    fetchMock.mockImplementation(async (url) => {
      const path = new URL(String(url)).pathname;
      if (path === "/protected") return response(++protectedCalls <= 2 ? 401 : 200);
      if (path === "/api/get-csrf-token") return response(200, csrf);
      started.resolve();
      return refresh.promise;
    });
    const first = firstRuntime.read();
    await started.promise;
    vi.resetModules();
    browser.crypto.randomUUID = () => remoteId;
    browser.sessionStorage = storage();
    const secondRuntime = await service();
    const second = secondRuntime.read();
    await queued.promise;
    refresh.resolve(response(204));
    await Promise.all([first, second]);
    expect(lock).toHaveBeenCalledTimes(2);
    expect(calls("/auth/refresh")).toHaveLength(1);
    expect(calls("/protected")).toHaveLength(4);
    expect(shared.getItem(STATE)).toBe("1");
  });

  it.each([{ skipAuthRefresh: true, authRefresh: true }, {}])(
    "leaves an opted-out 401 unrefreshed: %j",
    async (config) => {
      const api = await service();
      fetchMock.mockResolvedValueOnce(response(401));
      await expect(api.read(config)).rejects.toMatchObject({ status: 401 });
      expect(fetchMock).toHaveBeenCalledTimes(1);
      expect(lock).not.toHaveBeenCalled();
    }
  );

  it("rereads queued generation and skips the backend refresh", async () => {
    const api = await service();
    lock.mockImplementationOnce(async (_name, _options, action) => {
      shared.setItem(STATE, "1");
      await action();
    });
    fetchMock.mockResolvedValueOnce(response(401)).mockResolvedValueOnce(response());
    await api.read();
    expect(calls("/protected")).toHaveLength(2);
    expect(calls("/auth/refresh")).toHaveLength(0);
    expect(posted).toEqual([]);
  });

  it.each(["locks", "storage", "uuid"])("falls back once without %s", async (missing) => {
    if (missing === "locks") browser.navigator.locks = undefined;
    if (missing === "storage")
      shared.getItem.mockImplementation(() => {
        throw new Error("denied");
      });
    if (missing === "uuid")
      browser.crypto.randomUUID = () => {
        throw new Error("unsupported");
      };
    const api = await service();
    fetchMock
      .mockResolvedValueOnce(response(401))
      .mockResolvedValueOnce(response(200, csrf))
      .mockResolvedValueOnce(response(204))
      .mockResolvedValueOnce(response());
    await api.read();
    expect(calls("/auth/refresh")).toHaveLength(1);
    expect(calls("/protected")).toHaveLength(2);
    expect(shared.setItem).not.toHaveBeenCalled();
    expect(posted.filter((event) => event.kind === "REFRESH_SUCCEEDED")).toHaveLength(missing === "uuid" ? 0 : 1);
  });

  it("bounds lock waiting and suppresses a late grant after fallback", async () => {
    const api = await service();
    let deadline!: () => void;
    let grant!: () => Promise<void>;
    const waiting = deferred<void>();
    browser.setTimeout = vi.fn((callback: () => void) => {
      deadline = callback;
      return 1;
    }) as unknown as typeof setTimeout;
    browser.clearTimeout = vi.fn();
    lock.mockImplementation((_name, _options, action) => {
      grant = action;
      waiting.resolve();
      return new Promise<void>(() => {});
    });
    fetchMock
      .mockResolvedValueOnce(response(401))
      .mockResolvedValueOnce(response(200, csrf))
      .mockResolvedValueOnce(response(204))
      .mockResolvedValueOnce(response());
    const request = api.read();
    await waiting.promise;
    deadline();
    await request;
    await grant();
    expect(calls("/auth/refresh")).toHaveLength(1);
    expect(lock).toHaveBeenCalledTimes(1);
    expect(lock.mock.calls[0]?.[1].signal.aborted).toBe(true);
  });

  it("retries exact invalid-CSRF refresh once", async () => {
    const api = await service();
    fetchMock
      .mockResolvedValueOnce(response(401))
      .mockResolvedValueOnce(response(200, csrf))
      .mockResolvedValueOnce(response(403, invalidCsrf))
      .mockResolvedValueOnce(response(200, csrf))
      .mockResolvedValueOnce(response(204))
      .mockResolvedValueOnce(response());
    await api.read();
    expect(calls("/auth/refresh")).toHaveLength(2);
    expect(calls("/api/get-csrf-token")).toHaveLength(2);
    expect(lock).toHaveBeenCalledTimes(1);
  });

  it("terminates a second invalid-CSRF response with only one confirmation", async () => {
    const api = await service();
    const signedOut = vi.fn();
    api.subscribeAuthentication(signedOut);
    fetchMock
      .mockResolvedValueOnce(response(401))
      .mockResolvedValueOnce(response(200, csrf))
      .mockResolvedValueOnce(response(403, invalidCsrf))
      .mockResolvedValueOnce(response(200, csrf))
      .mockResolvedValueOnce(response(403, invalidCsrf))
      .mockResolvedValueOnce(response(401));
    await expect(api.read()).rejects.toMatchObject({ status: 403 });
    expect(calls("/auth/refresh")).toHaveLength(2);
    expect(calls("/api/instances/admins/me")).toHaveLength(1);
    expect(signedOut).toHaveBeenCalledTimes(1);
  });

  it.each([invalidCsrf, { error: "INVALID_CSRF_TOKEN" }])("bounds unsafe CSRF recovery for %j", async (body) => {
    const api = await service();
    fetchMock
      .mockResolvedValueOnce(response(200, csrf))
      .mockResolvedValueOnce(response(403, body))
      .mockResolvedValueOnce(response(200, csrf))
      .mockResolvedValueOnce(response(403, body));
    await expect(api.write()).rejects.toMatchObject({ status: 403 });
    expect(calls("/unsafe")).toHaveLength(2);
    expect(calls("/api/get-csrf-token")).toHaveLength(2);
    expect(calls("/auth/refresh")).toHaveLength(0);
  });

  it("does not retry a different CSRF error or recursively refresh", async () => {
    const api = await service();
    fetchMock
      .mockResolvedValueOnce(response(200, csrf))
      .mockResolvedValueOnce(response(403, { key: "OTHER" }))
      .mockResolvedValueOnce(response(401));
    await expect(api.write()).rejects.toMatchObject({ status: 403 });
    await expect(api.refreshDirect()).rejects.toMatchObject({ status: 401 });
    expect(calls("/unsafe")).toHaveLength(1);
    expect(calls("/auth/refresh")).toHaveLength(1);
    expect(lock).not.toHaveBeenCalled();
  });

  it("retries after a failed refresh when shared generation advanced", async () => {
    const api = await service();
    const signedOut = vi.fn();
    api.subscribeAuthentication(signedOut);
    fetchMock
      .mockResolvedValueOnce(response(401))
      .mockResolvedValueOnce(response(200, csrf))
      .mockImplementationOnce(async () => {
        shared.setItem(STATE, "1");
        return response(401);
      })
      .mockResolvedValueOnce(response());
    await api.read();
    expect(calls("/auth/refresh")).toHaveLength(1);
    expect(calls("/api/instances/admins/me")).toHaveLength(0);
    expect(signedOut).not.toHaveBeenCalled();
  });

  it.each([401, 403])("confirms terminal refresh %i before clearing auth", async (status) => {
    const api = await service();
    const signedOut = vi.fn();
    api.subscribeAuthentication(signedOut);
    fetchMock
      .mockResolvedValueOnce(response(401))
      .mockResolvedValueOnce(response(200, csrf))
      .mockResolvedValueOnce(response(status))
      .mockResolvedValueOnce(response(401));
    await expect(api.read()).rejects.toMatchObject({ status });
    expect(calls("/api/instances/admins/me")).toHaveLength(1);
    const probe = calls("/api/instances/admins/me")[0]?.[1];
    expect(probe?.method).toBe("GET");
    expect(new Headers(probe?.headers).has("X-CSRF-Token")).toBe(false);
    expect(calls("/auth/refresh")).toHaveLength(1);
    expect(signedOut).toHaveBeenCalledTimes(1);
    expect(posted.filter((event) => event.kind === "AUTH_STATE_CHANGED")).toHaveLength(1);
  });

  it("keeps valid auth after terminal refresh and never confirms twice", async () => {
    const api = await service();
    const signedOut = vi.fn();
    api.subscribeAuthentication(signedOut);
    fetchMock
      .mockResolvedValueOnce(response(401))
      .mockResolvedValueOnce(response(200, csrf))
      .mockResolvedValueOnce(response(401))
      .mockResolvedValueOnce(response())
      .mockResolvedValueOnce(response(401));
    await expect(api.read()).rejects.toMatchObject({ status: 401 });
    expect(calls("/protected")).toHaveLength(2);
    expect(calls("/api/instances/admins/me")).toHaveLength(1);
    expect(calls("/auth/refresh")).toHaveLength(1);
    expect(signedOut).not.toHaveBeenCalled();
  });

  it.each([200, 401])("checks a post-refresh 401 once; bootstrap %i controls local loss", async (status) => {
    const api = await service();
    const signedOut = vi.fn();
    api.subscribeAuthentication(signedOut);
    fetchMock
      .mockResolvedValueOnce(response(401))
      .mockResolvedValueOnce(response(200, csrf))
      .mockResolvedValueOnce(response(204))
      .mockResolvedValueOnce(response(401))
      .mockResolvedValueOnce(response(status));
    await expect(api.read()).rejects.toMatchObject({ status: 401 });
    expect(calls("/protected")).toHaveLength(2);
    expect(calls("/api/instances/admins/me")).toHaveLength(1);
    expect(signedOut).toHaveBeenCalledTimes(status === 401 ? 1 : 0);
  });

  it.each(["refresh-503", "refresh-network", "probe-503", "probe-network"])(
    "preserves uncertainty: %s",
    async (failure) => {
      const api = await service();
      const signedOut = vi.fn();
      api.subscribeAuthentication(signedOut);
      fetchMock.mockResolvedValueOnce(response(401)).mockResolvedValueOnce(response(200, csrf));
      if (failure.startsWith("probe")) fetchMock.mockResolvedValueOnce(response(401));
      if (failure.endsWith("503")) fetchMock.mockResolvedValueOnce(response(503));
      else fetchMock.mockRejectedValueOnce(new TypeError("offline"));
      await expect(api.read()).rejects.toBeInstanceOf(Error);
      expect(signedOut).not.toHaveBeenCalled();
      expect(posted.some((event) => event.kind === "AUTH_STATE_CHANGED")).toBe(false);
      expect(calls("/auth/refresh")).toHaveLength(1);
    }
  );
});

describe("advisory authentication and CSRF lifecycle", () => {
  it.each([200, 401, 503])("validates a remote advisory without refresh: %i", async (status) => {
    const api = await service();
    const signedOut = vi.fn();
    api.subscribeAuthentication(signedOut);
    const probe = deferred<Response>();
    fetchMock.mockReturnValueOnce(probe.promise);
    notice("AUTH_STATE_CHANGED");
    notice("AUTH_STATE_CHANGED");
    const check = api.confirm(); // Join the exact in-flight advisory check.
    probe.resolve(response(status));
    if (status === 503) await expect(check).rejects.toMatchObject({ status: 503 });
    else await check;
    expect(calls("/api/instances/admins/me")).toHaveLength(1);
    expect(calls("/auth/refresh")).toHaveLength(0);
    expect(signedOut).toHaveBeenCalledTimes(status === 401 ? 1 : 0);
    expect(posted).toEqual([]);
  });

  it.each(["generation", "protected-success"])("ignores a stale probe after newer %s", async (progress) => {
    const api = await service();
    const signedOut = vi.fn();
    api.subscribeAuthentication(signedOut);
    const probe = deferred<Response>();
    const started = deferred<void>();
    fetchMock
      .mockResolvedValueOnce(response(401))
      .mockResolvedValueOnce(response(200, csrf))
      .mockResolvedValueOnce(response(401))
      .mockImplementationOnce(() => {
        started.resolve();
        return probe.promise;
      });
    const request = api.read();
    const rejection = expect(request).rejects.toMatchObject({ status: 401 });
    await started.promise;
    if (progress === "generation") notice("REFRESH_SUCCEEDED", 1);
    else {
      fetchMock.mockResolvedValueOnce(response());
      await api.read();
    }
    probe.resolve(response(401));
    await rejection;
    expect(signedOut).not.toHaveBeenCalled();
    expect(posted.some((event) => event.kind === "AUTH_STATE_CHANGED")).toBe(false);
    expect(calls("/api/instances/admins/me")).toHaveLength(1);
  });

  it("invalidates SESSION CSRF only on replacement, without sharing the value", async () => {
    const api = await service();
    fetchMock.mockResolvedValueOnce(response(200, csrf)).mockResolvedValueOnce(response(200, csrf));
    await api.token();
    notice("REFRESH_SUCCEEDED", 1);
    await api.token();
    expect(fetchMock).toHaveBeenCalledTimes(1);
    notice("CSRF_REPLACED", 1);
    expect(session.removeItem).toHaveBeenCalledWith("syncturtle.admin.csrf.session.v1");
    await api.token();
    expect(fetchMock).toHaveBeenCalledTimes(2);
    for (const event of posted) {
      expect(Object.keys(event).sort()).toEqual(["generation", "kind", "runtimeId", "timestamp", "version"]);
      expect(JSON.stringify(event)).not.toContain(csrf.csrfToken);
    }
  });

  it("keeps authentication uncertain on a remote network failure", async () => {
    const api = await service();
    const signedOut = vi.fn();
    api.subscribeAuthentication(signedOut);
    fetchMock.mockRejectedValueOnce(new TypeError("offline"));
    notice("AUTH_STATE_CHANGED");
    await expect(api.confirm()).rejects.toThrow("offline");
    expect(signedOut).not.toHaveBeenCalled();
    expect(posted).toEqual([]);
    expect(calls("/auth/refresh")).toHaveLength(0);
  });

  it("unsubscribes advisory checks and disposes only on actual page destruction", async () => {
    const api = await service();
    const cleanup = api.subscribeAuthentication(vi.fn());
    cleanup();
    notice("AUTH_STATE_CHANGED");
    expect(fetchMock).not.toHaveBeenCalled();
    pagehide({ persisted: true });
    expect(closed).not.toHaveBeenCalled();
    pagehide({ persisted: false });
    expect(closed).toHaveBeenCalledTimes(1);
    expect(receive).toBeNull();
  });

  it("does not notify an unmounted subscriber when its probe completes", async () => {
    const api = await service();
    const signedOut = vi.fn();
    const cleanup = api.subscribeAuthentication(signedOut);
    const probe = deferred<Response>();
    fetchMock.mockReturnValueOnce(probe.promise);
    notice("AUTH_STATE_CHANGED");
    const pending = api.confirm();
    cleanup();
    probe.resolve(response(401));
    await pending;
    expect(signedOut).not.toHaveBeenCalled();
    expect(posted).toEqual([]);
  });

  it("does not touch browser APIs at module evaluation or service construction", async () => {
    vi.stubGlobal("window", undefined);
    const api = await service();
    expect(api).toBeDefined();
    expect(fetchMock).not.toHaveBeenCalled();
    expect(lock).not.toHaveBeenCalled();
    expect(receive).toBeNull();
  });
});
