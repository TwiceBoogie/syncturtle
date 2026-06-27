import React from "react";

export enum EPageType {
  PUBLIC = "PUBLIC",
  NON_AUTHENTICATED = "NON_AUTHENTICATED",
  SET_PASSWORD = "SET_PASSWORD",
  ONBOARDING = "ONBOARDING",
  AUTHENTICATED = "AUTHENTICATED",
}

export enum EAuthModes {
  SIGN_IN = "SIGN_IN",
  SIGN_UP = "SIGN_UP",
}

export enum EAuthSteps {
  EMAIL = "EMAIL",
  PASSWORD = "PASSWORD",
  UNIQUE_CODE = "UNIQUE_CODE",
}

export enum EAuthenticationErrorCodes {
  INSTANCE_NOT_CONFIGURED = "5000",
  INVALID_EMAIL = "5005",
  EMAIL_REQUIRED = "5010",
  INVALID_CSRF_TOKEN = "5012",
  SIGNUP_DISABLED = "5015",
  MAGIC_LINK_LOGIN_DISABLED = "5016",
  PASSWORD_LOGIN_DISABLED = "5018",
  USER_ACCOUNT_DEACTIVATED = "5019",
  INVALID_PASSWORD = "5020",
  SMTP_NOT_CONFIGURED = "5025",
  USER_ALREADY_EXISTS = "5030",
  AUTHENTICATION_FAILED_SIGN_UP = "5035",
  INVALID_EMAIL_MAGIC_SIGN_UP = "5050",
  USER_DOES_NOT_EXIST = "5060",
  AUTHENTICATION_FAILED_SIGN_IN = "5065",
  INVALID_EMAIL_MAGIC_SIGN_IN = "5080",
  INVALID_MAGIC_CODE_SIGN_IN = "5090",
  INVALID_MAGIC_CODE_SIGN_UP = "5092",
  EXPIRED_MAGIC_CODE_SIGN_IN = "5095",
  EXPIRED_MAGIC_CODE_SIGN_UP = "5097",
  RATE_LIMIT_EXCEEDED = "5900",
  AUTHENTICATION_FAILED = "5999",
}

export enum EErrorAlertType {
  BANNER_ALERT = "BANNER_ALERT",
  INLINE_FIRST_NAME = "INLINE_FIRST_NAME",
  INLINE_EMAIL = "INLINE_EMAIL",
  INLINE_PASSWORD = "INLINE_PASSWORD",
  INLINE_EMAIL_CODE = "INLINE_EMAIL_CODE",
}

export type TAuthErrorInfo = {
  type: EErrorAlertType;
  code: EAuthenticationErrorCodes;
  rawCode?: string;
  title: string;
  message: React.ReactNode;
};

export type TErrorCodeMessage = {
  title: string;
  message: (email?: string) => React.ReactNode;
};

const authenticationErrorCodeValues = new Set<string>(Object.values(EAuthenticationErrorCodes));

const isAuthenticationErrorCode = (value: string): value is EAuthenticationErrorCodes =>
  authenticationErrorCodeValues.has(value);

const normalizeAuthenticationErrorCode = (errorCode: string | undefined): EAuthenticationErrorCodes | undefined => {
  if (!errorCode) return undefined;

  if (isAuthenticationErrorCode(errorCode)) {
    return errorCode;
  }

  return EAuthenticationErrorCodes.AUTHENTICATION_FAILED;
};

