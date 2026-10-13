import Link from "next/link";
import Image from "next/image";
import { useTheme } from "next-themes";
// heroui
import { Button } from "@heroui/react";
// constants
import { GOD_MODE_URL } from "@syncturtle/constants";
// assets
import BlackHorizontalLogo from "@/public/syncturtle-logos/syncturtle-lockup-dark.svg";
import WhiteHorizontalLogo from "@/public/syncturtle-logos/syncturtle-lockup-light.svg";
import TurtleShellAssembly from "@/public/turtle-shell-assembly.png";

export const InstanceNotReady = () => {
  const { resolvedTheme } = useTheme();

  const logo = resolvedTheme === "dark" ? BlackHorizontalLogo : WhiteHorizontalLogo;
  return (
    <div className="relative">
      <div className="h-screen w-full overflow-hidden overflow-y-auto flex flex-col">
        <div className="container h-27.5 shrink-0 mx-auto px-5 lg:px-0 flex items-center justify-between gap-5 z-50">
          <div className="flex items-center gap-x-2 py-10">
            <Link href="/">
              <Image src={logo} alt="SyncTurtle" width={133} height={32} className="cursor-pointer" />
            </Link>
          </div>
        </div>

        <div className="absolute inset-0 z-0">{/* image */}</div>

        <div className="relative z-10 mb-27.5 grow">
          <div className="h-full w-full relative container px-5 mx-auto flex justify-center items-center">
            <div className="w-auto max-w-2xl relative space-y-8 py-10">
              <div className="relative flex flex-col justify-center items-center space-y-4">
                <h1 className="text-3xl font-bold pb-3">Welcome aboard SyncTurtle!</h1>
                <Image src={TurtleShellAssembly} alt="SyncTurtle logo" loading="eager" />
                <p className="font-medium text-base">Get started by setting up your instance and workspace</p>
              </div>
              <div>
                <Link href={GOD_MODE_URL}>
                  <Button fullWidth>Get started</Button>
                </Link>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
