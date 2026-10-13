/** @vitest-environment jsdom */

import { act, fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { EOnboardingSteps } from "@syncturtle/constants";
import type { TOnboardingStep } from "@syncturtle/types";
import { OnboardingRoot } from "./root";

const mocks = vi.hoisted(() => ({
  progress: { profileComplete: false, workspaceCreate: false, workspaceJoin: false, workspaceInvite: false },
  spaces: {} as Record<string, { id: string }>,
  update: vi.fn(),
  finish: vi.fn(),
}));
vi.mock("@heroui/react", () => ({ toast: vi.fn() }));
vi.mock("@/hooks/store/user", () => ({
  useUser: () => ({ data: { id: "user-1", email: "owner@example.test" } }),
  useUserProfile: () => ({
    data: { onboardingStep: mocks.progress },
    updateUserProfile: mocks.update,
    finishUserOnboarding: mocks.finish,
  }),
}));
vi.mock("@/hooks/store/use-workspace", () => ({ useWorkspace: () => ({ workspaces: mocks.spaces }) }));
vi.mock("./header", () => ({ OnboardingHeader: () => null }));
vi.mock("./steps", () => ({
  OnboardingStepRoot: ({
    currentStep,
    handleStepChange,
  }: {
    currentStep: TOnboardingStep;
    handleStepChange: (step: TOnboardingStep) => void;
  }) => (
    <div>
      <p>{currentStep}</p>
      <button onClick={() => handleStepChange(currentStep)}>Continue</button>
    </div>
  ),
}));

beforeEach(() => {
  vi.resetAllMocks();
  mocks.progress = { profileComplete: false, workspaceCreate: false, workspaceJoin: false, workspaceInvite: false };
  mocks.spaces = {};
  mocks.update.mockResolvedValue(undefined);
  mocks.finish.mockResolvedValue(undefined);
});

describe("Plane-shaped onboarding root", () => {
  it("moves Profile to Use Case, Workspace, then Invite without Role", async () => {
    render(<OnboardingRoot />);
    fireEvent.click(screen.getByRole("button", { name: "Continue" }));
    expect(screen.getByText(EOnboardingSteps.USE_CASE_SETUP)).toBeInTheDocument();
    expect(mocks.update).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", { name: "Continue" }));
    expect(screen.getByText(EOnboardingSteps.WORKSPACE_CREATE_OR_JOIN)).toBeInTheDocument();
    expect(mocks.update).toHaveBeenCalledWith({ onboardingStep: { ...mocks.progress, profileComplete: true } });
    fireEvent.click(screen.getByRole("button", { name: "Continue" }));
    expect(screen.getByText(EOnboardingSteps.INVITE_MEMBERS)).toBeInTheDocument();
    expect(screen.queryByText("ROLE_SETUP")).not.toBeInTheDocument();
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: "Continue" }));
    });
    expect(mocks.finish).toHaveBeenCalledOnce();
  });

  it("does not await a Plane-style progress write before changing steps", () => {
    mocks.update.mockReturnValue(new Promise(() => {}));
    render(<OnboardingRoot />);
    fireEvent.click(screen.getByRole("button", { name: "Continue" }));
    fireEvent.click(screen.getByRole("button", { name: "Continue" }));
    expect(screen.getByText(EOnboardingSteps.WORKSPACE_CREATE_OR_JOIN)).toBeInTheDocument();
  });

  it("uses Plane's flag-based inline resume", () => {
    mocks.progress.profileComplete = true;
    const first = render(<OnboardingRoot />);
    expect(screen.getByText(EOnboardingSteps.WORKSPACE_CREATE_OR_JOIN)).toBeInTheDocument();
    first.unmount();
    mocks.progress.workspaceCreate = true;
    render(<OnboardingRoot />);
    expect(screen.getByText(EOnboardingSteps.INVITE_MEMBERS)).toBeInTheDocument();
  });

  it("finishes from Purpose when a Space is already in the store", async () => {
    mocks.spaces = { "space-1": { id: "space-1" } };
    render(<OnboardingRoot />);
    fireEvent.click(screen.getByRole("button", { name: "Continue" }));
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: "Continue" }));
    });
    expect(mocks.finish).toHaveBeenCalledOnce();
    expect(screen.getByText(EOnboardingSteps.USE_CASE_SETUP)).toBeInTheDocument();
  });

  it("currently absorbs a finish failure and permits a later retry", async () => {
    mocks.spaces = { "space-1": { id: "space-1" } };
    mocks.finish.mockRejectedValueOnce(new Error("offline"));
    render(<OnboardingRoot />);
    fireEvent.click(screen.getByRole("button", { name: "Continue" }));
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: "Continue" }));
    });
    expect(screen.getByText(EOnboardingSteps.USE_CASE_SETUP)).toBeInTheDocument();
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: "Continue" }));
    });
    expect(mocks.finish).toHaveBeenCalledTimes(2);
  });
});
