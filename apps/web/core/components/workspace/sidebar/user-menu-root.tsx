import { Avatar, AvatarFallback, Dropdown, Label, Separator } from "@heroui/react";
import { LogOut, Settings, SlidersHorizontal } from "lucide-react";

export const UserMenuRoot = () => {
  const username = "Luna Snow";
  const initial = username.trim().charAt(0).toUpperCase() || "U";
  const email = "lunasnow@marvel.com";
  return (
    <Dropdown>
      <Dropdown.Trigger className="flex size-[28px] items-center justify-center rounded-sm p-0">
        <Avatar size="sm" className="size-[20px]">
          <AvatarFallback>{initial}</AvatarFallback>
        </Avatar>
      </Dropdown.Trigger>
      <Dropdown.Popover placement="bottom end" offset={6} className="rounded-lg">
        <div className="p2">
          <div className="flex min-h-[116px] flex-col items-center justify-center gap-2 overflow-hidden rounded-lg px-2 py-4 text-center">
            <Avatar>
              <AvatarFallback>{initial}</AvatarFallback>
            </Avatar>
            <div className="w-full min-w-0">
              <p className="truncate text-sm font-medium text-custom-text-100">{username}</p>
              <p className="truncate text-xs text-custom-text-300">{email}</p>
            </div>
          </div>
        </div>
        <Dropdown.Menu aria-label="Account" className="p-1">
          <Dropdown.Item id="settings" textValue="Settings" className="rounded-lg">
            <Settings aria-hidden="true" className="size-4" />
            <Label>Settings</Label>
          </Dropdown.Item>
          <Dropdown.Item id="preferences" textValue="Preferences" className="rounded-lg">
            <SlidersHorizontal aria-hidden="true" className="size-4" />
            <Label>Preferences</Label>
          </Dropdown.Item>

          <Separator className="my-1 border-custom-border-200" />

          <Dropdown.Item
            id="sign-out"
            textValue="Sign out"
            variant="danger"
            className={`text-custom-error-200 rounded-lg`}
          >
            <LogOut aria-hidden="true" className="size-4" />
            <Label>Sign out</Label>
          </Dropdown.Item>
        </Dropdown.Menu>
      </Dropdown.Popover>
    </Dropdown>
  );
};
