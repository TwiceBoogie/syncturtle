/** @vitest-environment jsdom */

import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it, vi } from "vitest";
import { act, cleanup, render, screen } from "@testing-library/react";
import { StrictMode } from "react";
import type { BrowserSessionEvent } from "@syncturtle/utils";
import StoreWrapper from "./store-wrapper";
import { AuthService } from "../../services/auth.service";
import { APIService } from "../../services/api.service";

const mocks = vi.hoisted(() => ({
  transition: vi.fn<() => void>(),
  hydrate: vi.fn(),
  query: vi.fn(),
  language: vi.fn(),
}));
vi.mock("next/navigation", () => ({ useParams: () => null }));
vi.mock("@syncturtle/i18n", () => ({ useTranslation: () => ({ changeLanguage: mocks.language }) }));
vi.mock("@syncturtle/constants", () => ({ API_BASE_URL: "https://web.test" }));
vi.mock("@syncturtle/utils", () => import("../../../../../packages/utils/src/browser-session-coordinator"));
vi.mock("@/hooks/store/use-app-theme", () => ({ useAppTheme: () => ({ hydrateSidebarCollapsed: mocks.hydrate }) }));
vi.mock("@/hooks/store/use-router-params", () => ({ useRouterParams: () => ({ setQuery: mocks.query }) }));
vi.mock("@/hooks/store/user", () => ({
  useUser: () => ({ transitionToSignedOut: mocks.transition }),
  useUserProfile: () => ({ data: undefined }),
}));

// Keep the actual AuthService/APIService/coordinator chain. Only external browser
// transport and the surrounding providers' hooks are fixtures.
let receive: ((event: { data: unknown }) => void) | null = null;
let posted: BrowserSessionEvent[] = [];
let fetchMock: ReturnType<typeof vi.fn<typeof fetch>>;

function notice(kind: BrowserSessionEvent["kind"] = "AUTH_STATE_CHANGED", generation = 0) {
  expect(receive).not.toBeNull();
  receive?.({
    data: {
      version: 1,
      kind,
      generation,
      runtimeId: "00000000-0000-4000-8000-000000000002",
      timestamp: 1,
    },
  });
}

function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((done) => {
    resolve = done;
  });
  return { promise, resolve };
}

class ProtectedService extends APIService {
  constructor() {
    super("https://web.test");
  }
  read() {
    return this.get("/protected", { authRefresh: true });
  }
}

beforeAll(() => {
  vi.spyOn(window.crypto, "randomUUID").mockReturnValue("00000000-0000-4000-8000-000000000001");
  vi.stubGlobal(
    "BroadcastChannel",
    class {
      set onmessage(listener: typeof receive) {
        receive = listener;
      }
      postMessage(event: BrowserSessionEvent) {
        posted.push(event);
      }
      close() {}
    }
  );
});

beforeEach(() => {
  vi.clearAllMocks();
  posted = [];
  fetchMock = vi.fn<typeof fetch>();
  vi.stubGlobal("fetch", fetchMock);
});
afterEach(() => {
  cleanup();
});
afterAll(() => {
  window.dispatchEvent(new PageTransitionEvent("pagehide", { persisted: false }));
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});

describe("web wrapper authentication subscription", () => {
  it("renders children, subscribes once across rerenders, and cleans up on unmount/remount", async () => {
    const subscribe = AuthService.prototype.subscribeAuthentication;
    const unsubscribes: Array<ReturnType<typeof vi.fn<() => void>>> = [];
    const subscription = vi.spyOn(AuthService.prototype, "subscribeAuthentication").mockImplementation(function (
      this: AuthService,
      signedOut: () => void
    ) {
      const unsubscribe = vi.fn(subscribe.call(this, signedOut));
      unsubscribes.push(unsubscribe);
      return unsubscribe;
    });

    const view = render(
      <StoreWrapper>
        <main>first</main>
      </StoreWrapper>
    );
    expect(screen.getByText("first")).toBeInTheDocument();
    expect(subscription).toHaveBeenCalledExactlyOnceWith(mocks.transition);
    view.rerender(
      <StoreWrapper>
        <main>second</main>
      </StoreWrapper>
    );
    expect(screen.getByText("second")).toBeInTheDocument();
    expect(subscription).toHaveBeenCalledTimes(1);
    view.unmount();
    expect(unsubscribes[0]).toHaveBeenCalledTimes(1);
    await act(async () => {
      notice();
    });
    expect(fetchMock).not.toHaveBeenCalled();

    const remount = render(<StoreWrapper>third</StoreWrapper>);
    expect(subscription).toHaveBeenCalledTimes(2);
    remount.unmount();
    expect(unsubscribes[1]).toHaveBeenCalledTimes(1);
    subscription.mockRestore();
  });

  it.each([200, 401, 503, "network"] as const)("authoritatively handles an advisory with %s", async (result) => {
    render(
      <StoreWrapper>
        <main>account</main>
      </StoreWrapper>
    );
    if (result === "network") fetchMock.mockRejectedValueOnce(new TypeError("offline"));
    else fetchMock.mockResolvedValueOnce(new Response(null, { status: result }));

    await act(async () => {
      notice();
    });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, config] = fetchMock.mock.calls[0]!;
    expect(url).toBe("https://web.test/api/users/me");
    expect(config?.method).toBe("GET");
    expect(new Headers(config?.headers).has("X-CSRF-Token")).toBe(false);
    expect(mocks.transition).toHaveBeenCalledTimes(result === 401 ? 1 : 0);
    expect(posted).toEqual([]);
  });

  it("coalesces duplicate notices under StrictMode and removes the active listener", async () => {
    const view = render(
      <StrictMode>
        <StoreWrapper>account</StoreWrapper>
      </StrictMode>
    );
    const probe = deferred<Response>();
    fetchMock.mockReturnValueOnce(probe.promise);
    await act(async () => {
      notice();
      notice();
    });
    expect(fetchMock).toHaveBeenCalledTimes(1);
    await act(async () => {
      probe.resolve(new Response(null, { status: 401 }));
    });
    expect(mocks.transition).toHaveBeenCalledTimes(1);
    view.unmount();
    await act(async () => {
      notice();
    });
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it.each(["generation", "protected-success"])("does not clear newer valid state after %s", async (progress) => {
    render(<StoreWrapper>account</StoreWrapper>);
    const probe = deferred<Response>();
    fetchMock.mockReturnValueOnce(probe.promise);
    await act(async () => {
      notice();
    });
    expect(fetchMock).toHaveBeenCalledTimes(1);

    await act(async () => {
      if (progress === "generation") notice("REFRESH_SUCCEEDED", 1);
      else {
        fetchMock.mockResolvedValueOnce(new Response(null, { status: 200 }));
        await new ProtectedService().read();
      }
      probe.resolve(new Response(null, { status: 401 }));
    });

    expect(mocks.transition).not.toHaveBeenCalled();
    expect(fetchMock).toHaveBeenCalledTimes(progress === "generation" ? 1 : 2);
    expect(posted).toEqual([]);
  });

  it("does not transition after unmount while a check is pending", async () => {
    const view = render(<StoreWrapper>account</StoreWrapper>);
    const probe = deferred<Response>();
    fetchMock.mockReturnValueOnce(probe.promise);
    await act(async () => {
      notice();
    });
    view.unmount();
    await act(async () => {
      probe.resolve(new Response(null, { status: 401 }));
    });
    expect(mocks.transition).not.toHaveBeenCalled();
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(posted).toEqual([]);
  });
});
