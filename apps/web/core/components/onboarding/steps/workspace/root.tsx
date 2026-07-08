"use client";

import type { FC } from "react";
import { useState } from "react";
// types
import type { TCreateOrJoinWorkspaceViews, IWorkspaceMemberInvitation, TOnboardingStep } from "@syncturtle/types";
import { ECreateOrJoinWorkspaceViews, EOnboardingSteps } from "@syncturtle/constants";
import { useUser } from "@/hooks/store/user";
import { WorkspaceJoinInvitesStep } from "./join-invites";
import { WorkspaceCreateStep } from "./create";

interface IWorkspaceSetupStepProps {
  invitations: IWorkspaceMemberInvitation[];
  handleStepChange: (step: TOnboardingStep, skipInvites?: boolean) => void;
}

export const WorkspaceSetupStep: FC<IWorkspaceSetupStepProps> = (props) => {
  const { invitations, handleStepChange } = props;
  // states
  const [selectedView, setSelectedView] = useState<TCreateOrJoinWorkspaceViews | null>(null);
  // hooks
  const { data: user } = useUser();
  // derived values
  const hasInvitations = invitations.length > 0;
  const currentView: TCreateOrJoinWorkspaceViews = hasInvitations
    ? (selectedView ?? ECreateOrJoinWorkspaceViews.WORKSPACE_JOIN)
    : ECreateOrJoinWorkspaceViews.WORKSPACE_CREATE;

  return (
    <>
      {currentView === ECreateOrJoinWorkspaceViews.WORKSPACE_JOIN ? (
        <WorkspaceJoinInvitesStep
          invitations={invitations}
          handleNextStep={async () => {
            handleStepChange(EOnboardingSteps.WORKSPACE_CREATE_OR_JOIN, true);
          }}
          handleCurrentViewChange={() => setSelectedView(ECreateOrJoinWorkspaceViews.WORKSPACE_CREATE)}
        />
      ) : (
        <WorkspaceCreateStep
          user={user}
          onComplete={(skipInvites) => handleStepChange(EOnboardingSteps.WORKSPACE_CREATE_OR_JOIN, skipInvites)}
          handleCurrentViewChange={() => setSelectedView(ECreateOrJoinWorkspaceViews.WORKSPACE_JOIN)}
          hasInvitations={invitations.length > 0}
        />
      )}
    </>
  );
};
