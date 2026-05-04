"use client";

import Link from "next/link";
// next-themes
// import { useTheme } from "next-themes";
// components
import { AuthRoot } from "@/components/account";
import { PageHead } from "@/components/core";
// constants
// helpers
import { EAuthModes, EPageType } from "@/helpers/authentication.helper";
// hooks
import { useTranslation } from "@syncturtle/i18n";
import { useInstance } from "@/hooks/store/use-instance";
// layouts
import DefaultLayout from "@/layouts/default-layout";
// wrappers
import { AuthenticationWrapper } from "@/lib/wrappers/authentication-wrapper";

export default function Home() {
  // theme
  // const { resolvedTheme } = useTheme();
  // i18n hook
  const { t } = useTranslation();
  // store hooks
  const { config } = useInstance();
  // derived
  const enableSignupConfig = config?.enableSignup ?? false;
  return (
    <DefaultLayout>
      <AuthenticationWrapper pageType={EPageType.NON_AUTHENTICATED}>
        <div className="relative w-screen h-screen overflow-hidden">
          <PageHead title={t("auth.common.login") + " - Syncturtle"} />
          <div className="absolute inset-0 z-0">{/* image */}</div>
          <div className="relative z-10 flex flex-col w-screen h-screen overflow-hidden overflow-y-auto">
            <div className="container relative flex items-center justify-between shrink-0 min-w-full px-10 pb-4 transition-all lg:px-20 xl:px-36">
              <div className="py-10">
                <Link href="/">Syncturtle</Link>
              </div>
              {enableSignupConfig && (
                <div className="flex flex-col items-end text-sm font-medium text-center sm:items-center sm:gap-2 sm:flex-row text-onboarding-text-300">
                  {t("auth.common.new_to_syncturtle")}
                  <Link href={`/sign-up`} className="font-semibold text-custom-primary-100 hover:underline">
                    {t("auth.common.create_account")}
                  </Link>
                </div>
              )}
            </div>
            <div className="flex flex-col justify-center grow container h-[100vh-60px] mx-auto max-w-lg px-10 lg:max-w-md lg:px-5 transition-all">
              <AuthRoot authMode={EAuthModes.SIGN_IN} />
            </div>
          </div>
        </div>
      </AuthenticationWrapper>
    </DefaultLayout>
  );
}
