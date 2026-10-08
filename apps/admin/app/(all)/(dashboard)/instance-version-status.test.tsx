/** @vitest-environment jsdom */

import type { ReactNode } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import type { IInstance } from "@syncturtle/types";
import { InstanceVersionStatus } from "./instance-version-status";
import { UpdateAvailableBanner } from "./update-available-banner";

const mocks = vi.hoisted(() => ({ instance: null as IInstance | null }));

vi.mock("@/hooks/store/use-instance", () => ({ useInstance: () => ({ instance: mocks.instance }) }));
vi.mock("@heroui/react", () => {
  const Container = ({ children }: { children: ReactNode }) => <div>{children}</div>;
  return { Tooltip: Object.assign(Container, { Content: Container }) };
});

const instance = (overrides: Partial<IInstance> = {}): IInstance => ({
  id: "instance-1",
  instanceName: "SyncTurtle",
  whitelistEmails: undefined,
  licenseKey: undefined,
  instanceId: "public-instance-1",
  currentVersion: "1.2.0",
  latestVersion: "v1.2.0",
  versionStatus: "CURRENT",
  lastCheckedAt: "2026-10-05T12:00:00Z",
  namespace: undefined,
  isTelemetryEnabled: false,
  isSupportRequired: false,
  isActivated: true,
  isSetupDone: true,
  isSignupScreenVisited: true,
  userCount: 1,
  isVerified: true,
  createdAt: "2026-10-01T00:00:00Z",
  updatedAt: "2026-10-05T12:00:00Z",
  createdBy: undefined,
  updatedBy: undefined,
  workspaceExist: true,
  ...overrides,
});

beforeEach(() => {
  window.localStorage.clear();
  mocks.instance = null;
});

describe("Admin instance version awareness", () => {
  it("shows the current version and last check without an update banner", () => {
    mocks.instance = instance();

    render(
      <>
        <InstanceVersionStatus />
        <UpdateAvailableBanner />
      </>
    );

    expect(screen.getByRole("button", { name: "Installed v1.2.0; Up to date" })).toBeInTheDocument();
    expect(screen.getAllByText("v1.2.0", { selector: "dd" })).toHaveLength(2);
    expect(screen.getByText("Oct 5, 2026, 12:00 PM UTC")).toBeInTheDocument();
    expect(screen.queryByRole("complementary", { name: "SyncTurtle update available" })).toBeNull();
  });

  it("shows an update indicator and informational banner for an available release", () => {
    mocks.instance = instance({ latestVersion: "v1.3.0", versionStatus: "UPDATE_AVAILABLE" });

    render(
      <>
        <InstanceVersionStatus />
        <UpdateAvailableBanner />
      </>
    );

    expect(screen.getByRole("button", { name: "Installed v1.2.0; Update available" })).toBeInTheDocument();
    expect(screen.getByRole("complementary", { name: "SyncTurtle update available" })).toHaveTextContent(
      "Latest: v1.3.0"
    );
    expect(screen.queryByRole("button", { name: /upgrade now|install update/i })).toBeNull();
  });

  it("dismisses one release across remounts and shows a newer release", () => {
    mocks.instance = instance({ latestVersion: "v1.3.0", versionStatus: "UPDATE_AVAILABLE" });
    const first = render(<UpdateAvailableBanner />);

    fireEvent.click(screen.getByRole("button", { name: "Dismiss update notice for v1.3.0" }));

    expect(screen.queryByRole("complementary", { name: "SyncTurtle update available" })).toBeNull();
    expect(window.localStorage.getItem("syncturtle.admin.version-banner.dismissed.instance-1")).toBe("v1.3.0");

    first.unmount();
    const next = render(<UpdateAvailableBanner />);
    expect(screen.queryByRole("complementary", { name: "SyncTurtle update available" })).toBeNull();

    mocks.instance = instance({ latestVersion: "v1.4.0", versionStatus: "UPDATE_AVAILABLE" });
    next.rerender(<UpdateAvailableBanner />);
    expect(screen.getByRole("complementary", { name: "SyncTurtle update available" })).toHaveTextContent(
      "Latest: v1.4.0"
    );
  });

  it("treats unknown and nullable release state as informational", () => {
    mocks.instance = instance({
      currentVersion: undefined,
      latestVersion: null,
      lastCheckedAt: null,
      versionStatus: "UNKNOWN",
    });

    render(
      <>
        <InstanceVersionStatus />
        <UpdateAvailableBanner />
      </>
    );

    expect(screen.getByRole("button", { name: "Installed Unavailable; Status unavailable" })).toBeInTheDocument();
    expect(screen.getByText("Never checked")).toBeInTheDocument();
    expect(screen.queryByRole("complementary", { name: "SyncTurtle update available" })).toBeNull();
    expect(screen.queryByText(/Invalid Date|null|undefined/)).toBeNull();
  });

  it("shows a non-error ahead-of-release status without a banner", () => {
    mocks.instance = instance({
      currentVersion: "1.3.0",
      latestVersion: "v1.2.0",
      versionStatus: "AHEAD_OF_RELEASE",
    });

    render(
      <>
        <InstanceVersionStatus />
        <UpdateAvailableBanner />
      </>
    );

    expect(screen.getByRole("button", { name: "Installed v1.3.0; Ahead of latest release" })).toBeInTheDocument();
    expect(screen.queryByRole("complementary", { name: "SyncTurtle update available" })).toBeNull();
  });

  it("does not render while instance data is unavailable", () => {
    const { container } = render(
      <>
        <InstanceVersionStatus />
        <UpdateAvailableBanner />
      </>
    );

    expect(container).toBeEmptyDOMElement();
  });
});
