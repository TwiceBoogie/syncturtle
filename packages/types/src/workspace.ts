import type { TPaginationInfo } from "./common";
import type { IUserLite, TUserPermissions } from "./users";

export type TLoader = "init-loader" | "mutation" | "pagination" | "loaded" | undefined;

export interface IWorkspace {
  readonly id: string;
  readonly owner: IUserLite;
  readonly createdAt: Date;
  readonly updatedAt: Date;
  name: string;
  url: string;
  logoUrl: string | null;
  readonly totalMembers: number;
  readonly slug: string;
  readonly createdById: string;
  readonly updatedById: string;
  organizationSize: string;
  role: TUserPermissions;
}

export type TWorkspacePaginationInfo = TPaginationInfo & {
  results: IWorkspace[];
};

export type TSlugCheckResult = {
  available: boolean;
  message: string;
};

export type TSlugStatus = "idle" | "checking" | "available" | "unavailable";

export interface IWorkspaceMemberInvitation {
  id: string;
  accepted: boolean;
  email: string;
  message: string;
  respondedAt: Date;
  role: TUserPermissions;
  token: string;
  inviteLink: string;
  workspace: {
    id: string;
    logoUrl: string;
    name: string;
    slug: string;
  };
}

export interface IWorkspaceBulkInviteFormData {
  emails: { email: string; role: TUserPermissions }[];
}

export type TOnboardingStep =
  | "PROFILE_SETUP"
  | "ROLE_SETUP"
  | "USE_CASE_SETUP"
  | "WORKSPACE_CREATE_OR_JOIN"
  | "INVITE_MEMBERS";
