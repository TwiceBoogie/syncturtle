import type { ReactNode } from "react";
import { AppRailProvider } from "@/hooks/context/app-rail-context";
import { WorkspaceContentWrapper } from "@/syncturtle-web/components/workspace/content-wrapper";

export default function WorkspaceLayout({ children }: { children: ReactNode }) {
  return (
    <AppRailProvider>
      <WorkspaceContentWrapper>{children}</WorkspaceContentWrapper>
    </AppRailProvider>
  );
}
