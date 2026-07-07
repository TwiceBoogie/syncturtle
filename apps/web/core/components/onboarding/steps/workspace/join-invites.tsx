"use client";

import type { FC } from "react";
import { useState } from "react";
// heroui
import { Button, Checkbox, CheckboxGroup, Spinner } from "@heroui/react";
// syncturtle imports
import type { IWorkspaceMemberInvitation } from "@syncturtle/types";
import { truncateText } from "@syncturtle/utils";
import { ROLE } from "@syncturtle/constants";
// store hooks
import { useWorkspace } from "@/hooks/store/use-workspace";
import { useUserSettings } from "@/hooks/store/user";
// components
import { CommonOnboardingHeader } from "../common";
import { WorkspaceLogo } from "@/components/workspace/logo";
// services
import { WorkspaceService } from "@/services/workspace.service";

interface IWorkspaceJoinInvitesStep {
  invitations: IWorkspaceMemberInvitation[];
  handleNextStep: () => Promise<void>;
  handleCurrentViewChange: () => void;
}

const workspaceService = new WorkspaceService();

export const WorkspaceJoinInvitesStep: FC<IWorkspaceJoinInvitesStep> = (props) => {
  const { invitations, handleNextStep, handleCurrentViewChange } = props;
  // states
  const [isJoiningWorkspaces, setIsJoiningWorkspaces] = useState(false);
  const [invitationsRespond, setInvitationsRespond] = useState<string[]>([]);
  // store hooks
  const { fetchWorkspaces } = useWorkspace();
  const { fetchCurrentUserSettings } = useUserSettings();

  const submitInvitations = async () => {
    const invitation = invitations?.find((invitation) => invitation.id === invitationsRespond[0]);

    if (invitationsRespond.length <= 0 && !invitation?.role) return;

    setIsJoiningWorkspaces(true);

    try {
      await workspaceService.joinWorkspaces({ invitations: invitationsRespond });
      await fetchWorkspaces();
      await fetchCurrentUserSettings();
      await handleNextStep();
    } catch (error) {
      console.error(error);
      setIsJoiningWorkspaces(false);
    }
  };

  if (!invitations.length) {
    return <div>No invitations found</div>;
  }

  return (
    <div className="flex flex-col gap-10">
      <CommonOnboardingHeader title="Join invite or create a workspace" description="All your work - unified" />
      <CheckboxGroup
        aria-label="Workspace invitations"
        value={invitationsRespond}
        onChange={setInvitationsRespond}
        isDisabled={isJoiningWorkspaces}
      >
        {invitations.map((invitation) => {
          const isSelected = invitationsRespond.includes(invitation.id);
          const invitedWorkspace = invitation.workspace;

          return (
            <Checkbox key={invitation.id} value={invitation.id} variant="secondary" className="w-full">
              <Checkbox.Content
                className={`flex w-full cursor-pointer items-center gap-2 rounded-lg border border-custom-border-200 px-3 py-2 hover:bg-custom-background-90 ${isSelected ? "bg-custom-background-90" : ""}`}
              >
                <div className="shrink-0">
                  <WorkspaceLogo logo={invitedWorkspace.logoUrl} name={invitedWorkspace.name} />
                </div>

                <div className="min-w-0 flex-1">
                  <div className="text-sm font-medium">{truncateText(invitedWorkspace.name, 30)}</div>
                  <p className="text-xs text-custom-text-200">{ROLE[invitation.role]}</p>
                </div>

                <Checkbox.Control>
                  <Checkbox.Indicator />
                </Checkbox.Control>
              </Checkbox.Content>
            </Checkbox>
          );
        })}
      </CheckboxGroup>

      <div className="flex flex-col gap-4">
        <Button
          size="sm"
          fullWidth
          onPress={submitInvitations}
          isDisabled={isJoiningWorkspaces || !invitationsRespond.length}
          isPending={isJoiningWorkspaces}
        >
          {({ isPending }) => (isPending ? <Spinner color="current" /> : "Continue")}
        </Button>

        <Button
          variant="tertiary"
          size="sm"
          fullWidth
          onPress={handleCurrentViewChange}
          isDisabled={isJoiningWorkspaces}
        >
          Create new workspace
        </Button>
      </div>
    </div>
  );
};
