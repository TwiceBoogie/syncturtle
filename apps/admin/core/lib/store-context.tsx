"use client";

import type { ReactNode } from "react";
import { createContext } from "react";
import { RootStore } from "@/syncturtle-admin/store/root.store";

let rootStore: RootStore | null = null;

const initializeStore = () => {
  const newRootStore = rootStore ?? new RootStore();
  if (typeof window === "undefined") return newRootStore;

  if (!rootStore) rootStore = newRootStore;

  return newRootStore;
};

export const store = initializeStore();
export const StoreContext = createContext<RootStore | null>(null);

export const StoreProvider = ({ children }: { children: ReactNode }) => (
  <StoreContext.Provider value={store}>{children}</StoreContext.Provider>
);
