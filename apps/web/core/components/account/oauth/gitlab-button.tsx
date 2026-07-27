import type { FC } from "react";
import { useSearchParams } from "next/navigation";
import Image from "next/image";
// helpers
import { API_BASE_URL } from "@syncturtle/constants";
// assets
import GitlabLogo from "@/public/logos/gitlab-logo.svg";
import { Button } from "@heroui/react";

interface IGitlabOAuthButtonProps {
  text: string;
}

export const GitlabOAuthButon: FC<IGitlabOAuthButtonProps> = ({ text }) => {
  const searchParams = useSearchParams();
  const nextPath = searchParams.get("nextPath");

  const handleSignIn = () => {
    window.location.assign(`${API_BASE_URL}/auth/gitlab/${nextPath ? `?nextPath=${nextPath}` : ``}`);
  };

  return (
    <Button type="button" fullWidth variant="tertiary" onPress={handleSignIn}>
      <Image src={GitlabLogo} height={20} width={20} alt="GitLab Logo" />
      {text}
    </Button>
  );
};
