/** @vitest-environment jsdom */

import type { ReactNode } from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { IWorkspace } from "@syncturtle/types";
import { WorkspaceSetupStep } from "./root";

const space = { id: "space-1", name: "Family", slug: "family" } as IWorkspace;
const mocks = vi.hoisted(() => ({
  spaces: {} as Record<string, IWorkspace>,
  loaded: true,
  loading: false,
  error: undefined as Error | undefined,
  config: { isWorkspaceCreationDisabled: false } as { isWorkspaceCreationDisabled: boolean } | undefined,
  instanceError: undefined as Error | undefined,
  fetch: vi.fn(),
  policy: vi.fn(),
  profile: vi.fn(),
  settings: vi.fn(),
}));
vi.mock("@/hooks/store/use-workspace", () => ({
  useWorkspace: () => ({
    workspaces: mocks.spaces,
    hasLoaded: mocks.loaded,
    loader: mocks.loading,
    error: mocks.error,
    fetchWorkspaces: mocks.fetch,
  }),
}));
vi.mock("@/hooks/store/use-instance", () => ({
  useInstance: () => ({ config: mocks.config, error: mocks.instanceError, fetchInstanceInfo: mocks.policy }),
}));
vi.mock("@/hooks/store/user", () => ({
  useUser: () => ({ data: { id: "user-1" } }),
  useUserProfile: () => ({ data: { lastWorkspaceId: "space-1" }, updateUserProfile: mocks.profile }),
  useUserSettings: () => ({ fetchCurrentUserSettings: mocks.settings }),
}));
vi.mock("./create", () => ({ WorkspaceCreateStep: () => <div>Create Personal</div> }));
vi.mock("@heroui/react", () => ({
  Spinner: () => <p role="status">Loading</p>,
  Button: ({ children, onPress, isDisabled }: { children: ReactNode; onPress: () => void; isDisabled?: boolean }) => (
    <button type="button" onClick={onPress} disabled={isDisabled}>
      {children}
    </button>
  ),
}));

beforeEach(() => {
  vi.resetAllMocks();
  mocks.spaces = {};
  mocks.loaded = true;
  mocks.loading = false;
  mocks.error = undefined;
  mocks.config = { isWorkspaceCreationDisabled: false };
  mocks.instanceError = undefined;
  mocks.fetch.mockResolvedValue([]);
  mocks.policy.mockResolvedValue(undefined);
  mocks.profile.mockResolvedValue(undefined);
  mocks.settings.mockResolvedValue(undefined);
});

