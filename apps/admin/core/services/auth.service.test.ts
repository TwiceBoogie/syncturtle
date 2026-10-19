/** @vitest-environment jsdom */

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("@syncturtle/constants", () => ({
  API_BASE_URL: "https://api.test",
  ADMIN_BASE_URL: "https://admin.test",
  ADMIN_BASE_PATH: "/god-mode",
}));

import { AuthService } from "./auth.service";

let fetchMock: ReturnType<typeof vi.fn<typeof fetch>>;

beforeEach(() => {
  fetchMock = vi.fn<typeof fetch>();
  vi.stubGlobal("fetch", fetchMock);
});
afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe("administrator logout", () => {
  it("uses the shared POST contract and completes after a successful response", async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }));
    await new AuthService().signOut();
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(String(fetchMock.mock.calls[0]?.[0])).toBe("https://api.test/auth/sign-out");
    const request = fetchMock.mock.calls[0]?.[1];
    expect(request?.method).toBe("POST");
    expect(request?.credentials).toBe("include");
    expect(new Headers(request?.headers).get("X-ST-Browser-Request")).toBe("1");
    expect(document.querySelector("iframe")).toBeNull();
  });

  it("does not confirm sign-out after a Redis 503 or network loss", async () => {
    fetchMock
      .mockResolvedValueOnce(new Response(null, { status: 503 }))
      .mockRejectedValueOnce(new TypeError("offline"));
    const service = new AuthService();
    await expect(service.signOut()).rejects.toBeDefined();
    await expect(service.signOut()).rejects.toThrow("offline");
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });

  it("keeps the signed-out landing on the Admin origin", () => {
    expect(AuthService.signedOutDestination()).toBe("https://admin.test/god-mode/");
  });
});
