import type { FC } from "react";
import { useInstance } from "@/hooks/store/use-instance";
import { GoogleOAuthButton } from "./google-button";
import { GithubOAuthButton } from "./github-button";
import { GitlabOAuthButon } from "./gitlab-button";

interface IOAuthOptions {
  isSignUp: boolean | undefined;
}

export const OAuthOptions: FC<IOAuthOptions> = (props) => {
  const { isSignUp } = props;
  console.log(isSignUp);
  // hooks
  const { config } = useInstance();

  const isOAuthEnabled =
    (config && (config.isGoogleEnabled || config.isGithubEnabled || config.isGitlabEnabled)) || false;

  if (!isOAuthEnabled) return null;

  return (
    <>
      <div className="mt-4 flex items-center">
        <hr className="w-full border-onboarding-border-100" />
        <p className="mx-3 shrink-0 text-center text-sm text-onboarding-text-400">or</p>
        <hr className="w-full border-onboarding-border-100" />
      </div>
      <div className="mt-7 grid gap-4 overflow-hidden">
        {config?.isGoogleEnabled && <GoogleOAuthButton text="Continue with Google" />}
        {config?.isGithubEnabled && <GithubOAuthButton text="Continue with GitHub" />}
        {config?.isGitlabEnabled && <GitlabOAuthButon text="Continue with GitLab" />}
      </div>
    </>
  );
};
