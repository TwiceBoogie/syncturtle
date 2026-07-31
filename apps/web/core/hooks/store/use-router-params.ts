import { useContext, useSyncExternalStore } from "react";
// store
import { StoreContext } from "@/lib/store-context";
import type { IRouterStore } from "@/store/router.store";

export const useRouterParams = (): IRouterStore => {
  const context = useContext(StoreContext);
  if (!context) throw new Error("useRouterParams must be used within StoreProvider");

  const store = context.router;
  useSyncExternalStore(store._subscribe, store._getSnapshot, store._getServerSnapshot);

  return store;
};
