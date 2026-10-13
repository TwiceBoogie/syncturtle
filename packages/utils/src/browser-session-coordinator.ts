export const BROWSER_SESSION_PROTOCOL = Object.freeze({
  version: 1 as const,
  lock: "syncturtle.auth.refresh.v1",
  channel: "syncturtle.auth.session.v1",
  state: "syncturtle.auth.refresh-generation.v1",
});

export type BrowserSessionEventKind = "REFRESH_SUCCEEDED" | "CSRF_REPLACED" | "AUTH_STATE_CHANGED";

export interface BrowserSessionEvent {
  readonly version: 1;
  readonly generation: number;
  readonly kind: BrowserSessionEventKind;
  readonly runtimeId: string;
  readonly timestamp: number;
}

/** Adapters own browser access. Construct one coordinator per runtime, never per request. */
export interface BrowserSessionEnvironment {
  /** Fresh random UUID for this runtime; never an authentication identity. */
  readonly runtimeId: string;
  readonly now: () => number;
  /** Schedule once and return cancellation. Must not invoke the callback synchronously. */
  readonly schedule: (callback: () => void, milliseconds: number) => () => void;
  readonly storage?: Pick<Storage, "getItem" | "setItem">;
  /** Maps to navigator.locks.request with exclusive mode and the supplied abort signal. */
  readonly locks?: {
    request(
      name: string,
      options: { mode: "exclusive"; signal: AbortSignal },
      action: () => Promise<void>
    ): Promise<void>;
  };
  /** Open a dedicated channel; close removes its message listener as well. */
  readonly openChannel?: (
    name: string,
    receive: (message: unknown) => void
  ) => {
    postMessage(event: BrowserSessionEvent): void;
    close(): void;
  };
}

export interface BrowserSessionRefreshResult {
  readonly status: "advanced" | "refreshed" | "fallback" | "disposed";
  readonly generation: number;
}

type Listener = (event: BrowserSessionEvent) => void;
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
const EVENT_KEYS = ["version", "generation", "kind", "runtimeId", "timestamp"];

function validGeneration(value: unknown): value is number {
  return typeof value === "number" && Number.isSafeInteger(value) && value >= 0;
}

function validKind(value: unknown): value is BrowserSessionEventKind {
  return value === "REFRESH_SUCCEEDED" || value === "CSRF_REPLACED" || value === "AUTH_STATE_CHANGED";
}

/** Never forward external objects or accept extra properties, accessors, or arbitrary data. */
function readEvent(value: unknown): BrowserSessionEvent | undefined {
  try {
    if (value === null || typeof value !== "object") return;
    const keys = Reflect.ownKeys(value);

    if (keys.length !== EVENT_KEYS.length || keys.some((key) => typeof key !== "string" || !EVENT_KEYS.includes(key)))
      return;

    const fields = Object.getOwnPropertyDescriptors(value);
    for (const key of EVENT_KEYS) {
      if (!fields[key] || !("value" in fields[key]!)) return;
    }

    const version: unknown = fields.version?.value;
    const generation: unknown = fields.generation?.value;
    const kind: unknown = fields.kind?.value;
    const runtimeId: unknown = fields.runtimeId?.value;
    const timestamp: unknown = fields.timestamp?.value;

    if (version !== 1 || !validGeneration(generation) || !validKind(kind)) return;

    if (typeof runtimeId !== "string" || !UUID.test(runtimeId) || !validGeneration(timestamp)) return;

    return Object.freeze({ version, generation, kind, runtimeId, timestamp });
  } catch {
    return;
  }
}

/**
 * Coordinates refresh and authentication notifications across browser tabs.
 */
export class BrowserSessionCoordinator {
  private generation = 0;
  private disposed = false;
  private readonly listeners = new Set<Listener>();
  private readonly pending = new Set<() => void>();
  private channel?: ReturnType<NonNullable<BrowserSessionEnvironment["openChannel"]>>;

  constructor(
    private readonly environment: BrowserSessionEnvironment,
    private readonly lockWaitMs: number
  ) {
    if (!UUID.test(environment.runtimeId)) throw new Error("Expected an ephemeral runtime UUID");

    if (!Number.isSafeInteger(lockWaitMs) || lockWaitMs < 1 || lockWaitMs > 10_000) {
      throw new Error("Lock acquisition wait must be between 1 and 10000 milliseconds");
    }

    this.observeGeneration();
    try {
      this.channel = environment.openChannel?.(BROWSER_SESSION_PROTOCOL.channel, (message) => this.receive(message));
    } catch {
      // Advisory transport availability never determines authentication state.
    }
  }

  /** Local high-water mark merged with available shared storage; never decreases. */
  observeGeneration(): number {
    this.readGeneration();
    return this.generation;
  }

  private readGeneration(): boolean {
    try {
      if (!this.environment.storage) return false;

      const stored = this.environment.storage.getItem(BROWSER_SESSION_PROTOCOL.state);
      if (stored === null) return true;

      if (!/^(0|[1-9][0-9]*)$/.test(stored)) return false;

      const value = Number(stored);
      if (!validGeneration(value)) return false;

      this.generation = Math.max(this.generation, value);

      return true;
    } catch {
      return false;
    }
  }

