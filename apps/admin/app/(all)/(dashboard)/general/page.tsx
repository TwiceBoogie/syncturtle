"use client";

// components
import { GeneralConfigurationForm } from "./form";
// hooks
import { useInstance } from "@/hooks/store/use-instance";

export default function GeneralPage() {
  const { instance, instanceAdmins } = useInstance();
  return (
    <div className="relative container mx-auto w-full h-full p-4 space-y-6 flex flex-col">
      <div className="border-b border-custom-border-100 mx-4 py-4 space-y-1 flex flex-col">
        <div className="text-xl font-medium text-custom-text-100">General settings</div>
        <div className="text-sm font-normal text-custom-text-300">
          Change the name of your instance and instance admin email address. Enable or disable telemetry in your
          instance.
        </div>
      </div>
      <div className="grow overflow-hidden overflow-y-scroll px-4">
        {instance && instanceAdmins && <GeneralConfigurationForm instance={instance} instanceAdmins={instanceAdmins} />}
      </div>
    </div>
  );
}
