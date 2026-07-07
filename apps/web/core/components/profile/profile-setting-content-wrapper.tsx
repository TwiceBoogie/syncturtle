"use client";

import type { FC, ReactNode } from "react";
import { ScrollShadow } from "@heroui/react";

import { cn } from "@syncturtle/ui";
import { SidebarHamburgerToggle } from "@/components/core/sidebar/sidebar-menu-hamburger-toggle";

type Props = {
  children: ReactNode;
  className?: string;
};

export const ProfileSettingContentWrapper: FC<Props> = ({ children, className }) => (
  <div className="flex h-full min-h-0 flex-col">
    <div className="block shrink-0 border-b border-custom-border-200 p-4 md:hidden">
      <SidebarHamburgerToggle />
    </div>

    <ScrollShadow
      orientation="vertical"
      size={48}
      offset={12}
      className={cn("min-h-0 flex-1 px-8 py-10 md:px-20 md:py-16 lg:px-36 xl:px-56", className)}
    >
      <div className="mx-auto flex w-full max-w-5xl flex-col">{children}</div>
    </ScrollShadow>
  </div>
);
