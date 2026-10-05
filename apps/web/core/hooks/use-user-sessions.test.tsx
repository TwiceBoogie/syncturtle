/** @vitest-environment jsdom */

import type { ReactNode } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { act, renderHook, waitFor } from "@testing-library/react";
import { SWRConfig } from "swr";
import { useUserSessions } from "./use-user-sessions";
import type { IUserSessionInventoryResponse, IUserSessionRevocationResponse } from "@syncturtle/types";

const sessions = vi.hoisted(() => ({
  list: vi.fn<() => Promise<IUserSessionInventoryResponse>>(),
  revokeOne: vi.fn<(sessionId: string) => Promise<IUserSessionRevocationResponse>>(),
  revokeOthers: vi.fn<() => Promise<IUserSessionRevocationResponse>>(),
  revokeAll: vi.fn<() => Promise<IUserSessionRevocationResponse>>(),
}));
const user = vi.hoisted(() => ({
  authenticated: true,
  transitionToSignedOut: vi.fn<() => void>(),
}));

vi.mock("@/services/user-session.service", () => ({
  UserSessionService: class {
    listSessions = sessions.list;
    revokeSession = sessions.revokeOne;
    revokeOtherSessions = sessions.revokeOthers;
    revokeAllSessions = sessions.revokeAll;
  },
}));
vi.mock("@/hooks/store/user", () => ({
  useUser: () => ({
    isAuthenticated: user.authenticated,
    transitionToSignedOut: user.transitionToSignedOut,
  }),
}));

const firstInventory: IUserSessionInventoryResponse = {
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
const refreshedInventory: IUserSessionInventoryResponse = { sessions: [] };

function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((done) => {
    resolve = done;
  });
  return { promise, resolve };
}

function Wrapper({ children }: { children: ReactNode }) {
  return <SWRConfig value={{ provider: () => new Map(), dedupingInterval: 0 }}>{children}</SWRConfig>;
}

beforeEach(() => {
  vi.clearAllMocks();
  user.authenticated = true;
  sessions.list.mockReset().mockResolvedValue(firstInventory);
  sessions.revokeOne.mockReset().mockResolvedValue({ currentSessionRevoked: false });
  sessions.revokeOthers.mockReset().mockResolvedValue({ currentSessionRevoked: false });
  sessions.revokeAll.mockReset().mockResolvedValue({ currentSessionRevoked: true });
});

describe("web session inventory ownership", () => {
  it("loads and explicitly retries the authoritative inventory", async () => {
    const view = renderHook(() => useUserSessions(), { wrapper: Wrapper });
    await waitFor(() => expect(view.result.current.sessions).toEqual(firstInventory.sessions));
    sessions.list.mockResolvedValueOnce(refreshedInventory);

    await act(async () => view.result.current.retry());

    expect(view.result.current.sessions).toEqual([]);
    expect(sessions.list).toHaveBeenCalledTimes(2);
  });

  it("refetches after revoking another session and after revoking others", async () => {
    const view = renderHook(() => useUserSessions(), { wrapper: Wrapper });
    await waitFor(() => expect(view.result.current.sessions).toEqual(firstInventory.sessions));
    sessions.list.mockResolvedValue(refreshedInventory);

    await act(async () => view.result.current.revokeSession("00000000-0000-4000-8000-000000000002"));
    await act(async () => view.result.current.revokeOtherSessions());

    expect(sessions.revokeOne).toHaveBeenCalledExactlyOnceWith("00000000-0000-4000-8000-000000000002");
    expect(sessions.revokeOthers).toHaveBeenCalledTimes(1);
    expect(sessions.list).toHaveBeenCalledTimes(3);
    expect(user.transitionToSignedOut).not.toHaveBeenCalled();
  });

  it("suppresses duplicate destructive actions while one is pending", async () => {
    const pending = deferred<IUserSessionRevocationResponse>();
    sessions.revokeOne.mockReturnValueOnce(pending.promise);
    const view = renderHook(() => useUserSessions(), { wrapper: Wrapper });
    await waitFor(() => expect(view.result.current.sessions).toEqual(firstInventory.sessions));
    let first!: Promise<void>;
    let duplicate!: Promise<void>;

    act(() => {
      first = view.result.current.revokeSession("00000000-0000-4000-8000-000000000002");
      duplicate = view.result.current.revokeAllSessions();
    });
    expect(view.result.current.isMutating).toBe(true);
    expect(sessions.revokeOne).toHaveBeenCalledTimes(1);
    expect(sessions.revokeAll).not.toHaveBeenCalled();

    pending.resolve({ currentSessionRevoked: false });
    await act(async () => Promise.all([first, duplicate]));
    expect(view.result.current.isMutating).toBe(false);
  });

  it("clears inventory and transitions once after confirmed current revocation", async () => {
    const view = renderHook(() => useUserSessions(), { wrapper: Wrapper });
    await waitFor(() => expect(view.result.current.sessions).toEqual(firstInventory.sessions));

    await act(async () => view.result.current.revokeAllSessions());

    expect(view.result.current.sessions).toBeUndefined();
    expect(sessions.list).toHaveBeenCalledTimes(1);
    expect(user.transitionToSignedOut).toHaveBeenCalledTimes(1);
  });

  it("retains a safe mutation error and clears inventory after a global signed-out transition", async () => {
    const failure = new TypeError("offline");
    sessions.revokeOthers.mockRejectedValueOnce(failure);
    const view = renderHook(() => useUserSessions(), { wrapper: Wrapper });
    await waitFor(() => expect(view.result.current.sessions).toEqual(firstInventory.sessions));

    await act(async () => {
      await expect(view.result.current.revokeOtherSessions()).rejects.toBe(failure);
    });
    expect(view.result.current.error).toBe(failure);
    expect(view.result.current.sessions).toEqual(firstInventory.sessions);

    user.authenticated = false;
    view.rerender();
    await waitFor(() => expect(view.result.current.sessions).toBeUndefined());
  });
});
