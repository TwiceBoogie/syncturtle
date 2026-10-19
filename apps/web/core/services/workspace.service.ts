import { APIService, HttpError } from "./api.service";
// constants
import { API_BASE_URL } from "@syncturtle/constants";
// types
import type {
  IApiErrorPayload,
  IWorkspace,
  IWorkspaceBulkInviteFormData,
  IWorkspaceLogoUpdateRequest,
  IWorkspaceMember,
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

  async inviteWorkspace(workspaceSlug: string, data: IWorkspaceBulkInviteFormData): Promise<{ message: string }> {
    try {
      const response = await this.post<{ message: string }>(`/api/workspaces/${workspaceSlug}/invitations`, data);
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  /** @deprecated Disconnected until implement-workspace-invitation-acceptance supplies a verified-principal contract. */
  async joinWorkspace(workspaceSlug: string, invitationId: string, data: unknown): Promise<unknown> {
    void workspaceSlug;
    void invitationId;
    void data;
    throw new Error("Workspace invitation acceptance is not available yet.");
  }

  /** @deprecated Retained for the immediate invitation child; never sends the unsupported bulk POST. */
  async joinWorkspaces(data: { invitations: string[] }): Promise<unknown> {
    void data;
    throw new Error("Workspace invitation acceptance is not available yet.");
  }

  /** @deprecated No supported detail endpoint exists; invitation links must not imply identity or membership. */
  async getWorkspaceInvitation(workspaceSlug: string, invitationId: string): Promise<IWorkspaceMemberInvitation> {
    void workspaceSlug;
    void invitationId;
    throw new Error("Workspace invitation details are not available yet.");
  }

  async workspaceSlugCheck(slug: string, signal?: AbortSignal): Promise<TSlugCheckResult> {
    try {
      const response = await this.get<TSlugCheckResult>("/api/workspaces/slug-check", {
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

  async fetchWorkspaceMembers(workspaceSlug: string): Promise<IWorkspaceMember[]> {
    try {
      const response = await this.get<IWorkspaceMember[]>(`/api/workspaces/${workspaceSlug}/members`);
      return response.data;
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }
}
