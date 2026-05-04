import js from "@eslint/js";
import tseslint from "typescript-eslint";

export default [
  js.configs.recommended,
  // Teypscript rules (non-type-aware; fast)
  ...tseslint.configs.recommended,

  {
    ignores: ["node_modules/**", "dist/**", "**/*.d.ts"],
  },

  {
    name: "@syncturtle/library-overrides",
    rules: {
      "no-useless-escape": "off",
      "prefer-const": "error",
      "no-irregular-whitespace": "error",
      "no-trailing-spaces": "error",
      "no-duplicate-imports": "error",
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
