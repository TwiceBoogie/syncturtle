import { cloneDeep, isPlainObject, merge, set } from "lodash";
import { UserService } from "@/services/user.service";
import { CoreRootStore } from "../root.store";
// syncturtle imports
import { ExternalStore } from "@syncturtle/utils";
import type { TUserProfile } from "@syncturtle/types";

type TError = {
  status: string;
  message: string;
};

export type TUserProfileSnapshot = {
  isLoading: boolean;
  isFinishingOnboarding: boolean;
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
  isFinishingOnboarding: false,
  data: createInitialProfile(),
  error: undefined,
});

export interface IUserProfileStore {
  // observables
  isLoading: boolean;
  isFinishingOnboarding: boolean;
  data: TUserProfile;
  error: TError | undefined;
  // actions
  fetchUserProfile: () => Promise<TUserProfile | undefined>;
  updateUserProfile: (data: Partial<TUserProfile>) => Promise<TUserProfile | undefined>;
  finishUserOnboarding: () => Promise<void>;
  updateTourCompleted: () => Promise<TUserProfile | undefined>;
  //   updateUserTheme: (data: Partial<IUserTheme>) => Promise<TUserProfile | undefined>;
  reset: () => void;
}

export interface IUserProfileStoreInternal extends IUserProfileStore {
  _subscribe: ExternalStore<TUserProfileSnapshot>["_subscribe"];
  _getSnapshot: ExternalStore<TUserProfileSnapshot>["_getSnapshot"];
  _getServerSnapshot: ExternalStore<TUserProfileSnapshot>["_getServerSnapshot"];
}

export class ProfileStore extends ExternalStore<TUserProfileSnapshot> implements IUserProfileStoreInternal {
  private readonly userService: UserService;

  private fetchUserProfileSeq = 0;

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

  get isFinishingOnboarding(): boolean {
    return this.state.isFinishingOnboarding;
  }

  get data(): TUserProfile {
    return this.state.data;
  }

  get error(): TError | undefined {
    return this.state.error;
  }

  public fetchUserProfile = async (): Promise<TUserProfile | undefined> => {
    const requestSeq = ++this.fetchUserProfileSeq;

    this.setState({ isLoading: true, error: undefined });

    try {
      const userProfile = await this.userService.getCurrentUserProfile(true);

      if (requestSeq !== this.fetchUserProfileSeq) {
        return userProfile;
      }

      this.setState({ isLoading: false, data: userProfile });

      return userProfile;
    } catch (error) {
      if (requestSeq === this.fetchUserProfileSeq) {
        this.setState({
          isLoading: false,
          error: {
            status: "user-profile-fetch-error",
            message: "Failed to fetch user profile",
          },
        });
      }

      throw error;
    }
  };

  public updateUserProfile = async (data: Partial<TUserProfile>): Promise<TUserProfile | undefined> => {
    const previousProfile = cloneDeep(this.data);

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
      return userProfile ?? this.data;
    } catch (error) {
      // revert
      this.setState({
        data: previousProfile,
        error: {
          status: "user-profile-update-error",
          message: "Failed to update user profile",
        },
      });
      throw error;
    }
  };

  public finishUserOnboarding = async (): Promise<void> => {
    const previousProfile = cloneDeep(this.data);

    const selectedWorkspace = this.store.workspace.getWorkspaceById(this.data.lastWorkspaceId ?? "");
    const firstWorkspace = selectedWorkspace ?? Object.values(this.store.workspace.workspaces ?? {})[0];
    if (!firstWorkspace) throw new Error("Choose a usable space before finishing onboarding.");
    const dataToUpdate: Partial<TUserProfile> = {
      onboardingStep: {
        ...this.data.onboardingStep,
        profileComplete: true,
        workspaceJoin: true,
        workspaceCreate: true,
        workspaceInvite: true,
      },
      lastWorkspaceId: firstWorkspace?.id,
    };

    this.setState((prev) => ({
      ...prev,
      isLoading: true,
      isFinishingOnboarding: true,
      data: this.mutateUserProfile(prev.data, dataToUpdate),
      error: undefined,
    }));

    try {
      // Persist canonical completion before publishing legacy all-true flags,
      // which old wrappers also recognize as completed after a reload.
      await this.userService.updateUserOnboard();
      await this.userService.updateCurrentUserProfile(dataToUpdate);
      // refresh profile/settings after backend has completed onboarding
      await Promise.all([this.fetchUserProfile(), this.store.user.userSettings.fetchCurrentUserSettings(true)]);

      // make sure local profile is onboarded even if the response is cached/stale
      this.setState((prev) => ({
        ...prev,
        isLoading: false,
        isFinishingOnboarding: false,
        data: this.mutateUserProfile(prev.data, {
          ...dataToUpdate,
          isOnboarded: true,
        }),
        error: undefined,
      }));
    } catch (error) {
      this.setState({
        isLoading: false,
        isFinishingOnboarding: false,
        data: previousProfile,
        error: {
          status: "user-profile-onboard-finish-error",
          message: "Failed to finish user onboarding",
        },
      });

      throw error;
    }
  };

  public updateTourCompleted = async (): Promise<TUserProfile | undefined> => {
    if (this.data.isTourCompleted) {
      return this.data;
    }

    const previousProfile = cloneDeep(this.data);

    // optimistic update
    this.setState((prev) => ({
      ...prev,
      data: this.mutateUserProfile(prev.data, {
        isTourCompleted: true,
      }),
      error: undefined,
    }));

    try {
      const userProfile = await this.userService.updateUserTourCompleted();

      if (userProfile) {
        this.setState({
          // data: userProfile,
          error: undefined,
        });
      }

      // return userProfile ?? this.data;
      return this.data;
    } catch (error) {
      this.setState({
        data: previousProfile,
        error: {
          status: "user-profile-tour-completed-error",
          message: "Failed to update user profile tour completed status",
        },
      });

      throw error;
    }
  };

  public reset() {
    this.fetchUserProfileSeq++;
    this.replaceState(createInitialSnapshot());
  }

  private mutateUserProfile = (prev: TUserProfile, patch: Partial<TUserProfile>): TUserProfile => {
    // deep merge into a fresh object so react sees a new reference
    const next = cloneDeep(prev);

    Object.entries(patch).forEach(([key, value]) => {
      if (!(key in next)) return;

      const profileKey = key as keyof TUserProfile & string;
      const currentValue = next[profileKey as keyof TUserProfile];

      if (isPlainObject(currentValue) && isPlainObject(value)) {
        set(next, profileKey, merge(cloneDeep(currentValue), value));
        return;
      }

      set(next, profileKey, value);
    });

    return next;
  };
}
