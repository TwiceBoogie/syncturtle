import { describe, expect, it } from "vitest";
import { BrowserSessionCoordinator, BROWSER_SESSION_PROTOCOL } from "./browser-session-coordinator";
import type {
  BrowserSessionEnvironment,
  BrowserSessionEvent,
  BrowserSessionEventKind,
} from "./browser-session-coordinator";

const FIRST = "00000000-0000-4000-8000-000000000001";
const SECOND = "00000000-0000-4000-8000-000000000002";
const NOW = 1_800_000_000_000;
const KEYS = ["generation", "kind", "runtimeId", "timestamp", "version"];

function deferred() {
  let resolve!: () => void;
  const promise = new Promise<void>((complete) => {
    resolve = complete;
  });
  return { promise, resolve };
}

/** Pure manual scheduler: deadlines fire only when the test explicitly advances them. */
class Harness {
  value: string | null = null;
  readonly timers = new Set<() => void>();
  readonly receivers = new Set<(message: unknown) => void>();
  readonly events: BrowserSessionEvent[] = [];
  requests = 0;
  closed = 0;
  tail = Promise.resolve();
  storage = {
    getItem: (key: string) => {
      expect(key).toBe(BROWSER_SESSION_PROTOCOL.state);
      return this.value;
    },
    setItem: (key: string, value: string) => {
      expect(key).toBe(BROWSER_SESSION_PROTOCOL.state);
      this.value = value;
    },
  };
  locks: NonNullable<BrowserSessionEnvironment["locks"]> = {
    request: (name, options, action) => {
      expect(name).toBe(BROWSER_SESSION_PROTOCOL.lock);
      expect(options.mode).toBe("exclusive");
      this.requests += 1;
      const next = this.tail.then(async () => {
        if (options.signal.aborted) throw new Error("aborted");
        await action();
      });
      this.tail = next.catch(() => {});
      return next;
    },
  };

  environment(runtimeId = FIRST): BrowserSessionEnvironment {
    return {
      runtimeId,
      now: () => NOW,
      schedule: (callback, milliseconds) => {
        expect(milliseconds).toBe(100);
        this.timers.add(callback);
        return () => {
          this.timers.delete(callback);
        };
      },
      storage: this.storage,
      locks: this.locks,
      openChannel: (name, receive) => {
        expect(name).toBe(BROWSER_SESSION_PROTOCOL.channel);
        this.receivers.add(receive);
        return {
          postMessage: (event) => {
            this.events.push(event);
            this.deliver(event); // Deliberately echoes own messages to test suppression.
          },
          close: () => {
            this.closed += 1;
            this.receivers.delete(receive);
          },
        };
      },
    };
  }

  deliver(message: unknown): void {
    for (const receive of this.receivers) receive(message);
  }

  expire(): void {
    for (const callback of [...this.timers]) callback();
  }
}

function event(kind: BrowserSessionEventKind = "REFRESH_SUCCEEDED", generation = 1): BrowserSessionEvent {
  return { version: 1, generation, kind, runtimeId: SECOND, timestamp: NOW };
}

