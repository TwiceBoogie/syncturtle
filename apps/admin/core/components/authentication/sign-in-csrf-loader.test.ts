import { describe, expect, it, vi } from "vitest";

import { createSignInCsrfLoader, isSignInSubmitDisabled } from "./sign-in-csrf-loader";

describe("administrator sign-in CSRF", () => {
  it("drops a persisted session token before requesting a pre-authentication token", async () => {
    const calls: string[] = [];
    const load = createSignInCsrfLoader(
      () => calls.push("clear"),
      async () => {
        calls.push("request");
        return { csrfToken: "fresh-preauth", scope: "PREAUTH", expiresAt: "2026-09-24T14:00:00Z" };
      }
    );

    await expect(load()).resolves.toBe("fresh-preauth");
    expect(calls).toEqual(["clear", "request"]);
  });

  it("does not submit with a session-scoped or missing token", async () => {
    const clear = vi.fn();
    const sessionToken = createSignInCsrfLoader(clear, async () => ({
      csrfToken: "old-session",
      scope: "SESSION",
      expiresAt: "2026-09-24T14:00:00Z",
    }));
    const emptyToken = createSignInCsrfLoader(clear, async () => ({
      csrfToken: "",
      scope: "PREAUTH",
      expiresAt: "2026-09-24T14:00:00Z",
    }));

    await expect(sessionToken()).rejects.toThrow("requires PREAUTH CSRF");
    await expect(emptyToken()).rejects.toThrow("requires PREAUTH CSRF");
    expect(clear).toHaveBeenCalledTimes(2);
    expect(
      isSignInSubmitDisabled({
        csrfToken: undefined,
        email: "admin@example.test",
        password: "password",
        isSubmitting: false,
      })
    ).toBe(true);
    expect(
      isSignInSubmitDisabled({
        csrfToken: "fresh-preauth",
        email: "admin@example.test",
        password: "password",
        isSubmitting: false,
      })
    ).toBe(false);
  });
});
