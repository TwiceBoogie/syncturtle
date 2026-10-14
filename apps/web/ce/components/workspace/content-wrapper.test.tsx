/** @vitest-environment jsdom */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AppRailProvider } from "@/hooks/context/app-rail-context";
import { WorkspaceContentWrapper } from "./content-wrapper";

vi.mock("next/navigation", () => ({
  useParams: () => ({ workspaceSlug: "personal" }),
  usePathname: () => "/personal",
}));
vi.mock("@syncturtle/i18n", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

const renderShell = (editionAvailable: boolean) =>
  render(
    <AppRailProvider editionAvailable={editionAvailable}>
      <WorkspaceContentWrapper>
        <main>Workspace content</main>
      </WorkspaceContentWrapper>
    </AppRailProvider>
  );

beforeEach(() => window.localStorage.clear());

describe("WorkspaceContentWrapper", () => {
  it("unmounts the rail on Undock and restores it from the header menu with sensible focus", async () => {
    renderShell(true);
    expect(screen.getByTestId("app-rail")).toBeTruthy();
    expect(screen.getByTestId("workspace-content-inset").className).toContain("pl-0");

    fireEvent.contextMenu(screen.getByLabelText("App rail options"), { clientX: 24, clientY: 180 });
    fireEvent.click(await screen.findByRole("menuitem", { name: "Undock App Rail" }));
    await waitFor(() => expect(screen.queryByTestId("app-rail")).toBeNull());
    expect(screen.getByTestId("workspace-content-inset").className).toContain("pl-2");

    const trigger = screen.getByRole("button", { name: "Open app menu" });
    await waitFor(() => expect(document.activeElement).toBe(trigger));
    fireEvent.click(trigger);
    fireEvent.click(await screen.findByRole("menuitem", { name: "Dock App Rail" }));
    await waitFor(() => expect(screen.getByTestId("app-rail")).toBeTruthy());
    expect(screen.queryByRole("button", { name: "Open app menu" })).toBeNull();
    expect(screen.getByTestId("workspace-content-inset").className).toContain("pl-0");
    await waitFor(() => expect(document.activeElement).toBe(screen.getByRole("link", { name: "Work" })));
  });

  it("renders neither rail nor restore menu when the edition lacks App Rail", () => {
    renderShell(false);
    expect(screen.queryByTestId("app-rail")).toBeNull();
    expect(screen.queryByRole("button", { name: "Open app menu" })).toBeNull();
    expect(screen.getByTestId("workspace-content-inset").className).toContain("pl-2");
  });
});
