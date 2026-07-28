"use client";

import type { FC, ReactNode } from "react";
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
  const { data: currentUserProfile } = useUserProfile();
  const { data: currentUserSettings } = useUserSettings();
  const { loader: workspacesLoader, workspaces } = useWorkspace();

  const { isLoading: isUserSWRLoading } = useSWR("USER_INFORMATION", async () => await fetchCurrentUser(), {
    revalidateOnFocus: false,
    shouldRetryOnError: false,
  });

  // console.log(`currentUser:`, currentUser);
  // console.log(`currentUserProfile: `, currentUserProfile);
  // console.log(`currentUserSettings: `, currentUserSettings);

  const isUserOnboarded =
    currentUserProfile?.isOnboarded ||
    (currentUserProfile?.onboardingStep?.profileComplete &&
      currentUserProfile?.onboardingStep?.workspaceCreate &&
      currentUserProfile?.onboardingStep?.workspaceInvite &&
      currentUserProfile?.onboardingStep?.workspaceJoin) ||
    false;

  const getWorkspaceRedirectionUrl = (): string => {
    let redirectionRoute = "/create-workspace";

    if (nextPath && isValidURL(nextPath.toString())) {
      redirectionRoute = nextPath.toString();
      return redirectionRoute;
    }

    const currentWorkspaceSlug =
      currentUserSettings?.workspace?.lastWorkspaceSlug || currentUserSettings?.workspace?.fallbackWorkspaceSlug;

    const isCurrentWorkspaceValid = Object.values(workspaces || {}).findIndex(
      (workspace) => workspace.slug === currentWorkspaceSlug
    );

    if (isCurrentWorkspaceValid >= 0) redirectionRoute = `/${currentWorkspaceSlug}`;

    return redirectionRoute;
  };

  if ((isUserSWRLoading || isUserLoading || workspacesLoader) && !currentUser?.id) {
    return (
      <div className="relative flex h-screen w-full items-center justify-center">
        <Spinner />
      </div>
    );
  }

  if (pageType === EPageType.PUBLIC) return <>{children}</>;

  if (pageType === EPageType.NON_AUTHENTICATED) {
    if (!currentUser?.id) return <>{children}</>;
    else {
      if (currentUserProfile?.id && isUserOnboarded) {
        const currentRedirectRoute = getWorkspaceRedirectionUrl();
        router.push(currentRedirectRoute);
        return <></>;
      } else {
        router.push("/onboarding");
        return <></>;
      }
    }
  }

  if (pageType === EPageType.ONBOARDING) {
    if (!currentUser?.id) {
      router.push(`/${pathname ? `?next_path=${pathname}` : ``}`);
      return <></>;
    } else {
      if (currentUser && currentUserProfile?.id && isUserOnboarded) {
        const currentRedirectRoute = getWorkspaceRedirectionUrl();
        router.replace(currentRedirectRoute);
        return <></>;
      } else return <>{children}</>;
    }
  }

  if (pageType === EPageType.SET_PASSWORD) {
    if (!currentUser?.id) {
      router.push(`/${pathname ? `?next_path=${pathname}` : ``}`);
      return <></>;
    } else {
      if (currentUser && !currentUser?.isPasswordAutoset && currentUserProfile?.id && isUserOnboarded) {
        const currentRedirectRoute = getWorkspaceRedirectionUrl();
        router.push(currentRedirectRoute);
        return <></>;
      } else return <>{children}</>;
    }
  }

  if (pageType === EPageType.AUTHENTICATED) {
    if (currentUser?.id) {
      if (currentUserProfile && currentUserProfile?.id && isUserOnboarded) return <>{children}</>;
      else {
        router.push(`/onboarding`);
        return <></>;
      }
    } else {
      router.push(`/${pathname ? `?next_path=${pathname}` : ``}`);
      return <></>;
    }
  }

  return <>{children}</>;
};