describe("BrowserSessionCoordinator protocol", () => {
  it("round-trips each closed event with no own notification or extra fields", () => {
    const harness = new Harness();
    const first = new BrowserSessionCoordinator(harness.environment(), 100);
    const second = new BrowserSessionCoordinator(harness.environment(SECOND), 100);
    const own: BrowserSessionEvent[] = [];
    const received: BrowserSessionEvent[] = [];
    first.subscribe((notice) => own.push(notice));
    second.subscribe((notice) => received.push(notice));

    first.recordRefreshSuccess();
    first.notifyCsrfReplaced();
    first.notifyAuthStateChanged();

    expect(own).toEqual([]);
    expect(received.map((notice) => notice.kind)).toEqual(["REFRESH_SUCCEEDED", "CSRF_REPLACED", "AUTH_STATE_CHANGED"]);
    expect(received.length).toBe(3);
    for (const notice of received) {
      expect(Object.keys(notice).sort()).toEqual(KEYS);
      expect(notice).toEqual({ version: 1, generation: 1, kind: notice.kind, runtimeId: FIRST, timestamp: NOW });
      expect(Object.isFrozen(notice)).toBe(true);
    }
    expect(harness.value).toBe(null);
  });

  const malformed: unknown[] = [
    null,
    undefined,
    "event",
    [],
    1,
    { ...event(), version: 2 },
    { ...event(), kind: "SIGNED_OUT" },
    { ...event(), generation: -1 },
    { ...event(), generation: 1.5 },
    { ...event(), generation: Number.MAX_SAFE_INTEGER + 1 },
    { ...event(), generation: NaN },
    { ...event(), generation: "1" },
    { ...event(), timestamp: Infinity },
    { ...event(), timestamp: -1 },
    { ...event(), runtimeId: "not-an-ephemeral-uuid" },
    { version: 1, generation: 1, kind: "REFRESH_SUCCEEDED", runtimeId: SECOND },
    { ...event(), [Symbol("extra")]: true },
    Object.defineProperty(event(), "generation", {
      get: () => {
        throw new Error("getter");
      },
    }),
    new Proxy(
      {},
      {
        ownKeys: () => {
          throw new Error("proxy");
        },
      }
    ),
  ];
  for (const [index, message] of malformed.entries()) {
    it(`ignores malformed/unsupported message ${index} without throwing`, () => {
      const harness = new Harness();
      const coordinator = new BrowserSessionCoordinator(harness.environment(), 100);
      let notices = 0;
      coordinator.subscribe(() => {
        notices += 1;
      });

      expect(() => harness.deliver(message)).not.toThrow();

      expect(notices).toBe(0);
      expect(coordinator.observeGeneration()).toBe(0);
    });
  }

  const forbidden = [
    "sessionId",
    "userId",
    "accessToken",
    "refreshToken",
    "csrfToken",
    "cookie",
    "Authorization",
    "url",
    "body",
    "headers",
    "provenanceAssertion",
    "clientBinding",
    "ip",
    "userAgent",
    "error",
    "response",
    "payload",
    "metadata",
    "deviceLabel",
    "recordVersion",
    "sessionVersion",
    "authVersion",
    "adminSessionVersion",
    "rotationCounter",
    "redisKey",
    "roles",
    "instanceId",
  ];
  for (const field of forbidden) {
    it(`rejects the extra ${field} field for every event kind`, () => {
      const harness = new Harness();
      const coordinator = new BrowserSessionCoordinator(harness.environment(), 100);
      let notices = 0;
      coordinator.subscribe(() => {
        notices += 1;
      });

      for (const kind of ["REFRESH_SUCCEEDED", "CSRF_REPLACED", "AUTH_STATE_CHANGED"] as const) {
        harness.deliver({ ...event(kind), [field]: "forbidden-fixture" });
      }

      expect(notices).toBe(0);
      expect(coordinator.observeGeneration()).toBe(0);
    });
  }

  it("isolates throwing subscribers and stops delivery after unsubscribe/dispose", () => {
    const harness = new Harness();
    const coordinator = new BrowserSessionCoordinator(harness.environment(), 100);
    let notices = 0;
    coordinator.subscribe(() => {
      throw new Error("subscriber");
    });
    const unsubscribe = coordinator.subscribe(() => {
      notices += 1;
    });

    harness.deliver(event());
    unsubscribe();
    harness.deliver(event());
    coordinator.dispose();
    coordinator.dispose();
    coordinator.subscribe(() => {
      notices += 1;
    });
    harness.deliver(event());
    coordinator.notifyCsrfReplaced();
    coordinator.notifyAuthStateChanged();
    coordinator.recordRefreshSuccess();

    expect(notices).toBe(1);
    expect(harness.closed).toBe(1);
    expect(harness.receivers.size).toBe(0);
    expect(harness.events.length).toBe(0);
    expect(harness.value).toBe(null);
  });
});

describe("BrowserSessionCoordinator generation", () => {
  it("starts at zero, reads persisted progress, and never regresses locally", () => {
    const harness = new Harness();
    const coordinator = new BrowserSessionCoordinator(harness.environment(), 100);
    expect(coordinator.observeGeneration()).toBe(0);
    harness.value = "4";

    expect(coordinator.observeGeneration()).toBe(4);
    expect(coordinator.recordRefreshSuccess()).toBe(5);
    harness.deliver(event("REFRESH_SUCCEEDED", 8));
    harness.deliver(event("REFRESH_SUCCEEDED", 2));
    harness.value = "1";

    expect(coordinator.observeGeneration()).toBe(8);
    expect(coordinator.recordRefreshSuccess()).toBe(9);
    expect(harness.value).toBe("1"); // Fallback must not write outside the lock.
  });

  it("reads advancement without an advisory channel", async () => {
    const harness = new Harness();
    const first = new BrowserSessionCoordinator({ ...harness.environment(), openChannel: undefined }, 100);
    const second = new BrowserSessionCoordinator({ ...harness.environment(SECOND), openChannel: undefined }, 100);
    const before = second.observeGeneration();

    await first.coordinateRefresh(0, async () => {});

    expect(before).toBe(0);
    expect(second.observeGeneration()).toBe(1);
  });

  it("fails explicitly at numeric exhaustion without publishing wrapped progress", () => {
    const harness = new Harness();
    harness.value = String(Number.MAX_SAFE_INTEGER);
    const coordinator = new BrowserSessionCoordinator(harness.environment(), 100);

    expect(() => coordinator.recordRefreshSuccess()).toThrow(/exhausted/);

    expect(harness.events.length).toBe(0);
    expect(harness.value).toBe(String(Number.MAX_SAFE_INTEGER));
  });
});

