"use client";

import { createContext, type ReactNode } from "react";
import { useParams } from "next/navigation";
import useLocalStorage from "@/hooks/use-local-storage";

export interface IAppRailContextType {
  isEnabled: boolean;
  shouldRenderAppRail: boolean;
  toggleAppRail: (value?: boolean) => void;
}

const AppRailContext = createContext<IAppRailContextType | undefined>(undefined);

export { AppRailContext };

interface IAppRailProviderProps {
  children: ReactNode;
}

export const AppRailProvider = ({ children }: IAppRailProviderProps) => {
  const { workspaceSlug } = useParams();
  const { storedValue: isAppRailVisible, setValue: setIsAppRailVisible } = useLocalStorage<boolean>(
    `APP_RAIL_${workspaceSlug}`,
    false
  );

  const isEnabled = false;

  const toggleAppRail = (value?: boolean) => {
    if (value === undefined) {
      setIsAppRailVisible(!isAppRailVisible);
    } else {
      setIsAppRailVisible(value);
    }
  };

  const contextValue: IAppRailContextType = {
    isEnabled,
    shouldRenderAppRail: !!isAppRailVisible && isEnabled,
    toggleAppRail,
  };

  return <AppRailContext.Provider value={contextValue}>{children}</AppRailContext.Provider>;
};
