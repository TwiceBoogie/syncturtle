import type { FC, ReactNode } from "react";
import useSWR from "swr";
// hooks
import { useInstance } from "@/hooks/store/use-instance";

interface IInstanceWrapperProps {
  children: ReactNode;
}

export const InstanceWrapper: FC<IInstanceWrapperProps> = (props) => {
  const { children } = props;

  const { fetchInstanceInfo } = useInstance();

  useSWR("INSTANCE_DETAILS", () => fetchInstanceInfo(), {
    revalidateOnFocus: false,
    revalidateIfStale: false,
    errorRetryCount: 0,
  });

  return <>{children}</>;
};
