"use client";

import { ScrollShadow } from "@heroui/react";
import { WorkspaceCreateForm } from "./form";

export default function WorkspaceManagementCreatePage() {
  return (
    <div className="container mx-auto w-full h-full p-4 py-4 space-y-6 flex flex-col">
      <div className="border-b border-custom-border-100 mx-4 py-4 space-y-1 shrink-0">
        <div className="text-xl font-medium text-custom-text-100">Create a new workspace on this instance.</div>
        <div className="text-sm font-normal text-custom-text-300">
          You will need to invite users from Workspace Settings after you create this instance
        </div>
      </div>
      <ScrollShadow orientation="vertical" size={40} className="min-h-0 flex-1 px-4">
        <WorkspaceCreateForm />
      </ScrollShadow>
    </div>
  );
}
