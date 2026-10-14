"use client";

import Link from "next/link";
import Image from "next/image";
import { useTheme } from "next-themes";
// assets
import BlackHorizontalLogo from "@/public/syncturtle-logos/syncturtle-lockup-dark.svg";
import WhiteHorizontalLogo from "@/public/syncturtle-logos/syncturtle-lockup-light.svg";

export const AuthHeader = () => {
  const { resolvedTheme } = useTheme();

  const logo = resolvedTheme === "dark" ? BlackHorizontalLogo : WhiteHorizontalLogo;
  return (
    <div className="flex items-center justify-between gap-6 w-full shrink-0 sticky top-0">
      <Link href={`/`}>
        <Image src={logo} alt="SyncTurtle" width={133} height={32} />
      </Link>
    </div>
  );
};
