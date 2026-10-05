"use client";

import { useRouter } from "next/navigation";
// hooks
import { useUser } from "@/hooks/store/user";
// components
import { WorkspaceSetupStep } from "@/components/onboarding/steps/workspace";
// wrappers
import { AuthenticationWrapper } from "@/lib/wrappers";
// helpers
import { EPageType } from "@/helpers/authentication.helper";

export default function CreateWorkspacePage() {
  const router = useRouter();
  const { data: user } = useUser();
  return (
    <AuthenticationWrapper pageType={EPageType.AUTHENTICATED}>
      <div className="mx-auto max-w-sm p-8">
        {user && (
          <WorkspaceSetupStep
            key={user.id}
            onComplete={async (space) => {
              router.replace(`/${space.slug}`);
            }}
          />
        )}
      </div>
    </AuthenticationWrapper>
  );
}
