import { useAppTheme } from "@/hooks/store/use-app-theme";
import { Avatar, Dropdown, Label } from "@heroui/react";
import { LogOut, Palette } from "lucide-react";
import { useTheme } from "next-themes";

export const AdminSidebarDropdown = () => {
  // store hooks
  const { isSidebarCollapsed } = useAppTheme();
  // hooks
  const { resolvedTheme, setTheme } = useTheme();

  const handleSwitchTheme = () => {
    const newTheme = resolvedTheme === "dark" ? "light" : "dark";
    setTheme(newTheme);
  };

  return (
    <div className="flex items-center gap-x-5 gap-y-2 border-b border-custom-sidebar-border-200 px-4 py-3.5">
      <div className="h-full w-full truncate">
        <div
          className={`flex grow items-center gap-x-2 truncate rounded py-1 ${isSidebarCollapsed ? "justify-center" : ""}`}
        >
          <Avatar size="sm" className="rounded-lg">
            <Avatar.Image
              alt="Small avatar"
              src="https://heroui-assets.nyc3.cdn.digitaloceanspaces.com/avatars/blue.jpg"
            />
            <Avatar.Fallback className="rounded-lg">SM</Avatar.Fallback>
          </Avatar>
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
              <Avatar.Fallback>LS</Avatar.Fallback>
            </Avatar>
          </Dropdown.Trigger>
          <Dropdown.Popover>
            <div className="px-3 pt-3 pb-1">
              <div className="flex flex-col gap-0">
                <p className="text-sm leading-5 font-medium">Luna Snow</p>
                <p className="text-xs leading-none text-muted">lunasnow@marvel.com</p>
              </div>
            </div>
            <Dropdown.Menu>
              <Dropdown.Item id={`theme-resolver`} textValue="theme-resolver" onPress={handleSwitchTheme}>
                <div className="flex gap-2">
                  <Palette className="h-4 w-4" /> Switch to {resolvedTheme === "dark" ? "light" : "dark"} mode
                </div>
              </Dropdown.Item>
              <Dropdown.Item id={`sign-out`} textValue="Sign-out" variant="danger">
                <div className="flex w-full items-center justify-between gap-2">
                  <Label>Log Out</Label>
                  <LogOut className="h-4 w-4" />
                </div>
              </Dropdown.Item>
            </Dropdown.Menu>
          </Dropdown.Popover>
        </Dropdown>
      )}
    </div>
  );
};
