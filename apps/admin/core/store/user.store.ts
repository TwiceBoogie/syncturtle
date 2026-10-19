import { AuthService } from "@/services/auth.service";
import { UserService } from "@/services/user.service";
import { ExternalStore } from "@syncturtle/utils";
import { CoreRootStore } from "./root.store";
import type { IUser } from "@syncturtle/types";

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
  transitionToSignedOut: () => void;
}

export interface IUserStoreInternal extends IUserStore {
  _subscribe: ExternalStore<TUserSnapshot>["_subscribe"];
  _getSnapshot: ExternalStore<TUserSnapshot>["_getSnapshot"];
  _getServerSnapshot: ExternalStore<TUserSnapshot>["_getServerSnapshot"];
}

export class UserStore extends ExternalStore<TUserSnapshot> implements IUserStoreInternal {
  private readonly userService: UserService;
  private readonly authService: AuthService;
  private fetchCurrentUserSeq = 0;
  private signedOut = false;
  private signOutPromise: Promise<void> | null = null;

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
    if (this.signedOut) throw new Error("Administrator session has ended");
    const requestSeq = ++this.fetchCurrentUserSeq;
    this.setState((s) => ({
      ...s,
      isLoading: s.currentUser === undefined ? true : s.isLoading,
      error: undefined,
    }));

    try {
      const currentUser = await this.userService.adminDetails();
      if (requestSeq !== this.fetchCurrentUserSeq) return currentUser;
      if (currentUser) {
        await this._store.instance.fetchInstanceAdmins();
        if (requestSeq !== this.fetchCurrentUserSeq) return currentUser;

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
      if (requestSeq !== this.fetchCurrentUserSeq) throw error;
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
    if (this.signedOut) return Promise.resolve();
    if (!this.signOutPromise) {
      this.signOutPromise = this.authService
        .signOut()
        .then(() => this.transitionToSignedOut())
        .finally(() => {
          this.signOutPromise = null;
        });
    }
    return this.signOutPromise;
  };

  public transitionToSignedOut = (): void => {
    if (this.signedOut) return;
    this.signedOut = true;
    this._store.resetOnSignOut();
    this.setState({ isUserLoggedIn: false });
    // Hard navigation disposes remaining app-local stores and SWR state.
    if (typeof window !== "undefined") window.location.assign(AuthService.signedOutDestination());
  };

  public reset = () => {
    this.fetchCurrentUserSeq += 1;
    this.replaceState(createInitialSnapshot());
  };
}
