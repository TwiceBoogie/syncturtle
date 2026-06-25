// constants
import { API_BASE_URL } from "@syncturtle/constants";
// services
import { APIService, HttpError } from "./api.service";
import type { IApiErrorPayload, IInstanceInfo } from "@syncturtle/types";

export class InstanceService extends APIService {
  constructor() {
    super(API_BASE_URL);
  }

  async getInstanceInfo(): Promise<IInstanceInfo> {
    try {
      const response = await this.get<IInstanceInfo>("/api/instances", {
        skipAuthRefresh: true,
      });
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }
}
