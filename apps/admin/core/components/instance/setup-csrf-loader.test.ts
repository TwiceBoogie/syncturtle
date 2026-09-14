import assert from "node:assert/strict";
import { describe, it } from "node:test";

// @ts-expect-error Node's native TypeScript runner requires the explicit extension.
import { createSetupCsrfLoader, isSetupSubmitDisabled } from "./setup-csrf-loader.ts";
// @ts-expect-error Node's native TypeScript runner requires the explicit extension.
import { CsrfTokenCache } from "../../services/csrf-token-cache.ts";

describe("createSetupCsrfLoader", () => {
  it("relies on the canonical cache for single-flight loading", async () => {
    const cache = new CsrfTokenCache(() => Date.parse("2026-08-27T12:00:00Z"));
    let issuanceCount = 0;
    const loader = createSetupCsrfLoader(() =>
      cache.get(async () => {
        issuanceCount += 1;
        return {
          csrfToken: "preauth-token",
          scope: "PREAUTH",
          expiresAt: "2026-08-27T12:30:00Z",
        };
      })
    );

    const firstLoad = loader();
    const secondLoad = loader();
    const firstToken = await firstLoad;
    const secondToken = await secondLoad;

    assert.equal(firstToken, "preauth-token");
    assert.equal(secondToken, "preauth-token");
    assert.equal(issuanceCount, 1);
  });

  it("does not retain a promise after canonical cache invalidation", async () => {
    const cache = new CsrfTokenCache(() => Date.parse("2026-08-27T12:00:00Z"));
    let issuanceCount = 0;
    const loader = createSetupCsrfLoader(() =>
      cache.get(async () => {
        issuanceCount += 1;
        return {
          csrfToken: issuanceCount === 1 ? "preauth-token" : "session-token",
          scope: issuanceCount === 1 ? "PREAUTH" : "SESSION",
          expiresAt: "2026-08-27T12:30:00Z",
        };
      })
    );

    const preauthToken = await loader();
    cache.invalidate();
    const sessionToken = await loader();

    assert.equal(preauthToken, "preauth-token");
    assert.equal(sessionToken, "session-token");
    assert.equal(issuanceCount, 2);
  });

  it("reports an invalid canonical issuance and permits a later retry", async () => {
    let requestCount = 0;
    const loader = createSetupCsrfLoader(async () => {
      requestCount += 1;
      return { csrfToken: requestCount === 1 ? "" : "retry-token" };
    });

    await assert.rejects(loader(), /CSRF token missing from response/);
    const retryToken = await loader();

    assert.equal(retryToken, "retry-token");
    assert.equal(requestCount, 2);
  });
});

describe("isSetupSubmitDisabled", () => {
  it("requires a ready token, valid form data, and an idle submit state", () => {
    const loadingDisabled = isSetupSubmitDisabled({
      csrfToken: undefined,
      hasValidFormData: true,
      isSubmitting: false,
    });
    const errorDisabled = isSetupSubmitDisabled({
      csrfToken: "",
      hasValidFormData: true,
      isSubmitting: false,
    });
    const invalidFormDisabled = isSetupSubmitDisabled({
      csrfToken: "ready-token",
      hasValidFormData: false,
      isSubmitting: false,
    });
    const submittingDisabled = isSetupSubmitDisabled({
      csrfToken: "ready-token",
      hasValidFormData: true,
      isSubmitting: true,
    });
    const readyDisabled = isSetupSubmitDisabled({
      csrfToken: "ready-token",
      hasValidFormData: true,
      isSubmitting: false,
    });

    assert.equal(loadingDisabled, true);
    assert.equal(errorDisabled, true);
    assert.equal(invalidFormDisabled, true);
    assert.equal(submittingDisabled, true);
    assert.equal(readyDisabled, false);
  });
});
