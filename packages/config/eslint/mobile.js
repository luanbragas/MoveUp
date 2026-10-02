// ESLint do app (apps/mobile): react-hooks + regras de camada e de feature
// (FRONTEND-PATTERN, seções 4 e 5), verificadas pelo eslint-plugin-boundaries.
import boundaries from "eslint-plugin-boundaries";
import reactHooks from "eslint-plugin-react-hooks";

const same = (layer) => ({
  element: { type: layer, captured: { feature: "{{from.element.captured.feature}}" } },
});
const el = (type) => ({ element: { type } });
const pkg = (source) => ({ module: { source } });

/** Pacotes que a UI e os hooks não podem usar: dado só chega pela porta (data/). */
const DATA_ONLY_PACKAGES = ["@moveup/api-client", "expo-sqlite", "drizzle-orm", "drizzle-orm/**"];

/**
 * @param {{ appDir: string, files: string[] }} options appDir = pasta do app (raiz das regras)
 */
export function mobileConfig({ appDir, files }) {
  return [
    {
      files,
      ...reactHooks.configs.flat["recommended-latest"],
    },
    {
      files,
      plugins: { boundaries },
      settings: {
        "boundaries/root-path": appDir,
        "boundaries/legacy-templates": false,
        // pacotes do workspace (@moveup/*) resolvem para fora do app: contam como externos
        "boundaries/flag-as-external": { outsideRootPath: true },
        "boundaries/ignore": ["**/*.test.ts", "**/*.test.tsx"],
        "import/resolver": { node: { extensions: [".ts", ".tsx", ".js", ".jsx"] } },
        "boundaries/elements": [
          { type: "feature-ui", pattern: "src/features/*/ui", capture: ["feature"] },
          { type: "feature-hooks", pattern: "src/features/*/hooks", capture: ["feature"] },
          { type: "feature-domain", pattern: "src/features/*/domain", capture: ["feature"] },
          { type: "feature-data", pattern: "src/features/*/data", capture: ["feature"] },
          // o primeiro padrão que casa vence: na raiz da feature sobra só o index.ts (API pública)
          { type: "feature-public", pattern: "src/features/*", capture: ["feature"] },
          { type: "providers", pattern: "src/providers" },
          { type: "shared", pattern: "src/shared" },
          { type: "app", pattern: "src/app" },
        ],
      },
      rules: {
        "boundaries/dependencies": [
          "error",
          {
            default: "disallow",
            // também imports de pacotes (por padrão a regra só olha imports locais)
            checkAllOrigins: true,
            // a ÚLTIMA política que casa decide: liberações gerais primeiro, restrições no fim
            policies: [
              // pacotes externos e do Node: liberados (as restrições por camada vêm no fim)
              { allow: { to: { module: { origin: "external" } } } },
              { allow: { to: { module: { origin: "core" } } } },
              // rotas: finas, só montam telas (API pública da feature) e providers
              {
                from: el("app"),
                allow: { to: [el("feature-public"), el("shared"), el("providers"), el("app")] },
              },
              // composition root: cria os adaptadores reais
              {
                from: el("providers"),
                allow: {
                  to: [
                    el("providers"),
                    el("shared"),
                    el("feature-public"),
                    el("feature-data"),
                    el("feature-domain"),
                  ],
                },
              },
              { from: el("shared"), allow: { to: el("shared") } },
              // index.ts de cada feature reexporta o que ela expõe
              {
                from: el("feature-public"),
                allow: {
                  to: [same("feature-ui"), same("feature-hooks"), same("feature-domain")],
                },
              },
              {
                from: el("feature-ui"),
                allow: {
                  to: [
                    same("feature-ui"),
                    same("feature-hooks"),
                    same("feature-domain"),
                    el("feature-public"),
                    el("shared"),
                  ],
                },
              },
              {
                from: el("feature-hooks"),
                allow: {
                  to: [
                    same("feature-hooks"),
                    same("feature-domain"),
                    el("feature-public"),
                    el("providers"),
                    el("shared"),
                  ],
                },
              },
              { from: el("feature-domain"), allow: { to: same("feature-domain") } },
              {
                from: el("feature-data"),
                allow: { to: [same("feature-data"), same("feature-domain"), el("shared")] },
              },

              // UI e hooks não falam com API nem SQLite: só pela porta
              {
                from: [el("feature-ui"), el("feature-hooks")],
                disallow: { to: DATA_ONLY_PACKAGES.map(pkg) },
              },
              // domínio é TypeScript puro
              {
                from: el("feature-domain"),
                disallow: {
                  to: ["react", "react-native", "@tanstack/react-query", ...DATA_ONLY_PACKAGES].map(
                    pkg,
                  ),
                },
              },
            ],
          },
        ],
      },
    },
    {
      // arquivos de configuração do app (CommonJS, rodam no Node)
      files: [`${files[0].split("/**")[0]}/*.js`],
      languageOptions: {
        sourceType: "commonjs",
        globals: { module: "writable", require: "readonly", process: "writable", jest: "readonly" },
      },
      rules: { "@typescript-eslint/no-require-imports": "off" },
    },
  ];
}
