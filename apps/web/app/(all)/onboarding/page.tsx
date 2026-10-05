"use client";

// hooks
import { useUser } from "@/hooks/store/user";
// components
import { OnboardingRoot } from "@/components/onboarding";
// wrappers
import { AuthenticationWrapper } from "@/lib/wrappers";
// types
import { EPageType } from "@/helpers/authentication.helper";

export default function OnboardingPage() {
  const { data: user } = useUser();

  return (
    <AuthenticationWrapper pageType={EPageType.ONBOARDING}>
      <div className="flex relative size-full overflow-hidden bg-custom-background-90 rounded-lg transition-all ease-in-out duration-300">
        <div className="size-full p-2 grow transition-all ease-in-out duration-300 overflow-hidden">
          <div className="relative flex flex-col h-full w-full overflow-hidden rounded-lg bg-custom-background-100 shadow-md border border-custom-background-200">
            {user && <OnboardingRoot key={user.id} />}
          </div>
        </div>
      </div>
    </AuthenticationWrapper>
  );
}
