import { useContext, useSyncExternalStore } from "react";
import { StoreContext } from "@/lib/store-context";
import type { IUserProfileStore } from "@/store/user/profile.store";

export const useUserProfile = (): IUserProfileStore => {
  const context = useContext(StoreContext);
  if (!context) throw new Error("useUserProfile must be used inside a StoreProvider");
  const store = context.user.userProfile;
  useSyncExternalStore(store._subscribe, store._getSnapshot, store._getServerSnapshot);

  return store;
};
