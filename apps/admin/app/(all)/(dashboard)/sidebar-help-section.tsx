"use client";

import { useAppTheme } from "@/hooks/store/use-app-theme";
import { Button, Tooltip } from "@heroui/react";
import { WEB_BASE_URL } from "@syncturtle/constants";
import { cn } from "@syncturtle/utils";
import { ExternalLink, MoveLeft } from "lucide-react";
import Link from "next/link";

export const AdminSidebarHelpSection = () => {
  // store hooks
  const { isSidebarCollapsed, toggleSidebar } = useAppTheme();

  const redirectionLink = encodeURI(WEB_BASE_URL + "/");
  return (
    <div
      className={cn(
        "flex w-full items-center justify-between self-baseline border-t border-custom-border-200 bg-custom-sidebar-background-100 px-4 h-14 shrink-0",
        {
          "flex-col h-auto py-1.5": isSidebarCollapsed,
        }
      )}
    >
      <div
        className={`flex items-center gap-1 ${isSidebarCollapsed ? "flex-col justify-center" : "w-full justify-between"}`}
      >
        <Tooltip delay={0}>
          <Link href={redirectionLink}>
            <Button size="sm" isIconOnly={isSidebarCollapsed}>
              <ExternalLink size={14} />
              {!isSidebarCollapsed && "Redirect to Syncturtle"}
            </Button>
          </Link>
          <Tooltip.Content placement={`${isSidebarCollapsed ? "right" : "top"}`}>
            Redirect to Syncturtle
          </Tooltip.Content>
        </Tooltip>
        <div>
          <Tooltip delay={0}>
            <Button size="sm" isIconOnly={isSidebarCollapsed} onPress={() => toggleSidebar(!isSidebarCollapsed)}>
              <MoveLeft className={`h-3.5 w-3.5 duration-300 ${isSidebarCollapsed ? "rotate-180" : ""}`} />
              <Tooltip.Content placement={`${isSidebarCollapsed ? "right" : "top"}`}>Toggle sidebar</Tooltip.Content>
            </Button>
          </Tooltip>
        </div>
      </div>
    </div>
  );
};
