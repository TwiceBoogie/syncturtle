import type { Listener, Unsubscribe } from "@syncturtle/types";
import { Emitter } from "@syncturtle/utils";
import { ParsedUrlQuery } from "node:querystring";

export type TRouterSnapshot = {
  query: ParsedUrlQuery;
};

const initialSnapshot: TRouterSnapshot = {
  query: {},
};

export interface IRouterStoreInternal {
  _subscribe(listener: Listener): Unsubscribe;
  _getSnapshot(): TRouterSnapshot;
  _getServerSnapshot(): TRouterSnapshot;
}

export type TRouterStore = Omit<RouterStore, keyof IRouterStoreInternal>;

/**
 * External store for router
 */
export class RouterStore implements IRouterStoreInternal {
  private emitter = new Emitter();
  private _snap: TRouterSnapshot = initialSnapshot;

  // useSyncExternalStore integration
  /** @internal */
  public _subscribe = (listener: Listener): Unsubscribe => this.emitter.subscribe(listener);
  /** @internal */
  public _getSnapshot = (): TRouterSnapshot => this._snap;
  /** @internal */
  public _getServerSnapshot = (): TRouterSnapshot => this._snap;

  // raw getters for data
  get query(): ParsedUrlQuery {
    return this._snap.query;
  }

  get workspaceSlug() {
    return this.getQueryValue("workspaceSlug");
  }

  get userId() {
    return this.getQueryValue("userId");
  }

  // actions

  /**
   * useParams() can return a new object identity across nav.
   * Since `set() -> next = {...prev, ...patch}` the query object
   * will always be different when a new object is passed
   */
  public setQuery = (query: ParsedUrlQuery) => {
    const normalized: ParsedUrlQuery = {};
    for (const [k, v] of Object.entries(query)) {
      normalized[k] = Array.isArray(v) ? String(v[0] ?? "") : String(v ?? "");
    }
    this.set({ query: normalized });
  };

  private getQueryValue(key: keyof ParsedUrlQuery): string | undefined {
    const value = this._snap.query?.[key];
    if (Array.isArray(value)) {
      return value[0]?.toString();
    }
    return value?.toString();
  }

  private set(patch: Partial<TRouterSnapshot>): void {
    const prev = this._snap;
    const next = { ...prev, ...patch };

    let changed = false;
    for (const key in next) {
      const k = key as keyof TRouterSnapshot;
      if (!Object.is(prev[k], next[k])) {
        changed = true;
        break;
      }
    }

    if (!changed) return;

    this._snap = next;
    this.emitter.emit();
  }
}
