/** @vitest-environment jsdom */

import type { ReactNode } from "react";
import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { InstanceSetupFrom } from "./setup-form";

vi.mock("next/navigation", () => ({ useSearchParams: () => new URLSearchParams() }));
vi.mock("@/services/auth.service", () => ({
  AuthService: class {
    requestCSRFToken = vi.fn().mockResolvedValue({ csrfToken: "test-csrf" });
  },
}));
vi.mock("../common/auth-header", () => ({ AuthHeader: () => null }));
vi.mock("../common/banner", () => ({ Banner: () => null }));
vi.mock("@heroui/react", () => {
  const Container = ({ children }: { children: ReactNode }) => <div>{children}</div>;
  return {
    Button: ({ children }: { children: ReactNode }) => (
      <button type="button">{typeof children === "function" ? "Continue" : children}</button>
    ),
    Checkbox: Object.assign(Container, { Control: Container, Indicator: Container, Content: Container }),
    Description: Container,
    FieldError: () => null,
    FieldGroup: Container,
    Fieldset: Object.assign(Container, { Legend: Container, Actions: Container }),
    Form: ({ children, action }: { children: ReactNode; action: string }) => <form action={action}>{children}</form>,
    Input: ({ type, name, value }: { type?: string; name?: string; value?: string }) =>
      type === "hidden" ? <input type="hidden" name={name} value={value} readOnly /> : null,
    InputGroup: Object.assign(Container, { Prefix: Container, Input: () => null, Suffix: Container }),
    Label: Container,
    Spinner: () => null,
    TextField: ({ children, name, value }: { children: ReactNode; name: string; value: string }) => (
      <div>
        {children}
        <input aria-label={name} value={value} readOnly />
      </div>
    ),
  };
});

describe("first administrator setup form", () => {
  it("posts a prefilled installation name through the existing admin signup endpoint", async () => {
    render(<InstanceSetupFrom />);
    expect(screen.getByText("Installation name")).toBeInTheDocument();
    expect(screen.getByText("Choose a strong password for this administrator.")).toBeInTheDocument();
    expect(screen.getByRole("textbox", { name: "companyName" })).toHaveValue("SyncTurtle");
    expect(screen.getByRole("textbox", { name: "companyName" }).closest("form")).toHaveAttribute(
      "action",
      expect.stringContaining("/api/instances/admins/sign-up")
    );
    await waitFor(() => expect(document.querySelector('input[name="csrfmiddlewaretoken"]')).toHaveValue("test-csrf"));
    expect(screen.queryByText(/workspace|organization size/i)).not.toBeInTheDocument();
  });
});
