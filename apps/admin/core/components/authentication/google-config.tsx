"use client";

import { FC } from "react";
import Link from "next/link";
// heroui
import { Button, Switch } from "@heroui/react";
// icons
import { Settings2 } from "lucide-react";
// hooks
import { useInstance } from "@/hooks/store/use-instance";
// types
import type { TInstanceAuthenticationMethodKeys } from "@syncturtle/types";

interface IGoogleConfiguration {
  disabled: boolean;
  updateConfig: (key: TInstanceAuthenticationMethodKeys, value: string) => void;
}

export const GoogleConfiguration: FC<IGoogleConfiguration> = (props) => {
  const { disabled, updateConfig } = props;
  // store hooks
  const { formattedConfig } = useInstance();
  // derived values
  const enableGoogleConfig = formattedConfig?.IS_GOOGLE_ENABLED ?? "";
  const isGoogleConfigured = !!formattedConfig?.GOOGLE_CLIENT_ID && !!formattedConfig?.GOOGLE_CLIENT_SECRET;

  return (
    <>
      {isGoogleConfigured ? (
        <div className="flex items-center gap-4">
          <Link href={"/authentication/google"}>Edit</Link>
          <Switch
            aria-label="Enable Google login"
            size="sm"
            name="isGoogleEnabled"
            isSelected={Boolean(parseInt(enableGoogleConfig))}
            onChange={(isSelected: boolean) => updateConfig("IS_GOOGLE_ENABLED", isSelected ? "1" : "0")}
            isDisabled={disabled}
          >
            <Switch.Control>
              <Switch.Thumb>
                <Switch.Icon />
              </Switch.Thumb>
            </Switch.Control>
          </Switch>
        </div>
      ) : (
        <Link href={"/authentication/google"}>
          <Button size="sm" variant="tertiary">
            <Settings2 className="h-4 w-4 p-0.5 text-custom-text-300/80" />
            Configure
          </Button>
        </Link>
      )}
    </>
  );
};
