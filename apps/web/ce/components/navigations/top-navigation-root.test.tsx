/** @vitest-environment jsdom */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AppRailProvider } from "@/hooks/context/app-rail-context";
import { TopNavigationRoot } from "./top-navigation-root";

vi.mock("next/navigation", () => ({
  useParams: () => ({ workspaceSlug: "personal" }),
  usePathname: () => "/personal",
}));
vi.mock("@syncturtle/i18n", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

describe("TopNavigationRoot", () => {
  beforeEach(() => window.localStorage.clear());

  it("renders the workspace switcher and centered search in the canvas bar without nested buttons", () => {
    const { container } = render(
      <AppRailProvider editionAvailable>
        <TopNavigationRoot />
      </AppRailProvider>
    );

    const topBar = container.firstElementChild;
    expect(topBar?.className).toContain("bg-canvas");
    expect(topBar?.className).toContain("min-h-[40px]");
    expect(screen.getByRole("button", { name: /Switch workspace/ })).toBeTruthy();
    expect(screen.getByRole("button", { name: "Inbox" })).toBeTruthy();
    expect(screen.getByPlaceholderText("Search commands...")).toBeTruthy();
    expect(container.querySelector("button button")).toBeNull();
    expect(container.querySelector('[role="button"] button')).toBeNull();
    expect(screen.queryByRole("button", { name: "Open app menu" })).toBeNull();
  });

  it("composes the collapsed App Rail menu before the workspace switcher and docks from its menu", async () => {
    window.localStorage.setItem("APP_RAIL_COLLAPSED_personal", "true");
    const { container } = render(
      <AppRailProvider editionAvailable>
        <TopNavigationRoot />
      </AppRailProvider>
    );
    const trigger = screen.getByRole("button", { name: "Open app menu" });
    const switcher = screen.getByRole("button", { name: /Switch workspace/ });
    expect(trigger.compareDocumentPosition(switcher) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(trigger.className).toContain("size-[28px]");
    expect(trigger.parentElement?.className).toContain("gap-[6px]");

    fireEvent.click(trigger);
    expect(await screen.findByRole("menu", { name: "Open app menu" })).toBeTruthy();
    expect(screen.getByRole("menuitemradio", { name: "Work" }).getAttribute("href")).toBe("/personal");
    expect(screen.getByRole("menuitemradio", { name: "Wiki" }).hasAttribute("href")).toBe(false);
    expect(screen.getByRole("menuitemradio", { name: "AI" }).hasAttribute("href")).toBe(false);
    fireEvent.click(screen.getByRole("menuitem", { name: "Dock App Rail" }));
    await waitFor(() => expect(screen.queryByRole("button", { name: "Open app menu" })).toBeNull());
    expect(window.localStorage.getItem("APP_RAIL_COLLAPSED_personal")).toBe("false");
    expect(container.querySelector("[data-app-rail-restore-trigger]")).toBeNull();
  });

  it("shows no restore UI in Community even when a collapsed preference is stored", () => {
    window.localStorage.setItem("APP_RAIL_COLLAPSED_personal", "true");
    render(
      <AppRailProvider editionAvailable={false}>
        <TopNavigationRoot />
      </AppRailProvider>
    );
    expect(screen.queryByRole("button", { name: "Open app menu" })).toBeNull();
  });

  it("opens and dismisses the restore menu from the keyboard", async () => {
    window.localStorage.setItem("APP_RAIL_COLLAPSED_personal", "true");
    render(
      <AppRailProvider editionAvailable>
        <TopNavigationRoot />
      </AppRailProvider>
    );
    const trigger = screen.getByRole("button", { name: "Open app menu" });
    trigger.focus();
    fireEvent.keyDown(trigger, { key: "ArrowDown" });
    const menu = await screen.findByRole("menu");
    fireEvent.keyDown(menu, { key: "Escape" });
    await waitFor(() => expect(screen.queryByRole("menu")).toBeNull());
    expect(document.activeElement).toBe(trigger);
  });
});
