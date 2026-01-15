"use client";

import { InstanceSignInForm } from "@/components/authentication";
import { InstanceFailureView, InstanceSetupFrom } from "@/components/instance";
import { useInstance } from "@/hooks/store/use-instance";

export default function Home() {
  // store hooks
  const { instance, error } = useInstance();

  if (!instance && !error) {
    return <div className="flex h-screen w-full items-center justify-center">Spinner</div>;
  }

  if (error) {
    return <InstanceFailureView />;
  }

  if (instance && !instance?.isSetupDone) {
    return <InstanceSetupFrom />;
  }

  return <InstanceSignInForm />;
}
