"use client";

import useSWR from "swr";
import { Spinner } from "@heroui/react";
// hooks
import { useUser } from "@/hooks/store/user";
import { useWorkspace } from "@/hooks/store/use-workspace";
// services
import { WorkspaceService } from "@/services/workspace.service";
// components
import { OnboardingRoot } from "@/components/onboarding";
// wrappers
import { AuthenticationWrapper } from "@/lib/wrappers";
// constants
import { USER_WORKSPACES_LIST } from "@/constants/fetch-keys";
// types
import { EPageType } from "@/helpers/authentication.helper";

const workspaceService = new WorkspaceService();

export default function OnboardingPage() {
  const { data: user } = useUser();
  const { fetchWorkspaces } = useWorkspace();

  useSWR(USER_WORKSPACES_LIST, () => {
    if (user?.id) {
      fetchWorkspaces();
    }
  });

  const { isLoading: invitationsLoader, data: invitations } = useSWR(
    `USER_WORKSPACE_INVITATIONS_LIST_${user?.id}`,
    () => {
      if (user?.id) return workspaceService.userWorkspaceInvitations();
    }
  );

  return (
    <AuthenticationWrapper pageType={EPageType.ONBOARDING}>
      <div className="flex relative size-full overflow-hidden bg-custom-background-90 rounded-lg transition-all ease-in-out duration-300">
        <div className="size-full p-2 grow transition-all ease-in-out duration-300 overflow-hidden">
          <div className="relative flex flex-col h-full w-full overflow-hidden rounded-lg bg-custom-background-100 shadow-md border border-custom-background-200">
            {user && !invitationsLoader ? (
              <OnboardingRoot invitations={invitations ?? []} />
            ) : (
              <div className="grid h-full w-full place-items-center">
                <Spinner />
              </div>
            )}
          </div>
        </div>
      </div>
    </AuthenticationWrapper>
  );
}
