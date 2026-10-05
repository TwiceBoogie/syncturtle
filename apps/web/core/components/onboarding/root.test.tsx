/** @vitest-environment jsdom */

import { act, fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { EOnboardingSteps } from "@syncturtle/constants";
import type { TOnboardingStep } from "@syncturtle/types";
import { OnboardingRoot } from "./root";

const mocks = vi.hoisted(() => ({
  steps: { profileComplete: false, workspaceCreate: false, workspaceJoin: false, workspaceInvite: false },
  update: vi.fn(),
  finish: vi.fn(),
}));
vi.mock("@/hooks/store/user", () => ({
  useUserProfile: () => ({
    data: { onboardingStep: mocks.steps },
    updateUserProfile: mocks.update,
    finishUserOnboarding: mocks.finish,
  }),
}));
vi.mock("./header", () => ({ OnboardingHeader: () => null }));
vi.mock("./steps", () => ({
  OnboardingStepRoot: ({
    currentStep,
    handleStepChange,
  }: {
    currentStep: TOnboardingStep;
    handleStepChange: (step: TOnboardingStep) => Promise<void>;
  }) => (
    <>
      <p>{currentStep}</p>
      <button type="button" onClick={() => void handleStepChange(currentStep).catch(() => {})}>
        Continue
      </button>
    </>
  ),
}));

beforeEach(() => {
  vi.resetAllMocks();
  mocks.steps = { profileComplete: false, workspaceCreate: false, workspaceJoin: false, workspaceInvite: false };
  mocks.update.mockResolvedValue(undefined);
  mocks.finish.mockResolvedValue(undefined);
});

describe("personal-first onboarding graph", () => {
  it("moves through profile, space and optional purpose before persisted completion", async () => {
    render(<OnboardingRoot />);
    expect(screen.getByText(EOnboardingSteps.PROFILE_SETUP)).toBeInTheDocument();
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: "Continue" }));
    });
    expect(mocks.update).toHaveBeenCalledWith({ onboardingStep: { ...mocks.steps, profileComplete: true } });
    expect(screen.getByText(EOnboardingSteps.WORKSPACE_CREATE_OR_JOIN)).toBeInTheDocument();
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: "Continue" }));
    });
    expect(screen.getByText(EOnboardingSteps.USE_CASE_SETUP)).toBeInTheDocument();
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: "Continue" }));
    });
    expect(mocks.finish).toHaveBeenCalledOnce();
    expect(screen.queryByText(EOnboardingSteps.ROLE_SETUP)).not.toBeInTheDocument();
    expect(screen.queryByText(EOnboardingSteps.INVITE_MEMBERS)).not.toBeInTheDocument();
  });

  it("resumes a legacy profile-complete user at the space step", () => {
    mocks.steps.profileComplete = true;
    mocks.steps.workspaceCreate = true;
    render(<OnboardingRoot />);
    expect(screen.getByText(EOnboardingSteps.WORKSPACE_CREATE_OR_JOIN)).toBeInTheDocument();
  });

  it("does not advance when profile persistence fails", async () => {
    mocks.update.mockRejectedValueOnce(new Error("offline"));
    render(<OnboardingRoot />);
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: "Continue" }));
    });
    expect(screen.getByText(EOnboardingSteps.PROFILE_SETUP)).toBeInTheDocument();
    expect(screen.getByRole("alert")).toHaveTextContent("Could not save onboarding.");
    expect(mocks.finish).not.toHaveBeenCalled();
  });

  it("leaves failed completion retryable at purpose", async () => {
    mocks.steps.profileComplete = true;
    mocks.finish.mockRejectedValueOnce(new Error("offline"));
    render(<OnboardingRoot />);
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: "Continue" }));
    });
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: "Continue" }));
    });
    expect(screen.getByText(EOnboardingSteps.USE_CASE_SETUP)).toBeInTheDocument();
    expect(screen.getByRole("alert")).toHaveTextContent("Could not save onboarding.");
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: "Continue" }));
    });
    expect(mocks.finish).toHaveBeenCalledTimes(2);
  });
});
