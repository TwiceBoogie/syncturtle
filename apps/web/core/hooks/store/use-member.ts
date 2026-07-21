import { useContext, useSyncExternalStore } from "react";
import { StoreContext } from "@/lib/store-context";
import type { IMemberRootStore } from "@/store/member";

export const useMember = (): IMemberRootStore => {
  const context = useContext(StoreContext);
  if (!context) throw new Error("useMember must be used within StoreProvider");

  const store = context.memberRoot;
  useSyncExternalStore(store._subscribe, store._getSnapshot, store._getServerSnapshot);

  return store;
};
