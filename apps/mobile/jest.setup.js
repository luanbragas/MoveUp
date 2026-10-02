// Ambiente dos testes: URL fictícia da API e NetInfo simulado (módulo nativo).
process.env.EXPO_PUBLIC_API_URL = "https://api.example.test";

jest.mock("@react-native-community/netinfo", () =>
  require("@react-native-community/netinfo/jest/netinfo-mock.js"),
);
