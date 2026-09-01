export type TCsrfTokenScope = "PREAUTH" | "SESSION";

export interface ICsrfTokenData {
  csrfToken: string;
  scope: TCsrfTokenScope;
  expiresAt: string;
}

export interface IEmailCheckData {
  email: string;
}

export interface IEmailCheckResponse {
  authenticationFlow: "MAGIC_CODE" | "CREDENTIAL";
  existingUser: boolean;
}
