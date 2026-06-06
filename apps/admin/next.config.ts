import type { NextConfig } from "next";

const ADMIN_BASE_PATH = process.env.NEXT_PUBLIC_ADMIN_BASE_PATH || "/god-mode";

console.log("Admin Next.js basePath:", ADMIN_BASE_PATH);

const nextConfig: NextConfig = {
  /* config options here */
  trailingSlash: true,
  basePath: ADMIN_BASE_PATH,
  experimental: {
    optimizePackageImports: ["@syncturtle/hooks"],
  },
};

export default nextConfig;
