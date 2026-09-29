/** @vitest-environment jsdom */

import { createRequire } from "node:module";
import { createElement, type ComponentType } from "react";
import { render } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import WorkspaceDashboardPage from "../../../app/(all)/[workspaceSlug]/(main)/page";

// Exercise the installed Next development logger and its searchParams proxy.
const require = createRequire(import.meta.url);
const { createRenderSearchParamsFromClient } = require("next/dist/client/request/search-params.browser.dev.js");
const {
  preLogSerializationClone,
  safeStringifyWithDepth,
} = require("next/dist/next-devtools/userspace/app/forward-logs-utils.js");

afterEach(() => vi.restoreAllMocks());

describe("dashboard header development logging", () => {
  it("reproduces the warning when Next serializes a logged React element with a client page owner", () => {
    const error = vi.spyOn(console, "error").mockImplementation(() => {});
    vi.spyOn(console, "log").mockImplementation((...args) => {
      for (const arg of args) safeStringifyWithDepth(arg);
    });
    const searchParams = createRenderSearchParamsFromClient({ view: "dashboard" });
    // Next injects these props even when a Client Page does not read them.
    function DiagnosticPage(_props: { searchParams: Promise<{ view: string }> }) {
      const header = <div>hi</div>;
      console.log(header);
      return header;
    }

    render(<DiagnosticPage searchParams={searchParams} />);

    expect(error).toHaveBeenCalledWith(expect.stringContaining("The keys of `searchParams` were accessed directly."));
  });

  it("does not enumerate the page's Promise-backed searchParams through React element owners", () => {
    const error = vi.spyOn(console, "error").mockImplementation(() => {});
    const log = vi.spyOn(console, "log").mockImplementation((...args) => {
      for (const arg of args) {
        safeStringifyWithDepth(arg);
        preLogSerializationClone(arg);
      }
    });
    const searchParams = createRenderSearchParamsFromClient({ view: "dashboard" });
    const Page = WorkspaceDashboardPage as ComponentType<{ searchParams: Promise<{ view: string }> }>;

    render(createElement(Page, { searchParams }));

    expect(error).not.toHaveBeenCalled();
    expect(log).not.toHaveBeenCalled();
  });
});
