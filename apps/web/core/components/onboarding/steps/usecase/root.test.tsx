/** @vitest-environment jsdom */

import type { FormEventHandler, ReactNode } from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { EOnboardingSteps } from "@syncturtle/constants";
import { UseCaseSetupStep } from "./root";

const mocks = vi.hoisted(() => ({ update: vi.fn(), toast: vi.fn() }));
vi.mock("@/hooks/store/user", () => ({
  useUserProfile: () => ({ data: { id: "profile-1", useCase: "" }, updateUserProfile: mocks.update }),
}));
vi.mock("../common", () => ({ CommonOnboardingHeader: ({ title }: { title: string }) => <h1>{title}</h1> }));
vi.mock("@heroui/react", () => ({
  toast: mocks.toast,
  Form: ({ children, onSubmit }: { children: ReactNode; onSubmit: FormEventHandler<HTMLFormElement> }) => (
    <form onSubmit={onSubmit}>{children}</form>
  ),
  Button: ({
    children,
    type,
    onPress,
    isDisabled,
  }: {
    children: ReactNode;
    type?: "submit" | "button";
    onPress?: () => void;
    isDisabled?: boolean;
  }) => (
    <button type={type ?? "button"} onClick={onPress} disabled={isDisabled}>
      {children}
    </button>
  ),
}));

beforeEach(() => {
  vi.resetAllMocks();
  mocks.update.mockResolvedValue(undefined);
});

describe("Use Case translated as Purpose", () => {
  it("persists the chosen purpose and then calls the step callback", async () => {
    const next = vi.fn();
    render(<UseCaseSetupStep handleStepChange={next} />);
    fireEvent.click(screen.getByRole("button", { name: "Fitness" }));
    await act(async () => {
      fireEvent.submit(screen.getByRole("button", { name: "Continue" }).closest("form")!);
    });
    expect(mocks.update).toHaveBeenCalledWith({ useCase: "Fitness" });
    expect(next).toHaveBeenCalledWith(EOnboardingSteps.USE_CASE_SETUP);
  });

  it("skips without writing useCase", () => {
    const next = vi.fn();
    render(<UseCaseSetupStep handleStepChange={next} />);
    fireEvent.click(screen.getByRole("button", { name: "Skip" }));
    expect(mocks.update).not.toHaveBeenCalled();
    expect(next).toHaveBeenCalledWith(EOnboardingSteps.USE_CASE_SETUP);
  });

  it("advance after a caught personalization failure", async () => {
    mocks.update.mockRejectedValueOnce(new Error("offline"));
    const next = vi.fn();
    render(<UseCaseSetupStep handleStepChange={next} />);
    fireEvent.click(screen.getByRole("button", { name: "Fitness" }));
    await act(async () => {
      fireEvent.submit(screen.getByRole("button", { name: "Continue" }).closest("form")!);
    });
    expect(mocks.toast).toHaveBeenCalledWith("Profile setup failed", { description: "Please try again later." });
    expect(next).toHaveBeenCalledWith(EOnboardingSteps.USE_CASE_SETUP);
  });
});
