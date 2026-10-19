export interface IEmailCheckData {
  email: string;
}

export interface IEmailCheckResponse {
  authenticationFlow: "MAGIC_CODE" | "CREDENTIAL";
  existingUser: boolean;
}
