import { baseConfig } from "@moveup/config/eslint";
import { mobileConfig } from "@moveup/config/eslint/mobile";

export default [
  {
    ignores: [
      "**/node_modules/**",
      "**/dist/**",
      "**/coverage/**",
      "**/.expo/**",
      "backend/**",
      "packages/api-client/src/generated/**",
      "apps/mobile/expo-env.d.ts",
    ],
  },
  ...baseConfig({ tsconfigRootDir: import.meta.dirname }),
  ...mobileConfig({
    appDir: `${import.meta.dirname}/apps/mobile`,
    files: ["apps/mobile/**/*.{ts,tsx,js}"],
  }),
];
