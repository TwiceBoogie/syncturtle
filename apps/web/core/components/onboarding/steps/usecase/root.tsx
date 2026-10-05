"use client";

import { useState } from "react";
// heroui
import { Button, Spinner } from "@heroui/react";
// components
import { CommonOnboardingHeader } from "../common";
// constants
import { EOnboardingSteps } from "@syncturtle/constants";
// helpers
import { cn } from "@syncturtle/ui";
// types
import type { TOnboardingStep } from "@syncturtle/types";

const PURPOSES = ["Personal goals", "Household", "Fitness", "Study", "Family", "Trip", "Side project", "Exploring"];

type Props = { handleStepChange: (step: TOnboardingStep) => Promise<void> };

export const UseCaseSetupStep = ({ handleStepChange }: Props) => {
  // A purpose is a local onboarding hint until planning templates exist.
  const [purpose, setPurpose] = useState<string>();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string>();

  const finish = async () => {
    if (isSubmitting) return;
    setIsSubmitting(true);
    setError(undefined);
    try {
      await handleStepChange(EOnboardingSteps.USE_CASE_SETUP);
    } catch {
      setError("Could not finish onboarding. Please try again.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="flex flex-col gap-8">
      <CommonOnboardingHeader
        title="What would you like to use your space for?"
        description="This is optional. You can decide what to work on later."
      />
      <div className="flex flex-col gap-3">
        {PURPOSES.map((option) => (
          <Button
            key={option}
            type="button"
            variant="ghost"
            fullWidth
            isDisabled={isSubmitting}
            className={cn(
              "h-auto justify-start rounded-lg border px-3 py-2",
              purpose === option ? "border-custom-primary-100 bg-custom-primary-10" : "border-custom-border-200"
            )}
            onPress={() => setPurpose(option)}
          >
            {option}
          </Button>
        ))}
      </div>
      {error && <p role="alert">{error}</p>}
      <div className="flex flex-col gap-3">
        <Button
          type="button"
          fullWidth
          isDisabled={isSubmitting}
          isPending={isSubmitting}
          onPress={() => void finish()}
        >
          {isSubmitting ? <Spinner color="current" /> : null}
          Open my space
        </Button>
        <Button type="button" variant="secondary" fullWidth isDisabled={isSubmitting} onPress={() => void finish()}>
          Skip and open my space
        </Button>
      </div>
    </div>
  );
};
