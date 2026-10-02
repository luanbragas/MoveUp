// Jest do app (FRONTEND-PATTERN, seção 12). Pacotes do workspace (@moveup/*) são
// TypeScript-fonte e passam pelo Babel como o código do app.
//
// MSW fixado em 2.10.x: a partir da 2.11 ele depende de um pacote só-ESM (rettime)
// que o Jest não carrega via require. Rever ao atualizar o Jest.
/** @type {import('jest').Config} */
module.exports = {
  preset: "jest-expo",
  setupFiles: ["<rootDir>/jest.setup.js"],
  testMatch: ["<rootDir>/src/**/*.test.ts?(x)"],
  // MSW resolve pelos export conditions de Node (msw/native nos testes).
  testEnvironmentOptions: { customExportConditions: [""] },
  // pnpm guarda os pacotes em node_modules/.pnpm/<nome>@<versão>/node_modules/<nome>
  transformIgnorePatterns: [
    "node_modules/(?!(\\.pnpm/[^/]+/node_modules/)?((jest-)?react-native|@react-native(-community)?|expo(nent)?|@expo(nent)?/.*|expo-.*|react-navigation|@react-navigation/.*))",
  ],
};
