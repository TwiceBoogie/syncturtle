import type { Listener, Unsubscribe } from "@syncturtle/types";
import { Emitter } from "@syncturtle/utils";

export type TThemeSnapshot = {
  sidebarCollapsed: boolean | undefined;
};

const initialSnapshot: TThemeSnapshot = {
  sidebarCollapsed: undefined,
};

export interface IThemeStoreInternal {
  _subscribe(listener: Listener): Unsubscribe;
  _getSnapshot(): TThemeSnapshot;
  _getServerSnapshot(): TThemeSnapshot;
  // observables
  sidebarCollapsed: boolean | undefined;
  toggleSidebar: (collapsed?: boolean) => void;
}

export type TThemeStore = Omit<IThemeStoreInternal, "_subscribe" | "_getSnapshot" | "_getServerSnapshot">;

export class ThemeStore implements IThemeStoreInternal {
  private emitter = new Emitter();
  private _snap: TThemeSnapshot = initialSnapshot;

  // useSyncExternalStore integration
  /** @internal */
  public _subscribe = (listener: Listener): Unsubscribe => this.emitter.subscribe(listener);
  /** @internal */
  public _getSnapshot = (): TThemeSnapshot => this._snap;
  /** @internal */
  public _getServerSnapshot = (): TThemeSnapshot => this._snap;

  // raw getters for data
  get sidebarCollapsed(): boolean | undefined {
    return this._snap.sidebarCollapsed;
  }

  public toggleSidebar = (collapsed?: boolean) => {
    const next = this.computeNext(this._snap.sidebarCollapsed, collapsed);
    this.set({ sidebarCollapsed: next });
    if (typeof window !== "undefined") {
      localStorage.setItem("app_sidebar_collapsed", next.toString());
    }
  };

  private computeNext(current: boolean | undefined, collapsed?: boolean): boolean {
    if (collapsed === undefined) {
      const prev = current ?? false;
      return !prev;
    }
    return collapsed;
  }

  private set(patch: Partial<TThemeSnapshot>) {
    const prev = this._snap;
    const next = { ...prev, ...patch };

    let changed = false;
    for (const key in next) {
      const k = key as keyof TThemeSnapshot;
      if (!Object.is(next[k], prev[k])) {
        changed = true;
        break;
      }
    }

    if (!changed) {
      return;
    }

    this._snap = next;
    this.emitter.emit();
  }
}
