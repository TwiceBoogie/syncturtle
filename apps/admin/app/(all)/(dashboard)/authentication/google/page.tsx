"use client";

import Image from "next/image";
import { useState } from "react";
import useSWR from "swr";
import { Spinner, Switch, toast } from "@heroui/react";
// components
import { AuthenticationMethodCard } from "@/components/authentication/authentication-method-card";
// hooks
import { useInstance } from "@/hooks/store/use-instance";
// icons
import GoogleLogo from "@/public/logos/google-logo.svg";
import { InstanceGoogleConfigForm } from "./form";

export default function InstanceGoogleAuthenticationPage() {
  // store hooks
  const { fetchInstanceConfigurations, formattedConfig, updateInstanceConfigurations } = useInstance();
  // state
  const [isSubmitting, setIsSubmitting] = useState(false);
  // derived values
  const enableGoogleConfig = formattedConfig?.IS_GOOGLE_ENABLED ?? "";

  useSWR("INSTANCE_CONFIGURATIONS", () => fetchInstanceConfigurations());

  const updateConfig = async (key: "IS_GOOGLE_ENABLED", value: string) => {
    if (isSubmitting) return;
    setIsSubmitting(true);

    const payload = {
      [key]: value,
    };

    try {
      toast.promise(updateInstanceConfigurations(payload), {
        loading: "Saving Google configuration...",
        success: () => `Google authentication is now ${value ? "active" : "disabled"}`,
        error: () => "Failed to save configuration",
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="relative container mx-auto w-full h-full p-4 py-4 space-y-6 flex flex-col">
      <div className="border-b border-custom-border-100 mx-4 py-4 space-y-1 shrink-0">
        <AuthenticationMethodCard
          name="GitHub"
          description="Allow members to login or sign up to plane with their Github accounts."
          icon={<Image src={GoogleLogo} height={24} width={24} alt="Google Logo" />}
          config={
            <Switch
              aria-label="Enable Google login"
              size="sm"
              name="isGoogleEnabled"
              isSelected={Boolean(parseInt(enableGoogleConfig))}
              onChange={(isSelected: boolean) => updateConfig("IS_GOOGLE_ENABLED", isSelected ? "1" : "0")}
              isDisabled={isSubmitting || !formattedConfig}
            >
              <Switch.Control>
                <Switch.Thumb>
                  <Switch.Icon />
                </Switch.Thumb>
              </Switch.Control>
            </Switch>
          }
          withBorder={false}
        />
      </div>
      <div className="grow overflow-hidden overflow-y-scroll vertical-scrollbar scrollbar-md px-4">
        {formattedConfig ? <InstanceGoogleConfigForm config={formattedConfig} /> : <Spinner />}
      </div>
    </div>
  );
}
