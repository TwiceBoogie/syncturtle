import { Listener, Unsubscribe } from "@syncturtle/types";
import { Emitter } from "./common";

type AnyRecord = Record<string, unknown>;

function shallowEqual<T extends AnyRecord>(a: T, b: T): boolean {
  if (Object.is(a, b)) return true;

  const aKeys = Object.keys(a);
  const bKeys = Object.keys(b);
  if (aKeys.length !== bKeys.length) return false;

  for (const k of aKeys) {
    if (!Object.prototype.hasOwnProperty.call(b, k)) return false;
    if (!Object.is(a[k], b[k])) return false;
  }
  return true;
}

export abstract class ExternalStore<S extends AnyRecord> {
  private readonly emitter = new Emitter();
  private snap: S;

  private batchDepth = 0;
  private pendingEmit = false;

  protected constructor(initial: S) {
    this.snap = initial;
  }

  /** @internal - useSyncExternalStore subscribe */
  public _subscribe = (listener: Listener): Unsubscribe => this.emitter.subscribe(listener);

  /** @internal - useSyncExternalStore snapshot */
  public _getSnapshot = (): S => this.snap;

  /** @internal - useSyncExternalStore SSR snapshot */
  public _getServerSnapshot = (): S => this.snap;

  protected get state(): Readonly<S> {
    return this.snap;
  }

  protected setState(patch: Partial<S> | ((prev: Readonly<S>) => S)): void {
    const prev = this.snap;

    const next = typeof patch === "function" ? (patch as (p: Readonly<S>) => S)(prev) : ({ ...prev, ...patch } as S);

    if (Object.is(prev, next) || shallowEqual(prev, next)) return;

    this.snap = next;
    this.emit();
  }

  protected replaceState(next: S): void {
    const prev = this.snap;
    if (Object.is(prev, next) || shallowEqual(prev, next)) return;
    this.snap = next;
    this.emit();
  }

  protected batch(fn: () => void): void {
    this.batchDepth++;
    try {
      fn();
    } finally {
      this.batchDepth--;
      if (this.batchDepth === 0 && this.pendingEmit) {
        this.pendingEmit = false;
        this.emitter.emit();
      }
    }
  }

  private emit(): void {
    if (this.batchDepth > 0) {
      this.pendingEmit = true;
      return;
    }
    this.emitter.emit();
  }
}
