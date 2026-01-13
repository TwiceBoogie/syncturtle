"use client";

import { RootStore } from "@/syncturtle-web/store/root.store";
import { createContext, ReactElement } from "react";

export let rootStore: RootStore | null = null;

const initializeStore = () => {
  // create new store instance on first access
  const newRootStore = rootStore ?? new RootStore();
  // on server always return a fresh instance (avoid cross request sharing)
  if (typeof window === "undefined") return newRootStore;

  // on the client persist the singleton
  if (!rootStore) rootStore = newRootStore;

  return newRootStore;
};

export const store = initializeStore();
export const StoreContext = createContext<RootStore | null>(null);

export const StoreProvider = ({ children }: { children: ReactElement }) => (
  <StoreContext.Provider value={store}>{children}</StoreContext.Provider>
);
