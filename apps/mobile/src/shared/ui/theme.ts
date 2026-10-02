import { useColorScheme } from "react-native";

// Design tokens (FRONTEND-PATTERN, seção 10): cores por tema, espaçamento e tipografia.
// Componentes usam só isto, nunca cor ou tamanho solto.

const light = {
  background: "#FFFFFF",
  surface: "#F4F4F6",
  text: "#111114",
  textMuted: "#55555C",
  border: "#D4D4DA",
  primary: "#1D4ED8",
  onPrimary: "#FFFFFF",
  danger: "#B42318",
};

const dark: typeof light = {
  background: "#0B0B0C",
  surface: "#1A1A1D",
  text: "#F5F5F7",
  textMuted: "#A1A1A8",
  border: "#3A3A40",
  primary: "#6E9BFF",
  onPrimary: "#0B0B0C",
  danger: "#FF8A80",
};

export type Colors = typeof light;

export const spacing = { xs: 4, sm: 8, md: 16, lg: 24, xl: 32 } as const;

export const typography = {
  title: { fontSize: 28, fontWeight: "700" },
  body: { fontSize: 17, lineHeight: 24 },
  label: { fontSize: 15, fontWeight: "600" },
  small: { fontSize: 14, lineHeight: 20 },
} as const;

/** Alvo de toque mínimo (44 pt no iOS, 48 dp no Android): usamos o maior. */
export const MIN_TOUCH = 48;

export function useColors(): Colors {
  return useColorScheme() === "dark" ? dark : light;
}
