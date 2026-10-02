// Config base do ESLint (flat config). Regras do FRONTEND-PATTERN.md, seção 4.
// O app (apps/mobile) estende esta base com react-hooks e eslint-plugin-boundaries.
import js from "@eslint/js";
import prettier from "eslint-config-prettier";
import tseslint from "typescript-eslint";

/**
 * @param {{ tsconfigRootDir: string }} options
 */
export function baseConfig({ tsconfigRootDir }) {
  return tseslint.config(
    js.configs.recommended,
    ...tseslint.configs.strictTypeChecked,
    ...tseslint.configs.stylisticTypeChecked,
    {
      languageOptions: {
        parserOptions: { projectService: true, tsconfigRootDir },
      },
      rules: {
        "@typescript-eslint/no-explicit-any": "error",
        "@typescript-eslint/no-non-null-assertion": "error",
        "@typescript-eslint/ban-ts-comment": [
          "error",
          { "ts-expect-error": "allow-with-description", "ts-ignore": true },
        ],
        "@typescript-eslint/switch-exhaustiveness-check": "error",
        "@typescript-eslint/no-floating-promises": "error",
        "@typescript-eslint/no-misused-promises": "error",
        "@typescript-eslint/consistent-type-imports": "error",
        "no-console": "error",
      },
    },
    {
      files: ["**/*.js", "**/*.cjs", "**/*.mjs"],
      ...tseslint.configs.disableTypeChecked,
    },
    prettier,
  );
}
