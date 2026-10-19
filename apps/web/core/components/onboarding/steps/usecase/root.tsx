"use client";

import type { FC, SyntheticEvent } from "react";
import { useState } from "react";
// heroui
import { Button, Form, toast } from "@heroui/react";
// store hooks
import { useUserProfile } from "@/hooks/store/user";
// components
import { CommonOnboardingHeader } from "../common";
// constants
import { EOnboardingSteps, USE_CASES } from "@syncturtle/constants";
// helpers
import { cn } from "@syncturtle/ui";
// types
import type { TOnboardingStep } from "@syncturtle/types";
import { Check } from "lucide-react";

interface IUseCaseSetupStepProps {
  handleStepChange: (step: TOnboardingStep, skipInvites?: boolean) => void;
}

export const UseCaseSetupStep: FC<IUseCaseSetupStepProps> = ({ handleStepChange }) => {
  // hooks
  const { data: profile, updateUserProfile } = useUserProfile();
  // states
  const [useCase, setUseCase] = useState<string>();
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmitUserPersonalization = async () => {
    try {
      await updateUserProfile({ useCase });
      toast("Profile setup completed", { description: "Your purpose was saved." });
    } catch {
      // the step still advances after this caught update failure.
      // Revisit progress and error behavior during post-parity hardening.
      toast("Profile setup failed", { description: "Please try again later." });
    }
  };

  const onSubmit = async (event: SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!profile || !useCase || isSubmitting) return;
    setIsSubmitting(true);
    await handleSubmitUserPersonalization();
    setIsSubmitting(false);
    handleStepChange(EOnboardingSteps.USE_CASE_SETUP);
  };

  const handleSkip = () => handleStepChange(EOnboardingSteps.USE_CASE_SETUP);

  return (
    <Form onSubmit={onSubmit} className="flex flex-col gap-5">
      <CommonOnboardingHeader
        title="What would you like to use SyncTurtle for?"
        description="Choose a purpose to personalize your Space."
      />
      <div className="flex flex-col gap-3">
        <p className="text-sm font-medium text-custom-text-400">Select any</p>
        {USE_CASES.map((option) => {
          const isSelected = useCase === option;
          return (
            <button
              key={option}
              type="button"
              className={cn(
                "flex w-full items-center gap-2 rounded-lg border px-3 py-2 text-left transition-all",
                isSelected
                  ? "border-custom-primary-100 bg-custom-primary-10 text-custom-primary-100"
                  : "border-custom-border-200 text-custom-text-300 hover:border-custom-border-300"
              )}
              onClick={() => setUseCase(option)}
            >
              <span
                className={cn(
                  "flex size-4 items-center justify-center rounded border-2",
                  isSelected ? "border-custom-primary-100 bg-custom-primary-100" : "border-custom-border-300"
                )}
              >
                <Check className={cn("size-3 text-white", !isSelected && "opacity-0")} />
              </span>
              <span className="font-medium">{option}</span>
            </button>
          );
        })}
      </div>
      <div className="space-y-3">
        <Button type="submit" fullWidth isDisabled={!useCase || isSubmitting} isPending={isSubmitting}>
          Continue
        </Button>
        <Button type="button" variant="tertiary" fullWidth onPress={handleSkip}>
          Skip
        </Button>
      </div>
    </Form>
  );
};
