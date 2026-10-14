import { usePathname } from "next/navigation";
import { Inbox } from "lucide-react";
// heroui
import { Button } from "@heroui/react";
// components
import { TopNavPowerK } from "@/components/navigation";
import { HelpMenuRoot } from "@/components/workspace/sidebar/help-section";
import { UserMenuRoot } from "@/components/workspace/sidebar/user-menu-root";
import { WorkspaceMenuRoot } from "@/components/workspace/sidebar/workspace-menu-root";
import { AppRailRestoreMenu } from "ee/components/app-rail";

export const TopNavigationRoot = () => {
  const pathname = usePathname();
  const totalNotifications = 5;

  return (
    <div className="z-27 flex min-h-[40px] w-full shrink-0 items-center bg-canvas px-[14px] transition-all duration-300">
      {/* Workspace Menu */}
      <div className="flex flex-1 shrink-0 items-center gap-[6px]">
        <AppRailRestoreMenu />
        <WorkspaceMenuRoot variant="top-navigation" />
      </div>
      {/* Power K Search */}
      <div className="shrink-0">
        <TopNavPowerK />
      </div>
      {/* Additional Actions */}
      <div className="flex flex-1 shrink-0 items-center justify-end gap-[4px]">
        <Button
          variant="secondary"
          size="md"
          className="h-[28px] min-w-[48px] rounded-sm border border-custom-border-400 bg-custom-background-90 px-[10px] text-[13px] font-medium leading-[20px] text-custom-text-100 shadow-sm hover:bg-custom-background-80 data-hovered:bg-custom-background-80"
        >
          Get started
        </Button>
        <Button
          isIconOnly
          variant="ghost"
          aria-label="Inbox"
          className={`relative size-[28px] min-w-[28px] rounded-md p-0 ${
            pathname?.includes("/notifications/")
              ? "bg-custom-background-80 text-custom-text-100"
              : "text-custom-text-200 hover:bg-custom-background-80"
          }`}
        >
          <span className="relative flex">
            <Inbox aria-hidden="true" className="size-5" />
            {totalNotifications > 0 && (
              <span className="absolute right-[4px] top-[3px] size-2 rounded-full bg-custom-error-200" />
            )}
          </span>
        </Button>
        <HelpMenuRoot />
        <UserMenuRoot />
      </div>
    </div>
  );
};
