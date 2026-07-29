import type { Metadata } from "next";
import type { ReactNode } from "react";

export const metadata: Metadata = {
  title: "Workspace Invitations",
};

export default function WorkspaceInvitationsLayout({ children }: { children: ReactNode }) {
  return <>{children}</>;
}
