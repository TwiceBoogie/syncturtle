PROJECT CONTEXT: State management system

## 1. Context

- **My State Management:** I make use of `useSyncExternalStore()`
- **Plane State Management:** They make use of mobx and wrap components with observer()

## 2. System Architecture

### A. My version

File: syncturtle/apps/web/core/lib/store-context.tsx

```tsx
"use client";

import type { ReactNode } from "react";
import { createContext, useState } from "react";
import { RootStore } from "@/syncturtle-web/store/root.store";

let rootStore: RootStore | null = null;

const createRootStore = (): RootStore => new RootStore();

const initializeStore = () => {
  if (typeof window === "undefined") {
    return createRootStore();
  }

  if (!rootStore) {
    rootStore = createRootStore();
  }

  return rootStore;
};

export const StoreContext = createContext<RootStore | null>(null);

export const StoreProvider = ({ children }: { children: ReactNode }) => {
  const [store] = useState<RootStore>(() => initializeStore());

  return <StoreContext.Provider value={store}>{children}</StoreContext.Provider>;
};
```

File: syncturtle/apps/web/ce/store/root.store.ts

```ts
import { CoreRootStore } from "@/store/root.store";

export class RootStore extends CoreRootStore {
  constructor() {
    super();
  }
}
```

File: syncturtle/apps/web/core/store/root.store.ts

```ts
import { ThemeStore } from "@/store/theme.store";
import { RouterStore } from "./router.store";
import { InstanceStore } from "./instance.store";
import { UserStore } from "./user";
import { WorkspaceStore } from "./workspace";
import { MemberRootStore } from "./member";

export class CoreRootStore {
  workspace: WorkspaceStore;
  router: RouterStore;
  theme: ThemeStore;
  instance: InstanceStore;
  user: UserStore;
  memberRoot: MemberRootStore;

  constructor() {
    this.router = new RouterStore();
    this.theme = new ThemeStore();
    this.instance = new InstanceStore();
    this.user = new UserStore(this);
    this.workspace = new WorkspaceStore(this);
    this.memberRoot = new MemberRootStore(this);
  }

  resetOnSignOut() {}
}
```

File: syncturtle/apps/web/core/store/user/index.ts

