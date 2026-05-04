import { UserService } from "@/services/user.service";
import { TUserProfile } from "@syncturtle/types";
import { ExternalStore } from "@syncturtle/utils";
import { CoreRootStore } from "../root.store";
import { cloneDeep, merge } from "lodash";

type TError = {
  status: string;
  message: string;
};

export type TUserProfileSnapshot = {
  isLoading: boolean;
  data: TUserProfile;
  error: TError | undefined;
};

const createInitialProfile = (): TUserProfile => ({
  id: undefined,
  user: undefined,
  role: undefined,
  lastWorkspaceId: undefined,
  theme: {
    theme: undefined,
    text: undefined,
    palette: undefined,
    primary: undefined,
    background: undefined,
    darkPalette: undefined,
    sidebarText: undefined,
    sidebarBackground: undefined,
  },
  onboardingStep: {
    workspaceJoin: false,
    profileComplete: false,
    workspaceCreate: false,
    workspaceInvite: false,
  },
  isOnboarded: false,
  isTourCompleted: false,
  useCase: undefined,
  billingAddressCountry: undefined,
  billingAddress: undefined,
  hasBillingAddress: false,
  hasMarketingEmailConsent: false,
  createdAt: "",
  updatedAt: "",
  language: "",
});

const createInitialSnapshot = (): TUserProfileSnapshot => ({
  isLoading: false,
  data: createInitialProfile(),
  error: undefined,
});

export interface IUserProfileStoreInternal {
  _subscribe: ExternalStore<TUserProfileSnapshot>["_subscribe"];
  _getSnapshot: ExternalStore<TUserProfileSnapshot>["_getSnapshot"];
  _getServerSnapshot: ExternalStore<TUserProfileSnapshot>["_getServerSnapshot"];
  // observables
  isLoading: boolean;
  data: TUserProfile;
  error: TError | undefined;
  // actions
  fetchUserProfile: () => Promise<TUserProfile | undefined>;
  updateUserProfile: (data: Partial<TUserProfile>) => Promise<TUserProfile | undefined>;
  //   finishUserOnboarding: () => Promise<void>;
  //   updateTourCompleted: () => Promise<TUserProfile | undefined>;
  //   updateUserTheme: (data: Partial<IUserTheme>) => Promise<TUserProfile | undefined>;
}

export type TUserProfileStore = Omit<IUserProfileStoreInternal, "_subscribe" | "_getSnapshot" | "_getServerSnapshot">;

export class ProfileStore extends ExternalStore<TUserProfileSnapshot> implements IUserProfileStoreInternal {
  private readonly userService: UserService;

  constructor(
    public readonly store: CoreRootStore,
    deps?: { userService?: UserService }
  ) {
    super(createInitialSnapshot());
    this.userService = deps?.userService ?? new UserService();
  }

  // raw getters for data
  get isLoading(): boolean {
    return this.state.isLoading;
  }

  get data(): TUserProfile {
    return this.state.data;
  }

  get error(): TError | undefined {
    return this.state.error;
  }

  public fetchUserProfile = async (): Promise<TUserProfile | undefined> => {
    this.setState({ isLoading: true, error: undefined });

    try {
      const userProfile = await this.userService.getCurrentUserProfile();
      this.setState({ isLoading: false, data: userProfile });
      return userProfile;
    } catch (error) {
      this.setState({
        isLoading: false,
        error: {
          status: "user-profile-fetch-error",
          message: "Failed to fetch user profile",
        },
      });
      throw error;
    }
  };

  public updateUserProfile = async (data: Partial<TUserProfile>): Promise<TUserProfile | undefined> => {
    const currentUserProfileData = this.data;

    // optimistic update
    this.setState((prev) => ({
      ...prev,
      data: this.mutateUserProfile(prev.data, data),
      error: undefined,
    }));

    try {
      const userProfile = await this.userService.updateCurrentUserProfile(data);
      if (userProfile) {
        this.setState({ data: userProfile });
      }
      return userProfile;
    } catch {
      // revert
      this.setState({
        data: currentUserProfileData,
        error: {
          status: "user-profile-update-error",
          message: "Failed to update user profile",
        },
      });
    }
  };

  public reset() {
    this.replaceState(createInitialSnapshot());
  }

  private mutateUserProfile = (prev: TUserProfile, patch: Partial<TUserProfile>): TUserProfile => {
    // deep merge into a fresh object so react sees a new reference
    const next = cloneDeep(prev);
    merge(next, patch);
    return next;
  };
}
