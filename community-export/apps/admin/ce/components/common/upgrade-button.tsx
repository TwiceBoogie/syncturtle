"use client";

// heroui
import { Button } from "@heroui/react";
// icons
import { SquareArrowOutUpRight } from "lucide-react";

export const UpgradeButton = () => (
  <a href="https://syncturtle.com/pricing?mode=self-hosted" target="_blank">
    <Button size="sm">
      Upgrade
      <SquareArrowOutUpRight className="h-3.5 w-3.5 p-0.5" />
    </Button>
  </a>
);
