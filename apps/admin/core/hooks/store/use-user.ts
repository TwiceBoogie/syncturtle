import { StoreContext } from "@/lib/store-context";
import type { IUserStore } from "@/store/user.store";
import { useContext, useSyncExternalStore } from "react";

export const useUser = (): IUserStore => {
  const context = useContext(StoreContext);
  if (!context) throw new Error("useUser must be used within a StoreProvider");

  const store = context.user;
  useSyncExternalStore(store._subscribe, store._getSnapshot, store._getServerSnapshot);

  return store;
};
