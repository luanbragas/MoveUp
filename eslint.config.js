import { baseConfig } from "@moveup/config/eslint";

export default [
  {
    ignores: [
      "**/node_modules/**",
      "**/dist/**",
      "**/coverage/**",
      "backend/**",
      "packages/api-client/src/generated/**",
    ],
  },
  ...baseConfig({ tsconfigRootDir: import.meta.dirname }),
];
