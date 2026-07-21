"use client";

import type { ReactNode } from "react";
import { createContext, useState } from "react";
import { RootStore } from "@/syncturtle-web/store/root.store";

let rootStore: RootStore | null = null;

const createRootStore = (): RootStore => new RootStore();

const initializeStore = () => {
  if (typeof window === "undefined") {
    return createRootStore();
  }

  if (!rootStore) {
    rootStore = createRootStore();
  }

  return rootStore;
};

export const StoreContext = createContext<RootStore | null>(null);

export const StoreProvider = ({ children }: { children: ReactNode }) => {
  const [store] = useState<RootStore>(() => initializeStore());

  return <StoreContext.Provider value={store}>{children}</StoreContext.Provider>;
};
