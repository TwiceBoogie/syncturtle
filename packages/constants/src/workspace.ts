import type {
  TCreateOrJoinWorkspaceViews,
  TOnboardingStep,
  TRoleDetails,
  TUserPermissionKey,
  TUserPermissions,
} from "@syncturtle/types";

export const ORGANIZATION_SIZE = ["Just myself", "2-10", "11-50", "51-200", "201-500", "500+"];

export const RESTRICTED_URLS = [
  "404",
  "accounts",
  "api",
  "create-workspace",
  "god-mode",
  "installations",
  "invitations",
  "onboarding",
  "profile",
  "spaces",
  "workspace-invitations",
  "password",
  "flags",
  "monitor",
  "monitoring",
  "ingest",
  "syncturtle-pro",
  "syncturtle-ultimate",
  "enterprise",
  "syncturtle-enterprise",
  "disco",
  "silo",
  "chat",
  "calendar",
  "drive",
  "channels",
  "upgrade",
  "billing",
  "sign-in",
  "sign-up",
  "signin",
  "signup",
  "config",
  "live",
  "admin",
  "m",
  "import",
  "importers",
  "integrations",
  "integration",
  "configuration",
  "initiatives",
  "initiative",
  "config",
  "workflow",
  "workflows",
  "epics",
  "epic",
  "story",
  "mobile",
  "dashboard",
  "desktop",
  "onload",
  "real-time",
  "one",
  "pages",
  "mobile",
  "business",
  "pro",
  "settings",
  "monitor",
  "license",
  "licenses",
  "instances",
  "instance",
];

export const USE_CASES = [
  "Plan and track product roadmaps",
  "Manage engineering sprints",
  "Coordinate cross-functional projects",
  "Replace our current tool",
  "Just exploring",
];

export const ECreateOrJoinWorkspaceViews = {
  WORKSPACE_CREATE: "WORKSPACE_CREATE",
  WORKSPACE_JOIN: "WORKSPACE_JOIN",
} as const satisfies Record<TCreateOrJoinWorkspaceViews, TCreateOrJoinWorkspaceViews>;

export const EOnboardingSteps = {
  PROFILE_SETUP: "PROFILE_SETUP",
  ROLE_SETUP: "ROLE_SETUP",
  USE_CASE_SETUP: "USE_CASE_SETUP",
  WORKSPACE_CREATE_OR_JOIN: "WORKSPACE_CREATE_OR_JOIN",
  INVITE_MEMBERS: "INVITE_MEMBERS",
} as const satisfies Record<TOnboardingStep, TOnboardingStep>;

export const EUserWorkspaceRoles = {
  ADMIN: 20,
  MEMBER: 15,
  GUEST: 5,
} as const satisfies Record<TUserPermissionKey, TUserPermissions>;

export const ROLE = {
  [EUserWorkspaceRoles.GUEST]: "Guest",
  [EUserWorkspaceRoles.MEMBER]: "Member",
  [EUserWorkspaceRoles.ADMIN]: "Admin",
} as const satisfies Record<TUserPermissions, string>;

export const ROLE_DETAILS = {
  [EUserWorkspaceRoles.GUEST]: {
    i18nTitle: "role_details.guest.title",
    i18nDescription: "role_details.guest.description",
  },
  [EUserWorkspaceRoles.MEMBER]: {
    i18nTitle: "role_details.member.title",
    i18nDescription: "role_details.member.description",
  },
  [EUserWorkspaceRoles.ADMIN]: {
    i18nTitle: "role_details.admin.title",
    i18nDescription: "role_details.admin.description",
  },
} as const satisfies Record<TUserPermissions, TRoleDetails>;

export const ROLE_OPTIONS = [
  {
    value: EUserWorkspaceRoles.GUEST,
    label: ROLE[EUserWorkspaceRoles.GUEST],
    ...ROLE_DETAILS[EUserWorkspaceRoles.GUEST],
  },
  {
    value: EUserWorkspaceRoles.MEMBER,
    label: ROLE[EUserWorkspaceRoles.MEMBER],
    ...ROLE_DETAILS[EUserWorkspaceRoles.MEMBER],
  },
  {
    value: EUserWorkspaceRoles.ADMIN,
    label: ROLE[EUserWorkspaceRoles.ADMIN],
    ...ROLE_DETAILS[EUserWorkspaceRoles.ADMIN],
  },
] as const satisfies readonly {
  value: TUserPermissions;
  label: string;
  i18nTitle: string;
  i18nDescription: string;
}[];
