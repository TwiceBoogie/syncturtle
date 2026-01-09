import { useContext } from "react";
// store
import { StoreContext } from "@/lib/store-context";
import type { TRouterStore } from "@/store/router.store";

export const useRouterParams = (): TRouterStore => {
  const context = useContext(StoreContext);
  if (!context) throw new Error("useRouterParams must be used within StoreProvider");
  return context.router;
};
