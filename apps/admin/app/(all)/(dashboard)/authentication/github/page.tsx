"use client";

import { useState } from "react";
import Image from "next/image";
import useSWR from "swr";
import { useTheme } from "next-themes";
// heroui
import { Spinner, Switch, toast } from "@heroui/react";
// store
import { useInstance } from "@/hooks/store/use-instance";
// components
import { AuthenticationMethodCard } from "@/components/authentication/authentication-method-card";
import { InstanceGithubConfigForm } from "./form";
// assets
import githubLightModeImage from "@/public/logos/github-black.png";
import githubDarkModeImage from "@/public/logos/github-white.png";

export default function InstanceGithubAuthenticationPage() {
  // store hooks
  const { fetchInstanceConfigurations, formattedConfig, updateInstanceConfigurations } = useInstance();
  // state
  const [isSubmitting, setIsSubmitting] = useState(false);
  // theme
  const { resolvedTheme } = useTheme();
  // derived values
  const enableGithubConfig = formattedConfig?.IS_GITHUB_ENABLED ?? "";

  useSWR("INSTANCE_CONFIGURATIONS", () => fetchInstanceConfigurations());

  const updateConfig = async (key: "IS_GITHUB_ENABLED", value: string) => {
    if (isSubmitting) return;
    setIsSubmitting(true);

    const payload = {
      [key]: value,
    };

    try {
      toast.promise(updateInstanceConfigurations(payload), {
        loading: "Saving Github configuration...",
        success: () => `Github authentication is now ${value ? "active" : "disabled"}`,
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
          icon={
            <Image
              src={resolvedTheme === "dark" ? githubDarkModeImage : githubLightModeImage}
              height={24}
              width={24}
              alt="GitHub Logo"
            />
          }
          config={
            <Switch
              aria-label="Enable Github login"
              size="sm"
              name="isGithubEnabled"
              isSelected={Boolean(parseInt(enableGithubConfig))}
              onChange={(isSelected: boolean) => updateConfig("IS_GITHUB_ENABLED", isSelected ? "1" : "0")}
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
        {formattedConfig ? <InstanceGithubConfigForm config={formattedConfig} /> : <Spinner />}
      </div>
    </div>
  );
}
