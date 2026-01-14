import { API_BASE_URL } from "@syncturtle/constants";
import { APIService, HttpError } from "./api.service";
import { IApiErrorPayload, IUser, TUserProfile } from "@syncturtle/types";

export class UserService extends APIService {
  constructor() {
    super(API_BASE_URL);
  }

  async currentUser(): Promise<IUser> {
    try {
      const response = await this.get<IUser>("/api/users/me/");
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async updateUser(data: Partial<IUser>): Promise<IUser> {
    try {
      const response = await this.patch<IUser>("/api/users/me", data);
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async changePassword(token: string, data: { oldPassword?: string; newPassword: string }): Promise<IUser> {
    try {
      const response = await this.post<IUser>("/auth/change-password", data, {
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

  async getCurrentUserProfile(): Promise<TUserProfile> {
    try {
      const response = await this.get<TUserProfile>("/api/users/me/profile");
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async updateCurrentUserProfile(data: Partial<TUserProfile>): Promise<TUserProfile> {
    try {
      const response = await this.patch<TUserProfile>("/api/users/me/profile", data);
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }
}
