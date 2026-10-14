/** @vitest-environment jsdom */

import type { ReactNode } from "react";
import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { IWorkspace } from "@syncturtle/types";
import CreateWorkspacePage from "./page";

const mocks = vi.hoisted(() => ({
  push: vi.fn(),
  back: vi.fn(),
  updateUserProfile: vi.fn(),
  finishUserOnboarding: vi.fn(),
  user: { firstName: "Ada", lastName: "Lovelace", email: "ada@example.test" },
  creationDisabled: false,
  createdWorkspace: { id: "space-new", slug: "newly-created-c" } as IWorkspace,
  formProps: undefined as
    | undefined
    | {
        onSubmit: (workspace: IWorkspace) => Promise<void>;
        defaultValues: { name: string; slug: string; organizationSize: string };
        setDefaultValues: unknown;
      },
}));

vi.mock("next-themes", () => ({ useTheme: () => ({ resolvedTheme: "light" }) }));
vi.mock("next/image", () => ({ default: ({ alt }: { alt: string }) => <span role="img" aria-label={alt} /> }));
vi.mock("next/link", () => ({ default: ({ children }: { children: ReactNode }) => <a href="/">{children}</a> }));
vi.mock("@heroui/react", () => ({
  Button: ({ children, onPress }: { children: ReactNode; onPress?: () => void }) => (
    <button type="button" onClick={onPress}>
      {children}
    </button>
  ),
}));
vi.mock("@syncturtle/i18n", () => ({ useTranslation: () => ({ t: (key: string) => key }) }));
vi.mock("@/hooks/use-app-router", () => ({ useAppRouter: () => ({ push: mocks.push, back: mocks.back }) }));
vi.mock("@/hooks/store/user", () => ({
  useUser: () => ({ data: mocks.user }),
  useUserProfile: () => ({
    updateUserProfile: mocks.updateUserProfile,
    finishUserOnboarding: mocks.finishUserOnboarding,
  }),
}));
vi.mock("@/hooks/store/use-workspace", () => ({
  useWorkspace: () => ({
    workspaces: {
      "space-a": { id: "space-a", slug: "existing-a" },
      "space-b": { id: "space-b", slug: "existing-b" },
    },
    getWorkspaceRedirectionUrl: () => "/existing-a",
  }),
}));
vi.mock("@/hooks/store/use-instance", () => ({
  useInstance: () => ({ config: { isWorkspaceCreationDisabled: mocks.creationDisabled } }),
}));
vi.mock("@/lib/wrappers", () => ({
  AuthenticationWrapper: ({ children }: { children: ReactNode }) => <>{children}</>,
}));
vi.mock("@/components/workspace/create-workspace-form", () => ({
  CreateWorkspaceForm: (props: typeof mocks.formProps) => {
    if (!props) throw new Error("CreateWorkspaceForm props are required");
    mocks.formProps = props;
    return (
      <button type="button" onClick={() => void props.onSubmit(mocks.createdWorkspace)}>
        Submit shared Create form
      </button>
    );
  },
}));

beforeEach(() => {
  vi.resetAllMocks();
  mocks.creationDisabled = false;
  mocks.createdWorkspace = { id: "space-new", slug: "newly-created-c" } as IWorkspace;
  mocks.formProps = undefined;
  mocks.updateUserProfile.mockResolvedValue({ lastWorkspaceId: "space-new" });
});

describe("standalone Create Workspace page", () => {
  it("renders Plane-shaped standalone content with the shared Create form", () => {
    render(<CreateWorkspacePage />);

    expect(screen.getByText("workspace_creation.heading")).toBeInTheDocument();
    expect(screen.getByText("ada@example.test")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Submit shared Create form" })).toBeInTheDocument();
    expect(mocks.formProps?.defaultValues).toEqual({ name: "", slug: "", organizationSize: "" });
    expect(mocks.formProps?.setDefaultValues).toEqual(expect.any(Function));
  });

  it("navigates to the exact created Space with multiple existing Spaces", async () => {
    // Existing Spaces may be first in WorkspaceStore or in the profile response.
    // This page must use the returned creation receipt instead.
    mocks.updateUserProfile.mockResolvedValue({ lastWorkspaceId: "space-a" });
    render(<CreateWorkspacePage />);

    fireEvent.click(screen.getByRole("button", { name: "Submit shared Create form" }));

    await waitFor(() => expect(mocks.push).toHaveBeenCalledWith("/newly-created-c"));
    expect(mocks.updateUserProfile).toHaveBeenCalledOnce();
    expect(mocks.updateUserProfile).toHaveBeenCalledWith({ lastWorkspaceId: "space-new" });
    expect(mocks.push).not.toHaveBeenCalledWith("/existing-a");
    expect(mocks.push).not.toHaveBeenCalledWith("/existing-b");
    expect(mocks.finishUserOnboarding).not.toHaveBeenCalled();
  });

  it("does not navigate if selecting the created Space fails", async () => {
    mocks.updateUserProfile.mockRejectedValue(new Error("profile update failed"));
    render(<CreateWorkspacePage />);

    await expect(
      act(async () => {
        await mocks.formProps?.onSubmit(mocks.createdWorkspace);
      })
    ).rejects.toThrow("profile update failed");

    expect(mocks.push).not.toHaveBeenCalled();
  });

  it("shows the creation-disabled state without mounting the form", () => {
    mocks.creationDisabled = true;
    render(<CreateWorkspacePage />);

    expect(screen.getByText("workspace_creation.errors.creation_disabled.title")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Submit shared Create form" })).not.toBeInTheDocument();
    expect(mocks.formProps).toBeUndefined();
  });
});
