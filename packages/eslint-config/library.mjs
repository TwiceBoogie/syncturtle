import js from "@eslint/js";
import prettier from "eslint-config-prettier/flat";
import tseslint from "typescript-eslint";

export default [
  js.configs.recommended,
  ...tseslint.configs.recommended,
  prettier,

  {
    ignores: ["node_modules/**", "dist/**", "build/**", "coverage/**", "storybook-static/**", "**/*.d.ts"],
  },

  {
    name: "@syncturtle/library-overrides",
    rules: {
      "no-useless-escape": "off",
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
      "@typescript-eslint/no-useless-empty-export": "error",
      "@typescript-eslint/prefer-ts-expect-error": "warn",
    },
  },
];
