import type { FC } from "react";
import { useSearchParams } from "next/navigation";
import Image from "next/image";
import { useTheme } from "next-themes";
// helpers
import { API_BASE_URL } from "@syncturtle/constants";
// assets
import githubLightModeImage from "@/public/logos/github-black.png";
import githubDarkModeImage from "@/public/logos/github-dark.svg";
import { Button } from "@heroui/react";

interface IGithubOAuthButtonProps {
  text: string;
}

export const GithubOAuthButton: FC<IGithubOAuthButtonProps> = ({ text }) => {
  const searchParams = useSearchParams();
  const nextPath = searchParams.get("nextPath");
  // hooks
  const { resolvedTheme } = useTheme();

  const handleSignIn = () => {
    window.location.assign(`${API_BASE_URL}/auth/github/${nextPath ? `?nextPath=${nextPath}` : ``}`);
  };

  return (
    <Button type="button" fullWidth variant="tertiary" onPress={handleSignIn}>
      <Image
        src={resolvedTheme === "dark" ? githubDarkModeImage : githubLightModeImage}
        height={20}
        width={20}
        alt="GitHub Logo"
      />
      {text}
    </Button>
  );
};
