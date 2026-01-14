import { StoreContext } from "@/lib/store-context";
import type { IThemeStoreInternal, TThemeStore } from "@/store/theme.store";
import { useContext, useSyncExternalStore } from "react";

export const useAppTheme = (): TThemeStore => {
  const context = useContext(StoreContext);
  if (!context) throw new Error("useAppTheme must be used within a StoreProvider");

  const store = context.theme as IThemeStoreInternal;
  useSyncExternalStore(store._subscribe, store._getSnapshot, store._getServerSnapshot);

  return store;
};
