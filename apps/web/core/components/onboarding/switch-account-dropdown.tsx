"use client";

import type { FC, Key } from "react";
import { useState } from "react";
// heroui
import { Button, Dropdown } from "@heroui/react";
// hooks
import { useUser } from "@/hooks/store/user";
// components
import { SwitchAccountModal } from "./switch-account-modal";
// helpers
import { getFileURL } from "@syncturtle/utils";

const SWITCH_ACCOUNT_ACTION = "switch-account";

interface ISwitchAccountDropdown {
  fullName?: string;
}

export const SwitchAccountDropdown: FC<ISwitchAccountDropdown> = (props) => {
  const { fullName } = props;
  // states
  const [showSwitchAccountModal, setShowSwitchAccountModal] = useState(false);
  // store hooks
  const { data: user } = useUser();
  // derived values
  const displayName = user?.firstName
    ? `${user?.firstName} ${user?.lastName ?? ""}`
    : fullName && fullName.trim().length > 0
      ? fullName
      : user?.email;

  const avatarFallback =
    displayName?.trim().charAt(0).toUpperCase() ?? user?.email?.trim().charAt(0).toUpperCase() ?? "R";

  const handleAction = (key: Key) => {
    if (key === SWITCH_ACCOUNT_ACTION) {
      setShowSwitchAccountModal(true);
    }
  };
  if (!displayName && !fullName) return null;

  return (
    <>
      <SwitchAccountModal isOpen={showSwitchAccountModal} onClose={() => setShowSwitchAccountModal(false)} />
      <Dropdown>
        <Button>
          {user?.avatarUrl ? (
            <img
              src={getFileURL(user.avatarUrl)}
              alt={user.displayName ?? displayName ?? "User avatar"}
              className="h-full w-full rounded-full object-cover"
            />
          ) : (
            <>{avatarFallback}</>
          )}
          <span className="text-sm font-medium text-custom-text-200">{displayName}</span>
        </Button>
        <Dropdown.Popover placement="bottom end">
          <Dropdown.Menu onAction={handleAction}>
            <Dropdown.Item id={SWITCH_ACCOUNT_ACTION} textValue="Wrong e-mail address?">
              Wrong e-mail address?
            </Dropdown.Item>
          </Dropdown.Menu>
        </Dropdown.Popover>
      </Dropdown>
    </>
  );
};
