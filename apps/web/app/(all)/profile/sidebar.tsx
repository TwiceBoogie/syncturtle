"use client";

import { useEffect, useRef, useState } from "react";
import { usePathname } from "next/navigation";
// heroui
import { Button, ScrollShadow, toast, Tooltip } from "@heroui/react";
// icons
import {
  ChevronLeft,
  LogOut,
  MoveLeft,
  Activity,
  Bell,
  CircleUser,
  KeyRound,
  Settings2,
  CirclePlus,
  Mails,
} from "lucide-react";
// syncturtle imports
import { useOutsideClickDetector } from "@syncturtle/hooks";
import { useTranslation } from "@syncturtle/i18n";
import { cn } from "@syncturtle/ui";
import type { IWorkspace } from "@syncturtle/types";
import { getFileURL } from "@syncturtle/utils";
// store hooks
import { useAppTheme } from "@/hooks/store/use-app-theme";
// import { useWorkspace } from "@/hooks/store/use-workspace";
import { useUser, useUserSettings } from "@/hooks/store/user";
// components
import { SidebarButtonLink } from "@/components/sidebar/sidebar-button-link";

export const PROFILE_SETTINGS = {
  profile: {
    key: "profile",
    i18n_label: "profile.actions.profile",
    href: `/settings/account`,
    highlight: (pathname: string) => pathname === "/settings/account/",
  },
  security: {
    key: "security",
    i18n_label: "profile.actions.security",
    href: `/settings/account/security`,
    highlight: (pathname: string) => pathname === "/settings/account/security/",
  },
  activity: {
    key: "activity",
    i18n_label: "profile.actions.activity",
    href: `/settings/account/activity`,
    highlight: (pathname: string) => pathname === "/settings/account/activity/",
  },
  preferences: {
    key: "preferences",
    i18n_label: "profile.actions.preferences",
    href: `/settings/account/preferences`,
    highlight: (pathname: string) => pathname === "/settings/account/preferences",
  },
  notifications: {
    key: "notifications",
    i18n_label: "profile.actions.notifications",
    href: `/settings/account/notifications`,
    highlight: (pathname: string) => pathname === "/settings/account/notifications/",
  },
  "api-tokens": {
    key: "api-tokens",
    i18n_label: "profile.actions.api-tokens",
    href: `/settings/account/api-tokens`,
    highlight: (pathname: string) => pathname === "/settings/account/api-tokens/",
  },
};
export const PROFILE_ACTION_LINKS: {
  key: string;
  i18n_label: string;
  href: string;
  highlight: (pathname: string) => boolean;
}[] = [
  PROFILE_SETTINGS["profile"],
  PROFILE_SETTINGS["security"],
  PROFILE_SETTINGS["activity"],
  PROFILE_SETTINGS["preferences"],
  PROFILE_SETTINGS["notifications"],
  PROFILE_SETTINGS["api-tokens"],
];

const WORKSPACE_ACTION_LINKS = [
  {
    key: "create_workspace",
    Icon: CirclePlus,
    i18n_label: "create_workspace",
    href: "/create-workspace",
  },
  {
    key: "invitations",
    Icon: Mails,
    i18n_label: "workspace_invites",
    href: "/invitations",
  },
];

