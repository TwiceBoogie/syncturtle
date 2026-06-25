import type { FC } from "react";
import { useInstance } from "@/hooks/store/use-instance";

interface IOAuthOptions {
  isSignUp: boolean | undefined;
}

export const OAuthOptions: FC<IOAuthOptions> = (props) => {
  const { isSignUp } = props;
  // hooks
  const { config } = useInstance();

  const isOAuthEnabled =
    (config && (config.isGoogleEnabled || config.isGithubEnabled || config.isGitlabEnabled)) || false;

  if (!isOAuthEnabled) return <></>;

  return <div>{isSignUp}</div>;
};
