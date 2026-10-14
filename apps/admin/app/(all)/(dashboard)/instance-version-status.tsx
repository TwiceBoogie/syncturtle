"use client";

import { Tooltip } from "@heroui/react";
// hooks
import { useInstance } from "@/hooks/store/use-instance";
// types
import type { IInstance } from "@syncturtle/types";

const checkedAtFormatter = new Intl.DateTimeFormat("en-US", {
  year: "numeric",
  month: "short",
  day: "numeric",
  hour: "numeric",
  minute: "2-digit",
  timeZone: "UTC",
  timeZoneName: "short",
});

const displayVersion = (value: string | null | undefined): string => {
  const version = value?.trim();
  if (!version) return "Unavailable";
  if (version === "development") return version;
  return /^[vV]/.test(version) ? version : `v${version}`;
};

const displayCheckedAt = (value: string | null): string => {
  if (!value?.trim()) return "Never checked";
  const checkedAt = new Date(value);
  if (Number.isNaN(checkedAt.getTime())) return "Unavailable";
  return checkedAtFormatter.format(checkedAt);
};

const statusPresentation: Record<IInstance["versionStatus"], { label: string; dot: string }> = {
  CURRENT: { label: "Up to date", dot: "bg-green-500" },
  UPDATE_AVAILABLE: { label: "Update available", dot: "bg-amber-500" },
  AHEAD_OF_RELEASE: { label: "Ahead of latest release", dot: "bg-blue-500" },
  UNKNOWN: { label: "Status unavailable", dot: "bg-custom-text-300" },
};

export const InstanceVersionStatus = () => {
  const { instance } = useInstance();
  if (!instance) return null;

  const installed = displayVersion(instance.currentVersion);
  const latest = displayVersion(instance.latestVersion);
  const checkedAt = displayCheckedAt(instance.lastCheckedAt);
  const status = statusPresentation[instance.versionStatus] ?? statusPresentation.UNKNOWN;

  return (
    <Tooltip delay={0}>
      <button
        type="button"
        className="flex max-w-48 shrink-0 items-center gap-2 rounded-md px-2 py-1 text-xs text-custom-text-200 hover:bg-custom-background-80 focus-visible:outline-2 focus-visible:outline-custom-primary-100"
        aria-label={`Installed ${installed}; ${status.label}`}
      >
        <span className="truncate font-medium text-custom-text-100">{installed}</span>
        <span className={`h-2 w-2 shrink-0 rounded-full ${status.dot}`} aria-hidden="true" />
        <span className="hidden truncate sm:inline">{status.label}</span>
      </button>
      <Tooltip.Content placement="bottom" className="max-w-64 p-3">
        <dl className="grid grid-cols-[auto_1fr] gap-x-3 gap-y-1 text-xs">
          <dt>Installed</dt>
          <dd className="text-right">{installed}</dd>
          <dt>Latest release</dt>
          <dd className="text-right">{latest}</dd>
          <dt>Status</dt>
          <dd className="text-right">{status.label}</dd>
          <dt>Last checked</dt>
          <dd className="text-right">{checkedAt}</dd>
        </dl>
      </Tooltip.Content>
    </Tooltip>
  );
};
