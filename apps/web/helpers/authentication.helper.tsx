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
  title: string;
  message: React.ReactNode;
};

const errorCodeMessages: {
  [key in EAuthenticationErrorCodes]: { title: string; message: (email?: string | undefined) => React.ReactNode };
} = {
  [EAuthenticationErrorCodes.INSTANCE_NOT_CONFIGURED]: {
    title: "Instance not configured",
    message: () => "Instance not configured. Please contact your administrator.",
  },
};

export const authErrorHandler = (
  errorCode: EAuthenticationErrorCodes,
  email?: string | undefined
): TAuthErrorInfo | undefined => {
  const bannerAlerErrorCodes = [EAuthenticationErrorCodes.INSTANCE_NOT_CONFIGURED];

  if (bannerAlerErrorCodes.includes(errorCode)) {
    return {
      type: EErrorAlertType.BANNER_ALERT,
      code: errorCode,
      title: errorCodeMessages[errorCode]?.title || "Error",
      message: errorCodeMessages[errorCode]?.message(email) || "Something went wrong. Please try again.",
    };
  }
  return undefined;
};
