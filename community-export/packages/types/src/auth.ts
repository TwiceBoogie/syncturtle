export interface ICsrfTokenData {
  csrfToken: string;
}

export interface IEmailCheckData {
  email: string;
}

export interface IEmailCheckResponse {
  status: "MAGIC_CODE" | "CREDENTIAL";
  existing: boolean;
  isPasswordAutoset: boolean;
}
