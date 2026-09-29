import { beforeEach, describe, expect, it, vi } from "vitest";
import type { IUserSessionInventoryResponse, IUserSessionRevocationResponse } from "@syncturtle/types";

const transport = vi.hoisted(() => ({
  get: vi.fn<(path: string, config: { authRefresh: boolean }) => Promise<{ data: unknown }>>(),
  delete: vi.fn<(path: string, body: unknown, config: { authRefresh: boolean }) => Promise<{ data: unknown }>>(),
  announceAuthStateChanged: vi.fn<() => void>(),
}));

vi.mock("@syncturtle/constants", () => ({ API_BASE_URL: "https://api.test" }));
vi.mock("./api.service", () => ({
  APIService: class {
    protected get = transport.get;
    protected delete = transport.delete;
    protected announceAuthStateChanged = transport.announceAuthStateChanged;
  },
  HttpError: class extends Error {},
}));

const inventory: IUserSessionInventoryResponse = {
  sessions: [
    {
      sessionId: "00000000-0000-4000-8000-000000000001",
      current: true,
      administrator: false,
      createdAt: "2026-09-01T00:00:00Z",
      lastUsedAt: "2026-09-22T12:00:00Z",
      idleExpiresAt: "2026-09-29T12:00:00Z",
      absoluteExpiresAt: "2026-10-01T00:00:00Z",
    },
  ],
};

beforeEach(() => {
  vi.clearAllMocks();
});

describe("web user session client", () => {
  it("lists the exact protected session collection", async () => {
    const { UserSessionService } = await import("./user-session.service");
    transport.get.mockResolvedValueOnce({ data: inventory });

    await expect(new UserSessionService().listSessions()).resolves.toEqual(inventory);

    expect(transport.get).toHaveBeenCalledExactlyOnceWith("/api/users/me/sessions", { authRefresh: true });
  });

  it.each([
    ["one", "fictional/session ?", "/api/users/me/sessions/fictional%2Fsession%20%3F"],
    ["others", undefined, "/api/users/me/sessions/others"],
    ["all", undefined, "/api/users/me/sessions"],
  ] as const)("revokes %s through the exact protected DELETE route", async (operation, sessionId, path) => {
    const { UserSessionService } = await import("./user-session.service");
    const response: IUserSessionRevocationResponse = { currentSessionRevoked: false };
    transport.delete.mockResolvedValueOnce({ data: response });
    const service = new UserSessionService();

    let result: IUserSessionRevocationResponse;
    if (operation === "one") {
      if (!sessionId) throw new Error("Expected a session ID for revoke-one");
      result = await service.revokeSession(sessionId);
    } else if (operation === "others") {
      result = await service.revokeOtherSessions();
    } else {
      result = await service.revokeAllSessions();
    }

    expect(result).toEqual(response);
    expect(transport.delete).toHaveBeenCalledExactlyOnceWith(path, undefined, { authRefresh: true });
    expect(transport.announceAuthStateChanged).not.toHaveBeenCalled();
  });

  it("announces only a backend-confirmed current-session revocation", async () => {
    const { UserSessionService } = await import("./user-session.service");
    const response: IUserSessionRevocationResponse = { currentSessionRevoked: true };
    transport.delete.mockResolvedValueOnce({ data: response });

    await expect(new UserSessionService().revokeAllSessions()).resolves.toEqual(response);

    expect(transport.announceAuthStateChanged).toHaveBeenCalledTimes(1);
  });
});