export const mockWorkspacesList = [
  {
    id: "workspace_001",
    owner: {
      id: "user_001",
      displayName: "Sebastian Rivera",
      email: "sebastian@example.com",
      firstName: "Sebastian",
      lastName: "Rivera",
      avatarUrl: "https://i.pravatar.cc/150?img=12",
      avatarAssetId: "asset_avatar_001",
      isBot: false,
      joining_date: "2026-01-15",
    },
    createdAt: new Date("2026-01-15T14:30:00.000Z"),
    updatedAt: new Date("2026-06-20T18:45:00.000Z"),
    name: "Sync Turtle",
    url: "https://syncturtle.com",
    logoUrl: "https://placehold.co/128x128/png?text=ST",
    logoAssetId: null,
    totalMembers: 8,
    slug: "sync-turtle",
    createdById: "user_001",
    updatedById: "user_001",
    organizationSize: "1-10",
    role: 20,
  },
  {
    id: "workspace_002",
    owner: {
      id: "user_002",
      displayName: "Maya Chen",
      email: "maya@example.com",
      firstName: "Maya",
      lastName: "Chen",
      avatarUrl: "https://i.pravatar.cc/150?img=32",
      avatarAssetId: "asset_avatar_002",
      isBot: false,
      joining_date: "2026-02-03",
    },
    createdAt: new Date("2026-02-03T09:10:00.000Z"),
    updatedAt: new Date("2026-06-12T11:20:00.000Z"),
    name: "Product Lab",
    url: "https://productlab.dev",
    logoUrl: null,
    logoAssetId: null,
    totalMembers: 23,
    slug: "product-lab",
    createdById: "user_002",
    updatedById: "user_001",
    organizationSize: "11-50",
    role: 15,
  },
  {
    id: "workspace_003",
    owner: {
      id: "user_003",
      displayName: "Alex Morgan",
      email: "alex@example.com",
      firstName: "Alex",
      lastName: "Morgan",
      avatarUrl: "https://i.pravatar.cc/150?img=45",
      avatarAssetId: "asset_avatar_003",
      isBot: false,
      joining_date: "2026-03-22",
    },
    createdAt: new Date("2026-03-22T16:00:00.000Z"),
    updatedAt: new Date("2026-06-01T21:15:00.000Z"),
    name: "Design Ops",
    url: "https://designops.example.com",
    logoUrl: "https://placehold.co/128x128/png?text=DO",
    logoAssetId: null,
    totalMembers: 5,
    slug: "design-ops",
    createdById: "user_003",
    updatedById: "user_003",
    organizationSize: "1-10",
    role: 5,
  },
  {
    id: "workspace_004",
    owner: {
      id: "user_004",
      displayName: "Jordan Lee",
      email: "jordan@example.com",
      firstName: "Jordan",
      lastName: "Lee",
      avatarUrl: "https://i.pravatar.cc/150?img=18",
      avatarAssetId: "asset_avatar_004",
      isBot: false,
      joining_date: "2026-04-08",
    },
    createdAt: new Date("2026-04-08T13:25:00.000Z"),
    updatedAt: new Date("2026-06-28T10:05:00.000Z"),
    name: "Engineering Platform",
    url: "https://platform.example.com",
    logoUrl: null,
    logoAssetId: null,
    totalMembers: 41,
    slug: "engineering-platform",
    createdById: "user_004",
    updatedById: "user_001",
    organizationSize: "51-200",
    role: 20,
  },
] satisfies IWorkspace[];

const ProjectActionIcons = ({ type, size, className = "" }: { type: string; size?: number; className?: string }) => {
  const icons = {
    profile: CircleUser,
    security: KeyRound,
    activity: Activity,
    preferences: Settings2,
    notifications: Bell,
    "api-tokens": KeyRound,
  };

  if (type === undefined) return null;
  const Icon = icons[type as keyof typeof icons];
  if (!Icon) return null;
  return <Icon size={size} className={className} />;
};

