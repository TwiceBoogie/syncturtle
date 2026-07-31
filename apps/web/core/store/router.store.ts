import type { ParsedUrlQuery } from "node:querystring";
import { ExternalStore } from "@syncturtle/utils";

export type TRouterSnapshot = {
  query: ParsedUrlQuery;
};

const createInitialSnapshot = (): TRouterSnapshot => ({
  query: {},
});

export interface IRouterStore {
  // observables
  query: ParsedUrlQuery;
  // computed
  workspaceSlug: string | undefined;
  userId: string | undefined;
  // actions
  setQuery: (query: ParsedUrlQuery) => void;
}

export interface IRouterStoreInternal extends IRouterStore {
  _subscribe: ExternalStore<TRouterSnapshot>["_subscribe"];
  _getSnapshot: ExternalStore<TRouterSnapshot>["_getSnapshot"];
  _getServerSnapshot: ExternalStore<TRouterSnapshot>["_getServerSnapshot"];
}

/**
 * External store for router
 */
export class RouterStore extends ExternalStore<TRouterSnapshot> implements IRouterStoreInternal {
  constructor() {
    super(createInitialSnapshot());
  }

  // raw getters for data
  get query(): ParsedUrlQuery {
    return this.state.query;
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
    const normalized = this.normalizeQuery(query);

    if (this.areQueriesEqual(this.state.query, normalized)) {
      return;
    }

    this.setState({ query: normalized });
  };

  private normalizeQuery(query: ParsedUrlQuery): ParsedUrlQuery {
    const normalized: ParsedUrlQuery = {};

    for (const [key, value] of Object.entries(query)) {
      normalized[key] = Array.isArray(value) ? String(value[0] ?? "") : String(value ?? "");
    }

    return normalized;
  }

  private areQueriesEqual(a: ParsedUrlQuery, b: ParsedUrlQuery): boolean {
    const aKeys = Object.keys(a);
    const bKeys = Object.keys(b);

    if (aKeys.length !== bKeys.length) return false;

    for (const key of aKeys) {
      if (a[key]?.toString() !== b[key]?.toString()) return false;
    }

    return true;
  }

  private getQueryValue(key: keyof ParsedUrlQuery): string | undefined {
    const value = this.state.query?.[key];
    if (Array.isArray(value)) {
      return value[0]?.toString();
    }
    return value?.toString();
  }
}
