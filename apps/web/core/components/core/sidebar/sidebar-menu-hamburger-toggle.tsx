"use client";

import { PanelRight } from "lucide-react";
// store hooks
import { useAppTheme } from "@/hooks/store/use-app-theme";
import { Button } from "@heroui/react";

export const SidebarHamburgerToggle = () => {
  const { toggleSidebar } = useAppTheme();

  return (
    <Button isIconOnly onPress={() => toggleSidebar()} size="sm" className="rounded-md" variant="secondary">
      <PanelRight className="size-3.5 text-custom-text-200 transition-all group-hover:text-custom-text-100" />
    </Button>
  );
};
