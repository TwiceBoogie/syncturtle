"use client";

import type { FC } from "react";
// import { useState } from "react";
import type { IWorkspaceMemberInvitation } from "@syncturtle/types";
// import { WorkspaceService } from "@/services/workspace.service";
// import { useWorkspace } from "@/hooks/store/use-workspace";
// import { useUserSettings } from "@/hooks/store/user";

interface IWorkspaceJoinInvitesStep {
  invitations: IWorkspaceMemberInvitation[];
  handleNextStep: () => Promise<void>;
  handleCurrentViewChange: () => void;
}

// const workspaceService = new WorkspaceService();

export const WorkspaceJoinInvitesStep: FC<IWorkspaceJoinInvitesStep> = (props) => {
  console.log(props);
  // const { invitations, handleNextStep, handleCurrentViewChange } = props;
  // // states
  // const [isJoiningWorkspaces, setIsJoiningWorkspaces] = useState(false);
  // const [invitationsRespond, setInvitationsRespond] = useState<string[]>([]);
  // // store hooks
  // const { fetchWorkspaces } = useWorkspace();
  // const { fetchCurrentUserSettings } = useUserSettings();
  return <div>hello there</div>;
};
