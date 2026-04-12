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

interface IGitlabConfiguration {
  disabled: boolean;
  updateConfig: (key: TInstanceAuthenticationMethodKeys, value: string) => void;
}

export const GitlabConfiguration: FC<IGitlabConfiguration> = (props) => {
  const { disabled, updateConfig } = props;
  // store hooks
  const { formattedConfig } = useInstance();
  // derived values
  const enableGitlabConfig = formattedConfig?.IS_GITLAB_ENABLED ?? "";
  const isGitlabConfigured = !!formattedConfig?.GITLAB_CLIENT_ID && !!formattedConfig?.GITLAB_CLIENT_SECRET;

  return (
    <>
      {isGitlabConfigured ? (
        <div className="flex items-center gap-4">
          <Link href={"/authentication/gitlab"}>Edit</Link>
          <Switch
            aria-label="Enable Gitlab login"
            size="sm"
            name="isGitlabEnabled"
            isSelected={Boolean(parseInt(enableGitlabConfig))}
            onChange={(isSelected: boolean) => updateConfig("IS_GITLAB_ENABLED", isSelected ? "1" : "0")}
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
        <Link href={"/authentication/gitlab"}>
          <Button size="sm" variant="tertiary">
            <Settings2 className="h-4 w-4 p-0.5 text-custom-text-300/80" />
            Configure
          </Button>
        </Link>
      )}
    </>
  );
};
