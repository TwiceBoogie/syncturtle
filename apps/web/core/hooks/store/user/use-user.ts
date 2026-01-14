import { StoreContext } from "@/lib/store-context";
import { IUserStoreInternal, TUserStore } from "@/store/user";
import { useContext, useSyncExternalStore } from "react";

export const useUser = (): TUserStore => {
  const context = useContext(StoreContext);
  if (!context) throw new Error("useUser must be used inside a StoreProvider");
  const store = context.user as IUserStoreInternal;
  useSyncExternalStore(store._subscribe, store._getSnapshot, store._getServerSnapshot);

  return store;
};
