import { StoreContext } from "@/lib/store-context";
import { IUserProfileStoreInternal, TUserProfileStore } from "@/store/user/profile.store";
import { useContext, useSyncExternalStore } from "react";

export const useUserProfile = (): TUserProfileStore => {
  const context = useContext(StoreContext);
  if (!context) throw new Error("useUserProfile must be used inside a StoreProvider");
  const store = context.user.userProfile as IUserProfileStoreInternal;
  useSyncExternalStore(store._subscribe, store._getSnapshot, store._getServerSnapshot);

  return store;
};
