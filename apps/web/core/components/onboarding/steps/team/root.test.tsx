/** @vitest-environment jsdom */

import type { FormEventHandler, ReactNode } from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { EOnboardingSteps } from "@syncturtle/constants";
import type { IWorkspace } from "@syncturtle/types";
import { InviteTeamStep } from "./root";

const space = { id: "space-1", slug: "personal-1234", name: "Personal" } as IWorkspace;
const mocks = vi.hoisted(() => ({
  invite: vi.fn(),
  getWorkspaceById: vi.fn(),
  profileWorkspaceId: undefined as string | undefined,
  spaces: {} as Record<string, IWorkspace>,
}));
vi.mock("@/hooks/store/user", () => ({
  useUserProfile: () => ({ data: { lastWorkspaceId: mocks.profileWorkspaceId } }),
}));
vi.mock("@/hooks/store/use-workspace", () => ({
  useWorkspace: () => ({ getWorkspaceById: mocks.getWorkspaceById }),
}));
vi.mock("@/services/workspace.service", () => ({
  WorkspaceService: class {
    inviteWorkspace = mocks.invite;
  },
}));
vi.mock("@syncturtle/i18n", () => ({ useTranslation: () => ({ t: (value: string) => value }) }));
vi.mock("../common", () => ({ CommonOnboardingHeader: ({ title }: { title: string }) => <h1>{title}</h1> }));
vi.mock("@heroui/react", () => {
  const Button = ({
    children,
    onPress,
    isDisabled,
    type,
  }: {
    children: ReactNode | ((state: { isPending: boolean }) => ReactNode);
    onPress?: () => void;
    isDisabled?: boolean;
    type?: "button" | "submit";
  }) => (
    <button type={type ?? "button"} onClick={onPress} disabled={isDisabled}>
      {typeof children === "function" ? children({ isPending: false }) : children}
    </button>
  );
  const Select = Object.assign(({ children }: { children: ReactNode }) => <div>{children}</div>, {
    Trigger: ({ children }: { children: ReactNode }) => <div>{children}</div>,
    Value: () => null,
    Indicator: () => null,
    Popover: ({ children }: { children: ReactNode }) => <div>{children}</div>,
  });
  const ListBox = Object.assign(({ children }: { children: ReactNode }) => <div>{children}</div>, {
    Item: ({ children }: { children: ReactNode }) => <div>{children}</div>,
    ItemIndicator: () => null,
  });
  return {
    toast: { promise: vi.fn() },
    Button,
    Select,
    ListBox,
    Spinner: () => <span>Saving</span>,
    Form: ({ children, onSubmit }: { children: ReactNode; onSubmit: FormEventHandler<HTMLFormElement> }) => (
      <form onSubmit={onSubmit}>{children}</form>
    ),
    TextField: ({ name, value, onChange }: { name: string; value: string; onChange: (value: string) => void }) => (
      <input aria-label={name} value={value} onChange={(event) => onChange(event.target.value)} />
    ),
    Input: () => null,
    Label: () => null,
    FieldError: () => null,
  };
});

beforeEach(() => {
  vi.resetAllMocks();
  mocks.profileWorkspaceId = space.id;
  mocks.spaces = { [space.id]: space };
  mocks.getWorkspaceById.mockImplementation((id: string) => mocks.spaces[id] ?? null);
  mocks.invite.mockResolvedValue({ message: "queued" });
});

describe("outgoing Invite Members", () => {
  it("skips honestly without sending, then reports the Plane step callback", async () => {
    const next = vi.fn().mockResolvedValue(undefined);
    render(<InviteTeamStep handleStepChange={next} />);
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: "I'll do it later" }));
    });
    expect(mocks.invite).not.toHaveBeenCalled();
    expect(next).toHaveBeenCalledWith(EOnboardingSteps.INVITE_MEMBERS);
  });

  it("sends through the existing endpoint for the selected onboarding Space", async () => {
    const next = vi.fn().mockResolvedValue(undefined);
    render(<InviteTeamStep handleStepChange={next} />);
    fireEvent.change(screen.getByRole("textbox", { name: "emails.0.email" }), {
      target: { value: "alex@example.test" },
    });
    await act(async () => {
      fireEvent.submit(screen.getByRole("button", { name: "Continue" }).closest("form")!);
    });
    expect(mocks.invite).toHaveBeenCalledWith(space.slug, {
      emails: [{ email: "alex@example.test", role: 15 }],
    });
    expect(mocks.getWorkspaceById).toHaveBeenCalledWith(space.id);
    expect(next).toHaveBeenCalledWith(EOnboardingSteps.INVITE_MEMBERS);
  });

  it("uses lastWorkspaceId even when another Space appears first in the store", async () => {
    const selected = { id: "space-2", slug: "family", name: "Family" } as IWorkspace;
    mocks.spaces = {
      [space.id]: space,
      [selected.id]: selected,
    };
    mocks.profileWorkspaceId = selected.id;
    render(<InviteTeamStep handleStepChange={vi.fn()} />);
    fireEvent.change(screen.getByRole("textbox", { name: "emails.0.email" }), {
      target: { value: "alex@example.test" },
    });
    await act(async () => {
      fireEvent.submit(screen.getByRole("button", { name: "Continue" }).closest("form")!);
    });
    expect(mocks.getWorkspaceById).toHaveBeenCalledWith(selected.id);
    expect(mocks.invite).toHaveBeenCalledWith(selected.slug, expect.anything());
    expect(mocks.invite).not.toHaveBeenCalledWith(space.slug, expect.anything());
  });

  it("does not send when lastWorkspaceId cannot resolve an accessible Space", async () => {
    mocks.profileWorkspaceId = "missing-space";
    const next = vi.fn();
    render(<InviteTeamStep handleStepChange={next} />);
    fireEvent.change(screen.getByRole("textbox", { name: "emails.0.email" }), {
      target: { value: "alex@example.test" },
    });
    const continueButton = screen.getByRole("button", { name: "Continue" });
    expect(continueButton).toBeDisabled();
    await act(async () => {
      fireEvent.submit(continueButton.closest("form")!);
    });
    expect(mocks.invite).not.toHaveBeenCalled();
    expect(next).not.toHaveBeenCalled();
  });

  it("can resubmit after the root absorbs a finish failure", async () => {
    const next = vi.fn();
    render(<InviteTeamStep handleStepChange={next} />);
    fireEvent.change(screen.getByRole("textbox", { name: "emails.0.email" }), {
      target: { value: "alex@example.test" },
    });
    const form = screen.getByRole("button", { name: "Continue" }).closest("form")!;
    await act(async () => {
      fireEvent.submit(form);
    });
    await act(async () => {
      fireEvent.submit(form);
    });
    expect(mocks.invite).toHaveBeenCalledTimes(2);
    expect(next).toHaveBeenCalledTimes(2);
  });
});
