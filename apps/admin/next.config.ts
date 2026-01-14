import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  /* config options here */
  trailingSlash: true,
  basePath: process.env.NEXT_PUBLIC_ADMIN_BASE_PATH || "",
};

export default nextConfig;
