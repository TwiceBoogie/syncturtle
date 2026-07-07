import { useContext, useSyncExternalStore } from "react";
import { StoreContext } from "@/lib/store-context";
import type { IUserSettingsStore } from "@/store/user/settings.store";

export const useUserSettings = (): IUserSettingsStore => {
  const context = useContext(StoreContext);
  if (!context) throw new Error("useUserSettings must be used inside a StoreProvider");
  const store = context.user.userSettings;
  useSyncExternalStore(store._subscribe, store._getSnapshot, store._getServerSnapshot);

  return store;
};
