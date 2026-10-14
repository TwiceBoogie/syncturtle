/** @vitest-environment jsdom */

import { fireEvent, render, screen } from "@testing-library/react";
import { act } from "react";
import { hydrateRoot } from "react-dom/client";
import { renderToString } from "react-dom/server";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useAppRail } from "@/hooks/use-app-rail";
import { AppRailProvider } from "./app-rail-context";

const route = vi.hoisted(() => ({ workspaceSlug: "workspace-a" }));

vi.mock("next/navigation", () => ({
  useParams: () => ({ workspaceSlug: route.workspaceSlug }),
}));

const StateReader = () => {
  const { editionAvailable, canUseAppRail, isCollapsed, shouldRenderAppRail, toggleAppRail } = useAppRail();
  return (
    <div>
      <span data-testid="state">
        {String(editionAvailable)}:{String(canUseAppRail)}:{String(isCollapsed)}:{String(shouldRenderAppRail)}
      </span>
      <button type="button" onClick={toggleAppRail}>
        Toggle
      </button>
    </div>
  );
};

beforeEach(() => {
  route.workspaceSlug = "workspace-a";
  window.localStorage.clear();
});

describe("AppRailProvider", () => {
  it("keeps collapse independent between workspaces and ignores the old visible-polarity key", () => {
    window.localStorage.setItem("APP_RAIL_workspace-a", "true");
    window.localStorage.setItem("APP_RAIL_COLLAPSED_workspace-b", "false");
    const view = render(
      <AppRailProvider editionAvailable>
        <StateReader />
      </AppRailProvider>
    );

    expect(screen.getByTestId("state").textContent).toBe("true:true:false:true");
    fireEvent.click(screen.getByRole("button", { name: "Toggle" }));
    expect(window.localStorage.getItem("APP_RAIL_COLLAPSED_workspace-a")).toBe("true");
    expect(screen.getByTestId("state").textContent).toBe("true:true:true:false");

    route.workspaceSlug = "workspace-b";
    view.rerender(
      <AppRailProvider editionAvailable>
        <StateReader />
      </AppRailProvider>
    );
    expect(screen.getByTestId("state").textContent).toBe("true:true:false:true");

    fireEvent.click(screen.getByRole("button", { name: "Toggle" }));
    expect(window.localStorage.getItem("APP_RAIL_COLLAPSED_workspace-b")).toBe("true");
    expect(window.localStorage.getItem("APP_RAIL_COLLAPSED_workspace-a")).toBe("true");

    route.workspaceSlug = "workspace-a";
    view.rerender(
      <AppRailProvider editionAvailable>
        <StateReader />
      </AppRailProvider>
    );
    expect(screen.getByTestId("state").textContent).toBe("true:true:true:false");
  });

  it("does not allow Community to toggle or render the rail", () => {
    render(
      <AppRailProvider editionAvailable={false}>
        <StateReader />
      </AppRailProvider>
    );
    expect(screen.getByTestId("state").textContent).toBe("false:false:false:false");
    fireEvent.click(screen.getByRole("button", { name: "Toggle" }));
    expect(window.localStorage.getItem("APP_RAIL_COLLAPSED_workspace-a")).toBeNull();
  });

  it("hydrates from the deterministic server snapshot before reading the saved preference", async () => {
    window.localStorage.setItem("APP_RAIL_COLLAPSED_workspace-a", "true");
    const element = (
      <AppRailProvider editionAvailable>
        <StateReader />
      </AppRailProvider>
    );
    const container = document.createElement("div");
    container.innerHTML = renderToString(element);
    document.body.appendChild(container);
    expect(container.querySelector('[data-testid="state"]')?.textContent).toBe("true:true:false:true");

    const consoleError = vi.spyOn(console, "error").mockImplementation(() => {});
    let root: ReturnType<typeof hydrateRoot> | undefined;
    try {
      await act(async () => {
        root = hydrateRoot(container, element);
      });
      expect(container.querySelector('[data-testid="state"]')?.textContent).toBe("true:true:true:false");
      expect(consoleError.mock.calls.some((call) => String(call[0]).includes("hydration"))).toBe(false);
    } finally {
      await act(async () => root?.unmount());
      consoleError.mockRestore();
      container.remove();
    }
  });
});
