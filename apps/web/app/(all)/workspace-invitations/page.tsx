"use client";

import Link from "next/link";
import { EPageType } from "@/helpers/authentication.helper";
import { AuthenticationWrapper } from "@/lib/wrappers";

export default function WorkspaceInvitationsPage() {
  return (
    <AuthenticationWrapper pageType={EPageType.PUBLIC}>
      <div className="flex h-full w-full flex-col items-center justify-center gap-4 px-3 text-center">
        <h1 className="text-xl font-semibold">Invitation acceptance is not available yet</h1>
        <p>A pending invitation does not grant access to a space. Contact the person who invited you for guidance.</p>
        <Link href="/">Continue to SyncTurtle</Link>
      </div>
    </AuthenticationWrapper>
  );
}
