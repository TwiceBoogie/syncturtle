/** @vitest-environment jsdom */

import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { EOnboardingSteps } from "@syncturtle/constants";
import type { IWorkspaceMemberInvitation } from "@syncturtle/types";
import { WorkspaceSetupStep } from "./root";

const invitation = { id: "invite-1", workspace: { id: "space-2", name: "Family" } } as IWorkspaceMemberInvitation;
vi.mock("@/hooks/store/user", () => ({ useUser: () => ({ data: { id: "user-1" } }) }));
vi.mock("./create", () => ({
  WorkspaceCreateStep: ({
    onComplete,
    handleCurrentViewChange,
    hasInvitations,
  }: {
    onComplete: (skipInvites?: boolean) => void;
    handleCurrentViewChange: () => void;
    hasInvitations: boolean;
  }) => (
    <div>
      <p>Create view</p>
      <button onClick={() => onComplete(true)}>Create solo</button>
      {hasInvitations && <button onClick={handleCurrentViewChange}>View invitations</button>}
    </div>
  ),
}));
vi.mock("./join-invites", () => ({
  WorkspaceJoinInvitesStep: ({
    invitations,
    handleCurrentViewChange,
  }: {
    invitations: IWorkspaceMemberInvitation[];
    handleCurrentViewChange: () => void;
  }) => (
    <div>
      <p>Join view: {invitations.length}</p>
      <button onClick={handleCurrentViewChange}>Create a Space</button>
    </div>
  ),
}));

describe("Plane workspace stage", () => {
  it("shows Create when no invitations exist and passes the solo skip callback", () => {
    const next = vi.fn();
    render(<WorkspaceSetupStep invitations={[]} handleStepChange={next} />);
    expect(screen.getByText("Create view")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Create solo" }));
    expect(next).toHaveBeenCalledWith(EOnboardingSteps.WORKSPACE_CREATE_OR_JOIN, true);
  });

  it("shows Join when incoming invitations exist, with a Create alternative", () => {
    const next = vi.fn();
    render(<WorkspaceSetupStep invitations={[invitation]} handleStepChange={next} />);
    expect(screen.getByText("Join view: 1")).toBeInTheDocument();
    expect(next).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", { name: "Create a Space" }));
    expect(screen.getByText("Create view")).toBeInTheDocument();
  });
});
