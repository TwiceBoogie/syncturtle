import type { FC } from "react";
import Image from "next/image";
import { useSearchParams } from "next/navigation";
// heroui
import { Button } from "@heroui/react";
// assets
import GoogleLogo from "@/public/logos/google-logo.svg";
import { API_BASE_URL } from "@syncturtle/constants";

interface IGoogleOAuthButtonProps {
  text: string;
}

export const GoogleOAuthButton: FC<IGoogleOAuthButtonProps> = ({ text }) => {
  const searchParams = useSearchParams();
  const nextPath = searchParams.get("nextPath");

  const handleSignIn = () => {
    window.location.assign(`${API_BASE_URL}/auth/google/${nextPath ? `?nextPath=${nextPath}` : ``}`);
  };

  return (
    <Button type="button" fullWidth variant="tertiary" onPress={handleSignIn}>
      <Image src={GoogleLogo} height={18} width={18} alt="Google Logo" />
      {text}
    </Button>
  );
};
