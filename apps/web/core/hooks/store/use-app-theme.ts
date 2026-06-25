import { useContext, useSyncExternalStore } from "react";
import { StoreContext } from "@/lib/store-context";
import type { IThemeStore } from "@/store/theme.store";

export const useAppTheme = (): IThemeStore => {
  const context = useContext(StoreContext);
  if (!context) throw new Error("useAppTheme must be used within a StoreProvider");

  const store = context.theme;
  useSyncExternalStore(store._subscribe, store._getSnapshot, store._getServerSnapshot);

  return store;
};
