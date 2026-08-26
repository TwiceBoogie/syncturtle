"use client";

import useSWR from "swr";
// heroui
import { Spinner } from "@heroui/react";
// hooks
import { useUser } from "@/hooks/store/user";
import { useWorkspace } from "@/hooks/store/use-workspace";
// components
import { OnboardingRoot } from "@/components/onboarding";
// services
import { WorkspaceService } from "@/services/workspace.service";
// wrappers
import { AuthenticationWrapper } from "@/lib/wrappers";
// types
import { EPageType } from "@/helpers/authentication.helper";

const workspaceService = new WorkspaceService();

export default function OnboardingPage() {
  // store hooks
  const { data: user } = useUser();
  const { fetchWorkspaces } = useWorkspace();

  useSWR("USER_WORKSPACES_LITE", () => {
    if (user?.id) {
      fetchWorkspaces();
    }
  });

  const { isLoading: invitationsLoader, data: invitations } = useSWR(
    `USER_WORKSPACE_INVITATIONS_LITE_${user?.id}`,
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
              <OnboardingRoot key={user.id} invitations={invitations ?? []} />
            ) : (
              <div className="">
                <Spinner color="current" />
              </div>
            )}
          </div>
        </div>
      </div>
    </AuthenticationWrapper>
  );
}
