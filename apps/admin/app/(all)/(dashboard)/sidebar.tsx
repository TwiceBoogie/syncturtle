"use client";

import { useAppTheme } from "@/hooks/store/use-app-theme";
import { useOutsideClickDetector } from "@syncturtle/hooks";
import { useEffect, useRef } from "react";
import { AdminSidebarDropdown } from "./sidebar-dropdown";
import { AdminSidebarMenu } from "./sidebar-menu";
import { AdminSidebarHelpSection } from "./sidebar-help-section";

export const AdminSidebar = () => {
  // store hooks
  const { isSidebarCollapsed, toggleSidebar } = useAppTheme();

  const ref = useRef<HTMLDivElement>(null);

  useOutsideClickDetector(ref, () => {
    if (isSidebarCollapsed === false) {
      if (window.innerWidth < 768) {
        toggleSidebar(!isSidebarCollapsed);
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
      className={`inset-y-0 z-20 flex h-full shrink-0 grow-0 flex-col border-r border-custom-sidebar-border-200 bg-custom-sidebar-background-100 duration-300 
        fixed md:relative 
        ${isSidebarCollapsed ? "-ml-72.5" : ""} 
        sm:${isSidebarCollapsed ? "-ml-72.5" : ""}
        md:ml-0 ${isSidebarCollapsed ? "w-17.5" : "w-72.5"}
        lg:ml-0 ${isSidebarCollapsed ? "w-17.5" : "w-72.5"}
        `}
    >
      <div ref={ref} className="flex h-full min-h-0 w-full flex-1 flex-col">
        <AdminSidebarDropdown />
        <AdminSidebarMenu />
        <AdminSidebarHelpSection />
      </div>
    </div>
  );
};
