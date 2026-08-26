import { defineConfig, globalIgnores } from "eslint/config";
import prettier from "eslint-config-prettier/flat";
import nextVitals from "eslint-config-next/core-web-vitals";
import nextTs from "eslint-config-next/typescript";

export default defineConfig([
  ...nextVitals,
  ...nextTs,
  prettier,

  globalIgnores([".next/**", "out/**", "build/**", "dist/**", "coverage/**", "storybook-static/**", "next-env.d.ts"]),

  {
    name: "@syncturtle/next-overrides",
    rules: {
      "prefer-const": "error",
      "no-irregular-whitespace": "error",
      "no-trailing-spaces": "error",
      "no-duplicate-imports": [
        "error",
        {
          allowSeparateTypeImports: true,
        },
      ],
      "no-useless-catch": "warn",
      "no-case-declarations": "error",
      "no-unreachable": "error",
      "arrow-body-style": ["error", "as-needed"],

      "@next/next/no-html-link-for-pages": "off",
      "@next/next/no-img-element": "off",

      "@typescript-eslint/no-unused-expressions": "warn",
      "@typescript-eslint/no-unused-vars": [
        "warn",
        {
          argsIgnorePattern: "^_",
          varsIgnorePattern: "^_",
          caughtErrorsIgnorePattern: "^_",
        },
      ],
      "@typescript-eslint/no-explicit-any": "warn",
      "@typescript-eslint/prefer-ts-expect-error": "warn",
    },
  },
]);
