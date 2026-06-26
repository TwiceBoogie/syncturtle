"use client";

import type { FC } from "react";
import { useCallback, useEffect, useState } from "react";
// syncturtle imports
import type { IWorkspaceMemberInvitation, TOnboardingStep, TOnboardingSteps, TUserProfile } from "@syncturtle/types";
import { EOnboardingSteps } from "@syncturtle/constants";
// components
import { OnboardingHeader } from "./header";
import { OnboardingStepRoot } from "./steps";
// hooks
import { useUser, useUserProfile } from "@/hooks/store/user";
import { useWorkspace } from "@/hooks/store/use-workspace";
import { toast } from "@heroui/react";

interface IOnboardingRootProps {
  invitations?: IWorkspaceMemberInvitation[];
}

export const OnboardingRoot: FC<IOnboardingRootProps> = (props) => {
  const { invitations = [] } = props;
  // state
  const [currentStep, setCurrentStep] = useState<TOnboardingStep>(EOnboardingSteps.PROFILE_SETUP);
  // store hooks
  const { data: user } = useUser();
  const { data: userProfile, updateUserProfile, finishUserOnboarding } = useUserProfile();
  const { workspaces } = useWorkspace();

  // derived values
  const workspacesList = Object.values(workspaces ?? {});
  const hasInvitations = invitations.length > 0;

  const finishOnboarding = useCallback(async () => {
    if (!user) return;

    try {
      await finishUserOnboarding();
    } catch (error) {
      console.log(error);
      toast.danger("Failed", {
        actionProps: {
          children: "remove",
          onPress: () => toast.clear(),
        },
        description: "Failed to update user profile.",
      });
    }
  }, [user, finishUserOnboarding]);

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
          setCurrentStep(EOnboardingSteps.ROLE_SETUP);
          break;
        case EOnboardingSteps.ROLE_SETUP:
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
        userProfile.onboardingStep.profileComplete &&
        !userProfile.onboardingStep.workspaceCreate &&
        !userProfile.onboardingStep.workspaceJoin
      ) {
        setCurrentStep(EOnboardingSteps.WORKSPACE_CREATE_OR_JOIN);
      }
      if (
        userProfile.onboardingStep.profileComplete &&
        userProfile.onboardingStep.workspaceCreate &&
        !userProfile.onboardingStep.workspaceInvite
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
