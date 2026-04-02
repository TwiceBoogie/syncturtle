"use client";

import { useInstance } from "@/hooks/store/use-instance";
import { Spinner, Switch, toast } from "@heroui/react";
import { useState } from "react";
import useSWR from "swr";
import { InstanceEmailForm } from "./email-config-form";

export default function EmailManagementPage() {
  // store hooks
  const { fetchInstanceConfigurations, formattedConfig, disableEmail } = useInstance();

  const { isLoading } = useSWR("INSTANCE_CONFIGURATIONS", () => fetchInstanceConfigurations());

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showSetupForm, setShowSetupForm] = useState(false);

  // derived values
  const persistedSMTPEnabled = formattedConfig?.ENABLE_SMTP === "1";
  const isSMTPEnabled = persistedSMTPEnabled || showSetupForm;

  const handleToggle = async (nextSelected: boolean) => {
    if (isSubmitting) return;

    if (persistedSMTPEnabled && !nextSelected) {
      setIsSubmitting(true);

      try {
        await disableEmail();
        setShowSetupForm(false);

        toast("Email feature disabled", {
          description: "Email feature has been disabled",
          variant: "success",
        });
      } catch (error) {
        console.log(error);

        toast("Error disabling email", {
          description: "Failed to disable email feature. Please try again.",
          variant: "danger",
        });
      } finally {
        setIsSubmitting(false);
      }

      return;
    }
    // User is turning ON the UI to configure SMTP,
    // but nothing is persisted yet.
    if (!persistedSMTPEnabled && nextSelected) {
      setShowSetupForm(true);
      return;
    }

    // User is turning OFF the unsaved setup form
    if (!persistedSMTPEnabled && !nextSelected) {
      setShowSetupForm(false);
    }
  };
  return (
    <div className="relative container mx-auto w-full h-full p-4 py-4 space-y-6 flex flex-col">
      <div className="flex items-center justify-between gap-4 border-b border-custom-border-100 mx-4 py-4 space-y-1 shrink-0">
        <div className="py-4 space-y-1 shrink-0">
          <div className="text-xl font-medium text-custom-text-100">Secure emails from your own instance</div>
          <div className="text-sm font-medium text-custom-text-300">
            Syncturtle can send useful emails to you and your users from your own instance without talking to the
            Internet.
          </div>
          <div className="text-sm font-normal text-custom-text-300">
            Set it up below and please test your settings before you save them.&nbsp;
            <span className="text-red-400">Misconfigs can lead to email bounces and errors.</span>
          </div>
        </div>
        {isLoading ? (
          <Spinner />
        ) : (
          <Switch
            aria-label="Disable email feature"
            size="sm"
            name="disableEmailFeature"
            isSelected={isSMTPEnabled}
            onChange={(isSelected: boolean) => {
              handleToggle(isSelected);
            }}
            isDisabled={isSubmitting}
          >
            <Switch.Control>
              <Switch.Thumb>
                <Switch.Icon />
              </Switch.Thumb>
            </Switch.Control>
          </Switch>
        )}
      </div>
      {isSMTPEnabled && !isLoading && (
        <div className="">{formattedConfig ? <InstanceEmailForm config={formattedConfig} /> : <Spinner />}</div>
      )}
    </div>
  );
}
