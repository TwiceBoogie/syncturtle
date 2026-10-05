/** @vitest-environment jsdom */

import type { ReactNode } from "react";
import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import WorkspaceInvitationsPage from "./page";
import AcceptPage from "./accept/page";

vi.mock("@/lib/wrappers", () => ({
  AuthenticationWrapper: ({ children }: { children: ReactNode }) => <>{children}</>,
}));
vi.mock("next/link", () => ({
  default: ({ children, href }: { children: ReactNode; href: string }) => <a href={href}>{children}</a>,
}));

describe("invitation routes before acceptance implementation", () => {
  it.each([WorkspaceInvitationsPage, AcceptPage])("states that acceptance is unavailable", (Page) => {
    render(<Page />);
    expect(screen.getByRole("heading", { name: "Invitation acceptance is not available yet" })).toBeInTheDocument();
    expect(screen.getByText(/pending invitation does not grant access/i)).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /accept|join/i })).not.toBeInTheDocument();
  });
});
