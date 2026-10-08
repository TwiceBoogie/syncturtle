import type { ICsrfTokenData } from "@syncturtle/types";

export const createSignInCsrfLoader =
  (clearCachedToken: () => void, requestCsrfToken: () => Promise<ICsrfTokenData>) => async (): Promise<string> => {
    clearCachedToken();
    const response = await requestCsrfToken();
    const token = response.csrfToken?.trim();
    if (response.scope !== "PREAUTH" || !token) {
      throw new Error("Administrator sign-in requires PREAUTH CSRF");
    }
    return token;
  };

export const isSignInSubmitDisabled = (state: {
  csrfToken: string | undefined;
  email: string;
  password: string;
  isSubmitting: boolean;
}): boolean => state.isSubmitting || !state.csrfToken || !state.email || !state.password;
