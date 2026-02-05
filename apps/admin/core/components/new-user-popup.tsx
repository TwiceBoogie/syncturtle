"use client";

import { useAppTheme } from "@/hooks/store/use-app-theme";
import { Button, Card, CloseButton } from "@heroui/react";
// import { useTheme } from "next-themes";
import Link from "next/link";

export const NewUserPopup = () => {
  // store hooks
  const { isNewUserPopup, toggleNewUserPopup } = useAppTheme();
  // theme
  // const { resolvedTheme } = useTheme();

  if (!isNewUserPopup) return <></>;
  return (
    <Card className="absolute bottom-8 right-8 w-96 z-20">
      <Card.Header>
        <Card.Header>Create workspace</Card.Header>
        <Card.Description>
          Instance setup done! Welcome to Syncturtle instance portal. Start your journey with by creating your first
          workspace.
        </Card.Description>
        <CloseButton aria-label="Close banner" className={`absolute top-3 right-3`} onPress={toggleNewUserPopup} />
      </Card.Header>
      <Card.Footer>
        <Link href={`/workspace/create`}>
          <Button size="sm">Create workspace</Button>
        </Link>
      </Card.Footer>
    </Card>
  );
};
