import { describe, expect, it } from "vitest";
import { bypassRuntimeCache } from "./sw-auth-policy";

describe("service worker authentication cache exclusion", () => {
  it("keeps authentication, identity, and Admin requests on the network", () => {
    for (const path of [
      "/auth/refresh",
      "/auth/sign-out",
      "/auth/google/callback",
      "/api/users/me",
      "/api/users/me/profile",
      "/api/instances/admins/me",
      "/god-mode/sessions/",
    ]) {
      expect(bypassRuntimeCache(path, "/god-mode")).toBe(true);
    }
  });

  it("does not classify unrelated content as authentication state", () => {
    expect(bypassRuntimeCache("/projects/acme", "/god-mode")).toBe(false);
    expect(bypassRuntimeCache("/authentication-guide", "/god-mode")).toBe(false);
  });
});
