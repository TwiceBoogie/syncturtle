import type { ReactNode } from "react";
import { cn } from "./utils/classname";

interface ILoaderProps {
  children?: ReactNode;
  className?: string;
}

interface ILoaderItemProps {
  height?: string;
  width?: string;
  className?: string;
}

const LoaderItem = ({ height = "auto", width = "auto", className = "" }: ILoaderItemProps) => (
  <div className={cn("bg-custom-background-80 rounded-md", className)} style={{ height, width }} />
);

type TLoaderComponent = ((props: ILoaderProps) => ReactNode) & {
  Item: typeof LoaderItem;
};

const Loader = (({ children, className = "" }: ILoaderProps) => (
  <div className={cn("animate-pulse", className)} role="status">
    {children}
  </div>
)) as TLoaderComponent;

Loader.Item = LoaderItem;

export { Loader };
