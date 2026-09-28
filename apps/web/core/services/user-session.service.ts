import { API_BASE_URL } from "@syncturtle/constants";
import { APIService, HttpError } from "./api.service";
import type {
  IApiErrorPayload,
  IUserSessionInventoryResponse,
  IUserSessionRevocationResponse,
} from "@syncturtle/types";

export class UserSessionService extends APIService {
  constructor() {
    super(API_BASE_URL);
  }

  async listSessions(): Promise<IUserSessionInventoryResponse> {
    try {
      const response = await this.get<IUserSessionInventoryResponse>("/api/users/me/sessions", {
        authRefresh: true,
      });
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  revokeSession(sessionId: string): Promise<IUserSessionRevocationResponse> {
    return this.revoke(`/api/users/me/sessions/${encodeURIComponent(sessionId)}`);
  }

  revokeOtherSessions(): Promise<IUserSessionRevocationResponse> {
    return this.revoke("/api/users/me/sessions/others");
  }

  revokeAllSessions(): Promise<IUserSessionRevocationResponse> {
    return this.revoke("/api/users/me/sessions");
  }

  private async revoke(path: string): Promise<IUserSessionRevocationResponse> {
    try {
      const response = await this.delete<IUserSessionRevocationResponse>(path, undefined, {
        authRefresh: true,
      });
      if (response.data.currentSessionRevoked) this.announceAuthStateChanged();
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }
}
