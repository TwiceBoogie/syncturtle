import { ExternalStore } from "@syncturtle/utils";

type TTheme = "dark" | "light";

export type TThemeSnapshot = {
  isNewUserPopup: boolean;
  theme: string | undefined;
  isSidebarCollapsed: boolean | undefined;
};

const initialSnapshot: TThemeSnapshot = {
  isNewUserPopup: false,
  theme: undefined,
  isSidebarCollapsed: undefined,
};

export interface IThemeStore {
  // observables
  isNewUserPopup: boolean;
  theme: string | undefined;
  isSidebarCollapsed: boolean | undefined;
  // actions
  toggleNewUserPopup: () => void;
  toggleSidebar: (collapsed: boolean) => void;
  setTheme: (currentTheme: TTheme) => void;
}

export interface IThemeStoreInternal extends IThemeStore {
  _subscribe: ExternalStore<TThemeSnapshot>["_subscribe"];
  _getSnapshot: ExternalStore<TThemeSnapshot>["_getSnapshot"];
  _getServerSnapshot: ExternalStore<TThemeSnapshot>["_getServerSnapshot"];
}

export class ThemeStore extends ExternalStore<TThemeSnapshot> implements IThemeStoreInternal {
  constructor() {
    super(initialSnapshot);
  }

  // raw getters for data
  get isNewUserPopup(): boolean {
    return this.state.isNewUserPopup;
  }

  get theme(): string | undefined {
    return this.state.theme;
  }

  get isSidebarCollapsed(): boolean | undefined {
    return this.state.isSidebarCollapsed;
  }

  public toggleNewUserPopup = () => {
    this.setState((prev) => ({
      ...prev,
      isNewUserPopup: !prev.isNewUserPopup,
    }));
  };

  public toggleSidebar = (isCollapsed: boolean) => {
    this.setState((prev) => ({
      ...prev,
      isSidebarCollapsed: isCollapsed === undefined ? !prev.isSidebarCollapsed : isCollapsed,
    }));
    localStorage.setItem("god_mode_sidebar_collapsed", isCollapsed.toString());
  };

  public setTheme = async (currentTheme: TTheme) => {
    try {
      localStorage.setItem("theme", currentTheme);
      this.setState({ theme: currentTheme });
    } catch (error) {
      console.error("setting user theme error", error);
    }
  };
}
