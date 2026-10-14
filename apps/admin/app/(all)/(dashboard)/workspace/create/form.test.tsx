/** @vitest-environment jsdom */

import type { FormEventHandler, ReactNode } from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { WorkspaceCreateForm } from "./form";

const mocks = vi.hoisted(() => ({ create: vi.fn(), push: vi.fn(), slug: vi.fn() }));
vi.mock("next/navigation", () => ({ useRouter: () => ({ push: mocks.push }) }));
vi.mock("@/hooks/store/use-workspace", () => ({ useWorkspace: () => ({ createWorkspace: mocks.create }) }));
vi.mock("@/services/workspace.service", () => ({
  WorkspaceService: class {
    slugCheck = mocks.slug;
  },
}));
vi.mock("@syncturtle/hooks", () => ({ useDebouncerValue: (value: string) => value }));
vi.mock("@heroui/react", () => ({
  toast: Object.assign(vi.fn(), { clear: vi.fn() }),
  Form: ({ children, onSubmit }: { children: ReactNode; onSubmit: FormEventHandler<HTMLFormElement> }) => (
    <form onSubmit={onSubmit}>{children}</form>
  ),
  TextField: ({
    children,
    value,
    name,
    onChange,
  }: {
    children: ReactNode;
    value: string;
    name: string;
    onChange: (value: string) => void;
  }) => (
    <div>
      {children}
      <input aria-label={name} value={value} onChange={(event) => onChange(event.target.value)} />
    </div>
  ),
  Input: () => null,
  InputGroup: Object.assign(({ children }: { children: ReactNode }) => <div>{children}</div>, {
    Prefix: ({ children }: { children: ReactNode }) => <span>{children}</span>,
    Input: () => null,
    Suffix: ({ children }: { children: ReactNode }) => <span>{children}</span>,
  }),
  Label: ({ children }: { children: ReactNode }) => <label>{children}</label>,
  FieldError: () => null,
  Button: ({ children }: { children: ReactNode }) => <button type="submit">{children}</button>,
}));

beforeEach(() => {
  vi.resetAllMocks();
  mocks.create.mockResolvedValue(undefined);
  mocks.slug.mockResolvedValue({ available: true });
});

describe("administrator workspace creation", () => {
  it("submits name and URL without requiring organization size", async () => {
    render(<WorkspaceCreateForm />);
    expect(screen.queryByText(/organization size/i)).not.toBeInTheDocument();
    fireEvent.change(screen.getByRole("textbox", { name: "name" }), { target: { value: "Family" } });
    fireEvent.change(screen.getByRole("textbox", { name: "slug" }), { target: { value: "family" } });
    await act(async () => {
      fireEvent.submit(screen.getByRole("button", { name: "Submit" }).closest("form")!);
    });
    expect(mocks.create).toHaveBeenCalledWith({ name: "Family", slug: "family" });
    expect(mocks.push).toHaveBeenCalledWith("/workspace");
  });
});
