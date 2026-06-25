"use client";

import type { FC, FormEvent } from "react";
import { useEffect, useMemo, useState } from "react";
// heroui
import { Button, Form, Spinner, toast } from "@heroui/react";
import { Check } from "lucide-react";
// hooks
import { useUserProfile } from "@/hooks/store/user";
// components
import { CommonOnboardingHeader } from "../common";
// helpers
// constants
import { EOnboardingSteps, USE_CASES } from "@syncturtle/constants";
// types
import type { TOnboardingStep, TUserProfile } from "@syncturtle/types";
import { cn } from "@syncturtle/ui";

interface IUseCaseSetupStep {
  handleStepChange: (step: TOnboardingStep, skipInvites?: boolean) => void;
}

interface IUseCaseFormData {
  useCase: string;
}

const defaultFormData: IUseCaseFormData = {
  useCase: "",
};

export const UseCaseSetupStep: FC<IUseCaseSetupStep> = (props) => {
  const { handleStepChange } = props;
  // store hooks
  const { data: profile, updateUserProfile } = useUserProfile();
  // states
  const [formData, setFormData] = useState<IUseCaseFormData>(() => ({
    ...defaultFormData,
    useCase: profile?.useCase ?? "",
  }));
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [wasSubmitted, setWasSubmitted] = useState(false);

  useEffect(() => {
    setFormData((prev) => ({ ...prev, useCase: profile.useCase ?? "" }));
  }, [profile.useCase]);

  const handleFormChange = (key: keyof IUseCaseFormData, value: string) =>
    setFormData((prev) => ({ ...prev, [key]: value }));

  const useCaseError = useMemo(() => {
    if (!wasSubmitted) return undefined;
    if (!formData.useCase.trim()) return "This field is required";

    return undefined;
  }, [formData.useCase, wasSubmitted]);

  const isButtonDisabled = useMemo(() => {
    if (isSubmitting) return true;
    if (!formData.useCase.trim()) return true;

    return false;
  }, [formData.useCase, isSubmitting]);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    setWasSubmitted(true);

    if (!profile || isSubmitting || !formData.useCase.trim()) return;

    const payload: Partial<TUserProfile> = {
      useCase: formData.useCase,
    };

    setIsSubmitting(true);

    try {
      const promise = updateUserProfile(payload);

      toast.promise(promise, {
        loading: "Saving preference...",
        success: "Preference saved successfully",
        error: () => <div>hi</div>,
      });

      await promise;

      handleStepChange(EOnboardingSteps.USE_CASE_SETUP);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleSkip = () => {
    if (isSubmitting) return;

    handleStepChange(EOnboardingSteps.USE_CASE_SETUP);
  };

  return (
    <Form onSubmit={handleSubmit} className="flex flex-col gap-10">
      <CommonOnboardingHeader title="What brings you to Syncturtle" description="Tell us your goals and team size." />

      <div className="flex flex-col gap-3">
        <p className="text-sm font-medium text-custom-text-400">Select one</p>

        <div className="flex flex-col gap-3">
          {USE_CASES.map((useCase) => {
            const isSelected = formData.useCase === useCase;

            return (
              <Button
                key={useCase}
                type="button"
                variant="ghost"
                fullWidth
                className={cn(
                  "h-auto justify-start gap-2 rounded-lg border px-3 py-2 transition-all duration-200",
                  isSelected
                    ? "border-custom-primary-100 bg-custom-primary-10 text-custom-primary-100"
                    : "border-custom-border-200 text-custom-text-300 hover:border-custom-border-300"
                )}
                onPress={() => handleFormChange("useCase", useCase)}
              >
                <span
                  className={cn(
                    "flex size-4 items-center justify-center rounded border-2",
                    isSelected ? "border-custom-primary-100 bg-custom-primary-100" : "border-custom-border-300"
                  )}
                >
                  <Check className={cn("size-3 text-white", isSelected ? "opacity-100" : "opacity-0")} />
                </span>
                <span className="font-medium">{useCase}</span>
              </Button>
            );
          })}
        </div>
        {useCaseError && <span className="text-sm text-red-500">{useCaseError}</span>}
      </div>

      <div className="space-y-3">
        <Button type="submit" size="sm" fullWidth isPending={isSubmitting} isDisabled={isButtonDisabled}>
          {({ isPending }) => (
            <>
              {isPending ? <Spinner color="current" /> : null}
              {isPending ? "Saving..." : "Continue"}
            </>
          )}
        </Button>
        <Button type="button" variant="secondary" size="sm" fullWidth isDisabled={isSubmitting} onPress={handleSkip}>
          Skip
        </Button>
      </div>
    </Form>
  );
};
