import { describe, expect, it } from "vitest";

// @ts-expect-error Node's native TypeScript runner requires the explicit extension.
import { currentUserBootstrapKey, isPublicAdministratorSignInRoute } from "./user-wrapper-policy.ts";

describe("currentUserBootstrapKey", () => {
  it("suppresses current-user bootstrap while instance state is unknown", () => {
    expect(currentUserBootstrapKey(undefined, "/general/")).toBe(null);
  });

  it("suppresses current-user bootstrap during first-run setup", () => {
    expect(currentUserBootstrapKey(false, "/general/")).toBe(null);
  });

  it("suppresses current-user bootstrap on the public administrator sign-in route", () => {
    expect(currentUserBootstrapKey(true, "/god-mode/")).toBe(null);
  });

  it("suppresses current-user bootstrap when the App Router reports its normalized root", () => {
    expect(currentUserBootstrapKey(true, "/")).toBe(null);
  });

  it("enables normal current-user bootstrap for a configured instance route", () => {
    expect(currentUserBootstrapKey(true, "/general/")).toBe("CURRENT_USER");
  });
});

describe("isPublicAdministratorSignInRoute", () => {
  it("matches the administrator base route with or without a trailing slash", () => {
    expect(isPublicAdministratorSignInRoute("/god-mode")).toBe(true);
    expect(isPublicAdministratorSignInRoute("/god-mode/")).toBe(true);
  });

  it("does not match authenticated administrator routes", () => {
    expect(isPublicAdministratorSignInRoute("/general/")).toBe(false);
    expect(isPublicAdministratorSignInRoute("/authentication/")).toBe(false);
  });
});