```ts
import { cloneDeep, merge } from "lodash";
import { ProfileStore, type IUserProfileStore } from "./profile.store";
import { UserService } from "@/services/user.service";
import { RootStore } from "@/syncturtle-web/store/root.store";
import { AuthService } from "@/services/auth.service";
import { UserSettingsStore, type IUserSettingsStore } from "./settings.store";
// syncturtle imports
import { ExternalStore } from "@syncturtle/utils";
import type { IUser } from "@syncturtle/types";

type TError = {
  status: string;
  message: string;
};

export type TUserSnapshot = {
  isLoading: boolean;
  isAuthenticated: boolean;
  data: IUser | undefined;
  error: TError | undefined;
};

const createInitialSnapshot = (): TUserSnapshot => ({
  isLoading: false,
  isAuthenticated: false,
  data: undefined,
  error: undefined,
});

export interface IUserStore {
  // observables
  isLoading: boolean;
  isAuthenticated: boolean;
  data: IUser | undefined;
  error: TError | undefined;
  // store observables
  userProfile: IUserProfileStore;
  userSettings: IUserSettingsStore;
  // computed
  //   localDBEnabled: boolean;
  //   canPerformAnyCreateAction: boolean;
  // actions
  fetchCurrentUser: () => Promise<IUser | undefined>;
  updateCurrentUser: (data: Partial<IUser>) => Promise<IUser | undefined>;
  handleSetPassword: (data: { password: string }) => Promise<IUser | undefined>;
  changePassword: (payload: { oldPassword?: string; newPassword: string }) => Promise<IUser | undefined>;
  deactivateAccount: () => Promise<void>;
  reset: () => void;
  signOut: () => Promise<void>;
}

export interface IUserStoreInternal extends IUserStore {
  _subscribe: ExternalStore<TUserSnapshot>["_subscribe"];
  _getSnapshot: ExternalStore<TUserSnapshot>["_getSnapshot"];
  _getServerSnapshot: ExternalStore<TUserSnapshot>["_getServerSnapshot"];
}

export class UserStore extends ExternalStore<TUserSnapshot> implements IUserStoreInternal {
  private readonly userService: UserService;
  private readonly authService: AuthService;
  // sub-stores
  public userProfile: ProfileStore;
  public userSettings: UserSettingsStore;

  private fetchCurrentUserSeq = 0;

  constructor(private readonly _store: RootStore) {
    super(createInitialSnapshot());

    this.userService = new UserService();
    this.authService = new AuthService();

    this.userProfile = new ProfileStore(_store, { userService: this.userService });
    this.userSettings = new UserSettingsStore();
  }

  // raw getters for data
  get isLoading(): boolean {
    return this.state.isLoading;
  }

  get isAuthenticated(): boolean {
    return this.state.isAuthenticated;
  }

  get data(): IUser | undefined {
    return this.state.data;
  }

  get error(): TError | undefined {
    return this.state.error;
  }

  public fetchCurrentUser = async (): Promise<IUser> => {
    const requestSeq = ++this.fetchCurrentUserSeq;

    this.setState({ isLoading: true, error: undefined });

    try {
      const user = await this.userService.currentUser();

      if (requestSeq !== this.fetchCurrentUserSeq) {
        return user;
      }

      if (user && user?.id) {
        await Promise.all([
          this.userProfile.fetchUserProfile(),
          this.userSettings.fetchCurrentUserSettings(),
          this._store.workspace.fetchWorkspaces(),
        ]);

        if (requestSeq !== this.fetchCurrentUserSeq) {
          return user;
        }

        this.setState({ data: user, isLoading: false, isAuthenticated: true });
      } else {
        this.setState({ data: user, isLoading: false, isAuthenticated: false });
      }
      return user;
    } catch (error) {
      this.setState({
        isLoading: false,
        isAuthenticated: false,
        error: {
          status: "user-fetch-error",
          message: "Failed to fetch current user",
        },
      });
      throw error;
    }
  };

  public updateCurrentUser = async (data: Partial<IUser>): Promise<IUser> => {
    const currentUserData = this.data;

    if (!currentUserData) {
      return await this.userService.updateUser(data);
    }

    // optimistic
    this.setState((s) => ({
      ...s,
      data: this.mutateUser(currentUserData, data),
      error: undefined,
    }));

    try {
      const user = await this.userService.updateUser(data);
      if (user) {
        this.setState({ data: user });
      }
      return user;
    } catch (error) {
      // revert
      this.setState({
        data: currentUserData,
        error: {
          status: "user-update-error",
          message: "Failed to update current user",
        },
      });
      throw error;
    }
  };

  public handleSetPassword = async (payload: { password: string }): Promise<IUser | undefined> => {
    const currentUserData = this.data;

    if (!currentUserData) return undefined;

    if (!currentUserData.isPasswordAutoset) return undefined;

    // optimistic: flip the flag locally
    this.setState((s) => ({
      ...s,
      data: s.data ? { ...s.data, isPasswordAutoset: false } : s.data,
      error: undefined,
    }));

    try {
      const user = await this.authService.setPassword(payload);
      if (user) {
        this.setState({ data: user });
        return user;
      }

      return this.data;
    } catch (error) {
      // revert optimistic flip
      this.setState({
        data: currentUserData,
        error: {
          status: "user-update-error",
          message: "Failed to set password",
        },
      });
      throw error;
    }
  };

  public changePassword = async (payload: {
    oldPassword?: string;
    newPassword: string;
  }): Promise<IUser | undefined> => {
    try {
      const user = await this.userService.changePassword(payload);

      this.setState((s) => ({
        ...s,
        data: s.data ? { ...s.data, isPasswordAutoset: false } : s.data,
        error: undefined,
      }));

      return user;
    } catch (error) {
      this.setState({
        error: {
          status: "user-password-change-error",
          message: "Failed to change password",
        },
      });
      throw error;
    }
  };

  public deactivateAccount = async (): Promise<void> => {};

  public signOut = async (): Promise<void> => {
    try {
      await this.authService.signOut();
    } catch (error) {
      console.log(error);
      throw error;
    }
  };

  public reset = (): void => {
    this.replaceState(createInitialSnapshot());
  };

  private mutateUser = (prev: IUser, patch: Partial<IUser>): IUser => {
    const next = cloneDeep(prev);
    merge(next, patch);
    return next;
  };
}
```

