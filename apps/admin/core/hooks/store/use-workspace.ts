import { StoreContext } from "@/lib/store-context";
import { IWorkspaceStoreInternal, TWorkspaceStore } from "@/store/workspace.store";
import { useContext, useSyncExternalStore } from "react";

export const useWorkspace = (): TWorkspaceStore => {
  const context = useContext(StoreContext);
  if (!context) throw new Error("useWorkspace must be within a StoreProvider");

  const store = context.workspace as IWorkspaceStoreInternal;
  useSyncExternalStore(store._subscribe, store._getSnapshot, store._getServerSnapshot);

  return store;
};
