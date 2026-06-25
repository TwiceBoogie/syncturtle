"use client";

import type { ReactNode } from "react";
import { AuthenticationWrapper } from "@/lib/wrappers/authentication-wrapper";
import { WorkspaceAuthWrapper } from "@/syncturtle-web/layouts/workspace-wrapper";

export default function WorkspaceLayout({ children }: { children: ReactNode }) {
  return (
    <AuthenticationWrapper>
      <div>hello there</div>
      <WorkspaceAuthWrapper>
        <div className="relative flex flex-col h-full w-full overflow-hidden rounded-lg border border-custom-border-200">
          <div id="full-screen-portal" className="inset-0 absolute w-full" />
          <div className="relative flex size-full overflow-hidden">
            {/* <ProjectAppSidebar /> */}
            <main className="relative flex h-full w-full flex-col overflow-hidden bg-custom-background-100">
              {children}
            </main>
          </div>
        </div>
      </WorkspaceAuthWrapper>
    </AuthenticationWrapper>
  );
}
