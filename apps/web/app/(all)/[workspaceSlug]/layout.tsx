"use client";

import type { ReactNode } from "react";
// hooks
import { AppRailProvider } from "@/hooks/context/app-rail-context";
// components
import { APP_RAIL_AVAILABLE } from "ee/components/app-rail";
import { AuthenticationWrapper } from "@/lib/wrappers";
import { WorkspaceContentWrapper } from "@/syncturtle-web/components/workspace/content-wrapper";
import { WorkspaceAuthWrapper } from "@/layouts/auth-layout/workspace-wrapper";

export default function WorkspaceLayout({ children }: { children: ReactNode }) {
  return (
    <AuthenticationWrapper>
      <WorkspaceAuthWrapper>
        <AppRailProvider editionAvailable={APP_RAIL_AVAILABLE}>
          <WorkspaceContentWrapper>{children}</WorkspaceContentWrapper>
        </AppRailProvider>
      </WorkspaceAuthWrapper>
    </AuthenticationWrapper>
  );
}
