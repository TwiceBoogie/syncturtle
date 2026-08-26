import { AuthService } from "@/services/auth.service";
import { UserService } from "@/services/user.service";
import type { IUser } from "@syncturtle/types";
import { ExternalStore } from "@syncturtle/utils";
import { CoreRootStore } from "./root.store";
import { API_BASE_PATH } from "@syncturtle/constants";

type TError = {
  status: string;
  message: string;
};

export type TUserSnapshot = {
  isLoading: boolean;
  isUserLoggedIn: boolean | undefined;
  currentUser: IUser | undefined;
  error: TError | undefined;
};

const createInitialSnapshot = (): TUserSnapshot => ({
  isLoading: false,
  isUserLoggedIn: undefined,
  currentUser: undefined,
  error: undefined,
});

export interface IUserStore {
  // observables
  isLoading: boolean;
  isUserLoggedIn: boolean | undefined;
  currentUser: IUser | undefined;
  error: TError | undefined;
  // actions
  fetchCurrentUser: () => Promise<IUser>;
  reset: () => void;
  signOut: () => void;
}

export interface IUserStoreInternal extends IUserStore {
  _subscribe: ExternalStore<TUserSnapshot>["_subscribe"];
  _getSnapshot: ExternalStore<TUserSnapshot>["_getSnapshot"];
  _getServerSnapshot: ExternalStore<TUserSnapshot>["_getServerSnapshot"];
}

export class UserStore extends ExternalStore<TUserSnapshot> implements IUserStoreInternal {
  private readonly userService: UserService;
  private readonly authService: AuthService;

  constructor(private readonly _store: CoreRootStore) {
    super(createInitialSnapshot());

    this.userService = new UserService();
    this.authService = new AuthService();
  }

  get isLoading(): boolean {
    return this.state.isLoading;
  }

  get isUserLoggedIn(): boolean | undefined {
    return this.state.isUserLoggedIn;
  }

  get currentUser(): IUser | undefined {
    return this.state.currentUser;
  }

  get error(): TError | undefined {
    return this.state.error;
  }

  public fetchCurrentUser = async (): Promise<IUser> => {
    this.setState((s) => ({
      ...s,
      isLoading: s.currentUser === undefined ? true : s.isLoading,
      error: undefined,
    }));

    try {
      const currentUser = await this.userService.adminDetails();
      console.log(`currentUser: ${currentUser}`);
      if (currentUser) {
        await this._store.instance.fetchInstanceAdmins();

        this.setState({
          isLoading: false,
          isUserLoggedIn: true,
          currentUser,
          error: undefined,
        });
      } else {
        this.setState({
          isLoading: false,
          isUserLoggedIn: false,
          currentUser: undefined,
          error: undefined,
        });
      }
      return currentUser;
    } catch (error) {
      this.setState({
        isLoading: false,
        error: {
          status: "admin-fetch-current-user-error",
          message: "Failed to fetch current user",
        },
      });

      throw error;
    }
  };

  public signOut = async (): Promise<void> => {
    try {
      await this.authService.signOut();
    } finally {
      this._store.resetOnSignOut();

      window.location.assign(API_BASE_PATH);
    }
  };

  public reset = () => {
    this.replaceState(createInitialSnapshot());
  };
}
