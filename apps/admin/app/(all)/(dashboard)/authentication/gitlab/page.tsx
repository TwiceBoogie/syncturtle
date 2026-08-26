"use client";

import { useState } from "react";
import Image from "next/image";
import useSWR from "swr";
// heroui
import { Spinner, Switch, toast } from "@heroui/react";
// store
import { useInstance } from "@/hooks/store/use-instance";
// components
import { AuthenticationMethodCard } from "@/components/authentication/authentication-method-card";
// icons
import GitlabLogo from "@/public/logos/gitlab-logo.svg";
import { InstanceGitlabConfigForm } from "./form";

export default function InstanceGitlabAuthenticationPage() {
  // store hooks
  // store hooks
  const { fetchInstanceConfigurations, formattedConfig, updateInstanceConfigurations } = useInstance();
  // state
  const [isSubmitting, setIsSubmitting] = useState(false);
  // derived values
  const enableGitlabConfig = formattedConfig?.IS_GITLAB_ENABLED ?? "";

  useSWR("INSTANCE_CONFIGURATIONS", () => fetchInstanceConfigurations());

  const updateConfig = async (key: "IS_GITLAB_ENABLED", value: string) => {
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
          icon={<Image src={GitlabLogo} height={24} width={24} alt="GitLab Logo" />}
          config={
            <Switch
              aria-label="Enable Github login"
              size="sm"
              name="isGithubEnabled"
              isSelected={Boolean(parseInt(enableGitlabConfig))}
              onChange={(isSelected: boolean) => updateConfig("IS_GITLAB_ENABLED", isSelected ? "1" : "0")}
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
        {formattedConfig ? <InstanceGitlabConfigForm config={formattedConfig} /> : <Spinner />}
      </div>
    </div>
  );
}