export const ProfileLayoutSidebar = () => {
  // states
  const [isSigningOut, setIsSigningOut] = useState(false);
  // router
  const pathname = usePathname();
  // store hooks
  const { sidebarCollapsed, toggleSidebar } = useAppTheme();
  const { data: currentUser, signOut } = useUser();
  const { data: currentUserSettings } = useUserSettings();
  // const { workspaces } = useWorkspace();
  //   const { isMobile } = usePlatformOS();
  const { t } = useTranslation();

  const workspacesList = mockWorkspacesList;
  const redirectWorkspaceSlug =
    currentUserSettings.workspace.lastWorkspaceId || currentUserSettings.workspace.fallbackWorkspaceId || "";

  const ref = useRef<HTMLDivElement>(null);

  useOutsideClickDetector(ref, () => {
    if (sidebarCollapsed === false) {
      if (window.innerWidth < 768) {
        toggleSidebar();
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

  const handleItemClick = () => {
    if (window.innerWidth < 768) {
      toggleSidebar();
    }
  };

  const handleSignOut = async () => {
    setIsSigningOut(true);
    await signOut()
      .catch(() => toast("nice"))
      .finally(() => setIsSigningOut(false));
  };

  return (
    <div
      className={`inset-y-0 z-20 flex h-full shrink-0 grow-0 flex-col border-r border-custom-sidebar-border-200 bg-custom-sidebar-background-100 duration-300 
        fixed md:relative 
        ${sidebarCollapsed ? "-ml-72.5" : ""} 
        sm:${sidebarCollapsed ? "-ml-72.5" : ""}
        md:ml-0 ${sidebarCollapsed ? "w-17.5" : "w-72.5"}
        lg:ml-0 ${sidebarCollapsed ? "w-17.5" : "w-72.5"}
        `}
    >
      <div ref={ref} className="flex h-full min-h-0 w-full flex-1 flex-col">
        <div className="flex items-center gap-x-5 gap-y-2 border-b border-custom-sidebar-border-200 px-4 py-3.5">
          <SidebarButtonLink
            href={`/${redirectWorkspaceSlug}`}
            label={t("profile_settings")}
            isCollapsed={sidebarCollapsed}
            onPress={handleItemClick}
          >
            <ChevronLeft aria-hidden className="size-5 shrink-0" />
            {!sidebarCollapsed && <span className="truncate">{t("profile_settings")}</span>}
          </SidebarButtonLink>
        </div>
        <div className="flex shrink-0 flex-col overflow-x-hidden">
          {!sidebarCollapsed && (
            <h6 className="rounded px-6 text-sm font-semibold text-custom-sidebar-text-400">{t("your_account")}</h6>
          )}
        </div>
        <ScrollShadow orientation="vertical" size={24} className="min-h-0 flex-1 px-4 py-4">
          <div className="flex flex-col w-full gap-2.5">
            {PROFILE_ACTION_LINKS.map((link) => {
              if (link.key === "change-password" && currentUser?.isPasswordAutoset) return null;
              const isActive = link.highlight(pathname);
              return (
                <Tooltip key={link.key} delay={0}>
                  <Tooltip.Trigger aria-label="links">
                    <SidebarButtonLink
                      href={link.href}
                      label={t(link.i18n_label)}
                      isActive={isActive}
                      isCollapsed={sidebarCollapsed}
                      onPress={handleItemClick}
                    >
                      <ProjectActionIcons type={link.key} size={16} className="shrink-0" />
                      {!sidebarCollapsed && <span className="truncate">{t(link.i18n_label)}</span>}
                    </SidebarButtonLink>
                  </Tooltip.Trigger>
                  <Tooltip.Content placement="right">{t(link.key)}</Tooltip.Content>
                </Tooltip>
              );
            })}
          </div>
        </ScrollShadow>
        <section className="flex min-h-0 flex-1 flex-col overflow-x-hidden">
          {!sidebarCollapsed && (
            <h6 className="rounded px-6 text-sm font-semibold text-custom-sidebar-text-400">{t("workspaces")}</h6>
          )}
          {workspacesList.length > 0 && (
            <ScrollShadow
              orientation="vertical"
              size={24}
              offset={8}
              hideScrollBar={sidebarCollapsed}
              className={cn("mt-2 min-h-0 flex-1 space-y-1.5 px-4", {
                "ml-2.5 px-1": sidebarCollapsed,
              })}
            >
              {workspacesList.map((workspace) => {
                const isActive = pathname === `/${workspace.slug}` || pathname.startsWith(`/${workspace.slug}/`);

                return (
                  <Tooltip key={workspace.id} delay={0}>
                    <Tooltip.Trigger className="w-full">
                      <SidebarButtonLink
                        href={`/${workspace.slug}`}
                        label={workspace.name}
                        isActive={isActive}
                        isCollapsed={sidebarCollapsed}
                        onPress={handleItemClick}
                      >
                        <span
                          className={cn(
                            "relative flex h-6 w-6 shrink-0 items-center justify-center overflow-hidden rounded text-xs uppercase",
                            {
                              "bg-custom-primary-500 text-white": !workspace.logoUrl,
                            }
                          )}
                        >
                          {workspace.logoUrl ? (
                            <img
                              src={getFileURL(workspace.logoUrl)}
                              className="absolute inset-0 h-full w-full object-cover"
                              alt={`${workspace.name} logo`}
                            />
                          ) : (
                            workspace.name.charAt(0)
                          )}
                        </span>

                        {!sidebarCollapsed && <span className="truncate">{workspace.name}</span>}
                      </SidebarButtonLink>
                    </Tooltip.Trigger>

                    {sidebarCollapsed && <Tooltip.Content placement="right">{workspace.name}</Tooltip.Content>}
                  </Tooltip>
                );
              })}
            </ScrollShadow>
          )}
          <div className="flex flex-col gap-2 mt-1.5 shrink-0 px-4">
            {WORKSPACE_ACTION_LINKS.map((link) => (
              <Tooltip key={link.key} delay={0}>
                <Tooltip.Trigger className="w-full">
                  <SidebarButtonLink
                    href={link.href}
                    label={t(link.i18n_label)}
                    isCollapsed={sidebarCollapsed}
                    onPress={handleItemClick}
                  >
                    <link.Icon aria-hidden className="size-4 shrink-0" />
                    {!sidebarCollapsed && <span className="truncate">{t(link.i18n_label)}</span>}
                  </SidebarButtonLink>
                </Tooltip.Trigger>
                <Tooltip.Content placement="right">{t(link.i18n_label)}</Tooltip.Content>
              </Tooltip>
            ))}
          </div>
        </section>
        <div className="flex shrink-0 grow items-end px-6 py-2">
          <div
            className={cn(
              "flex w-full items-center",
              sidebarCollapsed ? "flex-col justify-center gap-2" : "justify-between gap-2"
            )}
          >
            <Button type="button" size="sm" onPress={handleSignOut} isDisabled={isSigningOut} variant="danger">
              <LogOut className="size-3" />
              {!sidebarCollapsed && <span>{isSigningOut ? `${t("signing_out")}...` : t("sign_out")}</span>}
            </Button>
            <Button type="button" size="sm" onPress={() => toggleSidebar()} isIconOnly variant="ghost">
              <MoveLeft className={`size-3 duration-300 ${sidebarCollapsed ? "rotate-180" : ""}`} />
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
};
