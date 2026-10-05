"use client";

import { useEffect, useRef } from "react";
// syncturtle imports
import { EOnboardingSteps } from "@syncturtle/constants";
import type { TOnboardingStep } from "@syncturtle/types";
// components
import { ProfileSetupStep } from "./profile";
import { UseCaseSetupStep } from "./usecase";
import { WorkspaceSetupStep } from "./workspace";

type Props = {
  currentStep: TOnboardingStep;
  handleStepChange: (step: TOnboardingStep) => Promise<void>;
  onBusyChange: (busy: boolean) => void;
};

export const OnboardingStepRoot = ({ currentStep, handleStepChange, onBusyChange }: Props) => {
  const scrollContainerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    scrollContainerRef.current?.scrollTo({ top: 0, behavior: "smooth" });
  }, [currentStep]);

  return (
    <div ref={scrollContainerRef} className="flex-1 overflow-y-auto">
      <div className="flex items-center justify-center min-h-full p-8">
        <div className="w-full max-w-sm">
          {currentStep === EOnboardingSteps.PROFILE_SETUP && <ProfileSetupStep handleStepChange={handleStepChange} />}
          {currentStep === EOnboardingSteps.WORKSPACE_CREATE_OR_JOIN && (
            <WorkspaceSetupStep
              onComplete={() => handleStepChange(EOnboardingSteps.WORKSPACE_CREATE_OR_JOIN)}
              onBusyChange={onBusyChange}
            />
          )}
          {currentStep === EOnboardingSteps.USE_CASE_SETUP && <UseCaseSetupStep handleStepChange={handleStepChange} />}
        </div>
      </div>
    </div>
  );
};
