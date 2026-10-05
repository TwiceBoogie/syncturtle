/** @vitest-environment jsdom */

import type { ReactNode } from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { IWorkspace } from "@syncturtle/types";
import { EPageType } from "@/helpers/authentication.helper";
import CreateWorkspacePage from "./page";

const mocks = vi.hoisted(() => ({
  replace: vi.fn(),
  user: { id: "user-1" } as { id: string } | undefined,
  pageType: undefined as string | undefined,
}));
vi.mock("next/navigation", () => ({ useRouter: () => ({ replace: mocks.replace }) }));
vi.mock("@/hooks/store/user", () => ({ useUser: () => ({ data: mocks.user }) }));
vi.mock("@/lib/wrappers", () => ({
  AuthenticationWrapper: ({ children, pageType }: { children: ReactNode; pageType: string }) => {
    mocks.pageType = pageType;
    return <>{children}</>;
  },
}));
vi.mock("@/components/onboarding/steps/workspace", () => ({
  WorkspaceSetupStep: ({ onComplete }: { onComplete: (space: IWorkspace) => Promise<void> }) => (
    <button type="button" onClick={() => void onComplete({ id: "space-1", slug: "family" } as IWorkspace)}>
      Use selected space
    </button>
  ),
}));

beforeEach(() => {
  vi.resetAllMocks();
  mocks.user = { id: "user-1" };
  mocks.pageType = undefined;
});

describe("completed-user workspace destination", () => {
  it("uses the authenticated shared space flow and opens the selected space", async () => {
    render(<CreateWorkspacePage />);
    expect(mocks.pageType).toBe(EPageType.AUTHENTICATED);
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: "Use selected space" }));
    });
    expect(mocks.replace).toHaveBeenCalledWith("/family");
  });

  it("does not mount the space selector before the user is available", () => {
    mocks.user = undefined;
    render(<CreateWorkspacePage />);
    expect(screen.queryByRole("button", { name: "Use selected space" })).not.toBeInTheDocument();
  });
});
