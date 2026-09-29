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

export interface IUserSession {
  sessionId: string;
  current: boolean;
  administrator: boolean;
  createdAt: string;
  lastUsedAt: string;
  idleExpiresAt: string;
  absoluteExpiresAt: string;
}

export interface IUserSessionInventoryResponse {
  sessions: IUserSession[];
}

export interface IUserSessionRevocationResponse {
  currentSessionRevoked: boolean;
}