File: syncturtle/packages/utils/src/external-store.ts

```ts
import type { Listener, Unsubscribe } from "@syncturtle/types";
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
```

File: syncturtle/packages/utils/src/common.ts

```ts
import type { Listener, Unsubscribe } from "@syncturtle/types";

/**
 * pub/sub emitter used by external stores.
 *
 * - subscribe(listener) -> returns unsubscribe()
 * - emit() -> calls all listeners
 *
 * In a react external store, `emit()` should be called AFTER updating snapshot.
 */
export class Emitter {
  private listeners = new Set<Listener>();

  /**
   * Registers a listener to be called on every store update.
   * Returns a `Unsubscribe` function that removes that listener.
   */
  public subscribe = (listener: Listener): Unsubscribe => {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  };

  /**
   * Notify all listeners that something changed.
   *
   * React will call `getSnapshot` again and compare values.
   */
  public emit = (): void => this.listeners.forEach((listener) => listener());
}
```

File: syncturtle/apps/web/core/hooks/store/user/use-user.ts

```ts
import { useContext, useSyncExternalStore } from "react";
import { StoreContext } from "@/lib/store-context";
import type { IUserStore } from "@/store/user";

export const useUser = (): IUserStore => {
  const context = useContext(StoreContext);
  if (!context) throw new Error("useUser must be used inside a StoreProvider");
  const store = context.user;
  useSyncExternalStore(store._subscribe, store._getSnapshot, store._getServerSnapshot);

  return store;
};
```

File: syncturtle/apps/web/core/hooks/store/user/use-user-profile.ts

```ts
import { useContext, useSyncExternalStore } from "react";
import { StoreContext } from "@/lib/store-context";
import type { IUserProfileStore } from "@/store/user/profile.store";

export const useUserProfile = (): IUserProfileStore => {
  const context = useContext(StoreContext);
  if (!context) throw new Error("useUserProfile must be used inside a StoreProvider");
  const store = context.user.userProfile;
  useSyncExternalStore(store._subscribe, store._getSnapshot, store._getServerSnapshot);

  return store;
};
```

### B. plane's version

File: plane/apps/web/core/lib/store-context.tsx

```tsx
"use client";

import type { ReactElement } from "react";
import { createContext } from "react";
// plane web store
import { RootStore } from "@/plane-web/store/root.store";

export let rootStore = new RootStore();

export const StoreContext = createContext<RootStore>(rootStore);

const initializeStore = () => {
  const newRootStore = rootStore ?? new RootStore();
  if (typeof window === "undefined") return newRootStore;
  if (!rootStore) rootStore = newRootStore;
  return newRootStore;
};

export const store = initializeStore();

export const StoreProvider = ({ children }: { children: ReactElement }) => (
  <StoreContext.Provider value={store}>{children}</StoreContext.Provider>
);
```

File: plane/apps/web/ce/store/root.store.ts

```ts
// store
import { CoreRootStore } from "@/store/root.store";
import type { ITimelineStore } from "./timeline";
import { TimeLineStore } from "./timeline";

export class RootStore extends CoreRootStore {
  timelineStore: ITimelineStore;

  constructor() {
    super();

    this.timelineStore = new TimeLineStore(this);
  }
}
```

File: plane/apps/web/core/store/root.store.ts

