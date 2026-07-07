"use client";

import { useState } from "react";
import useSWR from "swr";
// heroui
import { Switch, toast } from "@heroui/react";
// syncturtle imports
import type { TInstanceConfigurationKeys } from "@syncturtle/types";
import { cn, Loader } from "@syncturtle/ui";
// store hooks
import { useInstance } from "@/hooks/store/use-instance";
// components
import { AuthenticationModes } from "@/syncturtle-admin/components/authentication";

export default function AuthenticationPage() {
  // store hooks
  const { fetchInstanceConfigurations, formattedConfig, updateInstanceConfigurations } = useInstance();

  useSWR("INSTANCE_CONFIGURATIONS", () => fetchInstanceConfigurations());

  // state
  const [isSubmitting, setIsSubmitting] = useState(false);
  // derived values
  const enableSignUpConfig = formattedConfig?.ENABLE_SIGNUP ?? "";

  const updateConfig = async (key: TInstanceConfigurationKeys, value: string) => {
    if (isSubmitting) return;
    setIsSubmitting(true);

    const payload = {
      [key]: value,
    };

    try {
      toast.promise(updateInstanceConfigurations(payload), {
        loading: "Saving configurations...",
        success: "Configuration saved successfully",
        error: (err) => {
          if (err instanceof Error) return err.message;

          if (typeof err === "object" && err !== null && "message" in err) {
            const message = (err as { message?: unknown }).message;
            if (typeof message === "string" && message.trim()) {
              return message;
            }
          }
          return "Failed to update authentication settings";
        },
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="relative container mx-auto w-full h-full p-4 py-4 space-y-6 flex flex-col">
      <div className="border-b border-custom-border-100 mx-4 py-4 space-y-1 shrink-0">
        <div className="text-xl font-medium text-custom-text-100">Manage authentication modes for your instance</div>
        <div className="text-sm font-normal text-custom-text-300">
          Configure authentication modes for your team and restrict sign-ups to be invite only.
        </div>
      </div>
      <div className="grow overflow-hidden overflow-y-scroll vertical-scrollbar scrollbar-md px-4">
        {formattedConfig ? (
          <div className="space-y-3">
            <div className={cn("w-full flex items-center gap-14 rounded")}>
              <div className="flex grow items-center gap-4">
                <div className="grow">
                  <div className="text-lg font-medium pb-1">Allow anyone to sign up even without an invite</div>
                  <div className="font-normal leading-5 text-custom-text-300 text-xs">
                    Toggling this off will only let users sign up when they are invited.
                  </div>
                </div>
              </div>
              <div className={`shrink-0 pr-4 ${isSubmitting && "opacity-70"}`}>
                <Switch
                  aria-label="Disable sign-up"
                  size="sm"
                  isSelected={Boolean(parseInt(enableSignUpConfig))}
                  onChange={(isSelected: boolean) => updateConfig("ENABLE_SIGNUP", isSelected ? "1" : "0")}
                  isDisabled={isSubmitting}
                >
                  <Switch.Control>
                    <Switch.Thumb>
                      <Switch.Icon />
                    </Switch.Thumb>
                  </Switch.Control>
                </Switch>
              </div>
            </div>
            <div className="text-lg font-medium pt-6">Available authentication modes</div>
            <AuthenticationModes disabled={isSubmitting} updateConfig={updateConfig} />
          </div>
        ) : (
          <Loader className="space-y-10">
            <Loader.Item height="50px" width="75%" />
            <Loader.Item height="50px" width="75%" />
            <Loader.Item height="50px" width="40%" />
            <Loader.Item height="50px" width="40%" />
            <Loader.Item height="50px" width="20%" />
          </Loader>
        )}
      </div>
    </div>
  );
}
