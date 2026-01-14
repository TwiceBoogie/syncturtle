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
