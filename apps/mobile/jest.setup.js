// Ambiente dos testes: URL fictícia da API e NetInfo simulado (módulo nativo).
process.env.EXPO_PUBLIC_API_URL = "https://api.example.test";

jest.mock("@react-native-community/netinfo", () =>
  require("@react-native-community/netinfo/jest/netinfo-mock.js"),
);
process.env.EXPO_PUBLIC_FIREBASE_API_KEY = "chave-de-teste-123";
process.env.EXPO_PUBLIC_FIREBASE_PROJECT_ID = "moveup-teste";
process.env.EXPO_PUBLIC_FIREBASE_APP_ID = "1:123:android:abc";
