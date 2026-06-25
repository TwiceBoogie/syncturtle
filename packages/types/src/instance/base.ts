import type { IUserLite } from "../users";
import type { TInstanceAuthenticationKeys } from "./auth";
import type { TInstanceEmailConfigurationKeys } from "./email";
import type { TInstanceWorkspaceConfigurationKeys } from "./workspace";

export interface IInstanceInfo {
  instance: IInstance;
  config: IInstanceConfig;
}

export interface IInstance {
  id: string;
  instanceName: string | undefined;
  whitelistEmails: string | undefined;
  licenseKey: string | undefined;
  instanceId: string | undefined;
  currentVersion: string | undefined;
  latestVersion: string | undefined;
  lastCheckedAt: string | undefined;
  namespace: string | undefined;
  isTelemetryEnabled: boolean;
  isSupportRequired: boolean;
  isActivated: boolean;
  isSetupDone: boolean;
  isSignupScreenVisited: boolean;
  userCount: number | undefined;
  isVerified: boolean;
  createdAt: string;
  updatedAt: string;
  createdBy: string | undefined;
  updatedBy: string | undefined;
  workspaceExist: boolean;
}

export interface IInstanceConfig {
  enableSignup: boolean;
  isWorkspaceCreationDisabled: boolean;
  isGoogleEnabled: boolean;
  isGithubEnabled: boolean;
  isGitlabEnabled: boolean;
  isMagicLoginEnabled: boolean;
  isEmailPasswordEnabled: boolean;
  githubAppName: string | undefined;
  posthogApiKey: string | undefined;
  posthogHost: string | undefined;
  fileSizeLimit: number | undefined;
  isSmtpConfigured: boolean;
  appBaseUrl: string | undefined;
  adminBaseUrl: string | undefined;
  isIntercomEnabled: boolean;
  intercomAppId: string | undefined;
  instanceChangelogUrl: string | undefined;
}

export interface IInstanceAdmin {
  id: string;
  instance: string;
  role: string;
  user: string;
  update_at: string;
  updated_by: string;
  userDetail: IUserLite;
  created_at: string | null;
  created_by: string | null;
}

export type TInstanceIntercomConfigurationKeys = "IS_INTERCOM_ENABLED" | "INTERCOM_APP_ID";

export type TInstanceConfigurationKeys =
  | TInstanceEmailConfigurationKeys
  | TInstanceAuthenticationKeys
  | TInstanceIntercomConfigurationKeys
  | TInstanceWorkspaceConfigurationKeys;

export interface IInstanceConfiguration {
  id: string;
  key: TInstanceConfigurationKeys;
  value: string;
  created_at: string;
  updated_at: string;
  created_by: string;
  updated_by: string;
}

export type TFormattedInstanceConfiguration = {
  [key in TInstanceConfigurationKeys]: string;
};
