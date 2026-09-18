const DEFAULT_ADMIN_BASE_PATH = "/god-mode";

const normalizePathname = (pathname: string): string => {
  if (pathname === "/") return pathname;

  const normalized = pathname.replace(/\/+$/, "");
  return normalized || "/";
};

export const isPublicAdministratorSignInRoute = (pathname: string): boolean => {
  const normalizedPathname = normalizePathname(pathname);
  const configuredBasePath = process.env.NEXT_PUBLIC_ADMIN_BASE_PATH || DEFAULT_ADMIN_BASE_PATH;
  const normalizedBasePath = normalizePathname(configuredBasePath);

  return normalizedPathname === "/" || normalizedPathname === normalizedBasePath;
};

export const currentUserBootstrapKey = (isSetupDone: boolean | undefined, pathname: string): "CURRENT_USER" | null => {
  if (isSetupDone !== true || isPublicAdministratorSignInRoute(pathname)) return null;

  return "CURRENT_USER";
};
