import { defineConfig } from "tsup";

export default defineConfig({
  entry: {
    index: "src/index.ts",
    loader: "src/loader.tsx",
  },
  format: ["esm"],
  dts: true,
  sourcemap: true,
  clean: true,
  treeshake: true,
  external: ["react", "react-dom", "@heroui/react"],
});
