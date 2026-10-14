/** @vitest-environment jsdom */

import { render } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { APP_RAIL_AVAILABLE, AppRailRestoreMenu, AppRailRoot } from "./index";

describe("Community App Rail contract", () => {
  it("exports false availability and renders no App Rail UI", () => {
    const { container } = render(
      <>
        <AppRailRoot />
        <AppRailRestoreMenu />
      </>
    );
    expect(APP_RAIL_AVAILABLE).toBe(false);
    expect(container.childNodes).toHaveLength(0);
  });
});