  /** Resolve the callback only after confirmed success. Rejections propagate without another attempt.
   * The acquisition deadline ends on entry; the app owns the operation's network deadline.
   * A fallback result does NOT invoke refresh. The app may attempt it once and then call
   * recordRefreshSuccess. Keep the existing app-local single-flight around this entire operation.
   */
  coordinateRefresh(observedGeneration: number, refresh: () => Promise<void>): Promise<BrowserSessionRefreshResult> {
    if (!validGeneration(observedGeneration)) throw new Error("Invalid observed generation");

    if (this.disposed) return Promise.resolve(this.result("disposed"));

    if (!this.environment.locks || !this.readGeneration()) return Promise.resolve(this.result("fallback"));

    const locks = this.environment.locks;
    return new Promise((resolve, reject) => {
      const abort = new AbortController();
      let entered = false;
      let finished = false;
      let cancelTimer = () => {};

      const cleanup = () => {
        cancelTimer();
        this.pending.delete(cancel);
      };

      const finish = (status: BrowserSessionRefreshResult["status"]) => {
        if (finished) return;
        finished = true;
        cleanup();
        resolve(this.result(status));
      };

      const cancel = () => {
        if (entered || finished) return;
        finish(this.disposed ? "disposed" : "fallback");
        abort.abort();
      };

      const failed = (error: unknown) => {
        if (finished) return;

        if (!entered) {
          cancel();

          return;
        }

        finished = true;
        cleanup();
        reject(error);
      };

      this.pending.add(cancel);
      try {
        cancelTimer = this.environment.schedule(cancel, this.lockWaitMs);
        const request = locks.request(
          BROWSER_SESSION_PROTOCOL.lock,
          { mode: "exclusive", signal: abort.signal },
          async () => {
            // Also guards a late/non-cooperative adapter after timeout or disposal.
            if (finished || this.disposed) return;

            entered = true;
            cleanup();

            if (!this.readGeneration()) {
              finish("fallback");

              return;
            }

            if (this.generation > observedGeneration) {
              finish("advanced");

              return;
            }

            await refresh();

            if (this.disposed) {
              finish("disposed");

              return;
            }

            this.advanceGeneration(true);
            finish("refreshed");
          }
        );

        void request.then(() => {
          // Defensive handling if an adapter resolves without invoking the callback.
          if (!finished) cancel();
        }, failed);
      } catch (error) {
        failed(error);
      }
    });
  }

  /** Called after confirmed success only, including the app-owned single fallback attempt.
   * Without shared storage/locking, only the available local high-water mark is guaranteed.
   * Fallback never writes shared storage outside the lock; it announces local progress only.
   * This prevents a fallback writer from overwriting newer locked progress.
   */
  recordRefreshSuccess(): number {
    return this.advanceGeneration(false);
  }

  private advanceGeneration(persist: boolean): number {
    if (this.disposed) return this.generation;

    this.observeGeneration();

    if (this.generation === Number.MAX_SAFE_INTEGER) throw new Error("Refresh generation exhausted");

    this.generation += 1;
    try {
      if (persist) this.environment.storage?.setItem(BROWSER_SESSION_PROTOCOL.state, String(this.generation));
    } catch {
      // Retain local progress when persistence is unavailable.
    }

    this.publish("REFRESH_SUCCEEDED");

    return this.generation;
  }

  notifyCsrfReplaced(): void {
    this.publish("CSRF_REPLACED");
  }

  /** Advisory only. A receiver must establish authoritative state in its own application. */
  notifyAuthStateChanged(): void {
    this.publish("AUTH_STATE_CHANGED");
  }

  subscribe(listener: Listener): () => void {
    if (this.disposed) return () => {};

    this.listeners.add(listener);

    return () => {
      this.listeners.delete(listener);
    };
  }

  dispose(): void {
    if (this.disposed) return;

    this.disposed = true;
    for (const cancel of this.pending) cancel();

    this.pending.clear();
    this.listeners.clear();
    try {
      this.channel?.close();
    } catch {
      // A failed close cannot reactivate the receive guard.
    }

    this.channel = undefined;
  }

  private result(status: BrowserSessionRefreshResult["status"]): BrowserSessionRefreshResult {
    return Object.freeze({ status, generation: this.observeGeneration() });
  }

  private publish(kind: BrowserSessionEventKind): void {
    if (this.disposed) return;

    try {
      const timestamp = this.environment.now();
      if (!validGeneration(timestamp)) return;

      const event: BrowserSessionEvent = Object.freeze({
        version: 1,
        generation: this.observeGeneration(),
        kind,
        runtimeId: this.environment.runtimeId,
        timestamp,
      });

      this.channel?.postMessage(event);
    } catch {
      // No browser contents or application errors are logged or transmitted.
    }
  }

  private receive(message: unknown): void {
    if (this.disposed) return;

    const event = readEvent(message);
    if (!event || event.runtimeId === this.environment.runtimeId) return;

    this.generation = Math.max(this.generation, event.generation);
    for (const listener of this.listeners) {
      try {
        listener(event);
      } catch {
        // One subscriber cannot prevent another from observing an advisory notice.
      }
    }
  }
}
