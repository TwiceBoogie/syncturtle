"use client";

import type { ReactNode } from "react";

export default function WorkspaceLayout({ children }: { children: ReactNode }) {
  return (
    <>
      <div className="relative flex h-full w-full flex-col overflow-hidden rounded-lg border border-custom-border-200">
        <div id="full-screen-portal" className="inset-0 absolute w-full" />
        <div className="relative flex size-full overflow-hidden">
          {/* <ProjectAppSidebar/> */}
          {/* <ExtendedProjectSidebar /> */}
          <main className="relative flex h-full w-full flex-col overflow-hidden bg-surface">{children}</main>
        </div>
      </div>
    </>
  );
}
