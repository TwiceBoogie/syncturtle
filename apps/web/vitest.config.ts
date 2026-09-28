import { mergeConfig } from "vitest/config";
import tsconfigPaths from "vite-tsconfig-paths";
import shared from "../../vitest.shared";

export default mergeConfig(shared, {
  esbuild: { jsx: "automatic" },
  plugins: [tsconfigPaths({ projects: ["./tsconfig.json"] })],
  test: {
    setupFiles: ["./vitest.setup.ts"],
  },
});
