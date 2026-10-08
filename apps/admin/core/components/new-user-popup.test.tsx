/** @vitest-environment jsdom */

import type { ReactNode } from "react";
import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { WEB_BASE_URL } from "@syncturtle/constants";
import { NewUserPopup } from "./new-user-popup";

vi.mock("@/hooks/store/use-app-theme", () => ({
  useAppTheme: () => ({ isNewUserPopup: true, toggleNewUserPopup: vi.fn() }),
}));
vi.mock("next/link", () => ({
  default: ({ href, children }: { href: string; children: ReactNode }) => <a href={href}>{children}</a>,
}));
vi.mock("@heroui/react", () => {
  const Content = ({ children }: { children: ReactNode }) => <div>{children}</div>;
  return {
    Card: Object.assign(Content, { Header: Content, Description: Content, Footer: Content }),
    Button: Content,
    CloseButton: () => null,
  };
});

describe("installation completion", () => {
  it("offers ordinary web onboarding instead of forced administrator workspace creation", () => {
    render(<NewUserPopup />);
    expect(screen.getByRole("link", { name: "Open SyncTurtle" })).toHaveAttribute("href", `${WEB_BASE_URL}/onboarding`);
    expect(screen.queryByText("Create workspace")).not.toBeInTheDocument();
  });
});
