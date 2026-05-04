"use client";

import { createContext, useEffect, useState, PropsWithChildren } from "react";
import { TranslationStore } from "../store";

export const TranslationContext = createContext<TranslationStore | null>(null);

export function TranslationProvider({ children }: PropsWithChildren) {
  const [store] = useState(() => new TranslationStore());

  /**
   * Start async bootstrap AFTER mount.
   * Avoids side effects during render
   */
  useEffect(() => {
    void store.init();
    return () => store.dispose();
  }, [store]);

  return <TranslationContext.Provider value={store}>{children}</TranslationContext.Provider>;
}
