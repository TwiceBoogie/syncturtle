"use client";

import { cn } from "@syncturtle/ui";
import type { FC, ReactNode } from "react";

interface ISidebarNavItemProps {
  className?: string;
  isActive?: boolean;
  children?: ReactNode;
}

export const SidebarNavItem: FC<ISidebarNavItemProps> = ({ className, isActive, children }) => (
  <div
    className={cn(
      "cursor-pointer relative group w-full flex items-center justify-between gap-1.5 rounded px-2 py-1 outline-none",
      {
        "text-custom-text-200 bg-custom-background-80/75": isActive,
        "text-custom-sidebar-text-200 hover:bg-custom-sidebar-background-90 active:bg-custom-sidebar-background-90":
          !isActive,
      },
      className
    )}
  >
    {children}
  </div>
);
