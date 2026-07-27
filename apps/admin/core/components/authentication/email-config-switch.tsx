import type { FC } from "react";
// heroui
import { Switch } from "@heroui/react";
// store hooks
import { useInstance } from "@/hooks/store/use-instance";
import type { TInstanceAuthenticationMethodKeys } from "@syncturtle/types";

interface IEmailCodesConfigurationProps {
  disabled: boolean;
  updateConfig: (key: TInstanceAuthenticationMethodKeys, value: string) => void;
}

export const EmailCodesConfiguration: FC<IEmailCodesConfigurationProps> = (props) => {
  const { disabled, updateConfig } = props;
  // store hooks
  const { formattedConfig } = useInstance();
  // derived values
  const enableMagicLogin = formattedConfig?.ENABLE_MAGIC_LINK_LOGIN ?? "";

  return (
    <Switch
      aria-label="Enable magic link login"
      size="sm"
      name="enableMagicLinkLogin"
      isSelected={Boolean(parseInt(enableMagicLogin))}
      onChange={(isSelected: boolean) => updateConfig("ENABLE_MAGIC_LINK_LOGIN", isSelected ? "1" : "0")}
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
