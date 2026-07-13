import { buttonVariants } from "@heroui/styles";
import { cn } from "@syncturtle/ui";
import Link from "next/link";
import type { FC, ReactNode } from "react";

interface ISidebarButtonLinkProps {
  href: string;
  label: string;
  isActive?: boolean;
  isCollapsed?: boolean;
  onPress: () => void;
  children: ReactNode;
}

export const SidebarButtonLink: FC<ISidebarButtonLinkProps> = ({
  href,
  label,
  isActive = false,
  isCollapsed = false,
  onPress,
  children,
}) => (
  <Link
    href={href}
    aria-label={label}
    onClick={onPress}
    className={cn(
      buttonVariants({
        variant: "ghost",
        size: "sm",
        fullWidth: true,
        isIconOnly: isCollapsed,
      }),
      "justify-start gap-3 rounded-md px-3 text-sm font-medium transition-colors",
      isCollapsed && "mx-auto size-8 min-w-8 justify-center px-0",
      isActive
        ? "bg-custom-primary-100/10 text-custom-primary-100"
        : "text-custom-sidebar-text-200 hover:bg-custom-sidebar-background-80 focus:bg-custom-sidebar-background-80"
    )}
  >
    {children}
  </Link>
);
