import { API_BASE_URL } from "@syncturtle/constants";
import { APIService, HttpError } from "./api.service";
import { IApiErrorPayload, ICsrfTokenData } from "@syncturtle/types";

export class AuthService extends APIService {
  constructor() {
    super(API_BASE_URL);
  }

  async requestCSRFToken(): Promise<ICsrfTokenData> {
    try {
      const response = await this.get<ICsrfTokenData>("/api/get-csrf-token");
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async signOut(): Promise<void> {
    try {
      await this.post<void>("/api/auth/sign-out", undefined, {
        validateStatus: (s) => s >= 200 && s < 500,
      });
    } catch (error) {
      console.log(error);
    }
  }
}
