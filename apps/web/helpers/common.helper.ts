export const createIdempotencyKey = (): string => {
  if (typeof crypto != "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }

  throw new Error("crypto.randomUUID() is not available in this browser context.");
};
