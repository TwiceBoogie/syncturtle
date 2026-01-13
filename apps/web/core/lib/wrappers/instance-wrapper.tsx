import { FC, ReactNode } from "react";

interface IInstanceWrapper {
  children: ReactNode;
}

export const InstanceWrapper: FC<IInstanceWrapper> = (props) => {
  const { children } = props;

  return <>{children}</>;
};
