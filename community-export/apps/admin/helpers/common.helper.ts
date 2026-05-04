type ApiLikeError = {
  message?: string;
};

export const getErrorMessage = (err: unknown): string => {
  if (err instanceof Error) {
    return err.message;
  }

  if (typeof err === "object" && err !== null) {
    const apiError = err as ApiLikeError;

    if (typeof apiError.message === "string" && apiError.message.trim()) {
      return apiError.message;
    }
  }

  return "Something went wrong";
};
