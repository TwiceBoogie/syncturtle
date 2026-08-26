import { API_BASE_URL } from "@syncturtle/constants";
import { APIService, HttpError } from "./api.service";
import type { IApiErrorPayload, IUser, TUserProfile } from "@syncturtle/types";

export class UserService extends APIService {
  constructor() {
    super(API_BASE_URL);
  }

  async me(): Promise<IUser> {
    try {
      const response = await this.get<IUser>("/api/users/me");
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async update(data: Partial<IUser>): Promise<IUser> {
    try {
      const response = await this.patch<IUser>("/api/users/me", data);
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async profile(): Promise<TUserProfile> {
    try {
      const response = await this.get<TUserProfile>("/api/users/me/profile");
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async updateProfile(data: Partial<TUserProfile>): Promise<TUserProfile> {
    try {
      const response = await this.patch<TUserProfile>("/api/users/me/profile", data);
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async adminDetails(): Promise<IUser> {
    try {
      const response = await this.get<IUser>("/api/instances/admins/me", {
        authRefresh: true,
      });
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }
}
