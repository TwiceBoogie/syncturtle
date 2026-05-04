import { APIService, HttpError } from "./api.service";
// constants
import { API_BASE_URL } from "@syncturtle/constants";
// types
import { IApiErrorPayload, IEmailCheckData, IEmailCheckResponse, IUser } from "@syncturtle/types";

export class AuthService extends APIService {
  constructor() {
    super(API_BASE_URL);
  }

  async setPassword(token: string, data: { password: string }): Promise<IUser> {
    try {
      const response = await this.post<IUser>("/auth/set-password", data, {
        headers: {
          "X-CSRF-TOKEN": token,
        },
      });
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async emailCheck(data: IEmailCheckData): Promise<IEmailCheckResponse> {
    try {
      const response = await this.post<IEmailCheckResponse>("/auth/email-check", data);
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }
}
