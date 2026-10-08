import { Button } from "@heroui/react";

export const HeaderRightActions = () => (
  <div className="flex flex-1 items-center justify-end gap-1">
    <Button
      variant="secondary"
      size="md"
      className="h-[28px] min-w-[48px] rounded-sm border border-custom-border-400 bg-custom-background-90 px-[10px] text-[13px] font-medium leading-[20px] text-custom-text-100 shadow-sm hover:bg-custom-background-80 data-hovered:bg-custom-background-80"
    >
      Get started
    </Button>
  </div>
);
