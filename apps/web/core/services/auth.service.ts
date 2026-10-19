import { APIService, HttpError } from "./api.service";
// constants
import { API_BASE_URL } from "@syncturtle/constants";
// types
import type { IApiErrorPayload, IEmailCheckData, IEmailCheckResponse, IUser } from "@syncturtle/types";

export class AuthService extends APIService {
  constructor() {
    super(API_BASE_URL);
  }

  async setPassword(data: { password: string }): Promise<IUser> {
    try {
      const response = await this.post<IUser>("/auth/set-password", data);
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async emailCheck(data: IEmailCheckData): Promise<IEmailCheckResponse> {
    try {
      const response = await this.post<IEmailCheckResponse>("/auth/email-check", data, { skipAuthRefresh: true });
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async signOut(): Promise<void> {
    try {
      await this.post<void>("/auth/sign-out", undefined, { skipAuthRefresh: true });
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async generateUniqueCode(email: string): Promise<{ key: string }> {
    try {
      const response = await this.post<{ key: string }>(
        "/auth/magic-generate",
        { email: email },
        { skipAuthRefresh: true }
      );
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }
}
