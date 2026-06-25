"use client";

import type { FC } from "react";
// import { useState } from "react";
// types
import type { IWorkspaceMemberInvitation, TOnboardingStep } from "@syncturtle/types";
// import { ECreateOrJoinWorkspaceViews } from "@syncturtle/constants";

interface IWorkspaceSetupStepProps {
  invitations: IWorkspaceMemberInvitation[];
  handleStepChange: (step: TOnboardingStep, skipInvites?: boolean) => void;
}

export const WorkspaceSetupStep: FC<IWorkspaceSetupStepProps> = (props) => {
  console.log(props);
  // const { invitations, handleStepChange } = props;
  // states
  return <div>hi</div>;
};
