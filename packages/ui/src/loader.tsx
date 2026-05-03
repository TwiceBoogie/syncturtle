import React from "react";
import { cn } from "./utils/classname";

interface ILoaderProps {
  children: React.ReactNode;
  className?: string;
}

const Loader = ({ children, className = "" }: ILoaderProps) => (
  <div className={cn("animate-pulse", className)} role="status">
    {children}
  </div>
);

interface IItemProps {
  height?: string;
  width?: string;
  className?: string;
}

const Item = ({ height = "auto", width = "auto", className = "" }: IItemProps) => (
  <div className={cn("rounded-md bg-custom-background-80", className)} style={{ height: height, width: width }} />
);

Loader.Item = Item;

Loader.displayName = "syncturtle-ui-loader";

export { Loader };
