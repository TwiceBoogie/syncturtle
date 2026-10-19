import { ADMIN_BASE_PATH, ADMIN_BASE_URL, API_BASE_URL } from "@syncturtle/constants";
import { APIService, HttpError } from "./api.service";
import type { IApiErrorPayload } from "@syncturtle/types";

export class AuthService extends APIService {
  constructor() {
    super(API_BASE_URL);
  }

  async signOut(): Promise<void> {
    try {
      await this.post<void>("/auth/sign-out", undefined, { skipAuthRefresh: true });
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  static signedOutDestination(): string {
    const path = `${(ADMIN_BASE_PATH || "/god-mode").replace(/\/+$/, "")}/`;
    return new URL(path, ADMIN_BASE_URL || window.location.origin).href;
  }
}
