import { ExternalStore } from "@syncturtle/utils";

const SIDEBAR_COLLAPSED_STORAGE_KEY = "app_sidebar_collapsed";

export type TThemeSnapshot = {
  sidebarCollapsed: boolean | undefined;
};

const createInitialSnapshot = (): TThemeSnapshot => ({
  sidebarCollapsed: undefined,
});

export interface IThemeStore {
  // observables
  sidebarCollapsed: boolean | undefined;
  // actions
  hydrateSidebarCollapsed: () => void;
  toggleSidebar: (collapsed?: boolean) => void;
  reset: () => void;
}

export interface IThemeStoreInternal extends IThemeStore {
  _subscribe: ExternalStore<TThemeSnapshot>["_subscribe"];
  _getSnapshot: ExternalStore<TThemeSnapshot>["_getSnapshot"];
  _getServerSnapshot: ExternalStore<TThemeSnapshot>["_getServerSnapshot"];
}

export class ThemeStore extends ExternalStore<TThemeSnapshot> implements IThemeStoreInternal {
  constructor() {
    super(createInitialSnapshot());
  }

  // raw getters for data
  get sidebarCollapsed(): boolean | undefined {
    return this.state.sidebarCollapsed;
  }

  // actions
  public hydrateSidebarCollapsed = (): void => {
    if (typeof window === "undefined") return;

    const storedValue = window.localStorage.getItem(SIDEBAR_COLLAPSED_STORAGE_KEY);

    if (storedValue === null) return;

    const sidebarCollapsed = this.parseStoredBoolean(storedValue);

    if (sidebarCollapsed === undefined) return;

    this.setSidebarCollapsed(sidebarCollapsed, {
      persist: false,
    });
  };

  public toggleSidebar = (collapsed?: boolean) => {
    const next = this.computeNextSidebarCollapsed(this.state.sidebarCollapsed, collapsed);

    this.setSidebarCollapsed(next, {
      persist: true,
    });
  };

  public reset = (): void => {
    this.replaceState(createInitialSnapshot());
  };

  // internal helpers
  private setSidebarCollapsed(sidebarCollapsed: boolean, options: { persist: boolean }): void {
    if (Object.is(this.state.sidebarCollapsed, sidebarCollapsed)) return;

    this.setState({ sidebarCollapsed });

    if (options.persist) {
      this.persistSidebarCollapsed(sidebarCollapsed);
    }
  }

  private computeNextSidebarCollapsed(current: boolean | undefined, collapsed?: boolean): boolean {
    if (collapsed !== undefined) return collapsed;

    return !(current ?? false);
  }

  private parseStoredBoolean(value: string): boolean | undefined {
    if (value === "true") return true;
    if (value === "false") return false;

    return undefined;
  }

  private persistSidebarCollapsed(sidebarCollapsed: boolean): void {
    if (typeof window === "undefined") return;

    window.localStorage.setItem(SIDEBAR_COLLAPSED_STORAGE_KEY, String(sidebarCollapsed));
  }
}
