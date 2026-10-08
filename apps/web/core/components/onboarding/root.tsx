"use client";

import type { FC } from "react";
import { useCallback, useEffect, useState } from "react";
// syncturtle imports
import { EOnboardingSteps } from "@syncturtle/constants";
import type { IWorkspaceMemberInvitation, TOnboardingStep, TOnboardingSteps, TUserProfile } from "@syncturtle/types";
// hooks
import { useUser, useUserProfile } from "@/hooks/store/user";
import { useWorkspace } from "@/hooks/store/use-workspace";
// components
import { OnboardingHeader } from "./header";
import { OnboardingStepRoot } from "./steps";

interface IOnboardingRootProps {
  invitations?: IWorkspaceMemberInvitation[];
}

export const OnboardingRoot: FC<IOnboardingRootProps> = ({ invitations = [] }) => {
  const [currentStep, setCurrentStep] = useState<TOnboardingStep>(EOnboardingSteps.PROFILE_SETUP);
  // store hooks
  const { data: user } = useUser();
  const { data: userProfile, updateUserProfile, finishUserOnboarding } = useUserProfile();
  const { workspaces } = useWorkspace();

  const workspacesList = Object.values(workspaces ?? {});

  // Calculate total steps based on whether invitations are available
  const hasInvitations = invitations.length > 0;

  const finishOnboarding = useCallback(async () => {
    if (!user) return;

    await finishUserOnboarding()
      .then(() => {
        // capture success feature but later
      })
      .catch(() => {
        // use heroui Toast component to showcase error. error could come from finishUserOnboarding()
      });
  }, [user, finishUserOnboarding]);

  // handle step change
  const stepChange = useCallback(
    async (steps: Partial<TOnboardingSteps>) => {
      if (!user) return;

      const payload: Partial<TUserProfile> = {
        onboardingStep: {
          ...userProfile.onboardingStep,
          ...steps,
        },
      };

      await updateUserProfile(payload);
    },
    [user, userProfile, updateUserProfile]
  );

  const handleStepChange = useCallback(
    (step: TOnboardingStep, skipInvites?: boolean) => {
      switch (step) {
        case EOnboardingSteps.PROFILE_SETUP:
          setCurrentStep(EOnboardingSteps.USE_CASE_SETUP);
          break;
        case EOnboardingSteps.USE_CASE_SETUP:
          stepChange({ profileComplete: true });
          if (workspacesList.length > 0) finishOnboarding();
          else setCurrentStep(EOnboardingSteps.WORKSPACE_CREATE_OR_JOIN);
          break;
        case EOnboardingSteps.WORKSPACE_CREATE_OR_JOIN:
          if (skipInvites) finishOnboarding();
          else {
            setCurrentStep(EOnboardingSteps.INVITE_MEMBERS);
            stepChange({ workspaceCreate: true });
          }
          break;
        case EOnboardingSteps.INVITE_MEMBERS:
          stepChange({ workspaceInvite: true });
          finishOnboarding();
          break;
      }
    },
    [stepChange, finishOnboarding, workspacesList]
  );

  const updateCurrentStep = (step: TOnboardingStep) => setCurrentStep(step);

  useEffect(() => {
    const handleInitialStep = () => {
      if (
        userProfile?.onboardingStep?.profileComplete &&
        !userProfile?.onboardingStep?.workspaceCreate &&
        !userProfile?.onboardingStep?.workspaceJoin
      ) {
        setCurrentStep(EOnboardingSteps.WORKSPACE_CREATE_OR_JOIN);
      }
      if (
        userProfile?.onboardingStep?.profileComplete &&
        userProfile?.onboardingStep?.workspaceCreate &&
        !userProfile?.onboardingStep?.workspaceInvite
      ) {
        setCurrentStep(EOnboardingSteps.INVITE_MEMBERS);
      }
    };

    handleInitialStep();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div className="flex flex-col h-full">
      <OnboardingHeader
        currentStep={currentStep}
        updateCurrentStep={updateCurrentStep}
        hasInvitations={hasInvitations}
      />

      <OnboardingStepRoot currentStep={currentStep} invitations={invitations} handleStepChange={handleStepChange} />
    </div>
  );
};
