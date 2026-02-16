import type { NextConfig } from "next";
import nextra from "nextra";

const withNextra = nextra({});

const BASE_PATH = process.env.NEXT_PUBLIC_BASE_PATH ?? "";

const nextConfig: NextConfig = {
  /* config options here */
  output: "export",
  trailingSlash: true,
  images: {
    unoptimized: true,
  },
  basePath: BASE_PATH || undefined,
  assetPrefix: BASE_PATH || undefined,
  pageExtensions: ["ts", "tsx", "js", "jsx", "md", "mdx"],
  turbopack: {
    resolveAlias: {
      "next-mdx-import-source-file": "./mdx-components.tsx",
    },
  },
};

export default withNextra(nextConfig);
