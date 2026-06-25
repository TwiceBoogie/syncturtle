import { useContext, useSyncExternalStore } from "react";
import { StoreContext } from "@/lib/store-context";
import type { IWorkspaceStore } from "@/store/workspace";

export const useWorkspace = (): IWorkspaceStore => {
  const context = useContext(StoreContext);
  if (!context) throw new Error("useWorkspace must be used within StoreProvider");

  const store = context.workspace;
  useSyncExternalStore(store._subscribe, store._getSnapshot, store._getServerSnapshot);

  return store;
};
