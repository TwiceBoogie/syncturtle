export type TOnboardingSteps = {
  profileComplete: boolean;
  workspaceCreate: boolean;
  workspaceInvite: boolean;
  workspaceJoin: boolean;
};

export interface IUserTheme {
  text: string | undefined;
  theme: string | undefined;
  palette: string | undefined;
  primary: string | undefined;
  background: string | undefined;
  darkPalette: boolean | undefined;
  sidebarText: string | undefined;
  sidebarBackground: string | undefined;
}

export type TLoginMediums = "email" | "magic-code" | "github" | "gitlab" | "google";

export interface IUserLite {
  id: string;
  displayName: string;
  email?: string;
  firstName: string;
  lastName: string;
  avatarUrl: string;
  avatarAssetId: string;
  isBot: boolean;
  joining_date?: string;
}

export interface IUser extends IUserLite {
  username: string;
  mobileNumber: string | null;
  email: string;
  // only for uploading the cover image
  coverImage?: string | null;
  coverImageAsset?: string | null;
  // only for rendering the cover image
  isActive: boolean;
  isEmailVerified: boolean;
  isPasswordAutoset: boolean;
  userTimezone: string;
  lastLoginMedium: TLoginMediums;
  theme: IUserTheme;
  coverImageUrl: string | null;
  isTourCompleted: boolean;
  lastWorkspaceId: string;
}

export type TUserProfile = {
  id: string | undefined;
  user: string | undefined;
  role: string | undefined;
  lastWorkspaceId: string | undefined;
  theme: {
    text: string | undefined;
    theme: string | undefined;
    palette: string | undefined;
    primary: string | undefined;
    background: string | undefined;
    darkPalette: boolean | undefined;
    sidebarText: string | undefined;
    sidebarBackground: string | undefined;
  };
  onboardingStep: TOnboardingSteps;
  isOnboarded: boolean;
  isTourCompleted: boolean;
  useCase: string | undefined;
  billingAddressCountry: string | undefined;
  billingAddress: string | undefined;
  hasBillingAddress: boolean;
  hasMarketingEmailConsent: boolean;
  language: string;
  createdAt: Date | string;
  updatedAt: Date | string;
};

export interface IUserSettings {
  id: string | undefined;
  email: string | undefined;
  workspace: {
    lastWorkspaceId: string | undefined;
    lastWorkspaceSlug: string | undefined;
    lastWorkspaceName: string | undefined;
    lastWorkspaceLogo: string | undefined;
    fallbackWorkspaceId: string | undefined;
    fallbackWorkspaceSlug: string | undefined;
    invites: number | undefined;
  };
}

/**
 * 20=ADMIN, 15=MEMBER, 5=GUEST
 */
export type TUserPermissions = 20 | 15 | 5;
export type TUserPermissionKey = "ADMIN" | "MEMBER" | "GUEST";
