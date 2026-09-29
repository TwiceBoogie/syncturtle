"use client";

import type { FC, ReactNode } from "react";
import { useEffect } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import useSWR from "swr";
// heroui
import { Spinner } from "@heroui/react";
// hooks
import { useUser, useUserProfile, useUserSettings } from "@/hooks/store/user";
import { useWorkspace } from "@/hooks/store/use-workspace";
// helpers
import { EPageType } from "@/helpers/authentication.helper";

type TPageType = EPageType;

interface IAuthenticationWrapper {
  children: ReactNode;
  pageType?: TPageType;
}

const isValidURL = (url: string): boolean => {
  const disallowedSchemes = /^(https?|ftp):\/\//i;
  return !disallowedSchemes.test(url);
};

export const AuthenticationWrapper: FC<IAuthenticationWrapper> = (props) => {
  const pathname = usePathname();
  const router = useRouter();
  const searchParams = useSearchParams();
  const nextPath = searchParams.get("next_path");
  // props
  const { children, pageType = EPageType.AUTHENTICATED } = props;
  // store hooks
  const { isLoading: isUserLoading, data: currentUser, fetchCurrentUser } = useUser();
  const { data: currentUserProfile, isLoading: isProfileLoading, isFinishingOnboarding } = useUserProfile();
  const { isLoading: isSettingsLoading } = useUserSettings();
  const {
    loader: workspacesLoader,
    hasLoaded: hasLoadedWorkspaces,
    error: workspaceError,
    fetchWorkspaces,
    getWorkspaceRedirectionUrl,
  } = useWorkspace();

  const { isLoading: isUserSWRLoading } = useSWR("USER_INFORMATION", async () => await fetchCurrentUser(), {
    revalidateOnFocus: false,
    shouldRetryOnError: false,
  });

  const isUserOnboarded =
    currentUserProfile?.isOnboarded ||
    (currentUserProfile?.onboardingStep?.profileComplete &&
      currentUserProfile?.onboardingStep?.workspaceCreate &&
      currentUserProfile?.onboardingStep?.workspaceInvite &&
      currentUserProfile?.onboardingStep?.workspaceJoin) ||
    false;

  const isWorkspaceLoadFailed = Boolean(currentUser?.id && !hasLoadedWorkspaces && workspaceError);
  const isLoading =
    pageType !== EPageType.PUBLIC &&
    (isUserSWRLoading ||
      isUserLoading ||
      isProfileLoading ||
      isSettingsLoading ||
      isFinishingOnboarding ||
      workspacesLoader ||
      Boolean(currentUser?.id && !hasLoadedWorkspaces && !workspaceError));

  let redirectRoute: string | null = null;
  if (pageType !== EPageType.PUBLIC && !isLoading && !isWorkspaceLoadFailed) {
    const workspaceRoute = nextPath && isValidURL(nextPath) ? nextPath : getWorkspaceRedirectionUrl();
    const signInRoute = `/${pathname ? `?next_path=${pathname}` : ``}`;

    if (pageType === EPageType.NON_AUTHENTICATED && currentUser?.id) {
      redirectRoute = currentUserProfile?.id && isUserOnboarded ? workspaceRoute : "/onboarding";
    } else if (pageType === EPageType.ONBOARDING) {
      if (!currentUser?.id) redirectRoute = signInRoute;
      else if (currentUserProfile?.id && isUserOnboarded) redirectRoute = workspaceRoute;
    } else if (pageType === EPageType.SET_PASSWORD) {
      if (!currentUser?.id) redirectRoute = signInRoute;
      else if (!currentUser.isPasswordAutoset && currentUserProfile?.id && isUserOnboarded) {
        redirectRoute = workspaceRoute;
      }
    } else if (pageType === EPageType.AUTHENTICATED) {
      if (!currentUser?.id) redirectRoute = signInRoute;
      else if (!currentUserProfile?.id || !isUserOnboarded) redirectRoute = "/onboarding";
    }
  }

  // Navigation follows committed state; rendering must not enqueue a stale redirect.
  useEffect(() => {
    if (!redirectRoute) return;
    if (pageType === EPageType.ONBOARDING) router.replace(redirectRoute);
    else router.push(redirectRoute);
  }, [redirectRoute, pageType, router]);

  if (isLoading) {
    return (
      <div className="relative flex h-screen w-full items-center justify-center">
        <Spinner />
      </div>
    );
  }

  if (pageType !== EPageType.PUBLIC && isWorkspaceLoadFailed) {
    return (
      <div role="alert">
        <p>Unable to load workspaces.</p>
        <button type="button" onClick={() => void fetchWorkspaces().catch(() => {})}>
          Retry
        </button>
      </div>
    );
  }

  if (redirectRoute) return <></>;
  return <>{children}</>;
};
