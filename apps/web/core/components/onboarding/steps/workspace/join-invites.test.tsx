/** @vitest-environment jsdom */

import type { ReactNode } from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import type { IWorkspaceMemberInvitation } from "@syncturtle/types";
import { WorkspaceJoinInvitesStep } from "./join-invites";

const mocks = vi.hoisted(() => ({
  join: vi.fn(),
  fetchWorkspaces: vi.fn(),
  fetchSettings: vi.fn(),
}));
vi.mock("@/hooks/store/use-workspace", () => ({
  useWorkspace: () => ({ fetchWorkspaces: mocks.fetchWorkspaces }),
}));
vi.mock("@/hooks/store/user", () => ({
  useUserSettings: () => ({ fetchCurrentUserSettings: mocks.fetchSettings }),
}));
vi.mock("@/services/workspace.service", () => ({
  WorkspaceService: class {
    joinWorkspaces = mocks.join;
  },
}));
vi.mock("../common", () => ({ CommonOnboardingHeader: ({ title }: { title: string }) => <h1>{title}</h1> }));
vi.mock("@/components/workspace/logo", () => ({ WorkspaceLogo: () => <span>Space logo</span> }));
vi.mock("@heroui/react", () => {
  const Checkbox = Object.assign(({ children }: { children: ReactNode }) => <div>{children}</div>, {
    Content: ({ children }: { children: ReactNode }) => <div>{children}</div>,
    Control: ({ children }: { children: ReactNode }) => <div>{children}</div>,
    Indicator: () => null,
  });
  return {
    Checkbox,
    CheckboxGroup: ({ children }: { children: ReactNode }) => <div>{children}</div>,
    Button: ({
      children,
      onPress,
      isDisabled,
    }: {
      children: ReactNode | ((state: { isPending: boolean }) => ReactNode);
      onPress?: () => void;
      isDisabled?: boolean;
    }) => (
      <button onClick={onPress} disabled={isDisabled}>
        {typeof children === "function" ? children({ isPending: false }) : children}
      </button>
    ),
  };
});

describe("incoming invitation backend gap", () => {
  it("shows real invitations but leaves acceptance action disabled", () => {
    const invitation = {
      id: "invite-1",
      accepted: false,
      email: "owner@example.test",
      message: "",
      // A pending response is null at runtime; the shared type currently declares Date.
      respondedAt: null as unknown as Date,
      role: 15,
      token: "test-token",
      inviteLink: "/invitations/test-token",
      workspace: { id: "space-2", name: "Family", slug: "family", logoUrl: "" },
    } satisfies IWorkspaceMemberInvitation;
    const create = vi.fn();
    const next = vi.fn();
    render(
      <WorkspaceJoinInvitesStep invitations={[invitation]} handleNextStep={next} handleCurrentViewChange={create} />
    );
    expect(screen.getByText("Family")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Continue" })).toBeDisabled();
    fireEvent.click(screen.getByRole("button", { name: "Create new workspace" }));
    expect(create).toHaveBeenCalledOnce();
    expect(next).not.toHaveBeenCalled();
    expect(mocks.join).not.toHaveBeenCalled();
    expect(mocks.fetchWorkspaces).not.toHaveBeenCalled();
  });
});
