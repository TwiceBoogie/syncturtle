/** @vitest-environment jsdom */

import type { ReactNode } from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { EOnboardingSteps } from "@syncturtle/constants";
import { OnboardingHeader } from "./header";

vi.mock("next/image", () => ({ default: ({ alt }: { alt: string }) => <span role="img" aria-label={alt} /> }));
vi.mock("next-themes", () => ({ useTheme: () => ({ resolvedTheme: "light" }) }));
vi.mock("@/hooks/store/user", () => ({ useUser: () => ({ data: { displayName: "Owner" } }) }));
vi.mock("./tool-tip", () => ({
  Tooltip: ({ children, tooltipContent }: { children: ReactNode; tooltipContent: string }) => (
    <div data-testid="progress" aria-label={tooltipContent}>
      {children}
    </div>
  ),
}));
vi.mock("./switch-account-dropdown", () => ({ SwitchAccountDropdown: () => null }));

describe("header minus Role", () => {
  it("counts three steps when incoming invitations exist", () => {
    render(
      <OnboardingHeader
        currentStep={EOnboardingSteps.WORKSPACE_CREATE_OR_JOIN}
        updateCurrentStep={vi.fn()}
        hasInvitations
      />
    );
    expect(screen.getByTestId("progress")).toHaveAttribute("aria-label", "3/3");
  });

  it("missing Invite index after choosing Create despite invitations", () => {
    render(
      <OnboardingHeader currentStep={EOnboardingSteps.INVITE_MEMBERS} updateCurrentStep={vi.fn()} hasInvitations />
    );
    expect(screen.getByTestId("progress")).toHaveAttribute("aria-label", "0/3");
  });

  it("goes Back from Workspace to Use Case", () => {
    const update = vi.fn();
    render(
      <OnboardingHeader
        currentStep={EOnboardingSteps.WORKSPACE_CREATE_OR_JOIN}
        updateCurrentStep={update}
        hasInvitations={false}
      />
    );
    fireEvent.click(screen.getByRole("button"));
    expect(update).toHaveBeenCalledWith(EOnboardingSteps.USE_CASE_SETUP);
  });
});
