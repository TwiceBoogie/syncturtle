import type { FC, ReactNode } from "react";
import { WorkspaceAuthWrapper as CoreWorkspaceAuthWrapper } from "@/layouts/auth-layout/workspace-wrapper";

interface IWorkspaceAuthWrapper {
  children: ReactNode;
}

export const WorkspaceAuthWrapper: FC<IWorkspaceAuthWrapper> = ({ children }) => (
  <CoreWorkspaceAuthWrapper>{children}</CoreWorkspaceAuthWrapper>
);
