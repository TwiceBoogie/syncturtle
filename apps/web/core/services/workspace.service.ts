import { APIService, HttpError } from "./api.service";
// constants
import { API_BASE_URL } from "@syncturtle/constants";
// types
import type {
  IApiErrorPayload,
  IWorkspace,
  IWorkspaceBulkInviteFormData,
  IWorkspaceLogoUpdateRequest,
  IWorkspaceMemberInvitation,
  TSlugCheckResult,
} from "@syncturtle/types";

export class WorkspaceService extends APIService {
  constructor() {
    super(API_BASE_URL);
  }

  async userWorkspaces(): Promise<IWorkspace[]> {
    try {
      const response = await this.get<IWorkspace[]>("/api/users/me/workspaces");
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async getWorkspace(workspaceSlug: string): Promise<IWorkspace> {
    try {
      const response = await this.get<IWorkspace>(`/api/workspaces/${workspaceSlug}`);
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async createWorkspace(data: Partial<IWorkspace>): Promise<IWorkspace> {
    try {
      const response = await this.post<IWorkspace>("/api/workspaces", data);
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async updateWorkspace(workspaceSlug: string, data: Partial<IWorkspace>): Promise<IWorkspace> {
    try {
      const response = await this.patch<IWorkspace>(`/api/workspaces/${workspaceSlug}`, data);
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async deleteWorkspace(workspaceSlug: string): Promise<unknown> {
    try {
      const response = await this.delete<unknown>(`/api/workspaces/${workspaceSlug}`);
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async inviteWorkspace(workspaceSlug: string, data: IWorkspaceBulkInviteFormData): Promise<unknown> {
    try {
      const response = await this.post<unknown>(`/api/workspaces/${workspaceSlug}/invitations`, data);
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async joinWorkspace(workspaceSlug: string, invitationId: string, data: unknown): Promise<unknown> {
    try {
      const response = await this.post<unknown>(
        `/api/workspaces/${workspaceSlug}/invitations/${invitationId}/join`,
        data,
        {
          headers: {},
        }
      );
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async getWorkspaceInvitation(workspaceSlug: string, invitationId: string): Promise<IWorkspaceMemberInvitation> {
    try {
      const response = await this.get<IWorkspaceMemberInvitation>(
        `/api/workspaces/${workspaceSlug}/invitations/${invitationId}/join/`
      );
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async workspaceSlugCheck(slug: string, signal?: AbortSignal): Promise<TSlugCheckResult> {
    try {
      const response = await this.get<TSlugCheckResult>("/api/instances/workspaces/slug-check", {
        params: { slug },
        signal,
      });
      return response.data;
    } catch (error) {
      if (error instanceof DOMException && error.name === "AbortError") throw error;
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async updateWorkspaceLogo(workspaceSlug: string, data: IWorkspaceLogoUpdateRequest): Promise<void> {
    try {
      await this.patch<void>(`/api/workspaces/${workspaceSlug}/logo`, data);
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async clearWorkspaceLogo(workspaceSlug: string): Promise<void> {
    try {
      await this.delete<void>(`/api/workspaces/${workspaceSlug}/logo`);
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async userWorkspaceInvitations(): Promise<IWorkspaceMemberInvitation[]> {
    try {
      const response = await this.get<IWorkspaceMemberInvitation[]>("/api/users/me/workspaces/invitations");
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }
}
