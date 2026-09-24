/** @vitest-environment jsdom */

import { beforeEach, describe, expect, it, vi } from "vitest";
import { act, fireEvent, render, screen } from "@testing-library/react";
import { AdminSidebarDropdown } from "./sidebar-dropdown";
import type { ReactNode } from "react";

const mocks = vi.hoisted(() => ({
  signOut: vi.fn<() => Promise<void>>(),
  setTheme: vi.fn<(theme: string) => void>(),
}));
vi.mock("next-themes", () => ({ useTheme: () => ({ resolvedTheme: "light", setTheme: mocks.setTheme }) }));
vi.mock("@/hooks/store/use-app-theme", () => ({ useAppTheme: () => ({ isSidebarCollapsed: false }) }));
vi.mock("@/hooks/store/use-user", () => ({
  useUser: () => ({ currentUser: { firstName: "Test", lastName: "Admin" }, signOut: mocks.signOut }),
}));
vi.mock("@syncturtle/constants", () => ({ ADMIN_BASE_PATH: "/god-mode" }));
vi.mock("@syncturtle/ui", () => ({
  cn: (...values: unknown[]) => values.filter((value) => typeof value === "string").join(" "),
}));
// Keep the actual logout handler; replace only menu presentation with accessible
// DOM primitives so these tests exercise app orchestration rather than HeroUI internals.
vi.mock("@heroui/react", () => {
  const Container = ({ children }: { children?: ReactNode }) => <div>{children}</div>;
  const Item = ({
    children,
    href,
    onPress,
    isDisabled,
  }: {
    children?: ReactNode;
    href?: string;
    onPress?: () => void;
    isDisabled?: boolean;
  }) =>
    href ? (
      <a href={href}>{children}</a>
    ) : (
      <button type="button" onClick={onPress} disabled={isDisabled}>
        {children}
      </button>
    );
  return {
    Dropdown: Object.assign(Container, { Popover: Container, Menu: Container, Item, Trigger: Container }),
    Avatar: Object.assign(Container, { Image: () => null, Fallback: Container }),
    Label: Container,
  };
});

function deferred() {
  let resolve!: () => void;
  let reject!: (reason: unknown) => void;
  const promise = new Promise<void>((done, fail) => {
    resolve = done;
    reject = fail;
  });
  return { promise, resolve, reject };
}

beforeEach(() => {
  vi.clearAllMocks();
  mocks.signOut.mockReset().mockResolvedValue(undefined);
});

describe("existing administrator logout entry", () => {
  it("links to sessions from the personal menu", () => {
    render(<AdminSidebarDropdown />);
    expect(screen.getByRole("link", { name: "Sessions" })).toHaveAttribute("href", "/god-mode/sessions/");
  });

  it("uses one store logout action and contains no obsolete native form", async () => {
    const { container } = render(<AdminSidebarDropdown />);
    expect(container.querySelector("form")).toBeNull();
    expect(container.querySelector('input[name="csrfmiddlewaretoken"]')).toBeNull();
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: /Log Out/ }));
    });
    expect(mocks.signOut).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole("alert")).toBeNull();
  });

  it("disables duplicate logout while confirmation is pending", async () => {
    const pending = deferred();
    mocks.signOut.mockReturnValueOnce(pending.promise);
    render(<AdminSidebarDropdown />);
    fireEvent.click(screen.getByRole("button", { name: /Log Out/ }));
    const button = screen.getByRole("button", { name: /Signing out/ });
    expect(button).toBeDisabled();
    fireEvent.click(button);
    expect(mocks.signOut).toHaveBeenCalledTimes(1);
    await act(async () => {
      pending.resolve();
    });
  });

  it("shows a safe error and permits a deliberate retry after a failed logout", async () => {
    const pending = deferred();
    mocks.signOut.mockReturnValueOnce(pending.promise);
    render(<AdminSidebarDropdown />);
    fireEvent.click(screen.getByRole("button", { name: /Log Out/ }));
    await act(async () => {
      pending.reject(new Error("private upstream detail"));
    });
    expect(screen.getByRole("alert")).toHaveTextContent("Unable to confirm sign out. Please try again.");
    expect(screen.queryByText(/private upstream detail/)).toBeNull();
    expect(mocks.signOut).toHaveBeenCalledTimes(1);
    const retry = screen.getByRole("button", { name: /Log Out/ });
    expect(retry).toBeEnabled();
    await act(async () => {
      fireEvent.click(retry);
    });
    expect(mocks.signOut).toHaveBeenCalledTimes(2);
    expect(screen.queryByRole("alert")).toBeNull();
  });
});
