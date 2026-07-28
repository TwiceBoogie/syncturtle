"use client";

import type { FC, FormEvent } from "react";
import { useEffect, useMemo, useState } from "react";
import { Box, Check, PenTool, Rocket, Monitor, RefreshCw, Kanban } from "lucide-react";
// heroui
import { Button, Form, Spinner, toast } from "@heroui/react";
// syncturtle imports
import type { TOnboardingStep, TUserProfile } from "@syncturtle/types";
import { EOnboardingSteps } from "@syncturtle/constants";
import { cn } from "@syncturtle/ui";
// hooks
import { useUserProfile } from "@/hooks/store/user";
// constants
import { CommonOnboardingHeader } from "../common";

interface IRoleSetupStepProps {
  handleStepChange: (step: TOnboardingStep, skipInvites?: boolean) => void;
}

interface IRoleFormData {
  role: string;
}

const ROLES = [
  { id: "product-manager", label: "Product Manager", icon: Box },
  { id: "engineering-manager", label: "Engineering Manager", icon: Kanban },
  { id: "designer", label: "Designer", icon: PenTool },
  { id: "developer", label: "Developer", icon: Monitor },
  { id: "founder-executive", label: "Founder/Executive", icon: Rocket },
  { id: "operations-manager", label: "Operations Manager", icon: RefreshCw },
  { id: "others", label: "Others", icon: Box },
];

const defaultFormData: IRoleFormData = {
  role: "",
};

export const RoleSetupStep: FC<IRoleSetupStepProps> = (props) => {
  const { handleStepChange } = props;
  // store hooks
  const { data: profile, updateUserProfile } = useUserProfile();
  // states
  const [formData, setFormData] = useState<IRoleFormData>(() => ({
    ...defaultFormData,
    role: profile?.role ?? "",
  }));
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [wasSubmitted, setWasSubmitted] = useState(false);

  useEffect(() => {
    setFormData((prev) => ({ ...prev, role: profile?.role ?? "" }));
  }, [profile?.role]);

  const handleFormChange = (key: keyof IRoleFormData, value: string) =>
    setFormData((prev) => ({ ...prev, [key]: value }));

  const roleError = useMemo(() => {
    if (!wasSubmitted) return undefined;
    if (!formData.role.trim()) return "This field is required";

    return undefined;
  }, [formData.role, wasSubmitted]);

  const isButtonDisabled = useMemo(() => {
    if (isSubmitting) return true;
    if (!formData.role.trim()) return true;

    return false;
  }, [formData.role, isSubmitting]);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    setWasSubmitted(true);

    if (!profile || isSubmitting || !formData.role.trim()) return;

    const payload: Partial<TUserProfile> = {
      role: formData.role,
    };

    setIsSubmitting(true);

    try {
      const promise = updateUserProfile(payload);

      toast.promise(promise, {
        loading: "Saving role...",
        success: "Role saved successfully",
        error: () => <div>hi</div>,
      });

      await promise;

      handleStepChange(EOnboardingSteps.ROLE_SETUP);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleSkip = () => {
    if (isSubmitting) return;

    handleStepChange(EOnboardingSteps.ROLE_SETUP);
  };

  return (
    <Form onSubmit={handleSubmit} className="flex flex-col gap-16">
      <CommonOnboardingHeader title="What's your role?" description="Let's setup Syncturtle for how you work." />

      <div className="flex flex-col gap-3">
        <p className="text-sm font-medium text-custom-text-400">Select one</p>
        <div className="flex flex-col gap-3">
          {ROLES.map((role) => {
            const Icon = role.icon;
            const isSelected = formData.role === role.id;

            return (
              <Button
                key={role.id}
                type="button"
                variant="ghost"
                fullWidth
                className={cn(
                  "h-auto justify-between rounded-lg border px-3 py-2 transition-all duration-200",
                  isSelected
                    ? "border-custom-primary-100 bg-custom-primary-10 text-custom-primary-100"
                    : "border-custom-border-200 text-custom-text-300 hover:border-custom-border-300"
                )}
                onPress={() => handleFormChange("role", role.id)}
              >
                <div className="flex items-center gap-3">
                  <Icon className="size-3.5" />
                  <span className="font-medium">{role.label}</span>
                </div>

                <span className={""}>
                  <Check className={cn("size-3 text-white", isSelected ? "opacity-100" : "opacity-0")} />
                </span>
              </Button>
            );
          })}
        </div>

        {roleError && <span className="text-sm text-red-500">{roleError}</span>}
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
