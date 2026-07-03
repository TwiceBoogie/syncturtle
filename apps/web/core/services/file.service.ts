import { API_BASE_PATH } from "@syncturtle/constants";
import { APIService, HttpError } from "./api.service";
import type { IApiErrorPayload, IFileUploadCreateRequest, IFileUploadCreateResponse } from "@syncturtle/types";

export class FileService extends APIService {
  constructor() {
    super(API_BASE_PATH);
  }

  async createUpload(data: IFileUploadCreateRequest, idempotencyKey?: string): Promise<IFileUploadCreateResponse> {
    try {
      const response = await this.post<IFileUploadCreateResponse>("/api/assets/v1/uploads", data, {
        headers: idempotencyKey
          ? {
              "Idempotency-Key": idempotencyKey,
            }
          : undefined,
      });

      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async completeUpload(assetId: string): Promise<void> {
    try {
      await this.patch<void>(`/api/assets/v1/uploads/${assetId}/complete`);
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async deleteAsset(assetId: string): Promise<void> {
    try {
      await this.delete<void>(`/api/assets/v1/${assetId}`);
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }
}
