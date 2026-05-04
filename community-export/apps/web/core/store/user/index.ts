import { IUser } from "@syncturtle/types";
import { ProfileStore, TUserProfileStore } from "./profile.store";
import { ExternalStore } from "@syncturtle/utils";
import { UserService } from "@/services/user.service";
import { RootStore } from "@/syncturtle-web/store/root.store";
import { cloneDeep, merge } from "lodash";
import { AuthService } from "@/services/auth.service";

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

export interface IUserStoreInternal {
  _subscribe: ExternalStore<TUserSnapshot>["_subscribe"];
  _getSnapshot: ExternalStore<TUserSnapshot>["_getSnapshot"];
  _getServerSnapshot: ExternalStore<TUserSnapshot>["_getServerSnapshot"];
  // observables
  isLoading: boolean;
  isAuthenticated: boolean;
  data: IUser | undefined;
  error: TError | undefined;
  // store observables
  userProfile: TUserProfileStore;
  // computed
  //   localDBEnabled: boolean;
  //   canPerformAnyCreateAction: boolean;
  // actions
  fetchCurrentUser: () => Promise<IUser | undefined>;
  updateCurrentUser: (data: Partial<IUser>) => Promise<IUser | undefined>;
  handleSetPassword: (csrfToken: string, data: { password: string }) => Promise<IUser | undefined>;
  changePassword: (
    csrfToken: string,
    payload: { oldPassword?: string; newPassword: string }
  ) => Promise<IUser | undefined>;
  // deactivateAccount: () => Promise<void>;
  reset: () => void;
  // signOut: () => Promise<void>;
}

export type TUserStore = Omit<IUserStoreInternal, "_subscribe" | "_getSnapshot" | "_getServerSnapshot">;

export class UserStore extends ExternalStore<TUserSnapshot> implements IUserStoreInternal {
  private readonly userService: UserService;
  private readonly authService: AuthService;
  // sub-stores
  public userProfile: ProfileStore;

  constructor(private readonly store: RootStore) {
    super(createInitialSnapshot());
    this.userService = new UserService();
    this.authService = new AuthService();

    this.userProfile = new ProfileStore(store, { userService: this.userService });
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
    this.setState({ isLoading: true, error: undefined });

    try {
      const user = await this.userService.currentUser();
      if (user && user?.id) {
        await Promise.all([this.userProfile.fetchUserProfile()]);
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

  public handleSetPassword = async (csrfToken: string, payload: { password: string }): Promise<IUser | undefined> => {
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
      const user = await this.authService.setPassword(csrfToken, payload);
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

  public changePassword = async (
    csrfToken: string,
    payload: { oldPassword?: string; newPassword: string }
  ): Promise<IUser | undefined> => {
    try {
      const user = await this.userService.changePassword(csrfToken, payload);

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

  public reset = (): void => {
    this.replaceState(createInitialSnapshot());
  };

  private mutateUser = (prev: IUser, patch: Partial<IUser>): IUser => {
    const next = cloneDeep(prev);
    merge(next, patch);
    return next;
  };
}
