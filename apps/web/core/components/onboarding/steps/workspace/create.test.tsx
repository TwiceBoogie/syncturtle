/** @vitest-environment jsdom */

import type { FormEventHandler, ReactNode } from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { IUser } from "@syncturtle/types";
import { WorkspaceCreateStep } from "./create";

const mocks = vi.hoisted(() => ({
  slugCheck: vi.fn(),
  create: vi.fn(),
  fetchWorkspaces: vi.fn(),
  updateProfile: vi.fn(),
  settings: vi.fn(),
}));
vi.mock("@/hooks/store/user", () => ({
  useUserProfile: () => ({ updateUserProfile: mocks.updateProfile }),
  useUserSettings: () => ({ fetchCurrentUserSettings: mocks.settings }),
}));
vi.mock("@/hooks/store/use-workspace", () => ({
  useWorkspace: () => ({ createWorkspace: mocks.create, fetchWorkspaces: mocks.fetchWorkspaces }),
}));
vi.mock("@/services/workspace.service", () => ({
  WorkspaceService: class {
    workspaceSlugCheck = mocks.slugCheck;
  },
}));
vi.mock("@syncturtle/i18n", () => ({ useTranslation: () => ({ t: (key: string) => key }) }));
vi.mock("../common", () => ({ CommonOnboardingHeader: () => null }));
vi.mock("@heroui/react", () => {
  const Content = ({ children }: { children: ReactNode }) => <>{children}</>;
  const Empty = () => null;
  return {
    Form: ({ children, onSubmit }: { children: ReactNode; onSubmit: FormEventHandler<HTMLFormElement> }) => (
      <form data-testid="workspace-form" onSubmit={onSubmit}>
        {children}
      </form>
    ),
    TextField: ({ id, value, onChange }: { id: string; value: string; onChange: (value: string) => void }) => (
      <input aria-label={id} value={value} onChange={(event) => onChange(event.target.value)} />
    ),
    RadioGroup: ({ value, onChange }: { value: string; onChange: (value: string) => void }) => (
      <select aria-label="Organization size" value={value} onChange={(event) => onChange(event.target.value)}>
        <option value="" />
        <option value="Just myself">Just myself</option>
      </select>
    ),
    Radio: Object.assign(Empty, { Content, Control: Content, Indicator: Empty }),
    InputGroup: Object.assign(Content, { Prefix: Content, Input: Empty }),
    Input: Empty,
    Label: Content,
    Description: Content,
    FieldError: Content,
    Spinner: Empty,
    Button: () => <button type="submit">Create workspace</button>,
    toast: { promise: vi.fn() },
  };
});

beforeEach(() => {
  vi.resetAllMocks();
  mocks.slugCheck.mockResolvedValue({ available: true });
  mocks.create.mockResolvedValue({ id: "workspace-1", slug: "turtles" });
  mocks.fetchWorkspaces.mockResolvedValue([]);
  mocks.updateProfile.mockResolvedValue(undefined);
});

describe("WorkspaceCreateStep", () => {
  it("publishes the workspace and selection, then awaits fresh settings before completing onboarding", async () => {
    let resolveSettings!: () => void;
    mocks.settings.mockReturnValue(
      new Promise<void>((resolve) => {
        resolveSettings = resolve;
      })
    );
    const onComplete = vi.fn();
    render(
      <WorkspaceCreateStep user={{ id: "user-1" } as IUser} onComplete={onComplete} handleCurrentViewChange={vi.fn()} />
    );
    fireEvent.change(screen.getByRole("textbox", { name: "name" }), { target: { value: "Turtles" } });
    fireEvent.change(screen.getByRole("combobox", { name: "Organization size" }), { target: { value: "Just myself" } });

    await act(async () => {
      fireEvent.submit(screen.getByTestId("workspace-form"));
    });

    expect(mocks.create).toHaveBeenCalledWith({ name: "Turtles", slug: "turtles", organizationSize: "Just myself" });
    expect(mocks.fetchWorkspaces).toHaveBeenCalledTimes(1);
    expect(mocks.updateProfile).toHaveBeenCalledWith({ lastWorkspaceId: "workspace-1" });
    expect(mocks.settings).toHaveBeenCalledExactlyOnceWith(true);
    expect(onComplete).not.toHaveBeenCalled();

    await act(async () => {
      resolveSettings();
    });

    expect(onComplete).toHaveBeenCalledExactlyOnceWith(true);
  });
});