```ts
import { enableStaticRendering } from "mobx-react";
// plane imports
import { FALLBACK_LANGUAGE, LANGUAGE_STORAGE_KEY } from "@plane/i18n";
import type { IWorkItemFilterStore } from "@plane/shared-state";
import { WorkItemFilterStore } from "@plane/shared-state";
// plane web store
import type { IAnalyticsStore } from "@/plane-web/store/analytics.store";
import { AnalyticsStore } from "@/plane-web/store/analytics.store";
import type { ICommandPaletteStore } from "@/plane-web/store/command-palette.store";
import { CommandPaletteStore } from "@/plane-web/store/command-palette.store";
import type { RootStore } from "@/plane-web/store/root.store";
import type { IStateStore } from "@/plane-web/store/state.store";
import { StateStore } from "@/plane-web/store/state.store";
// stores
import type { ICycleStore } from "./cycle.store";
import { CycleStore } from "./cycle.store";
import type { ICycleFilterStore } from "./cycle_filter.store";
import { CycleFilterStore } from "./cycle_filter.store";
import type { IDashboardStore } from "./dashboard.store";
import { DashboardStore } from "./dashboard.store";
import type { IEditorAssetStore } from "./editor/asset.store";
import { EditorAssetStore } from "./editor/asset.store";
import type { IProjectEstimateStore } from "./estimates/project-estimate.store";
import { ProjectEstimateStore } from "./estimates/project-estimate.store";
import type { IFavoriteStore } from "./favorite.store";
import { FavoriteStore } from "./favorite.store";
import type { IGlobalViewStore } from "./global-view.store";
import { GlobalViewStore } from "./global-view.store";
import type { IProjectInboxStore } from "./inbox/project-inbox.store";
import { ProjectInboxStore } from "./inbox/project-inbox.store";
import type { IInstanceStore } from "./instance.store";
import { InstanceStore } from "./instance.store";
import type { IIssueRootStore } from "./issue/root.store";
import { IssueRootStore } from "./issue/root.store";
import type { ILabelStore } from "./label.store";
import { LabelStore } from "./label.store";
import type { IMemberRootStore } from "./member";
import { MemberRootStore } from "./member";
import type { IModuleStore } from "./module.store";
import { ModulesStore } from "./module.store";
import type { IModuleFilterStore } from "./module_filter.store";
import { ModuleFilterStore } from "./module_filter.store";
import type { IMultipleSelectStore } from "./multiple_select.store";
import { MultipleSelectStore } from "./multiple_select.store";
import type { IWorkspaceNotificationStore } from "./notifications/workspace-notifications.store";
import { WorkspaceNotificationStore } from "./notifications/workspace-notifications.store";
import type { IProjectPageStore } from "./pages/project-page.store";
import { ProjectPageStore } from "./pages/project-page.store";
import type { IProjectRootStore } from "./project";
import { ProjectRootStore } from "./project";
import type { IProjectViewStore } from "./project-view.store";
import { ProjectViewStore } from "./project-view.store";
import type { IRouterStore } from "./router.store";
import { RouterStore } from "./router.store";
import type { IStickyStore } from "./sticky/sticky.store";
import { StickyStore } from "./sticky/sticky.store";
import type { IThemeStore } from "./theme.store";
import { ThemeStore } from "./theme.store";
import type { ITransientStore } from "./transient.store";
import { TransientStore } from "./transient.store";
import type { IUserStore } from "./user";
import { UserStore } from "./user";
import type { IWorkspaceRootStore } from "./workspace";
import { WorkspaceRootStore } from "./workspace";

enableStaticRendering(typeof window === "undefined");

export class CoreRootStore {
  workspaceRoot: IWorkspaceRootStore;
  projectRoot: IProjectRootStore;
  memberRoot: IMemberRootStore;
  cycle: ICycleStore;
  cycleFilter: ICycleFilterStore;
  module: IModuleStore;
  moduleFilter: IModuleFilterStore;
  projectView: IProjectViewStore;
  globalView: IGlobalViewStore;
  issue: IIssueRootStore;
  state: IStateStore;
  label: ILabelStore;
  dashboard: IDashboardStore;
  analytics: IAnalyticsStore;
  projectPages: IProjectPageStore;
  router: IRouterStore;
  commandPalette: ICommandPaletteStore;
  theme: IThemeStore;
  instance: IInstanceStore;
  user: IUserStore;
  projectInbox: IProjectInboxStore;
  projectEstimate: IProjectEstimateStore;
  multipleSelect: IMultipleSelectStore;
  workspaceNotification: IWorkspaceNotificationStore;
  favorite: IFavoriteStore;
  transient: ITransientStore;
  stickyStore: IStickyStore;
  editorAssetStore: IEditorAssetStore;
  workItemFilters: IWorkItemFilterStore;

  constructor() {
    this.router = new RouterStore();
    this.commandPalette = new CommandPaletteStore();
    this.instance = new InstanceStore();
    this.user = new UserStore(this as unknown as RootStore);
    this.theme = new ThemeStore();
    this.workspaceRoot = new WorkspaceRootStore(this);
    this.projectRoot = new ProjectRootStore(this);
    this.memberRoot = new MemberRootStore(this as unknown as RootStore);
    this.cycle = new CycleStore(this);
    this.cycleFilter = new CycleFilterStore(this);
    this.module = new ModulesStore(this);
    this.moduleFilter = new ModuleFilterStore(this);
    this.projectView = new ProjectViewStore(this);
    this.globalView = new GlobalViewStore(this);
    this.issue = new IssueRootStore(this as unknown as RootStore);
    this.state = new StateStore(this as unknown as RootStore);
    this.label = new LabelStore(this);
    this.dashboard = new DashboardStore(this);
    this.multipleSelect = new MultipleSelectStore();
    this.projectInbox = new ProjectInboxStore(this);
    this.projectPages = new ProjectPageStore(this as unknown as RootStore);
    this.projectEstimate = new ProjectEstimateStore(this);
    this.workspaceNotification = new WorkspaceNotificationStore(this);
    this.favorite = new FavoriteStore(this);
    this.transient = new TransientStore();
    this.stickyStore = new StickyStore();
    this.editorAssetStore = new EditorAssetStore();
    this.analytics = new AnalyticsStore();
    this.workItemFilters = new WorkItemFilterStore();
  }

  resetOnSignOut() {
    // handling the system theme when user logged out from the app
    localStorage.setItem("theme", "system");
    localStorage.setItem(LANGUAGE_STORAGE_KEY, FALLBACK_LANGUAGE);
    this.router = new RouterStore();
    this.commandPalette = new CommandPaletteStore();
    this.instance = new InstanceStore();
    this.user = new UserStore(this as unknown as RootStore);
    this.workspaceRoot = new WorkspaceRootStore(this);
    this.projectRoot = new ProjectRootStore(this);
    this.memberRoot = new MemberRootStore(this as unknown as RootStore);
    this.cycle = new CycleStore(this);
    this.cycleFilter = new CycleFilterStore(this);
    this.module = new ModulesStore(this);
    this.moduleFilter = new ModuleFilterStore(this);
    this.projectView = new ProjectViewStore(this);
    this.globalView = new GlobalViewStore(this);
    this.issue = new IssueRootStore(this as unknown as RootStore);
    this.state = new StateStore(this as unknown as RootStore);
    this.label = new LabelStore(this);
    this.dashboard = new DashboardStore(this);
    this.projectInbox = new ProjectInboxStore(this);
    this.projectPages = new ProjectPageStore(this as unknown as RootStore);
    this.multipleSelect = new MultipleSelectStore();
    this.projectEstimate = new ProjectEstimateStore(this);
    this.workspaceNotification = new WorkspaceNotificationStore(this);
    this.favorite = new FavoriteStore(this);
    this.transient = new TransientStore();
    this.stickyStore = new StickyStore();
    this.editorAssetStore = new EditorAssetStore();
    this.workItemFilters = new WorkItemFilterStore();
  }
}
```

