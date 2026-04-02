import { API_BASE_URL } from "@syncturtle/constants";
import { APIService, HttpError } from "./api.service";
import {
  IApiErrorPayload,
  IInstance,
  IInstanceAdmin,
  IInstanceConfiguration,
  IInstanceInfo,
  TFormattedInstanceConfiguration,
} from "@syncturtle/types";

export class InstanceService extends APIService {
  constructor() {
    super(API_BASE_URL);
  }

  async info(): Promise<IInstanceInfo> {
    try {
      const response = await this.get<IInstanceInfo>("/api/instances");
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async update(data: Partial<IInstance>): Promise<IInstance> {
    try {
      const response = await this.patch<IInstance>("/api/instances", data);
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async admins(): Promise<IInstanceAdmin[]> {
    try {
      const response = await this.get<IInstanceAdmin[]>("/api/instances/admins", {
        validateStatus: (s) => s >= 200 && s < 500,
      });
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async configurations(): Promise<IInstanceConfiguration[]> {
    try {
      const response = await this.get<IInstanceConfiguration[]>("/api/instances/configurations");
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async updateConfigurations(data: Partial<TFormattedInstanceConfiguration>): Promise<IInstanceConfiguration[]> {
    try {
      const response = await this.patch<IInstanceConfiguration[]>("/api/instances/configurations", data);
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async sendTesetEmail(receiverEmail: string): Promise<void> {
    try {
      await this.post<void>("/api/instances/email-credentials-check", {
        receiverEmail: receiverEmail,
      });
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async disableEmail(): Promise<void> {
    try {
      await this.delete("/api/instances/configurations/disable-email-feature");
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }
}
