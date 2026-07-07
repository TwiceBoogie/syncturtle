"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Cog, Mail, Lock, BrainCog, Image, Network } from "lucide-react";
// heroui
import { cn, ScrollShadow, Tooltip } from "@heroui/react";
import { useAppTheme } from "@/hooks/store/use-app-theme";

const INSTANCE_ADMIN_LINKS = [
  {
    Icon: Cog,
    name: "General",
    description: "Identify your instances and get key details.",
    href: `/general/`,
  },
  {
    Icon: Network,
    name: "Workspaces",
    description: "Manage all workspaces on this instance.",
    href: `/workspace/`,
  },
  {
    Icon: Mail,
    name: "Email",
    description: "Configure your SMTP controls.",
    href: `/email/`,
  },
  {
    Icon: Lock,
    name: "Authentication",
    description: "Configure authentication modes.",
    href: `/authentication/`,
  },
  {
    Icon: BrainCog,
    name: "Artificial intelligence",
    description: "Configure your OpenAI creds.",
    href: `/ai/`,
  },
  {
    Icon: Image,
    name: "Images in Syncturtle",
    description: "Allow third-party image libraries.",
    href: `/image/`,
  },
];

export const AdminSidebarMenu = () => {
  // store hooks
  const { isSidebarCollapsed, toggleSidebar } = useAppTheme();
  // router
  const pathName = usePathname();

  const handleItemClick = () => {
    if (window.innerWidth < 768) {
      toggleSidebar(!isSidebarCollapsed);
    }
  };
  return (
    <ScrollShadow orientation="vertical" size={24} className="min-h-0 flex-1 px-4 py-4">
      <div className="flex flex-col w-full gap-2.5">
        {INSTANCE_ADMIN_LINKS.map((item, index) => {
          const isActive = item.href === pathName || pathName.includes(item.href);
          return (
            <Link key={index} href={item.href} onClick={handleItemClick}>
              <div>
                <Tooltip>
                  <div
                    className={cn(
                      `group flex w-full items-center gap-3 rounded-md px-3 py-2 outline-none transition-colors`,
                      isActive
                        ? "bg-custom-primary-100/10 text-custom-primary-100"
                        : "text-custom-sidebar-text-200 hover:bg-custom-sidebar-background-80 focus:bg-custom-sidebar-background-80",
                      isSidebarCollapsed ? "justify-center" : "w-65"
                    )}
                  >
                    {<item.Icon className="h-4 w-4 shrink-0" />}
                    {!isSidebarCollapsed && (
                      <div className="w-full ">
                        <div
                          className={cn(
                            `text-sm font-medium transition-colors`,
                            isActive ? "text-custom-primary-100" : "text-custom-sidebar-text-200"
                          )}
                        >
                          {item.name}
                        </div>
                        <div
                          className={cn(
                            `text-[10px] transition-colors`,
                            isActive ? "text-custom-primary-90" : "text-custom-sidebar-text-400"
                          )}
                        >
                          {item.description}
                        </div>
                      </div>
                    )}
                  </div>
                </Tooltip>
              </div>
            </Link>
          );
        })}
      </div>
    </ScrollShadow>
  );
};
