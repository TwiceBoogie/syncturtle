import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("@syncturtle/constants", () => ({ API_BASE_URL: "https://api.test" }));

import { AuthService } from "./auth.service";

let fetchMock: ReturnType<typeof vi.fn<typeof fetch>>;
beforeEach(() => {
  fetchMock = vi.fn<typeof fetch>();
  vi.stubGlobal("fetch", fetchMock);
});
afterEach(() => vi.unstubAllGlobals());

describe("Web logout", () => {
  it("posts the refresh cookie transport and accepts only a successful response", async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }));
    await new AuthService().signOut();
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(String(fetchMock.mock.calls[0]?.[0])).toBe("https://api.test/auth/sign-out");
    const request = fetchMock.mock.calls[0]?.[1];
    expect(request?.method).toBe("POST");
    expect(request?.credentials).toBe("include");
    expect(new Headers(request?.headers).get("X-ST-Browser-Request")).toBe("1");
  });

  it("does not turn Redis or network failure into confirmed logout", async () => {
    fetchMock
      .mockResolvedValueOnce(new Response(null, { status: 503 }))
      .mockRejectedValueOnce(new TypeError("offline"));
    const service = new AuthService();
    await expect(service.signOut()).rejects.toBeDefined();
    await expect(service.signOut()).rejects.toThrow("offline");
  });
});
