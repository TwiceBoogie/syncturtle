import path from "node:path";
import type { NextConfig } from "next";

const ADMIN_BASE_PATH = process.env.NEXT_PUBLIC_ADMIN_BASE_PATH || "/god-mode";

const nextConfig: NextConfig = {
  output: "standalone",
  outputFileTracingRoot: path.join(process.cwd(), "../.."),
  trailingSlash: true,
  basePath: ADMIN_BASE_PATH,
  transpilePackages: ["@syncturtle/constants", "@syncturtle/hooks", "@syncturtle/ui", "@syncturtle/utils"],
  experimental: {
    optimizePackageImports: ["@syncturtle/hooks", "@syncturtle/ui"],
  },
};

export default nextConfig;
