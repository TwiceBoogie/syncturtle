"use client";

import type { FC } from "react";
import { useEffect, useMemo, useRef } from "react";
// syncturtle imports
import type { IWorkspaceMemberInvitation, TOnboardingStep } from "@syncturtle/types";
import { EOnboardingSteps } from "@syncturtle/constants";
// components
import { ProfileSetupStep } from "./profile";
import { RoleSetupStep } from "./role";
import { UseCaseSetupStep } from "./usecase";
import { WorkspaceSetupStep } from "./workspace";
import { InviteTeamStep } from "./team";

interface IOnboardingStepRootProps {
  currentStep: TOnboardingStep;
  invitations: IWorkspaceMemberInvitation[];
  handleStepChange: (step: TOnboardingStep, skipInvites?: boolean) => void;
}

export const OnboardingStepRoot: FC<IOnboardingStepRootProps> = (props) => {
  const { currentStep, invitations, handleStepChange } = props;

  const scrollContainerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (scrollContainerRef.current) {
      scrollContainerRef.current.scrollTo({
        top: 0,
        behavior: "smooth",
      });
    }
  }, [currentStep]);

  const stepComponents = useMemo(
    () => ({
      [EOnboardingSteps.PROFILE_SETUP]: <ProfileSetupStep handleStepChange={handleStepChange} />,
      [EOnboardingSteps.ROLE_SETUP]: <RoleSetupStep handleStepChange={handleStepChange} />,
      [EOnboardingSteps.USE_CASE_SETUP]: <UseCaseSetupStep handleStepChange={handleStepChange} />,
      [EOnboardingSteps.WORKSPACE_CREATE_OR_JOIN]: (
        <WorkspaceSetupStep invitations={invitations} handleStepChange={handleStepChange} />
      ),
      [EOnboardingSteps.INVITE_MEMBERS]: <InviteTeamStep handleStepChange={handleStepChange} />,
    }),
    [handleStepChange, invitations]
  );

  return (
    <div ref={scrollContainerRef} className="flex-1 overflow-y-auto">
      <div className="flex items-center justify-center min-h-full p-8">
        <div className="w-full max-w-sm">{stepComponents[currentStep]}</div>
      </div>
    </div>
  );
};
