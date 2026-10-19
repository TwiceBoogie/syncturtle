"use client";

import { useState, useSyncExternalStore } from "react";
import { Info, X } from "lucide-react";
// hooks
import { useInstance } from "@/hooks/store/use-instance";

const dismissalKey = (instanceId: string): string => `syncturtle.admin.version-banner.dismissed.${instanceId}`;
const NOT_HYDRATED = "__not_hydrated__";

const subscribeToStorage = (onStoreChange: () => void): (() => void) => {
  window.addEventListener("storage", onStoreChange);
  return () => window.removeEventListener("storage", onStoreChange);
};

const readDismissedVersion = (instanceId: string | undefined): string | null => {
  if (!instanceId) return null;
  try {
    return window.localStorage.getItem(dismissalKey(instanceId));
  } catch {
    return null;
  }
};

type Dismissal = {
  instanceId: string;
  latestVersion: string | null;
};

export const UpdateAvailableBanner = () => {
  const { instance } = useInstance();
  const [dismissal, setDismissal] = useState<Dismissal | null>(null);
  const instanceId = instance?.id;
  const persistedDismissal = useSyncExternalStore(
    subscribeToStorage,
    () => readDismissedVersion(instanceId),
    () => NOT_HYDRATED
  );

  const latestVersion = instance?.latestVersion?.trim();
  if (
    !instance ||
    instance.versionStatus !== "UPDATE_AVAILABLE" ||
    !latestVersion ||
    persistedDismissal === NOT_HYDRATED ||
    persistedDismissal === latestVersion ||
    (dismissal?.instanceId === instance.id && dismissal.latestVersion === latestVersion)
  ) {
    return null;
  }

  const dismiss = () => {
    setDismissal({ instanceId: instance.id, latestVersion });
    try {
      window.localStorage.setItem(dismissalKey(instance.id), latestVersion);
    } catch {
      // Keep the in-memory dismissal until this component unmounts.
    }
  };

  return (
    <aside
      aria-label="SyncTurtle update available"
      className="flex shrink-0 items-start gap-3 border-b border-custom-border-100 bg-custom-primary-100/5 px-4 py-3 text-sm text-custom-text-100"
    >
      <Info className="mt-0.5 h-4 w-4 shrink-0 text-custom-primary-100" aria-hidden="true" />
      <p className="min-w-0 flex-1">
        <span className="font-medium">A new SyncTurtle release is available.</span>{" "}
        <span>Installed: {instance.currentVersion?.trim() || "Unavailable"}.</span>{" "}
        <span>Latest: {latestVersion}.</span>
      </p>
      <button
        type="button"
        onClick={dismiss}
        aria-label={`Dismiss update notice for ${latestVersion}`}
        className="rounded p-1 text-custom-text-300 hover:text-custom-text-100 focus-visible:outline-2 focus-visible:outline-custom-primary-100"
      >
        <X className="h-4 w-4" aria-hidden="true" />
      </button>
    </aside>
  );
};
