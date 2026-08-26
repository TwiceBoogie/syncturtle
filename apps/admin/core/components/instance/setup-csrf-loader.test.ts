import assert from "node:assert/strict";
import { describe, it } from "node:test";

// @ts-expect-error Node's native TypeScript runner requires the explicit extension.
import { createSetupCsrfLoader, isSetupSubmitDisabled } from "./setup-csrf-loader.ts";

describe("createSetupCsrfLoader", () => {
  it("fetches one token and reuses it for repeated load requests", async () => {
    let requestCount = 0;
    const loader = createSetupCsrfLoader(async () => {
      requestCount += 1;
      return { csrfToken: "ready-token" };
    });

    const firstLoad = loader();
    const secondLoad = loader();
    const firstToken = await firstLoad;
    const secondToken = await secondLoad;

    assert.equal(firstToken, "ready-token");
    assert.equal(secondToken, "ready-token");
    assert.equal(requestCount, 1);
  });

  it("reports invalid issuance and retries with a new request", async () => {
    let requestCount = 0;
    const loader = createSetupCsrfLoader(async () => {
      requestCount += 1;
      if (requestCount === 1) return { csrfToken: "" };
      return { csrfToken: "retry-token" };
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
