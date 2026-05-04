import { StoreContext } from "@/lib/store-context";
import { IInstanceStoreInternal, TInstanceStore } from "@/store/instance.store";
import { useContext, useSyncExternalStore } from "react";

export const useInstance = (): TInstanceStore => {
  const context = useContext(StoreContext);
  if (!context) throw new Error("useInstance must be used within StoreProvider");

  const store = context.instance as IInstanceStoreInternal;
  useSyncExternalStore(store._subscribe, store._getSnapshot, store._getServerSnapshot);

  return store;
};