describe("space destination and policy", () => {
  it("selects an existing membership when creation is disabled", async () => {
    mocks.config = { isWorkspaceCreationDisabled: true };
    mocks.spaces = { [space.id]: space };
    const next = vi.fn().mockResolvedValue(undefined);
    render(<WorkspaceSetupStep onComplete={next} />);
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: "Use this space" }));
    });
    expect(mocks.profile).toHaveBeenCalledWith({ lastWorkspaceId: space.id });
    expect(mocks.settings).toHaveBeenCalledWith(true);
    expect(next).toHaveBeenCalledWith(space);
    expect(screen.queryByText("Create Personal")).not.toBeInTheDocument();
  });

  it("does not advance or refresh settings when selection persistence fails", async () => {
    mocks.spaces = { [space.id]: space };
    mocks.profile.mockRejectedValueOnce(new Error("offline"));
    const next = vi.fn();
    render(<WorkspaceSetupStep onComplete={next} />);
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: "Use this space" }));
    });
    expect(screen.getByRole("alert")).toHaveTextContent("Could not save your space selection.");
    expect(mocks.settings).not.toHaveBeenCalled();
    expect(next).not.toHaveBeenCalled();
  });

  it("keeps a failed settings refresh retryable without advancing", async () => {
    mocks.spaces = { [space.id]: space };
    mocks.settings.mockRejectedValueOnce(new Error("offline"));
    const next = vi.fn().mockResolvedValue(undefined);
    render(<WorkspaceSetupStep onComplete={next} />);
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: "Use this space" }));
    });
    expect(next).not.toHaveBeenCalled();
    expect(screen.getByRole("alert")).toHaveTextContent("Could not save your space selection.");
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: "Use this space" }));
    });
    expect(next).toHaveBeenCalledOnce();
  });

  it("ignores a duplicate selection while persistence is pending", async () => {
    mocks.spaces = { [space.id]: space };
    let resolveProfile!: () => void;
    mocks.profile.mockReturnValueOnce(
      new Promise<void>((resolve) => {
        resolveProfile = resolve;
      })
    );
    const next = vi.fn().mockResolvedValue(undefined);
    render(<WorkspaceSetupStep onComplete={next} />);
    await act(async () => {
      const button = screen.getByRole("button", { name: "Use this space" });
      fireEvent.click(button);
      fireEvent.click(button);
    });
    expect(mocks.profile).toHaveBeenCalledOnce();
    await act(async () => {
      resolveProfile();
    });
    expect(mocks.settings).toHaveBeenCalledOnce();
    expect(next).toHaveBeenCalledOnce();
  });

  it("uses an existing membership even when creation policy cannot load", async () => {
    mocks.config = undefined;
    mocks.instanceError = new Error("offline");
    mocks.spaces = { [space.id]: space };
    const next = vi.fn().mockResolvedValue(undefined);
    render(<WorkspaceSetupStep onComplete={next} />);
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: "Use this space" }));
    });
    expect(next).toHaveBeenCalledWith(space);
    expect(screen.queryByText("Create Personal")).not.toBeInTheDocument();
  });

  it("shows an unavailable state and retries when creation is disabled without membership", async () => {
    mocks.config = { isWorkspaceCreationDisabled: true };
    const next = vi.fn();
    render(<WorkspaceSetupStep onComplete={next} />);
    expect(screen.getByText("Space creation is unavailable")).toBeInTheDocument();
    expect(screen.getByText(/Ask your instance administrator/)).toBeInTheDocument();
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: "Retry availability" }));
    });
    expect(mocks.fetch).toHaveBeenCalledOnce();
    expect(mocks.policy).toHaveBeenCalledOnce();
    expect(next).not.toHaveBeenCalled();
  });

  it("never treats a pending or failed workspace load as confirmed empty", async () => {
    mocks.loaded = false;
    mocks.loading = true;
    const next = vi.fn();
    const view = render(<WorkspaceSetupStep onComplete={next} />);
    expect(screen.getByRole("status")).toHaveTextContent("Loading");
    mocks.loading = false;
    mocks.error = new Error("offline");
    view.rerender(<WorkspaceSetupStep onComplete={next} />);
    expect(screen.getByRole("alert")).toHaveTextContent("Unable to load spaces.");
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: "Retry spaces" }));
    });
    expect(mocks.fetch).toHaveBeenCalledOnce();
    expect(next).not.toHaveBeenCalled();
  });

  it("requires known enabled policy to create, but switches to a membership arriving later", () => {
    mocks.config = undefined;
    const next = vi.fn();
    const view = render(<WorkspaceSetupStep onComplete={next} />);
    expect(screen.getByText("Checking space creation policy...")).toBeInTheDocument();
    expect(screen.queryByText("Create Personal")).not.toBeInTheDocument();
    mocks.config = { isWorkspaceCreationDisabled: false };
    view.rerender(<WorkspaceSetupStep onComplete={next} />);
    expect(screen.getByText("Create Personal")).toBeInTheDocument();
    mocks.spaces = { [space.id]: space };
    view.rerender(<WorkspaceSetupStep onComplete={next} />);
    expect(screen.getByRole("button", { name: "Use this space" })).toBeInTheDocument();
    expect(screen.queryByText("Create Personal")).not.toBeInTheDocument();
  });

  it("keeps invitation acceptance deferred", () => {
    render(<WorkspaceSetupStep onComplete={vi.fn()} />);
    expect(screen.getByText(/Pending invitation acceptance is coming/)).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /join|accept/i })).not.toBeInTheDocument();
  });
});
