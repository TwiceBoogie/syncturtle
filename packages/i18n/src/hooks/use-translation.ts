import { useContext, useSyncExternalStore } from "react";
import type { TTranslationStore } from "../store";
import { TranslationContext } from "../context";

export const useTranslation = (): TTranslationStore => {
  const store = useContext(TranslationContext);
  if (!store) throw new Error("useTranslation must be used inside a TranslationProvider");

  useSyncExternalStore(store._subscribe, store._getSnapshot, store._getServerSnapshot);

  return store;
};
