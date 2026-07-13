"use client";

import type { ReactNode } from "react";

export const WorkspaceContentWrapper = ({ children }: { children: ReactNode }) => (
  <div className="flex relative size-full overflow-hidden bg-custom-background-90 rounded-lg transition-all ease-in-out duration-300">
    <div className="size-full p-2 grow transition-all ease-in-out duration-300 overflow-hidden">{children}</div>
  </div>
);
