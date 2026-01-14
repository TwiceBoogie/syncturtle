import { API_BASE_URL } from "@syncturtle/constants";
import { APIService } from "./api.service";

export class AuthService extends APIService {
  constructor() {
    super(API_BASE_URL);
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
