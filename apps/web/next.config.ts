import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  trailingSlash: true,
  transpilePackages: [
    "@t3-oss/env-core",
    "@t3-oss/env-nextjs",
    "@syncturtle/constants",
    "@syncturtle/hooks",
    "@syncturtle/i18n",
    "@syncturtle/ui",
    "@syncturtle/utils",
  ],
  serverExternalPackages: ["esbuild"],
  async rewrites() {
    const posthogHost = process.env.NEXT_PUBLIC_POSTHOG_HOST || "https://app.posthog.com";

    const rewrites = [
      {
        source: "/ingest/static/:path*",
        destination: `${posthogHost}/static/:path*`,
      },
      {
        source: "/ingest/:path*",
        destination: `${posthogHost}/:path*`,
      },
    ];

    const adminBaseUrl = process.env.NEXT_PUBLIC_ADMIN_BASE_URL || "";
    const adminBasePath = process.env.NEXT_PUBLIC_ADMIN_BASE_PATH || "";

    if (adminBaseUrl || adminBasePath) {
      const godModeBaseUrl = `${adminBaseUrl}${adminBasePath}`;

      rewrites.push({
        source: "/god-mode",
        destination: `${godModeBaseUrl}/`,
      });

      rewrites.push({
        source: "/god-mode/:path*",
        destination: `${godModeBaseUrl}/:path*`,
      });
    }

    return rewrites;
  },
};

export default nextConfig;
