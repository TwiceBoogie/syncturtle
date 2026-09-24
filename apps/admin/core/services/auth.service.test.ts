/** @vitest-environment jsdom */

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { BrowserSessionEvent } from "@syncturtle/utils";

vi.mock("@syncturtle/constants", () => ({
  API_BASE_URL: "https://api.test",
  ADMIN_BASE_URL: "",
  ADMIN_BASE_PATH: "/god-mode",
}));
vi.mock("@syncturtle/utils", () => import("../../../../packages/utils/src/browser-session-coordinator"));

const csrf = { csrfToken: "fictional-session-csrf", scope: "SESSION", expiresAt: "2099-01-01T00:00:00Z" };
let fetchMock: ReturnType<typeof vi.fn<typeof fetch>>;
let submitted: HTMLFormElement[];
let started: ReturnType<typeof deferred<void>>;
let posted: BrowserSessionEvent[];

function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((done) => {
    resolve = done;
  });
  return { promise, resolve };
}

function frame() {
  const value = document.querySelector("iframe");
  if (!value) throw new Error("Expected pending logout frame");
  return value;
}

function landed(target: HTMLIFrameElement, url: string, readable = true) {
  // jsdom does not navigate native forms. Model only the browser-observable
  // destination/document; the action remains the real AuthService.signOut().
  Object.defineProperty(target, "contentWindow", {
    configurable: true,
    value: { location: { href: url } },
  });
  Object.defineProperty(target, "contentDocument", {
    configurable: true,
    value: readable ? { URL: url, documentElement: {} } : null,
  });
  target.dispatchEvent(new Event("load"));
}

function expectCleanedUp(deadlineExpected = true) {
  expect(document.querySelector("iframe")).toBeNull();
  expect(document.querySelector("form")).toBeNull();
  // jsdom queues storage-event timers when SESSION CSRF is cached/invalidated.
  // Track the logout deadline's handle instead of the unrelated global queue.
  const scheduled = vi.mocked(window.setTimeout).mock;
  const deadlines = scheduled.calls.filter(([, delay]) => delay === 15_000);
  expect(deadlines).toHaveLength(deadlineExpected ? 1 : 0);
  if (deadlineExpected) {
    const index = scheduled.calls.findIndex(([, delay]) => delay === 15_000);
    const result = scheduled.results[index];
    if (!result || result.type !== "return") throw new Error("Expected logout deadline handle");
    expect(window.clearTimeout).toHaveBeenCalledWith(result.value);
  }
  for (const form of submitted) {
    expect(form.querySelector("input")?.value).toBe("");
  }
}

