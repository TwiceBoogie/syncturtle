/** Root-scoped Web workers must always fetch browser authentication and identity from the network. */
export const bypassRuntimeCache = (pathname: string, adminBasePath: string): boolean =>
  pathname === adminBasePath ||
  pathname.startsWith(`${adminBasePath}/`) ||
  pathname === "/api/instances" ||
  pathname.startsWith("/api/instances/") ||
  pathname === "/api/users/me" ||
  pathname.startsWith("/api/users/me/") ||
  pathname.startsWith("/auth/");
