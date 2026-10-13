"use client";

import Link from "next/link";
// import { useTheme } from "next-themes";
// heroui
import { Button, Card, CloseButton } from "@heroui/react";
// hooks
import { useAppTheme } from "@/hooks/store/use-app-theme";
// utils
import { WEB_BASE_URL } from "@syncturtle/constants";

export const NewUserPopup = () => {
  // store hooks
  const { isNewUserPopup, toggleNewUserPopup } = useAppTheme();
  // theme
  // const { resolvedTheme } = useTheme();

  if (!isNewUserPopup) return <></>;
  return (
    <Card className="absolute bottom-8 right-8 w-96 z-20">
      <Card.Header>
        <Card.Header>Instance setup complete</Card.Header>
        <Card.Description>
          Manage this installation here. Open SyncTurtle to set up your profile and choose a space for personal use.
        </Card.Description>
        <CloseButton aria-label="Close banner" className={`absolute top-3 right-3`} onPress={toggleNewUserPopup} />
      </Card.Header>
      <Card.Footer>
        <Link href={`${WEB_BASE_URL}/onboarding`}>
          <Button size="sm">Open SyncTurtle</Button>
        </Link>
      </Card.Footer>
    </Card>
  );
};
