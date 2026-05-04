import { cn } from "@syncturtle/utils";
import { FC, ReactNode } from "react";

interface IDefaultProps {
  children: ReactNode;
  gradient?: boolean;
  className?: string;
}

const DefaultLayout: FC<IDefaultProps> = ({ children, gradient = false, className }) => (
  <div className={cn(`h-screen w-full overflow-hidden ${gradient ? "" : "bg-custom-background-100"}`, className)}>
    {children}
  </div>
);

export default DefaultLayout;
