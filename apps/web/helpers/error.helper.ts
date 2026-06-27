import type { IApiErrorPayload, IFieldViolation, TFieldErrors } from "@syncturtle/types";

type THttpErrorLike = {
  data?: unknown;
};

type TRedirectErrorPayload = {
  error_code?: string;
  errorCode?: string;
  code?: string;
  error_key?: string;
  errorKey?: string;
  key?: string;
  message?: string;
  email?: string;
  nextPath?: string;
};

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function isHttpErrorLike(error: unknown): error is THttpErrorLike {
  return isObject(error) && "data" in error;
}

function isFieldViolation(value: unknown): value is IFieldViolation {
  return isObject(value) && typeof value.field === "string" && value.field.trim().length > 0;
}

export function isApiErrorPayload(error: unknown): error is IApiErrorPayload {
  return (
    isObject(error) &&
    typeof error.code === "number" &&
    typeof error.key === "string" &&
    typeof error.message === "string"
  );
}

export function getApiErrorPayload(error: unknown): IApiErrorPayload | undefined {
  if (isApiErrorPayload(error)) {
    return error;
  }

  if (isHttpErrorLike(error) && isApiErrorPayload(error.data)) {
    return error.data;
  }

  return undefined;
}

export function getFieldErrors(error: unknown): TFieldErrors {
  const apiError = getApiErrorPayload(error);

  if (!apiError?.fields?.length) {
    return {};
  }

  return apiError.fields.reduce<TFieldErrors>((acc, violation) => {
    if (!isFieldViolation(violation)) {
      return acc;
    }

    acc[violation.field] = violation.message || violation.errorMessage || "Invalid field value.";

    return acc;
  }, {});
}

export function getPublicErrorMessage(error: unknown, fallback = "Something went wrong."): string {
  const apiError = getApiErrorPayload(error);

  if (apiError?.message) {
    return apiError.message;
  }

  if (error instanceof Error && error.message) {
    return error.message;
  }

  return fallback;
}

export function getFieldError(fieldErrors: TFieldErrors, fieldName: string): string | undefined {
  return fieldErrors[fieldName];
}

export function hasFieldErrors(fieldErrors: TFieldErrors): boolean {
  return Object.keys(fieldErrors).length > 0;
}

export function getRedirectErrorPayload(searchParams: URLSearchParams): TRedirectErrorPayload {
  return {
    error_code: searchParams.get("error_code") || searchParams.get("errorCode") || undefined,
    errorCode: searchParams.get("error_code") || searchParams.get("errorCode") || undefined,
    code: searchParams.get("code") || undefined,
    error_key: searchParams.get("error_key") || searchParams.get("errorKey") || undefined,
    errorKey: searchParams.get("error_key") || searchParams.get("errorKey") || undefined,
    key: searchParams.get("key") || undefined,
    message: searchParams.get("message") || undefined,
    email: searchParams.get("email") || undefined,
    nextPath: searchParams.get("nextPath") || undefined,
  };
}

export function getRedirectErrorCode(searchParams: URLSearchParams): string | undefined {
  return searchParams.get("error_code") || searchParams.get("errorCode") || searchParams.get("code") || undefined;
}

export function getRedirectErrorKey(searchParams: URLSearchParams): string | undefined {
  return searchParams.get("error_key") || searchParams.get("errorKey") || searchParams.get("key") || undefined;
}
