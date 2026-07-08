import type { FC } from "react";
import { cn } from "@heroui/react";

interface ICardLayout {
  children: React.ReactNode;
  className?: string;
}

export const CardLayout: FC<ICardLayout> = ({ children, className }) => (
  <div className={cn("grid grid-cols-1 sm:grid-cols-3 gap-4 mt-4", className)}>{children}</div>
);
