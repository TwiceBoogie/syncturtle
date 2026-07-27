import { useContext, useSyncExternalStore } from "react";
import { StoreContext } from "@/lib/store-context";
import type { IUserStore } from "@/store/user";

export const useUser = (): IUserStore => {
  const context = useContext(StoreContext);
  if (!context) throw new Error("useUser must be used inside a StoreProvider");
  const store = context.user;
  useSyncExternalStore(store._subscribe, store._getSnapshot, store._getServerSnapshot);

  return store;
};
