"use client";

import type { FC } from "react";
import Image from "next/image";
import { ChevronLeft } from "lucide-react";
import { useTheme } from "next-themes";
// syncturtle imports
import type { TOnboardingStep } from "@syncturtle/types";
import { EOnboardingSteps } from "@syncturtle/constants";
// components
import { Tooltip } from "./tool-tip";
import { SwitchAccountDropdown } from "./switch-account-dropdown";
// hooks
import { useUser } from "@/hooks/store/user";
import { cn } from "@syncturtle/ui";
// assets
import BlackHorizontalLogo from "@/public/syncturtle-logos/syncturtle-logo-dark.png";
import WhiteHorizontalLogo from "@/public/syncturtle-logos/syncturtle-logo-light.png";

interface IOnboardingHeaderProps {
  currentStep: TOnboardingStep;
  updateCurrentStep: (step: TOnboardingStep) => void;
  isBusy: boolean;
}

export const OnboardingHeader: FC<IOnboardingHeaderProps> = (props) => {
  const { currentStep, updateCurrentStep, isBusy } = props;
  // theme
  const { resolvedTheme } = useTheme();
  // store hooks
  const { data: user } = useUser();

  // derived values
  const stepOrder: TOnboardingStep[] = [
    EOnboardingSteps.PROFILE_SETUP,
    EOnboardingSteps.WORKSPACE_CREATE_OR_JOIN,
    EOnboardingSteps.USE_CASE_SETUP,
  ];
  const currentStepNumber = stepOrder.indexOf(currentStep) + 1;
  const totalSteps = stepOrder.length;
  const canGoBack = currentStepNumber > 1 && !isBusy;
  const handleStepBack = () => {
    const previousStep = stepOrder[currentStepNumber - 2];
    if (canGoBack && previousStep) updateCurrentStep(previousStep);
  };

  const username =
    user?.displayName || [user?.firstName, user?.lastName].filter(Boolean).join(" ") || user?.email || "";

  const logo = resolvedTheme === "dark" ? BlackHorizontalLogo : WhiteHorizontalLogo;
  return (
    <div className="flex flex-col gap-4 sticky top-0 z-10">
      <div className="h-1.5 rounded-t-lg w-full bg-custom-background-100 overflow-hidden cursor-pointer">
        <Tooltip tooltipContent={`${currentStepNumber}/${totalSteps}`} position="bottom-end">
          <div
            className="h-full bg-custom-primary-100 transition-all duration-700 ease-out"
            style={{ width: `${(currentStepNumber / totalSteps) * 100}%` }}
          />
        </Tooltip>
      </div>
      <div className={cn("flex items-center justify-between gap-6 w-full px-6", canGoBack && "pl-4 pr-6")}>
        <div className="flex items-center gap-2.5">
          {canGoBack && (
            <button onClick={handleStepBack} className="cursor-pointer" type="button" disabled={!canGoBack}>
              <ChevronLeft className="size-6 text-custom-text-400" />
            </button>
          )}
          <Image src={logo} alt="Syncturtle logo" className="h-7.5 w-33.25" />
        </div>
        <SwitchAccountDropdown fullName={username} />
      </div>
    </div>
  );
};
