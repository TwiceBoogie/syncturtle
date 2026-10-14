import type { ReactNode } from "react";
// hooks
import { useAppRail } from "@/hooks/use-app-rail";
// components
import { AppRailRoot } from "ee/components/app-rail";
import { TopNavigationRoot } from "../navigations";

export const WorkspaceContentWrapper = ({ children }: { children: ReactNode }) => {
  const { shouldRenderAppRail } = useAppRail();

  return (
    <div className="relative flex size-full flex-col overflow-hidden bg-canvas transition-all duration-300 ease-in-out">
      <TopNavigationRoot />
      <div className="relative flex size-full overflow-hidden">
        {shouldRenderAppRail && <AppRailRoot />}
        <div
          data-testid="workspace-content-inset"
          className={`relative size-full grow overflow-hidden pr-2 pb-2 transition-all duration-300 ease-in-out ${shouldRenderAppRail ? "pl-0" : "pl-2"}`}
        >
          {children}
        </div>
      </div>
    </div>
  );
};
