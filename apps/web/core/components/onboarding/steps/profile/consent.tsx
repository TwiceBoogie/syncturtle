"use client";

import type { FC } from "react";
import { Checkbox, Label } from "@heroui/react";

interface IMarketingConsentProps {
  isChecked: boolean;
  handleChange: (checked: boolean) => void;
  disabled?: boolean;
}

export const MarketingConsent: FC<IMarketingConsentProps> = (props) => {
  const { isChecked, handleChange, disabled } = props;

  return (
    <Checkbox
      name="hasMarketingEmailConsent"
      isSelected={isChecked}
      isDisabled={disabled}
      onChange={handleChange}
      variant="secondary"
      className={"justify-center"}
    >
      <Checkbox.Content className="items-center gap-1.5">
        <Checkbox.Control>
          <Checkbox.Indicator />
        </Checkbox.Control>

        <Label className="text-sm font-normal text-custom-text-300">
          I agree to Syncturtle marketing communications
        </Label>
      </Checkbox.Content>
    </Checkbox>
  );
};
