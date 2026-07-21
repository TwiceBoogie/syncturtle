import { UserService } from "@/services/user.service";
// syncturtle imports
import type { IUserSettings } from "@syncturtle/types";
import { ExternalStore } from "@syncturtle/utils";

type TError = {
  status: string;
  message: string;
};

export type TUserSettingsSnapshot = {
  isLoading: boolean;
  error: TError | undefined;
  data: IUserSettings;
  canUseLocalDB: boolean;
  sidebarCollapsed: boolean;
  isScrolled: boolean;
};

const createInitialSnapshot = (): TUserSettingsSnapshot => ({
  isLoading: false,
  error: undefined,
  data: {
    id: undefined,
    email: undefined,
    workspace: {
      lastWorkspaceId: undefined,
      lastWorkspaceSlug: undefined,
      lastWorkspaceName: undefined,
      lastWorkspaceLogo: undefined,
      fallbackWorkspaceId: undefined,
      fallbackWorkspaceSlug: undefined,
      invites: undefined,
    },
  },
  canUseLocalDB: false,
  sidebarCollapsed: true,
  isScrolled: false,
});

export interface IUserSettingsStore {
  // observables
  isLoading: boolean;
  error: TError | undefined;
  data: IUserSettings;
  canUseLocalDB: boolean;
  sidebarCollapsed: boolean;
  isScrolled: boolean;
  // actions
  fetchCurrentUserSettings: (bustCache?: boolean) => Promise<IUserSettings | undefined>;
  toggleSidebar: (collapsed?: boolean) => void;
  toggleIsScrolled: (isScrolled?: boolean) => void;
}

export interface IUserSettingsStoreInternal extends IUserSettingsStore {
  _subscribe: ExternalStore<TUserSettingsSnapshot>["_subscribe"];
  _getSnapshot: ExternalStore<TUserSettingsSnapshot>["_getSnapshot"];
  _getServerSnapshot: ExternalStore<TUserSettingsSnapshot>["_getServerSnapshot"];
}

export class UserSettingsStore extends ExternalStore<TUserSettingsSnapshot> implements IUserSettingsStoreInternal {
  private readonly userService: UserService;

  constructor() {
    super(createInitialSnapshot());
    this.userService = new UserService();
  }

  // raw getters for data
  get isLoading(): boolean {
    return this.state.isLoading;
  }

  get error(): TError | undefined {
    return this.state.error;
  }

  get data(): IUserSettings {
    return this.state.data;
  }

  get canUseLocalDB(): boolean {
    return this.state.canUseLocalDB;
  }

  get sidebarCollapsed(): boolean {
    return this.state.sidebarCollapsed;
  }

  get isScrolled(): boolean {
    return this.state.isScrolled;
  }

  public fetchCurrentUserSettings = async (bustCache: boolean = false) => {
    this.setState({ isLoading: true, error: undefined });

    try {
      const userSettings = await this.userService.currentUserSettings(bustCache);
      this.setState({ isLoading: false, data: userSettings });
      return userSettings;
    } catch (error) {
      this.setState({
        isLoading: false,
        error: {
          status: "user-settings-fetch-error",
          message: "Failed to fetch current user settings",
        },
      });
      throw error;
    }
  };

  public toggleSidebar = (collapsed?: boolean) => {
    this.setState((prev) => ({
      ...prev,
      sidebarCollapsed: collapsed === undefined ? !prev.sidebarCollapsed : collapsed,
    }));
  };

  public toggleIsScrolled = (isScrolled?: boolean) => {
    this.setState((prev) => ({
      ...prev,
      isScrolled: isScrolled ?? !prev.isScrolled,
    }));
  };
}
