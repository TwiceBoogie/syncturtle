import { API_BASE_URL } from "@syncturtle/constants";
import { APIService, HttpError } from "./api.service";
import type {
  IApiErrorPayload,
  IUser,
  IUserAvatarUpdateRequest,
  IUserCoverUpdateRequest,
  IUserSettings,
  IUserUpdateRequest,
  TUserProfile,
} from "@syncturtle/types";

export class UserService extends APIService {
  constructor() {
    super(API_BASE_URL);
  }

  async currentUser(): Promise<IUser> {
    try {
      const response = await this.get<IUser>("/api/users/me", {
        authRefresh: true,
      });
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async updateUser(data: IUserUpdateRequest): Promise<IUser> {
    try {
      const response = await this.patch<IUser>("/api/users/me", data);
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async changePassword(data: { oldPassword?: string; newPassword: string }): Promise<IUser> {
    try {
      const response = await this.post<IUser>("/auth/change-password", data);
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

  async currentUserSettings(bustCache: boolean = false): Promise<IUserSettings> {
    const url = bustCache ? `/api/users/me/settings?t=${Date.now()}` : "/api/users/me/settings";
    try {
      const response = await this.get<IUserSettings>(url);
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async updateUserOnboard(): Promise<{ message: string }> {
    try {
      const response = await this.patch<{ message: string }>("/api/users/me/onboard", {
        isOnboarded: true,
      });

      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async updateUserTourCompleted(): Promise<{ message: string }> {
    try {
      const response = await this.patch<{ message: string }>("/api/users/me/tour-completed", {
        isTourCompleted: true,
      });
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

  async updateAvatar(data: IUserAvatarUpdateRequest): Promise<IUser> {
    try {
      const response = await this.patch<IUser>("/api/users/me/avatar", data);
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async clearAvatar(): Promise<IUser> {
    try {
      const response = await this.delete<IUser>("/api/users/me/avatar");
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async updateCoverImage(data: IUserCoverUpdateRequest): Promise<IUser> {
    try {
      const response = await this.patch<IUser>("/api/users/me/cover-image", data);
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async clearCoverImage(): Promise<IUser> {
    try {
      const response = await this.delete<IUser>("/api/users/me/cover-image");
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }
}
