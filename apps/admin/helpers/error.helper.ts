import type { IApiErrorPayload, TFieldErrors } from "@syncturtle/types";

export function isApiErrorPayload(error: unknown): error is IApiErrorPayload {
  return (
    typeof error === "object" &&
    error !== null &&
    "ok" in error &&
    "code" in error &&
    "key" in error &&
    "message" in error
  );
}

export function getPublicErrorMessage(error: unknown, fallback = "Something went wrong."): string {
  if (isApiErrorPayload(error) && error.message) {
    return error.message;
  }

  if (error instanceof Error && error.message) {
    return error.message;
  }

  return fallback;
}

export function getFieldErrors(error: unknown): TFieldErrors {
  if (!isApiErrorPayload(error) || !error.fields?.length) {
    return {};
  }

  return error.fields.reduce<TFieldErrors>((acc, field) => {
    if (!field.field) return acc;

    acc[field.field] = field.message || field.errorMessage || "Invalid field value.";
    return acc;
  }, {});
}
