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
