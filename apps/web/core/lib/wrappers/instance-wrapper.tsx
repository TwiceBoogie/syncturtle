import { FC, ReactNode } from "react";
import useSWR from "swr";
// components
import { InstanceNotReady, MaintenanceView } from "@/components/instance";
// hooks
import { useInstance } from "@/hooks/store/use-instance";
import { Spinner } from "@heroui/react";

interface IInstanceWrapper {
  children: ReactNode;
}

export const InstanceWrapper: FC<IInstanceWrapper> = (props) => {
  const { children } = props;
  // store
  const { isLoading, instance, error, fetchInstanceInfo } = useInstance();

  const { isLoading: isInstanceSWRLoading, error: instanceSWRError } = useSWR(
    "INSTANCE_INFORMATION",
    async () => await fetchInstanceInfo(),
    { revalidateOnFocus: false }
  );

  if ((isLoading || isInstanceSWRLoading) && !instance) {
    return (
      <div className="relative flex h-screen w-full items-center justify-center">
        <Spinner />
      </div>
    );
  }

  if (instanceSWRError) return <MaintenanceView />;

  if (error && error?.status === "error") return <>{children}</>;

  // instance is not ready and setup is not done
  if (instance?.isSetupDone === false) return <InstanceNotReady />;

  return <>{children}</>;
};
