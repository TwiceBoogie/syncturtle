"use client";

import { cn } from "@syncturtle/ui";
import { forwardRef, type FC, type ReactNode } from "react";

interface IAppHeaderProps {
  header: ReactNode;
  mobileHeader?: ReactNode;
}

export const AppHeader: FC<IAppHeaderProps> = (props) => {
  const { header, mobileHeader } = props;
  console.log(header, mobileHeader);
  return (
    <div className="z-18">
      <Row className="h-header flex gap-2 w-full items-center border-b border-custom-border-200 bg-custom-sidebar-background-100">
        hi
      </Row>
    </div>
  );
};

enum ERowVariant {
  REGULAR = "regular",
  HUGGING = "hugging",
}
type TRowVariant = ERowVariant.REGULAR | ERowVariant.HUGGING;
interface IRowProperties {
  [key: string]: string;
}
const rowStyle: IRowProperties = {
  [ERowVariant.REGULAR]: "px-page-x",
  [ERowVariant.HUGGING]: "px-0",
};

interface RowProps extends React.HTMLAttributes<HTMLDivElement> {
  variant?: TRowVariant;
  className?: string;
  children: React.ReactNode;
}

const Row = forwardRef<HTMLDivElement, RowProps>((props, ref) => {
  const { variant = ERowVariant.REGULAR, className = "", children, ...rest } = props;

  const style = rowStyle[variant];

  return (
    <div ref={ref} className={cn(style, className)} {...rest}>
      {children}
    </div>
  );
});

Row.displayName = "plane-ui-row";
