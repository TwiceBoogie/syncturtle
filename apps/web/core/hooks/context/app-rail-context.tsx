"use client";

import { createContext, useCallback, useSyncExternalStore, type ReactNode } from "react";
import { useParams } from "next/navigation";

export interface IAppRailContextType {
  editionAvailable: boolean;
  canUseAppRail: boolean;
  isCollapsed: boolean;
  shouldRenderAppRail: boolean;
  toggleAppRail: () => void;
}

const AppRailContext = createContext<IAppRailContextType | undefined>(undefined);

export { AppRailContext };

interface IAppRailProviderProps {
  children: ReactNode;
  editionAvailable: boolean;
}

const getCollapsedKey = (workspaceSlug: string) => `APP_RAIL_COLLAPSED_${workspaceSlug}`;

const getServerSnapshot = () => false;

const readCollapsed = (key: string) => {
  try {
    return window.localStorage.getItem(key) === "true";
  } catch {
    return false;
  }
};

export const AppRailProvider = ({ children, editionAvailable }: IAppRailProviderProps) => {
  const { workspaceSlug } = useParams<{ workspaceSlug: string }>();
  const key = workspaceSlug ? getCollapsedKey(workspaceSlug) : null;

  const subscribe = useCallback(
    (onStoreChange: () => void) => {
      if (!key) return () => {};
      const onStorage = (event: StorageEvent) => {
        if (event.key === key) onStoreChange();
      };
      window.addEventListener("storage", onStorage);
      window.addEventListener(`local-storage:${key}`, onStoreChange);
      return () => {
        window.removeEventListener("storage", onStorage);
        window.removeEventListener(`local-storage:${key}`, onStoreChange);
      };
    },
    [key]
  );

  const getSnapshot = useCallback(() => (key ? readCollapsed(key) : false), [key]);
  const isCollapsed = useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot);
  const canUseAppRail = editionAvailable;
  const shouldRenderAppRail = canUseAppRail && !isCollapsed;

  const toggleAppRail = useCallback(() => {
    if (!canUseAppRail || !key) return;
    try {
      window.localStorage.setItem(key, JSON.stringify(!readCollapsed(key)));
      window.dispatchEvent(new Event(`local-storage:${key}`));
    } catch {
      // Storage can be unavailable; the rail remains in its current state.
    }
  }, [canUseAppRail, key]);

  return (
    <AppRailContext.Provider
      value={{ editionAvailable, canUseAppRail, isCollapsed, shouldRenderAppRail, toggleAppRail }}
    >
      {children}
    </AppRailContext.Provider>
  );
};
