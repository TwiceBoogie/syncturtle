"use client";

import * as React from "react";
import { Tooltip as HeroTooltip } from "@heroui/react";
import { cn } from "@syncturtle/ui";

export type TPlacement =
  | "auto"
  | "auto-start"
  | "auto-end"
  | "top-start"
  | "top-end"
  | "bottom-start"
  | "bottom-end"
  | "right-start"
  | "right-end"
  | "left-start"
  | "left-end"
  | "top"
  | "bottom"
  | "right"
  | "left";

export type TSide = "top" | "bottom" | "left" | "right";
export type TAlign = "start" | "center" | "end";

type TooltipProps = {
  tooltipHeading?: string;
  tooltipContent?: string | React.ReactNode | null;
  position?: TPlacement;
  children: React.ReactElement;
  disabled?: boolean;
  className?: string;
  openDelay?: number;
  closeDelay?: number;
  isMobile?: boolean;
  renderByDefault?: boolean;
  side?: TSide;
  align?: TAlign;
  sideOffset?: number;
};

type THeroTooltipContentProps = React.ComponentProps<typeof HeroTooltip.Content>;

type THeroTooltipPlacement = NonNullable<THeroTooltipContentProps["placement"]>;

function toHeroTooltipPlacement(props: { position?: TPlacement; side: TSide; align: TAlign }): THeroTooltipPlacement {
  const { position, side, align } = props;

  if (position) {
    switch (position) {
      case "auto":
        return "bottom";
      case "auto-start":
        return "bottom start";
      case "auto-end":
        return "bottom end";

      case "top":
        return "top";
      case "top-start":
        return "top start";
      case "top-end":
        return "top end";

      case "bottom":
        return "bottom";
      case "bottom-start":
        return "bottom start";
      case "bottom-end":
        return "bottom end";

      case "left":
        return "left";
      case "left-start":
        return "left top";
      case "left-end":
        return "left bottom";

      case "right":
        return "right";
      case "right-start":
        return "right top";
      case "right-end":
        return "right bottom";
    }
  }

  if (align === "center") {
    return side;
  }

  if (side === "top" || side === "bottom") {
    return `${side} ${align}` as THeroTooltipPlacement;
  }

  return `${side} ${align === "start" ? "top" : "bottom"}` as THeroTooltipPlacement;
}

export function Tooltip(props: TooltipProps) {
  const {
    tooltipHeading,
    tooltipContent,
    position = "top",
    children,
    disabled = false,
    className,
    openDelay = 200,
    closeDelay = 0,
    isMobile = false,
    side = "bottom",
    align = "center",
    sideOffset = 10,
  } = props;

  const placement = React.useMemo(() => toHeroTooltipPlacement({ position, side, align }), [position, side, align]);

  const hasTooltipContent = Boolean(tooltipHeading || tooltipContent);

  if (!hasTooltipContent || disabled || isMobile) {
    return children;
  }

  return (
    <HeroTooltip delay={openDelay} closeDelay={closeDelay} isDisabled={disabled}>
      <HeroTooltip.Trigger>{children}</HeroTooltip.Trigger>

      <HeroTooltip.Content
        placement={placement}
        offset={sideOffset}
        className={cn(
          "z-tooltip max-w-xs gap-1 overflow-hidden wrap-break-word rounded-md bg-custom-background-100 p-2 text-xs text-custom-text-200 shadow-custom-shadow-xs",
          className
        )}
      >
        {tooltipHeading && <h5 className="font-medium text-custom-text-100">{tooltipHeading}</h5>}

        {tooltipContent}
      </HeroTooltip.Content>
    </HeroTooltip>
  );
}