const errorCodeMessages: Partial<Record<EAuthenticationErrorCodes, TErrorCodeMessage>> = {
  [EAuthenticationErrorCodes.INSTANCE_NOT_CONFIGURED]: {
    title: "Instance not configured",
    message: () => "Instance is not configured. Please contact your administrator.",
  },
  [EAuthenticationErrorCodes.SIGNUP_DISABLED]: {
    title: "Sign up disabled",
    message: () => "Sign up is currently disabled.",
  },
  [EAuthenticationErrorCodes.PASSWORD_LOGIN_DISABLED]: {
    title: "Password login disabled",
    message: () => "Email and password authentication is currently disabled.",
  },
  [EAuthenticationErrorCodes.MAGIC_LINK_LOGIN_DISABLED]: {
    title: "Magic code login disabled",
    message: () => "Magic code authentication is currently disabled.",
  },
  [EAuthenticationErrorCodes.SMTP_NOT_CONFIGURED]: {
    title: "Email delivery unavailable",
    message: () => "Email delivery is not configured yet.",
  },
  [EAuthenticationErrorCodes.RATE_LIMIT_EXCEEDED]: {
    title: "Too many requests",
    message: () => "Too many attempts. Please try again later.",
  },
  [EAuthenticationErrorCodes.AUTHENTICATION_FAILED]: {
    title: "Authentication failed",
    message: () => "Authentication failed.",
  },
  [EAuthenticationErrorCodes.AUTHENTICATION_FAILED_SIGN_IN]: {
    title: "Authentication failed",
    message: () => "Email or password is incorrect.",
  },
  [EAuthenticationErrorCodes.AUTHENTICATION_FAILED_SIGN_UP]: {
    title: "Authentication failed",
    message: () => "Email or password is incorrect.",
  },
  [EAuthenticationErrorCodes.INVALID_MAGIC_CODE_SIGN_IN]: {
    title: "Invalid code",
    message: () => "Verification code is invalid.",
  },
  [EAuthenticationErrorCodes.INVALID_MAGIC_CODE_SIGN_UP]: {
    title: "Invalid code",
    message: () => "Verification code is invalid.",
  },
  [EAuthenticationErrorCodes.INVALID_EMAIL_MAGIC_SIGN_IN]: {
    title: "Invalid email",
    message: () => "Verification email does not match.",
  },
  [EAuthenticationErrorCodes.INVALID_EMAIL_MAGIC_SIGN_UP]: {
    title: "Invalid email",
    message: () => "Verification email does not match.",
  },
  [EAuthenticationErrorCodes.EXPIRED_MAGIC_CODE_SIGN_IN]: {
    title: "Expired code",
    message: () => "Verification code has expired.",
  },
  [EAuthenticationErrorCodes.EXPIRED_MAGIC_CODE_SIGN_UP]: {
    title: "Expired code",
    message: () => "Verification code has expired.",
  },
};

const inlineEmailCodes = new Set<EAuthenticationErrorCodes>([
  EAuthenticationErrorCodes.INVALID_EMAIL,
  EAuthenticationErrorCodes.EMAIL_REQUIRED,
  EAuthenticationErrorCodes.USER_ALREADY_EXISTS,
  EAuthenticationErrorCodes.USER_DOES_NOT_EXIST,
]);

const inlinePasswordCodes = new Set<EAuthenticationErrorCodes>([EAuthenticationErrorCodes.INVALID_PASSWORD]);

const inlineEmailCodeCodes = new Set<EAuthenticationErrorCodes>([
  EAuthenticationErrorCodes.INVALID_MAGIC_CODE_SIGN_IN,
  EAuthenticationErrorCodes.INVALID_MAGIC_CODE_SIGN_UP,
  EAuthenticationErrorCodes.EXPIRED_MAGIC_CODE_SIGN_IN,
  EAuthenticationErrorCodes.EXPIRED_MAGIC_CODE_SIGN_UP,
]);

export const authErrorHandler = (errorCode: string | undefined, email?: string): TAuthErrorInfo | undefined => {
  const code = normalizeAuthenticationErrorCode(errorCode);

  if (!code) {
    return undefined;
  }

  const rawCode = errorCode !== code ? errorCode : undefined;

  if (inlineEmailCodes.has(code)) {
    return {
      type: EErrorAlertType.INLINE_EMAIL,
      code,
      title: "Email error",
      message: errorCodeMessages[code]?.message(email) || "Email is invalid.",
    };
  }

  if (inlinePasswordCodes.has(code)) {
    return {
      type: EErrorAlertType.INLINE_PASSWORD,
      code,
      title: "Password error",
      message: errorCodeMessages[code]?.message(email) || "Password is invalid.",
    };
  }

  if (inlineEmailCodeCodes.has(code)) {
    return {
      type: EErrorAlertType.INLINE_EMAIL_CODE,
      code,
      title: "Verification code error",
      message: errorCodeMessages[code]?.message(email) || "Verification code is invalid or expired.",
    };
  }

  const fallback = errorCodeMessages[code];

  return {
    type: EErrorAlertType.BANNER_ALERT,
    code,
    rawCode,
    title: fallback?.title || "Error",
    message: fallback?.message(email) || "Something went wrong. Please try again.",
  };
};
