"use client";

import type { FC } from "react";
// heroui
import { Switch } from "@heroui/react";
// store hooks
import { useInstance } from "@/hooks/store/use-instance";
import type { TInstanceAuthenticationMethodKeys } from "@syncturtle/types";

interface IPasswordLoginConfiguration {
  disabled: boolean;
  updateConfig: (key: TInstanceAuthenticationMethodKeys, value: string) => void;
}

export const PasswordLoginConfiguration: FC<IPasswordLoginConfiguration> = (props) => {
  const { disabled, updateConfig } = props;
  // store hooks
  const { formattedConfig } = useInstance();
  // derived values
  const enableEmailPassword = formattedConfig?.ENABLE_EMAIL_PASSWORD ?? "";

  return (
    <Switch
      aria-label="Enable email password"
      size="sm"
      name="enableEmailPassword"
      isSelected={Boolean(parseInt(enableEmailPassword))}
      onChange={(isSelected: boolean) => updateConfig("ENABLE_EMAIL_PASSWORD", isSelected ? "1" : "0")}
      isDisabled={disabled}
    >
      <Switch.Control>
        <Switch.Thumb>
          <Switch.Icon />
        </Switch.Thumb>
      </Switch.Control>
    </Switch>
  );
};
