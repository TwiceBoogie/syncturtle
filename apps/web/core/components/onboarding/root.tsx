"use client";

import { useState } from "react";
// syncturtle imports
import { EOnboardingSteps } from "@syncturtle/constants";
import type { TOnboardingStep } from "@syncturtle/types";
import { useUserProfile } from "@/hooks/store/user";
// components
import { OnboardingHeader } from "./header";
import { OnboardingStepRoot } from "./steps";

export const OnboardingRoot = () => {
  // store hooks
  const { data: profile, updateUserProfile, finishUserOnboarding } = useUserProfile();
  // Old workspaceCreate/workspaceJoin/workspaceInvite flags are experience history,
  // never proof of membership. Resume through the current workspace collection.
  const [currentStep, setCurrentStep] = useState<TOnboardingStep>(
    profile.onboardingStep.profileComplete ? EOnboardingSteps.WORKSPACE_CREATE_OR_JOIN : EOnboardingSteps.PROFILE_SETUP
  );
  const [isAdvancing, setIsAdvancing] = useState(false);
  const [isStepBusy, setIsStepBusy] = useState(false);
  const [error, setError] = useState<string>();

  const handleStepChange = async (step: TOnboardingStep) => {
    if (isAdvancing) return;
    setIsAdvancing(true);
    setError(undefined);
    try {
      if (step === EOnboardingSteps.PROFILE_SETUP) {
        await updateUserProfile({ onboardingStep: { ...profile.onboardingStep, profileComplete: true } });
        setCurrentStep(EOnboardingSteps.WORKSPACE_CREATE_OR_JOIN);
      } else if (step === EOnboardingSteps.WORKSPACE_CREATE_OR_JOIN) {
        setCurrentStep(EOnboardingSteps.USE_CASE_SETUP);
      } else if (step === EOnboardingSteps.USE_CASE_SETUP) {
        // AuthenticationWrapper navigates only after persisted completion settles.
        await finishUserOnboarding();
      }
    } catch (failure) {
      setError("Could not save onboarding. Please try again.");
      throw failure;
    } finally {
      setIsAdvancing(false);
    }
  };

  return (
    <div className="flex flex-col h-full">
      <OnboardingHeader
        currentStep={currentStep}
        updateCurrentStep={setCurrentStep}
        isBusy={isAdvancing || isStepBusy}
      />
      {error && <p role="alert">{error}</p>}
      <OnboardingStepRoot currentStep={currentStep} handleStepChange={handleStepChange} onBusyChange={setIsStepBusy} />
    </div>
  );
};
