"use client";

import { useMemo, useRef, useState } from "react";
import { LogOut, Palette, Shield } from "lucide-react";
// next-theme
import { useTheme } from "next-themes";
// heroui
import { Avatar, Dropdown, Label } from "@heroui/react";
// hooks
import { useAppTheme } from "@/hooks/store/use-app-theme";
import { useUser } from "@/hooks/store/use-user";
// syncturtle imports
import { cn } from "@syncturtle/ui";
import { ADMIN_BASE_PATH } from "@syncturtle/constants";

const sessionsHref = `${(ADMIN_BASE_PATH || "/god-mode").replace(/\/+$/, "")}/sessions/`;

export const AdminSidebarDropdown = () => {
  // store hooks
  const { isSidebarCollapsed } = useAppTheme();
  const { currentUser, signOut } = useUser();
  // hooks
  const { resolvedTheme, setTheme } = useTheme();
  // state
  const [isSigningOut, setIsSigningOut] = useState(false);
  const [signOutError, setSignOutError] = useState<string | null>(null);
  const signOutPending = useRef(false);
  const [leftOpen, setLeftOpen] = useState(false);

  const handleSwitchTheme = () => {
    const newTheme = resolvedTheme === "dark" ? "light" : "dark";
    setTheme(newTheme);
  };

  const handleSignOut = async () => {
    if (signOutPending.current) return;
    signOutPending.current = true;
    setIsSigningOut(true);
    setSignOutError(null);
    try {
      await signOut();
    } catch {
      setSignOutError("Unable to confirm sign out. Please try again.");
    } finally {
      signOutPending.current = false;
      setIsSigningOut(false);
    }
  };

  // derived state
  const initials = useMemo(() => {
    const f = currentUser?.firstName?.[0] ?? "";
    const l = currentUser?.lastName?.[0] ?? "";
    return `${f}${l}`.trim() || "IA";
  }, [currentUser?.firstName, currentUser?.lastName]);

  const renderDropdownMenu = () => (
    <Dropdown.Popover>
      <div className="px-3 pt-3 pb-1">
        <div className="flex flex-col gap-0">
          <p className="text-sm leading-5 font-medium">
            {currentUser?.firstName} {currentUser?.lastName}
          </p>
          <p className="text-xs leading-none text-muted">{currentUser?.email}</p>
        </div>
      </div>
      <Dropdown.Menu>
        <Dropdown.Item id="sessions" textValue="Sessions" href={sessionsHref}>
          <div className="flex gap-2">
            <Shield className="h-4 w-4" /> <Label>Sessions</Label>
          </div>
        </Dropdown.Item>
        <Dropdown.Item id={`theme-resolver`} textValue="theme-resolver" onPress={handleSwitchTheme}>
          <div className="flex gap-2">
            <Palette className="h-4 w-4" /> Switch to {resolvedTheme === "dark" ? "light" : "dark"} mode
          </div>
        </Dropdown.Item>
        <Dropdown.Item
          id={`sign-out`}
          textValue="Sign-out"
          variant="danger"
          onPress={handleSignOut}
          isDisabled={isSigningOut}
        >
          <div className="flex w-full items-center justify-between gap-2">
            <Label>{isSigningOut ? "Signing out..." : "Log Out"}</Label>
            <LogOut className="h-4 w-4" />
          </div>
        </Dropdown.Item>
      </Dropdown.Menu>
    </Dropdown.Popover>
  );

  return (
    <div className="flex items-center gap-x-5 gap-y-2 border-b border-custom-sidebar-border-200 px-4 py-3.5">
      {signOutError && <p role="alert">{signOutError}</p>}
      <div className="h-full w-full truncate">
        <div
          className={`flex grow items-center gap-x-2 truncate rounded py-1 ${isSidebarCollapsed ? "justify-center" : ""}`}
        >
          <Dropdown
            isOpen={isSidebarCollapsed && leftOpen}
            onOpenChange={(isOpen: boolean) => setLeftOpen(isSidebarCollapsed ? isOpen : false)}
          >
            <Dropdown.Trigger className={cn({ "cursor-default": !isSidebarCollapsed })}>
              <Avatar size="sm" className="rounded-lg">
                <Avatar.Image
                  alt="Small avatar"
                  src="https://heroui-assets.nyc3.cdn.digitaloceanspaces.com/avatars/blue.jpg"
                />
                <Avatar.Fallback className="rounded-lg">{initials}</Avatar.Fallback>
              </Avatar>
            </Dropdown.Trigger>
            {isSidebarCollapsed && <>{renderDropdownMenu()}</>}
          </Dropdown>
          {!isSidebarCollapsed && (
            <div className="flex w-full gap-2">
              <h4 className="grow truncate text-base font-medium">Instance admin</h4>
            </div>
          )}
        </div>
      </div>
      {!isSidebarCollapsed && (
        <Dropdown>
          <Dropdown.Trigger>
            <Avatar size="sm" className="rounded-lg">
              <Avatar.Fallback>{initials}</Avatar.Fallback>
            </Avatar>
          </Dropdown.Trigger>
          {renderDropdownMenu()}
        </Dropdown>
      )}
    </div>
  );
};
