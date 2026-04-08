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

interface IGithubConfigurationProps {
  disabled: boolean;
  updateConfig: (key: TInstanceAuthenticationMethodKeys, value: string) => void;
}

export const GithubConfiguration: FC<IGithubConfigurationProps> = (props) => {
  const { disabled, updateConfig } = props;
  // store hooks
  const { formattedConfig } = useInstance();
  // derived values
  const enableGithubConfig = formattedConfig?.IS_GITHUB_ENABLED ?? "";
  const isGithubConfigured = !!formattedConfig?.GITHUB_CLIENT_ID && !!formattedConfig?.GITHUB_CLIENT_SECRET;

  return (
    <>
      {isGithubConfigured ? (
        <div className="flex items-center gap-4">
          <Link href={"/authentication/github"}>Edit</Link>
          <Switch
            aria-label="Enable Github login"
            size="sm"
            name="isGithubEnabled"
            isSelected={Boolean(parseInt(enableGithubConfig))}
            onChange={(isSelected: boolean) => updateConfig("IS_GITHUB_ENABLED", isSelected ? "1" : "0")}
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
        <Link href={"/authentication/github"}>
          <Button size="sm" variant="tertiary">
            <Settings2 className="h-4 w-4 p-0.5 text-custom-text-300/80" />
            Configure
          </Button>
        </Link>
      )}
    </>
  );
};
