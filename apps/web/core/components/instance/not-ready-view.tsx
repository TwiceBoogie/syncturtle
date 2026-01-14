import { GOD_MODE_URL } from "@syncturtle/constants";
// import { useTheme } from "next-themes";
import Link from "next/link";

export const InstanceNotReady = () => (
  <div className="relative">
    <div className="h-screen w-full overflow-hidden overflow-y-auto flex flex-col">
      <div className="container">
        <div className="flex">
          <Link href={`/`} className=""></Link>
        </div>
      </div>

      <div className="absolute inset-0 z-0">{/* image */}</div>

      <div className="relative">
        <div className="h-full w-full">
          <div className="w-auto">
            <div className="relative">
              <h1 className="text-3xl">Welcome aboard Syncturtle!</h1>
              {/* image */}
              <p className="font-medium">Get started by setting up your instance and workspace</p>
            </div>
            <div>
              <a href={GOD_MODE_URL}>
                <button>Get started</button>
              </a>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
);
