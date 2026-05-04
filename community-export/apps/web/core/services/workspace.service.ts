import { API_BASE_URL } from "@syncturtle/constants";
import { APIService, HttpError } from "./api.service";
import type { IApiErrorPayload, IWorkspaceMemberInvitation } from "@syncturtle/types";

export class WorkspaceService extends APIService {
  constructor() {
    super(API_BASE_URL);
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
}
