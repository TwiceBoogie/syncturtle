import { useContext, useSyncExternalStore } from "react";
// context
import { StoreContext } from "@/lib/store-context";
// types
import type { IInstanceStore } from "@/store/instance.store";

export const useInstance = (): IInstanceStore => {
  const context = useContext(StoreContext);
  if (!context) throw new Error("useInstance must be used within a StoreProvider");

  const store = context.instance;
  useSyncExternalStore(store._subscribe, store._getSnapshot, store._getServerSnapshot);

  return store;
};
