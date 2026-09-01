import assert from "node:assert/strict";
import { describe, it } from "node:test";

// @ts-expect-error Node's native TypeScript runner requires the explicit extension.
import { currentUserBootstrapKey, isPublicAdministratorSignInRoute } from "./user-wrapper-policy.ts";

describe("currentUserBootstrapKey", () => {
  it("suppresses current-user bootstrap while instance state is unknown", () => {
    assert.equal(currentUserBootstrapKey(undefined, "/general/"), null);
  });

  it("suppresses current-user bootstrap during first-run setup", () => {
    assert.equal(currentUserBootstrapKey(false, "/general/"), null);
  });

  it("suppresses current-user bootstrap on the public administrator sign-in route", () => {
    assert.equal(currentUserBootstrapKey(true, "/god-mode/"), null);
  });

  it("suppresses current-user bootstrap when the App Router reports its normalized root", () => {
    assert.equal(currentUserBootstrapKey(true, "/"), null);
  });

  it("enables normal current-user bootstrap for a configured instance route", () => {
    assert.equal(currentUserBootstrapKey(true, "/general/"), "CURRENT_USER");
  });
});

describe("isPublicAdministratorSignInRoute", () => {
  it("matches the administrator base route with or without a trailing slash", () => {
    assert.equal(isPublicAdministratorSignInRoute("/god-mode"), true);
    assert.equal(isPublicAdministratorSignInRoute("/god-mode/"), true);
  });

  it("does not match authenticated administrator routes", () => {
    assert.equal(isPublicAdministratorSignInRoute("/general/"), false);
    assert.equal(isPublicAdministratorSignInRoute("/authentication/"), false);
  });
});
