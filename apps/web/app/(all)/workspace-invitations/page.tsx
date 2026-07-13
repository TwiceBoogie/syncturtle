"use client";

import { Check, X } from "lucide-react";
import { EmptySpace, EmptySpaceItem } from "@/components/ui/empty-space";
import { EPageType } from "@/helpers/authentication.helper";
import { AuthenticationWrapper } from "@/lib/wrappers";

export default function WorkspaceInvitationsPage() {
  return (
    <AuthenticationWrapper pageType={EPageType.PUBLIC}>
      <div className="flex h-full w-full flex-col items-center justify-center px-3">
        <EmptySpace
          title={`You have been invited to Marvel`}
          description="Your workspace is where you'll create projects, collaborate on your work items, and organize different streams of work in your Syncturtle account."
        >
          <EmptySpaceItem Icon={Check} title="Accept" action={() => console.log("Accepted")} />
          <EmptySpaceItem Icon={X} title="Ignore" action={() => console.log("Ignored")} />
        </EmptySpace>
      </div>
    </AuthenticationWrapper>
  );
}
