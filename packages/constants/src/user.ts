import type { TUserPermissions, TUserPermissionKey } from "@syncturtle/types";

export const EUserPermissions = {
  ADMIN: 20,
  MEMBER: 15,
  GUEST: 5,
} as const satisfies Record<TUserPermissionKey, TUserPermissions>;
