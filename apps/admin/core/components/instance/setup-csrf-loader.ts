type TCsrfTokenResponse = {
  csrfToken?: string | null;
};

type TRequestCsrfToken = () => Promise<TCsrfTokenResponse>;

type TSetupSubmitState = {
  csrfToken: string | undefined;
  hasValidFormData: boolean;
  isSubmitting: boolean;
};

export const createSetupCsrfLoader = (requestCsrfToken: TRequestCsrfToken) => {
  let tokenPromise: Promise<string> | undefined;

  return (): Promise<string> => {
    if (tokenPromise !== undefined) return tokenPromise;

    tokenPromise = requestCsrfToken()
      .then((data) => {
        const token = data.csrfToken?.trim();
        if (!token) throw new Error("CSRF token missing from response");
        return token;
      })
      .catch((error: unknown) => {
        tokenPromise = undefined;
        throw error;
      });

    return tokenPromise;
  };
};

export const isSetupSubmitDisabled = (state: TSetupSubmitState): boolean =>
  state.isSubmitting || !state.hasValidFormData || !state.csrfToken?.trim();
