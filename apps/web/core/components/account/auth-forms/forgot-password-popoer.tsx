import type { FC } from "react";
import { useState } from "react";
// heroui
import { CloseButton, Popover } from "@heroui/react";
import { X } from "lucide-react";
// store hooks
import { useTranslation } from "@syncturtle/i18n";

export const ForgotPasswordPopover: FC = () => {
  const { t } = useTranslation();

  const [isOpen, setIsOpen] = useState(false);

  return (
    <Popover isOpen={isOpen} onOpenChange={setIsOpen}>
      <Popover.Trigger>
        <button type="button" className="text-sm font-medium text-custom-primary-100 outline-none">
          {t("auth.common.forgot_password")}
        </button>
      </Popover.Trigger>
      <Popover.Content>
        <Popover.Dialog className="flex gap-3 justify-center items-center">
          <p className="text-xs">{t("auth.forgot_password.errors.smtp_not_enabled")}</p>
          <CloseButton>
            <X className="size-3 text-custom-text-200" />
          </CloseButton>
        </Popover.Dialog>
      </Popover.Content>
    </Popover>
  );
};
