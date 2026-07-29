"use client";

import { useOutsideClickDetector } from "@syncturtle/hooks";
import { useAppTheme } from "@/hooks/store/use-app-theme";
import { cn } from "@syncturtle/ui";
import { useEffect, useRef, type FC } from "react";

export const ProjectAppSidebar: FC = () => {
  // store hooks
  const { sidebarCollapsed, toggleSidebar } = useAppTheme();

  const ref = useRef<HTMLDivElement>(null);

  useOutsideClickDetector(ref, () => {
    if (sidebarCollapsed === false) {
      if (window.innerWidth < 768) {
        toggleSidebar(!sidebarCollapsed);
      }
    }
  });

  useEffect(() => {
    const handleResize = () => {
      if (window.innerWidth <= 768) {
        toggleSidebar(true);
      }
    };
    handleResize();
    window.addEventListener("resize", handleResize);
    return () => {
      window.removeEventListener("resize", handleResize);
    };
  }, [toggleSidebar]);

  return (
    <div
      className={cn(
        "fixed inset-y-0 z-20 flex h-full shrink-0 grow-0 flex-col border-r border-custom-sidebar-border-200 bg-custom-sidebar-background-100 duration-300 w-62.5 md:relative md:ml-0",
        { "w-17.5 -ml-62.5": sidebarCollapsed }
      )}
    >
      <div
        ref={ref}
        className={cn("size-full flex flex-col flex-1 pt-4 pb-0", {
          "p-2 pt-4": sidebarCollapsed,
        })}
      >
        hello there
      </div>
    </div>
  );
};
