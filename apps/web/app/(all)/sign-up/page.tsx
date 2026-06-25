"use client";

import Link from "next/link";
import Image from "next/image";
// next-themes
import { useTheme } from "next-themes";
// syncturtle imports
import { useTranslation } from "@syncturtle/i18n";
// components
import { AuthRoot } from "@/components/account";
// helpers
import { EAuthModes, EPageType } from "@/helpers/authentication.helper";
// wrappers
import { AuthenticationWrapper } from "@/lib/wrappers";
// assets
import BlackHorizontalLogo from "@/public/syncturtle-logos/syncturtle-logo-dark.png";
import WhiteHorizontalLogo from "@/public/syncturtle-logos/syncturtle-logo-light.png";

export default function SignInPage() {
  const { t } = useTranslation();
  const { resolvedTheme } = useTheme();

  const logo = resolvedTheme === "dark" ? BlackHorizontalLogo : WhiteHorizontalLogo;
  return (
    <AuthenticationWrapper pageType={EPageType.NON_AUTHENTICATED}>
      <div className="relative w-screen h-screen overflow-hidden">
        <div className="absolute inset-0 z-0">{/*image */}</div>
        <div className="relative z-10 w-screen h-screen overflow-hidden overflow-y-auto flex flex-col">
          <div className="container min-w-full px-10 lg:px-20 xl:px-36 shrink-0 relative flex items-center justify-between pb-4 transition-all">
            <div className="flex items-center py-10 gap-x-2">
              <Link href="/" className="h-7.5 w-33.25">
                <Image src={logo} alt="Syncturtle logo" />
              </Link>
            </div>
            <div className="flex flex-col items-end sm:items-center sm:gap-2 sm:flex-row text-center text-sm font-medium text-onboarding-text-300">
              {t("auth.common.already_have_an_account")}
              <Link href={"/"} className="font-semibold text-custom-primary-100 hover:underline">
                {t("auth.common.login")}
              </Link>
            </div>
          </div>
          <div className="flex flex-col justify-center grow container h[100vh-60px] mx-auto max-w-lg px-10 lg:max-w-md lg:px-5 transition-all">
            <AuthRoot authMode={EAuthModes.SIGN_UP} />
          </div>
        </div>
      </div>
    </AuthenticationWrapper>
  );
}
