/** @vitest-environment jsdom */

import type { ReactNode } from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { EOnboardingSteps } from "@syncturtle/constants";
import { UseCaseSetupStep } from "./root";

vi.mock("../common", () => ({ CommonOnboardingHeader: ({ title }: { title: string }) => <h1>{title}</h1> }));
vi.mock("@heroui/react", () => ({
  Spinner: () => <span>Saving</span>,
  Button: ({ children, onPress, isDisabled }: { children: ReactNode; onPress: () => void; isDisabled: boolean }) => (
    <button type="button" onClick={onPress} disabled={isDisabled}>
      {children}
    </button>
  ),
}));

describe("optional purpose", () => {
  it("completes with no purpose and does not save profile metadata", async () => {
    const finish = vi.fn().mockResolvedValue(undefined);
    render(<UseCaseSetupStep handleStepChange={finish} />);
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: "Skip and open my space" }));
    });
    expect(finish).toHaveBeenCalledWith(EOnboardingSteps.USE_CASE_SETUP);
  });

  it("offers personal purposes and leaves failed completion retryable", async () => {
    const finish = vi.fn().mockRejectedValueOnce(new Error("offline")).mockResolvedValueOnce(undefined);
    render(<UseCaseSetupStep handleStepChange={finish} />);
    fireEvent.click(screen.getByRole("button", { name: "Fitness" }));
    expect(screen.queryByText(/engineering sprint|team size/i)).not.toBeInTheDocument();
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: "Open my space" }));
    });
    expect(screen.getByRole("alert")).toHaveTextContent("Could not finish onboarding.");
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: "Open my space" }));
    });
    expect(finish).toHaveBeenCalledTimes(2);
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });
});
