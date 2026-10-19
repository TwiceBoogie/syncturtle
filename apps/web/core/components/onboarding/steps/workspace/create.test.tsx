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
  fetchSettings: vi.fn(),
  toastPromise: vi.fn(),
}));

vi.mock("@/hooks/store/use-workspace", () => ({
  useWorkspace: () => ({ createWorkspace: mocks.create, fetchWorkspaces: mocks.fetchWorkspaces }),
}));
vi.mock("@/hooks/store/user", () => ({
  useUserProfile: () => ({ updateUserProfile: mocks.updateProfile }),
  useUserSettings: () => ({ fetchCurrentUserSettings: mocks.fetchSettings }),
}));
vi.mock("@/services/workspace.service", () => ({
  WorkspaceService: class {
    workspaceSlugCheck = mocks.slugCheck;
  },
}));
vi.mock("@syncturtle/i18n", () => ({ useTranslation: () => ({ t: (key: string) => key }) }));
vi.mock("../common", () => ({
  CommonOnboardingHeader: ({ title }: { title: string }) => <h1>{title}</h1>,
}));
vi.mock("@heroui/react", async () => {
  const { createContext, useContext } = await import("react");
  const RadioContext = createContext<(value: string) => void>(() => {});
  const Button = ({
    children,
    type,
    onPress,
    isDisabled,
  }: {
    children: ReactNode | ((state: { isPending: boolean }) => ReactNode);
    type?: "button" | "submit";
    onPress?: () => void;
    isDisabled?: boolean;
  }) => (
    <button type={type ?? "button"} onClick={onPress} disabled={isDisabled}>
      {typeof children === "function" ? children({ isPending: false }) : children}
    </button>
  );
  const Radio = Object.assign(
    ({ value }: { value: string }) => {
      const choose = useContext(RadioContext);
      return (
        <button type="button" onClick={() => choose(value)}>
          {value}
        </button>
      );
    },
    { Content: () => null, Control: () => null, Indicator: () => null }
  );
  const InputGroup = Object.assign(({ children }: { children: ReactNode }) => <div>{children}</div>, {
    Prefix: ({ children }: { children: ReactNode }) => <span>{children}</span>,
    Input: () => null,
  });
  return {
    toast: { promise: mocks.toastPromise },
    Form: ({ children, onSubmit }: { children: ReactNode; onSubmit: FormEventHandler<HTMLFormElement> }) => (
      <form data-testid="workspace-create-form" onSubmit={onSubmit}>
        {children}
      </form>
    ),
    TextField: ({
      name,
      value,
      onChange,
      children,
    }: {
      name: string;
      value: string;
      onChange: (value: string) => void;
      children: ReactNode;
    }) => (
      <div>
        <input aria-label={name} value={value} onChange={(event) => onChange(event.target.value)} />
        {children}
      </div>
    ),
    Input: () => null,
    InputGroup,
    Label: () => null,
    Description: () => null,
    FieldError: ({ children }: { children: ReactNode }) => <>{children}</>,
    RadioGroup: ({ children, onChange }: { children: ReactNode; onChange: (value: string) => void }) => (
      <RadioContext.Provider value={onChange}>{children}</RadioContext.Provider>
    ),
    Radio,
    Button,
    Spinner: () => <span>Saving</span>,
  };
});

const user = { id: "user-1" } as IUser;
const workspace = { id: "space-1", name: "Family plans", slug: "family-plans", organizationSize: "Just myself" };

beforeEach(() => {
  vi.resetAllMocks();
  mocks.slugCheck.mockResolvedValue({ available: true, message: "" });
  mocks.create.mockResolvedValue(workspace);
  mocks.fetchWorkspaces.mockResolvedValue([workspace]);
  mocks.updateProfile.mockResolvedValue(undefined);
  mocks.fetchSettings.mockResolvedValue(undefined);
});

const submit = async () => {
  await act(async () => {
    fireEvent.submit(screen.getByTestId("workspace-create-form"));
  });
};

describe("workspace creation", () => {
  it("sends organization size and persists the exact created Space destination", async () => {
    const complete = vi.fn();
    render(<WorkspaceCreateStep user={user} onComplete={complete} handleCurrentViewChange={vi.fn()} />);
    fireEvent.change(screen.getByRole("textbox", { name: "name" }), {
      target: { value: "Family plans" },
    });
    expect(screen.getByRole("textbox", { name: "slug" })).toHaveValue("family-plans");
    fireEvent.click(screen.getByRole("button", { name: "Just myself" }));
    await submit();

    expect(mocks.slugCheck).toHaveBeenCalledWith("family-plans");
    expect(mocks.create).toHaveBeenCalledWith({
      name: "Family plans",
      slug: "family-plans",
      organizationSize: "Just myself",
    });
    expect(mocks.fetchWorkspaces).toHaveBeenCalledOnce();
    expect(mocks.updateProfile).toHaveBeenCalledWith({ lastWorkspaceId: workspace.id });
    expect(mocks.fetchSettings).toHaveBeenCalledWith(true);
    expect(complete).toHaveBeenCalledWith(true);
  });

  it("lets the owner edit a colliding slug before creation", async () => {
    mocks.slugCheck
      .mockResolvedValueOnce({ available: false, message: "taken" })
      .mockResolvedValueOnce({ available: true, message: "" });
    render(<WorkspaceCreateStep user={user} onComplete={vi.fn()} handleCurrentViewChange={vi.fn()} />);
    fireEvent.change(screen.getByRole("textbox", { name: "name" }), {
      target: { value: "Family plans" },
    });
    fireEvent.click(screen.getByRole("button", { name: "2-10" }));
    await submit();
    expect(mocks.create).not.toHaveBeenCalled();
    expect(screen.getByText("Workspace URL is already taken.")).toBeInTheDocument();
    fireEvent.change(screen.getByRole("textbox", { name: "slug" }), {
      target: { value: "family-new" },
    });
    await submit();
    expect(mocks.create).toHaveBeenCalledWith(
      expect.objectContaining({
        slug: "family-new",
        organizationSize: "2-10",
      })
    );
  });

  it("POST risk after a post-create profile failure", async () => {
    mocks.updateProfile.mockRejectedValueOnce(new Error("offline"));
    render(<WorkspaceCreateStep user={user} onComplete={vi.fn()} handleCurrentViewChange={vi.fn()} />);
    fireEvent.change(screen.getByRole("textbox", { name: "name" }), {
      target: { value: "Family plans" },
    });
    fireEvent.click(screen.getByRole("button", { name: "2-10" }));
    await submit();
    await submit();
    expect(mocks.create).toHaveBeenCalledTimes(2);
  });
});
