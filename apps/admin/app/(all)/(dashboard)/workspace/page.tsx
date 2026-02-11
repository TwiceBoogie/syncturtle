"use client";

// import { useState } from "react";
// hooks
// import { useInstance } from "@/hooks/store/use-instance";

export default function WorkspaceMangagementPage() {
  // states
  // const [isSubmitting, setIsSubmitting] = useState(false);
  // store hooks
  // const { formattedConfig, fetchInstanceConfigurations, updateInstanceConfigurations } = useInstance();
  return (
    <div className="bg-emerald-500 relative container mx-auto w-full h-full p-4 py-4 space-y-6 flex flex-col">
      <div className="bg-red-500">
        <div className="space-y-2">
          <div className="text-xl font-medium">Workspaces on this instance</div>
          <div className="text-sm font-normal">See all workspaces and control who can create them.</div>
        </div>
      </div>
      <div>
        <div className="space-y-3">hello</div>
      </div>
    </div>
  );
}
