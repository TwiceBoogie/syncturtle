"use client";

import { FC, ReactNode } from "react";
// helpers
import { EPageType } from "@/helpers/authentication.helper";
// import { usePathname, useRouter, useSearchParams } from "next/navigation";

type TPageType = EPageType;

interface IAuthenticationWrapper {
  children: ReactNode;
  pageType?: TPageType;
}

export const AuthenticationWrapper: FC<IAuthenticationWrapper> = (props) => {
  // const pathname = usePathname();
  // const router = useRouter();
  // const searchParams = useSearchParams();
  // const nextPath = searchParams.get("next_path");
  // props
  const { children } = props;

  return <>{children}</>;
};
