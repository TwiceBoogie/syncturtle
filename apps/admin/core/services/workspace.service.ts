// constants
import { API_BASE_URL } from "@syncturtle/constants";
// services
import { APIService, HttpError } from "./api.service";
// types
import { IApiErrorPayload, IWorkspace, TSlugCheckResult, TWorkspacePaginationInfo } from "@syncturtle/types";

export class WorkspaceService extends APIService {
  constructor() {
    super(API_BASE_URL);
  }

  async list(nextPageCursor?: string): Promise<TWorkspacePaginationInfo> {
    try {
      const response = await this.get<TWorkspacePaginationInfo>("/api/instances/workspaces", {
        params: {
          cursor: nextPageCursor,
        },
      });
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async create(data: Partial<IWorkspace>): Promise<IWorkspace> {
    try {
      const response = await this.post<IWorkspace>("/api/instances/workspaces", data);
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async slugCheck(slug: string, signal?: AbortSignal): Promise<TSlugCheckResult> {
    try {
      const response = await this.get<TSlugCheckResult>("/api/instances/workspaces/slug-check", {
        params: { slug },
        signal,
      });
      return response.data;
    } catch (error: unknown) {
      if (error instanceof DOMException && error.name === "AbortError") throw error;
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }
}