beforeEach(() => {
  vi.resetModules();
  vi.useFakeTimers();
  vi.spyOn(window, "setTimeout");
  vi.spyOn(window, "clearTimeout");
  window.sessionStorage.clear();
  window.localStorage.clear();
  submitted = [];
  posted = [];
  started = deferred<void>();
  vi.spyOn(window.crypto, "randomUUID").mockReturnValue("00000000-0000-4000-8000-000000000001");
  vi.spyOn(HTMLFormElement.prototype, "submit").mockImplementation(function (this: HTMLFormElement) {
    submitted.push(this);
    started.resolve();
  });
  vi.stubGlobal(
    "BroadcastChannel",
    class {
      onmessage: ((event: { data: unknown }) => void) | null = null;
      postMessage(event: BrowserSessionEvent) {
        posted.push(event);
      }
      close() {}
    }
  );
  fetchMock = vi.fn<typeof fetch>().mockResolvedValue(new Response(JSON.stringify(csrf), { status: 200 }));
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => {
  window.dispatchEvent(new PageTransitionEvent("pagehide", { persisted: false }));
  document.body.replaceChildren();
  vi.useRealTimers();
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});

describe("administrator native logout confirmation", () => {
  it("submits one SESSION-protected ADMIN form and publishes only after the exact landing", async () => {
    const { AuthService } = await import("./auth.service");
    const auth = new AuthService();
    const pending = auth.signOut();
    await started.promise;
    expect(submitted).toHaveLength(1);
    const form = submitted[0]!;
    const target = frame();
    expect(form.action).toBe("https://api.test/auth/admin/sign-out");
    expect(form.method).toBe("post");
    expect(form.target).toBe(target.name);
    expect(form.querySelector('input[name="csrfmiddlewaretoken"]')?.getAttribute("type")).toBe("hidden");
    expect(form.querySelector("input")?.value).toBe(csrf.csrfToken);
    expect(target.getAttribute("sandbox")).toBe("allow-forms allow-same-origin");
    expect(target.hidden).toBe(true);
    expect(form.hidden).toBe(true);
    expect(posted.some((event) => event.kind === "AUTH_STATE_CHANGED")).toBe(false);
    target.dispatchEvent(new Event("load")); // initial about:blank is not success
    expect(posted.some((event) => event.kind === "AUTH_STATE_CHANGED")).toBe(false);
    landed(target, AuthService.signedOutDestination());
    await pending;
    expect(posted.filter((event) => event.kind === "AUTH_STATE_CHANGED")).toHaveLength(1);
    expect(window.sessionStorage.getItem("syncturtle.admin.csrf.session.v1")).toBeNull();
    expect(fetchMock).toHaveBeenCalledTimes(1); // CSRF issuance only; no fetch logout or refresh
    expect(String(fetchMock.mock.calls[0]?.[0])).toBe("https://api.test/api/get-csrf-token");
    expect(submitted).toHaveLength(1);
    for (const event of posted) {
      expect(Object.keys(event).sort()).toEqual(["generation", "kind", "runtimeId", "timestamp", "version"]);
      expect(JSON.stringify(event)).not.toContain(csrf.csrfToken);
      expect(JSON.stringify(event)).not.toContain("god-mode");
    }
    expectCleanedUp();
  });

  it.each(["/auth/admin/sign-out", "/god-mode/?error=failed", "/other/"])(
    "rejects a loaded error/unexpected destination %s without publishing",
    async (path) => {
      const { AuthService } = await import("./auth.service");
      const pending = new AuthService().signOut();
      const rejected = expect(pending).rejects.toThrow("logout was not confirmed");
      await started.promise;
      landed(frame(), new URL(path, window.location.origin).href);
      await rejected;
      expect(posted.some((event) => event.kind === "AUTH_STATE_CHANGED")).toBe(false);
      expect(submitted).toHaveLength(1);
      expect(fetchMock).toHaveBeenCalledTimes(1);
      expectCleanedUp();
    }
  );

  it("does not mistake an inaccessible cross-origin error response for the success redirect", async () => {
    const { AuthService } = await import("./auth.service");
    const pending = new AuthService().signOut();
    const rejected = expect(pending).rejects.toThrow("logout was not confirmed");
    await started.promise;
    const target = frame();
    Object.defineProperty(target, "contentWindow", {
      configurable: true,
      get: () => {
        throw new DOMException("cross origin", "SecurityError");
      },
    });
    target.dispatchEvent(new Event("load"));
    await rejected;
    expect(posted.some((event) => event.kind === "AUTH_STATE_CHANGED")).toBe(false);
    expect(submitted).toHaveLength(1);
    expectCleanedUp();
  });

  it("requires a readable committed document as well as the destination URL", async () => {
    const { AuthService } = await import("./auth.service");
    const pending = new AuthService().signOut();
    const rejected = expect(pending).rejects.toThrow("logout was not confirmed");
    await started.promise;
    landed(frame(), AuthService.signedOutDestination(), false);
    await rejected;
    expect(posted.some((event) => event.kind === "AUTH_STATE_CHANGED")).toBe(false);
    expectCleanedUp();
  });

  it("times out uncertainty, removes resources, and ignores late load events", async () => {
    const { AuthService } = await import("./auth.service");
    const pending = new AuthService().signOut();
    const rejected = expect(pending).rejects.toThrow("logout was not confirmed");
    await started.promise;
    const target = frame();
    await vi.advanceTimersByTimeAsync(15_000);
    await rejected;
    landed(target, AuthService.signedOutDestination());
    expect(posted.some((event) => event.kind === "AUTH_STATE_CHANGED")).toBe(false);
    expect(submitted).toHaveLength(1);
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expectCleanedUp();
  });

  it("cleans up pending logout when the page is hidden, including bfcache", async () => {
    const { AuthService } = await import("./auth.service");
    const pending = new AuthService().signOut();
    const rejected = expect(pending).rejects.toThrow("logout was not confirmed");
    await started.promise;
    window.dispatchEvent(new PageTransitionEvent("pagehide", { persisted: true }));
    await rejected;
    expect(posted.some((event) => event.kind === "AUTH_STATE_CHANGED")).toBe(false);
    expectCleanedUp();
  });

  it("cleans up if native submission throws", async () => {
    const { AuthService } = await import("./auth.service");
    vi.mocked(HTMLFormElement.prototype.submit).mockImplementationOnce(() => {
      throw new Error("blocked");
    });
    await expect(new AuthService().signOut()).rejects.toThrow("logout was not confirmed");
    expect(posted.some((event) => event.kind === "AUTH_STATE_CHANGED")).toBe(false);
    expectCleanedUp();
  });

  it("does not submit a PREAUTH value as logout SESSION CSRF", async () => {
    const { AuthService } = await import("./auth.service");
    fetchMock.mockResolvedValueOnce(new Response(JSON.stringify({ ...csrf, scope: "PREAUTH" }), { status: 200 }));
    await expect(new AuthService().signOut()).rejects.toThrow("requires SESSION CSRF");
    expect(submitted).toHaveLength(0);
    expect(posted.some((event) => event.kind === "AUTH_STATE_CHANGED")).toBe(false);
    expectCleanedUp(false);
  });

  it("does not submit after a CSRF issuance network failure", async () => {
    const { AuthService } = await import("./auth.service");
    fetchMock.mockRejectedValueOnce(new TypeError("offline"));
    await expect(new AuthService().signOut()).rejects.toThrow("offline");
    expect(submitted).toHaveLength(0);
    expect(posted).toEqual([]);
    expectCleanedUp(false);
  });
});