File: plane/apps/web/core/store/user/index.ts

```ts
import { cloneDeep, set } from "lodash-es";
import { action, makeObservable, observable, runInAction, computed } from "mobx";
// plane imports
import { EUserPermissions, API_BASE_URL } from "@plane/constants";
import type { IUser, TUserPermissions } from "@plane/types";
// local
import { persistence } from "@/local-db/storage.sqlite";
// plane web imports
import type { RootStore } from "@/plane-web/store/root.store";
import type { IUserPermissionStore } from "@/plane-web/store/user/permission.store";
import { UserPermissionStore } from "@/plane-web/store/user/permission.store";
// services
import { AuthService } from "@/services/auth.service";
import { UserService } from "@/services/user.service";
// stores
import type { IAccountStore } from "@/store/user/account.store";
import type { IUserProfileStore } from "@/store/user/profile.store";
import { ProfileStore } from "@/store/user/profile.store";
// local imports
import type { IUserSettingsStore } from "./settings.store";
import { UserSettingsStore } from "./settings.store";

type TUserErrorStatus = {
  status: string;
  message: string;
};

export interface IUserStore {
  // observables
  isAuthenticated: boolean;
  isLoading: boolean;
  error: TUserErrorStatus | undefined;
  data: IUser | undefined;
  // store observables
  userProfile: IUserProfileStore;
  userSettings: IUserSettingsStore;
  accounts: Record<string, IAccountStore>;
  permission: IUserPermissionStore;
  // actions
  fetchCurrentUser: () => Promise<IUser | undefined>;
  updateCurrentUser: (data: Partial<IUser>) => Promise<IUser | undefined>;
  handleSetPassword: (csrfToken: string, data: { password: string }) => Promise<IUser | undefined>;
  deactivateAccount: () => Promise<void>;
  changePassword: (
    csrfToken: string,
    payload: { old_password?: string; new_password: string }
  ) => Promise<IUser | undefined>;
  reset: () => void;
  signOut: () => Promise<void>;
  // computed
  localDBEnabled: boolean;
  canPerformAnyCreateAction: boolean;
  projectsWithCreatePermissions: { [projectId: string]: number } | null;
}

export class UserStore implements IUserStore {
  // observables
  isAuthenticated: boolean = false;
  isLoading: boolean = false;
  error: TUserErrorStatus | undefined = undefined;
  data: IUser | undefined = undefined;
  // store observables
  userProfile: IUserProfileStore;
  userSettings: IUserSettingsStore;
  accounts: Record<string, IAccountStore> = {};
  permission: IUserPermissionStore;
  // service
  userService: UserService;
  authService: AuthService;

  constructor(private store: RootStore) {
    // stores
    this.userProfile = new ProfileStore(store);
    this.userSettings = new UserSettingsStore();
    this.permission = new UserPermissionStore(store);
    // service
    this.userService = new UserService();
    this.authService = new AuthService();
    // observables
    makeObservable(this, {
      // observables
      isAuthenticated: observable.ref,
      isLoading: observable.ref,
      error: observable,
      // model observables
      data: observable,
      userProfile: observable,
      userSettings: observable,
      accounts: observable,
      permission: observable,
      // actions
      fetchCurrentUser: action,
      updateCurrentUser: action,
      handleSetPassword: action,
      deactivateAccount: action,
      changePassword: action,
      reset: action,
      signOut: action,
      // computed
      canPerformAnyCreateAction: computed,
      projectsWithCreatePermissions: computed,

      localDBEnabled: computed,
    });
  }

  /**
   * @description fetches the current user
   * @returns {Promise<IUser>}
   */
  fetchCurrentUser = async (): Promise<IUser> => {
    try {
      runInAction(() => {
        this.isLoading = true;
        this.error = undefined;
      });
      const user = await this.userService.currentUser();
      if (user && user?.id) {
        await Promise.all([
          this.userProfile.fetchUserProfile(),
          this.userSettings.fetchCurrentUserSettings(),
          this.store.workspaceRoot.fetchWorkspaces(),
        ]);
        runInAction(() => {
          this.data = user;
          this.isLoading = false;
          this.isAuthenticated = true;
        });
      } else
        runInAction(() => {
          this.data = user;
          this.isLoading = false;
          this.isAuthenticated = false;
        });
      return user;
    } catch (error) {
      runInAction(() => {
        this.isLoading = false;
        this.isAuthenticated = false;
        this.error = {
          status: "user-fetch-error",
          message: "Failed to fetch current user",
        };
      });
      throw error;
    }
  };

  /**
   * @description updates the current user
   * @param data
   * @returns {Promise<IUser>}
   */
  updateCurrentUser = async (data: Partial<IUser>): Promise<IUser> => {
    const currentUserData = this.data;
    try {
      if (currentUserData) {
        Object.keys(data).forEach((key: string) => {
          const userKey: keyof IUser = key as keyof IUser;
          if (this.data) set(this.data, userKey, data[userKey]);
        });
      }
      const user = await this.userService.updateUser(data);
      return user;
    } catch (error) {
      if (currentUserData) {
        Object.keys(currentUserData).forEach((key: string) => {
          const userKey: keyof IUser = key as keyof IUser;
          if (this.data) set(this.data, userKey, currentUserData[userKey]);
        });
      }
      runInAction(() => {
        this.error = {
          status: "user-update-error",
          message: "Failed to update current user",
        };
      });
      throw error;
    }
  };

  /**
   * @description update the user password
   * @param data
   * @returns {Promise<IUser>}
   */
  handleSetPassword = async (csrfToken: string, data: { password: string }): Promise<IUser | undefined> => {
    const currentUserData = cloneDeep(this.data);
    try {
      if (currentUserData && currentUserData.is_password_autoset && this.data) {
        const user = await this.authService.setPassword(csrfToken, { password: data.password });
        set(this.data, ["is_password_autoset"], false);
        return user;
      }
      return undefined;
    } catch (error) {
      if (this.data) set(this.data, ["is_password_autoset"], true);
      runInAction(() => {
        this.error = {
          status: "user-update-error",
          message: "Failed to update current user",
        };
      });
      throw error;
    }
  };

  changePassword = async (
    csrfToken: string,
    payload: {
      old_password?: string;
      new_password: string;
    }
  ): Promise<IUser | undefined> => {
    try {
      const user = await this.userService.changePassword(csrfToken, payload);
      if (this.data) set(this.data, ["is_password_autoset"], false);
      return user;
    } catch (error) {
      console.log(error);
      throw error;
    }
  };

  /**
   * @description deactivates the current user
   * @returns {Promise<void>}
   */
  deactivateAccount = async (): Promise<void> => {
    await this.userService.deactivateAccount();
    this.store.resetOnSignOut();
  };

  /**
   * @description resets the user store
   * @returns {void}
   */
  reset = (): void => {
    runInAction(() => {
      this.isAuthenticated = false;
      this.isLoading = false;
      this.error = undefined;
      this.data = undefined;
      this.userProfile = new ProfileStore(this.store);
      this.userSettings = new UserSettingsStore();
      this.permission = new UserPermissionStore(this.store);
    });
  };

  /**
   * @description signs out the current user
   * @returns {Promise<void>}
   */
  signOut = async (): Promise<void> => {
    await this.authService.signOut(API_BASE_URL);
    await persistence.clearStorage(true);
    this.store.resetOnSignOut();
  };

  // helper actions
  /**
   * @description fetches the projects with write permissions
   * @returns {{[projectId: string]: number} || null}
   */
  fetchProjectsWithCreatePermissions = (): { [key: string]: TUserPermissions } => {
    const { workspaceSlug } = this.store.router;

    const allWorkspaceProjectRoles = this.permission.getProjectRolesByWorkspaceSlug(workspaceSlug || "");

    const userPermissions =
      (allWorkspaceProjectRoles &&
        Object.keys(allWorkspaceProjectRoles)
          .filter((key) => allWorkspaceProjectRoles[key] >= EUserPermissions.MEMBER)
          .reduce(
            (res: { [projectId: string]: number }, key: string) => ((res[key] = allWorkspaceProjectRoles[key]), res),
            {}
          )) ||
      null;

    return userPermissions;
  };

  /**
   * @description returns projects where user has permissions
   * @returns {{[projectId: string]: number} || null}
   */
  get projectsWithCreatePermissions() {
    return this.fetchProjectsWithCreatePermissions();
  }

  /**
   * @description returns true if user has permissions to write in any project
   * @returns {boolean}
   */
  get canPerformAnyCreateAction() {
    const filteredProjects = this.fetchProjectsWithCreatePermissions();
    return filteredProjects ? Object.keys(filteredProjects).length > 0 : false;
  }

  get localDBEnabled() {
    return this.userSettings.canUseLocalDB;
  }
}
```

File: plane/apps/web/core/hooks/store/user/use-user.ts

```ts
import { useContext } from "react";
// mobx store
import { StoreContext } from "@/lib/store-context";
// types
import type { IUserStore } from "@/store/user";

export const useUser = (): IUserStore => {
  const context = useContext(StoreContext);
  if (context === undefined) throw new Error("useUser must be used within StoreProvider");
  return context.user;
};
```
