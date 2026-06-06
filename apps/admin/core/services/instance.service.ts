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

// type ApiMaybeError<T> = T | IApiErrorPayload;

export class InstanceService extends APIService {
  constructor() {
    super(API_BASE_URL);
  }

  async info(): Promise<IInstanceInfo> {
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

  async update(data: Partial<IInstance>): Promise<IInstance> {
    try {
      const response = await this.patch<IInstance>("/api/instances", data, {
        authRefresh: true,
      });
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async admins(): Promise<IInstanceAdmin[]> {
    try {
      const response = await this.get<IInstanceAdmin[]>("/api/instances/admins", {
        skipAuthRefresh: true,
        validateStatus: (status) => status === 200 || status === 401 || status === 403,
      });

      if (response.status === 401 || response.status === 403) {
        return [];
      }
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async configurations(): Promise<IInstanceConfiguration[]> {
    try {
      const response = await this.get<IInstanceConfiguration[]>("/api/instances/configurations", {
        authRefresh: true,
      });

      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async updateConfigurations(data: Partial<TFormattedInstanceConfiguration>): Promise<IInstanceConfiguration[]> {
    try {
      const response = await this.patch<IInstanceConfiguration[]>("/api/instances/configurations", data, {
        authRefresh: true,
      });
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async sendTestEmail(receiverEmail: string): Promise<void> {
    try {
      await this.post<void>(
        "/api/instances/email-credentials-check",
        {
          receiverEmail: receiverEmail,
        },
        {
          authRefresh: true,
        }
      );
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async disableEmail(): Promise<void> {
    try {
      await this.delete<void>("/api/instances/configurations/disable-email-feature", undefined, {
        authRefresh: true,
      });
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }
}