describe("BrowserSessionCoordinator refresh acquisition", () => {
  it("keeps one leader through completion and rereads generation for the queued runtime", async () => {
    const harness = new Harness();
    const first = new BrowserSessionCoordinator(harness.environment(), 100);
    const second = new BrowserSessionCoordinator(harness.environment(SECOND), 100);
    const entered = deferred();
    const release = deferred();
    let leaders = 0;
    const firstGeneration = first.observeGeneration();
    const secondGeneration = second.observeGeneration();

    const firstResult = first.coordinateRefresh(firstGeneration, async () => {
      leaders += 1;
      entered.resolve();
      await release.promise;
    });
    await entered.promise;
    const secondResult = second.coordinateRefresh(secondGeneration, async () => {
      leaders += 1;
    });
    expect(leaders).toBe(1);
    expect(harness.value).toBe(null);
    release.resolve();
    const results = await Promise.all([firstResult, secondResult]);

    expect(results).toEqual([
      { status: "refreshed", generation: 1 },
      { status: "advanced", generation: 1 },
    ]);
    expect(leaders).toBe(1);
    expect(harness.requests).toBe(2);
    expect(harness.events.length).toBe(1);
    expect(harness.events[0]?.kind).toBe("REFRESH_SUCCEEDED");
    expect(harness.timers.size).toBe(0);
  });

  it("does not impose the acquisition deadline on an entered operation", async () => {
    const harness = new Harness();
    const coordinator = new BrowserSessionCoordinator(harness.environment(), 100);
    const entered = deferred();
    const release = deferred();

    const pending = coordinator.coordinateRefresh(0, async () => {
      entered.resolve();
      await release.promise;
    });
    await entered.promise;
    harness.expire();
    release.resolve();

    expect((await pending).status).toBe("refreshed");
    expect(harness.requests).toBe(1);
  });

  it("propagates leader failure without advancement, fallback, or another attempt", async () => {
    const harness = new Harness();
    const coordinator = new BrowserSessionCoordinator(harness.environment(), 100);
    const failure = new Error("app-owned failure");
    let attempts = 0;

    await expect(
      coordinator.coordinateRefresh(0, async () => {
        attempts += 1;
        throw failure;
      })
    ).rejects.toBe(failure);

    expect(attempts).toBe(1);
    expect(harness.requests).toBe(1);
    expect(harness.events.length).toBe(0);
    expect(harness.value).toBe(null);
    expect(harness.timers.size).toBe(0);
  });

  it("times out once, aborts acquisition, and suppresses a late grant", async () => {
    const harness = new Harness();
    let lateGrant!: () => Promise<void>;
    let signal!: AbortSignal;
    let attempts = 0;
    const coordinator = new BrowserSessionCoordinator(
      {
        ...harness.environment(),
        locks: {
          request: (_name, options, action) => {
            signal = options.signal;
            lateGrant = action;
            return new Promise(() => {});
          },
        },
      },
      100
    );

    const pending = coordinator.coordinateRefresh(0, async () => {
      attempts += 1;
    });
    harness.expire();
    const result = await pending;
    await lateGrant();

    expect(result).toEqual({ status: "fallback", generation: 0 });
    expect(signal.aborted).toBe(true);
    expect(attempts).toBe(0);
    expect(harness.timers.size).toBe(0);
    expect(harness.events.length).toBe(0);
  });

  for (const failure of [
    "missing-locks",
    "missing-storage",
    "read-fails",
    "bad-storage",
    "request-rejects",
    "request-throws",
    "schedule-throws",
  ]) {
    it(`returns fallback without invoking refresh when ${failure}`, async () => {
      const harness = new Harness();
      const base = harness.environment();
      const unavailable = () => {
        throw new Error("unavailable");
      };
      const environment: BrowserSessionEnvironment = {
        ...base,
        locks:
          failure === "missing-locks"
            ? undefined
            : failure === "request-rejects"
              ? {
                  request: async () => {
                    throw new Error("unavailable");
                  },
                }
              : failure === "request-throws"
                ? { request: unavailable }
                : base.locks,
        storage:
          failure === "missing-storage"
            ? undefined
            : failure === "read-fails"
              ? {
                  getItem: unavailable,
                  setItem: unavailable,
                }
              : base.storage,
        schedule: failure === "schedule-throws" ? unavailable : base.schedule,
      };
      if (failure === "bad-storage") harness.value = '{"generation":1}';
      const coordinator = new BrowserSessionCoordinator(environment, 100);
      let attempts = 0;

      const result = await coordinator.coordinateRefresh(0, async () => {
        attempts += 1;
      });

      expect(result.status).toBe("fallback");
      expect(attempts).toBe(0);
      expect(harness.timers.size).toBe(0);
      expect(harness.events.length).toBe(0);
    });
  }

  it("rereads storage availability after lock entry", async () => {
    const harness = new Harness();
    const coordinator = new BrowserSessionCoordinator(harness.environment(), 100);
    let attempts = 0;

    const pending = coordinator.coordinateRefresh(0, async () => {
      attempts += 1;
    });
    harness.value = "invalid";
    const result = await pending;

    expect(result.status).toBe("fallback");
    expect(attempts).toBe(0);
  });

  it("retains local progress with unavailable storage and channel", async () => {
    const harness = new Harness();
    const coordinator = new BrowserSessionCoordinator(
      { ...harness.environment(), storage: undefined, openChannel: undefined },
      100
    );

    const result = await coordinator.coordinateRefresh(0, async () => {
      throw new Error("fallback must be app-owned");
    });
    const generation = coordinator.recordRefreshSuccess(); // Simulated confirmed app fallback success.

    expect(result.status).toBe("fallback");
    expect(generation).toBe(1);
    expect(coordinator.observeGeneration()).toBe(1);
    expect(harness.value).toBe(null);
  });

  it("tolerates persistence and advisory transport failures after confirmed success", async () => {
    const harness = new Harness();
    const unavailable = () => {
      throw new Error("unavailable");
    };
    const coordinator = new BrowserSessionCoordinator(
      {
        ...harness.environment(),
        storage: { getItem: () => null, setItem: unavailable },
        openChannel: () => ({ postMessage: unavailable, close: unavailable }),
      },
      100
    );

    const result = await coordinator.coordinateRefresh(0, async () => {});
    coordinator.notifyCsrfReplaced();
    coordinator.dispose();

    expect(result).toEqual({ status: "refreshed", generation: 1 });
  });

  it("coordinates through storage when channel construction fails", async () => {
    const harness = new Harness();
    const coordinator = new BrowserSessionCoordinator(
      {
        ...harness.environment(),
        openChannel: () => {
          throw new Error("unavailable");
        },
      },
      100
    );

    const result = await coordinator.coordinateRefresh(0, async () => {});

    expect(result).toEqual({ status: "refreshed", generation: 1 });
    expect(harness.value).toBe("1");
  });

  it("disposes pending acquisition without starting an operation", async () => {
    const harness = new Harness();
    const coordinator = new BrowserSessionCoordinator(harness.environment(), 100);
    let attempts = 0;

    const pending = coordinator.coordinateRefresh(0, async () => {
      attempts += 1;
    });
    coordinator.dispose();
    const result = await pending;
    await harness.tail;
    const after = await coordinator.coordinateRefresh(0, async () => {
      attempts += 1;
    });

    expect(result.status).toBe("disposed");
    expect(after.status).toBe("disposed");
    expect(attempts).toBe(0);
    expect(harness.timers.size).toBe(0);
  });

  it("suppresses publication when disposed during an app-owned operation", async () => {
    const harness = new Harness();
    const coordinator = new BrowserSessionCoordinator(harness.environment(), 100);
    const entered = deferred();
    const release = deferred();

    const pending = coordinator.coordinateRefresh(0, async () => {
      entered.resolve();
      await release.promise;
    });
    await entered.promise;
    coordinator.dispose();
    release.resolve();
    const result = await pending;

    expect(result.status).toBe("disposed");
    expect(harness.events.length).toBe(0);
    expect(harness.value).toBe(null);
  });

  it("rejects invalid local configuration and observed generations", () => {
    const harness = new Harness();
    for (const wait of [0, -1, 1.5, 10001, Infinity, NaN]) {
      expect(() => new BrowserSessionCoordinator(harness.environment(), wait)).toThrow(/wait/);
    }
    expect(() => new BrowserSessionCoordinator({ ...harness.environment(), runtimeId: "identity" }, 100)).toThrow(
      /UUID/
    );
    const coordinator = new BrowserSessionCoordinator(harness.environment(), 100);

    expect(() => coordinator.coordinateRefresh(-1, async () => {})).toThrow(/generation/);

    expect(harness.requests).toBe(0);
  });
});
