/** @vitest-environment jsdom */

import type { ReactNode, FormEventHandler } from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { IUser } from "@syncturtle/types";
import { WorkspaceCreateStep } from "./create";

const mocks = vi.hoisted(() => ({ slugCheck: vi.fn(), create: vi.fn() }));
vi.mock("@/hooks/store/use-workspace", () => ({ useWorkspace: () => ({ createWorkspace: mocks.create }) }));
vi.mock("@/services/workspace.service", () => ({
  WorkspaceService: class {
    workspaceSlugCheck = mocks.slugCheck;
  },
}));
vi.mock("../common", () => ({ CommonOnboardingHeader: ({ title }: { title: string }) => <h1>{title}</h1> }));
vi.mock("@heroui/react", () => ({
  Form: ({ children, onSubmit }: { children: ReactNode; onSubmit: FormEventHandler<HTMLFormElement> }) => (
    <form data-testid="space-form" onSubmit={onSubmit}>
      {children}
    </form>
  ),
  TextField: ({
    value,
    onChange,
    isDisabled,
  }: {
    value: string;
    onChange: (value: string) => void;
    isDisabled: boolean;
  }) => (
    <input
      aria-label="Space name"
      value={value}
      disabled={isDisabled}
      onChange={(event) => onChange(event.target.value)}
    />
  ),
  Input: () => null,
  Label: () => null,
  FieldError: ({ children }: { children: ReactNode }) => <>{children}</>,
  Button: ({ children, isDisabled }: { children: ReactNode; isDisabled: boolean }) => (
    <button type="submit" disabled={isDisabled}>
      {children}
    </button>
  ),
}));
const user = { id: "user-1" } as IUser;
beforeEach(() => {
  vi.resetAllMocks();
  mocks.slugCheck.mockResolvedValue({ available: true });
  mocks.create.mockResolvedValue({ id: "space-1", slug: "personal-1234", name: "Personal" });
});

describe("solo space creation", () => {
  it("prefills Personal, hides company/slug fields, and creates without organization size", async () => {
    const next = vi.fn().mockResolvedValue(undefined);
    render(<WorkspaceCreateStep user={user} onComplete={next} />);
    expect(screen.getByRole("textbox", { name: "Space name" })).toHaveValue("Personal");
    expect(screen.queryByText(/organization|company size/i)).not.toBeInTheDocument();
    expect(screen.getAllByRole("textbox")).toHaveLength(1);
    await act(async () => {
      fireEvent.submit(screen.getByTestId("space-form"));
    });
    expect(mocks.create).toHaveBeenCalledWith({
      name: "Personal",
      slug: expect.stringMatching(/^personal-[a-f0-9]{8}$/),
    });
    expect(next).toHaveBeenCalledWith(expect.objectContaining({ id: "space-1" }));
  });

  it("allows an edited name and checks an alternative after a slug collision", async () => {
    mocks.slugCheck.mockResolvedValueOnce({ available: false }).mockResolvedValueOnce({ available: true });
    render(<WorkspaceCreateStep user={user} onComplete={vi.fn().mockResolvedValue(undefined)} />);
    fireEvent.change(screen.getByRole("textbox", { name: "Space name" }), { target: { value: "Family plans" } });
    await act(async () => {
      fireEvent.submit(screen.getByTestId("space-form"));
    });
    expect(mocks.slugCheck).toHaveBeenCalledTimes(2);
    expect(mocks.create).toHaveBeenCalledWith({
      name: "Family plans",
      slug: expect.stringMatching(/^family-plans-[a-f0-9]{8}$/),
    });
  });

  it("retries selection after successful POST without creating a duplicate", async () => {
    const next = vi.fn().mockRejectedValueOnce(new Error("settings unavailable")).mockResolvedValueOnce(undefined);
    render(<WorkspaceCreateStep user={user} onComplete={next} />);
    await act(async () => {
      fireEvent.submit(screen.getByTestId("space-form"));
    });
    expect(screen.getByRole("alert")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Continue with created space" })).toBeInTheDocument();
    await act(async () => {
      fireEvent.submit(screen.getByTestId("space-form"));
    });
    expect(mocks.create).toHaveBeenCalledTimes(1);
    expect(next).toHaveBeenCalledTimes(2);
  });

  it("awaits continuation and prevents another POST while it is pending", async () => {
    let resolve!: () => void;
    const next = vi.fn(
      () =>
        new Promise<void>((done) => {
          resolve = done;
        })
    );
    const busy = vi.fn();
    render(<WorkspaceCreateStep user={user} onComplete={next} onBusyChange={busy} />);
    await act(async () => {
      fireEvent.submit(screen.getByTestId("space-form"));
    });
    expect(busy).toHaveBeenLastCalledWith(true);
    fireEvent.submit(screen.getByTestId("space-form"));
    expect(mocks.create).toHaveBeenCalledTimes(1);
    await act(async () => {
      resolve();
    });
    expect(busy).toHaveBeenLastCalledWith(false);
  });
});
