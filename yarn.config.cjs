/** @type {import("@yarnpkg/types")} */
const { defineConfig } = require("@yarnpkg/types");

const updateNonPeerDependencies = (Yarn, ident, range) => {
  for (const workspace of Yarn.workspaces()) {
    for (const dependencyType of ["dependencies", "devDependencies", "optionalDependencies"]) {
      const dependencies = workspace[dependencyType];

      if (!dependencies) continue;

      const dependency = dependencies.get(ident);
      if (!dependency) continue;

      dependency.update(range);
    }
  }
};

module.exports = defineConfig({
  async constraints({ Yarn }) {
    const defaultCatalogPackages = [
      "typescript",
      "eslint",
      "prettier",
      "prettier-plugin-tailwindcss",
      "turbo",
      "tsup",
      "vite",
      "@eslint/js",
      "eslint-config-prettier",
      "typescript-eslint",
      "lodash",
      "@types/lodash",
      "clsx",
      "tailwind-merge",
      "tailwindcss",
      "@tailwindcss/postcss",
      "lucide-react",
      "next-themes",
      "swr",
      "zod",
      "intl-messageformat",
      "@t3-oss/env-nextjs",
      "file-type",
      "react-dropzone",
      "string-ts",
      "ufo",
    ];

    for (const ident of defaultCatalogPackages) {
      updateNonPeerDependencies(Yarn, ident, "catalog:");
    }

    const reactCatalogPackages = ["react", "react-dom", "@types/react", "@types/react-dom"];

    for (const ident of reactCatalogPackages) {
      updateNonPeerDependencies(Yarn, ident, "catalog:react19");
    }

    const nextCatalogPackages = ["next", "eslint-config-next", "@types/node"];

    for (const ident of nextCatalogPackages) {
      updateNonPeerDependencies(Yarn, ident, "catalog:next16");
    }

    const herouiCatalogPackages = ["@heroui/react", "@heroui/styles"];

    for (const ident of herouiCatalogPackages) {
      updateNonPeerDependencies(Yarn, ident, "catalog:heroui3");
    }

    const storybookCatalogPackages = ["storybook", "@storybook/addon-docs", "@storybook/react-vite"];

    for (const ident of storybookCatalogPackages) {
      updateNonPeerDependencies(Yarn, ident, "catalog:storybook10");
    }

    const internalPackages = [
      "@syncturtle/constants",
      "@syncturtle/eslint-config",
      "@syncturtle/hooks",
      "@syncturtle/i18n",
      "@syncturtle/tailwind-config",
      "@syncturtle/types",
      "@syncturtle/typescript-config",
      "@syncturtle/ui",
      "@syncturtle/utils",
    ];

    for (const ident of internalPackages) {
      updateNonPeerDependencies(Yarn, ident, "workspace:*");
    }
  },
});
