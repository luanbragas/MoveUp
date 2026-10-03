// Design tokens do MoveUp, direção "Impacto" (aprovada em 03/10/2026; ver o canvas de design).
// Componentes usam só isto, nunca cor, fonte ou tamanho soltos. O app é escuro por decisão de
// design: lima é a cor das ações e conquistas; vermelho só para dor, risco e erro.

export const palette = {
  background: "#0A0A0B",
  surface: "#161618",
  surface2: "#1C1C1F",
  line: "#26262A",
  text: "#F4F4F5",
  textSoft: "#C4C4CC",
  /** Cinza mínimo para texto: 5,3:1 sobre `surface`. Nada mais escuro em texto. */
  muted: "#8A8A93",
  lime: "#C6FF3D",
  limeDark: "#1F2A12",
  onLime: "#0A0A0B",
  red: "#FF3B3B",
  /** Fundo translúcido de avisos, restrições e ações destrutivas (texto em `red`). */
  redSoft: "rgba(46, 14, 14, 0.58)",
} as const;

export type Colors = typeof palette & {
  // nomes antigos, mantidos para as telas que ainda não foram refeitas
  readonly border: string;
  readonly textMuted: string;
  readonly primary: string;
  readonly onPrimary: string;
  readonly danger: string;
};

const colors: Colors = {
  ...palette,
  border: palette.line,
  textMuted: palette.muted,
  primary: palette.lime,
  onPrimary: palette.onLime,
  danger: palette.red,
};

/** Nomes registrados no `useFonts` (arquivos em assets/fonts, licença OFL). */
export const fonts = {
  /** Títulos em caixa alta: Archivo larga 900. Até ~10 letras por linha. */
  display: "ArchivoExpanded-Black",
  /** Números grandes, sempre com unidade ao lado. */
  number: "Archivo-Black",
  regular: "Manrope-Regular",
  medium: "Manrope-Medium",
  semibold: "Manrope-SemiBold",
  bold: "Manrope-Bold",
  extrabold: "Manrope-ExtraBold",
} as const;

export const spacing = { xs: 4, sm: 8, md: 16, lg: 24, xl: 32 } as const;

export const radius = { sm: 12, md: 18, lg: 24, xl: 32, pill: 999 } as const;

// Com fonte própria não se usa fontWeight (o Android troca para a fonte do sistema).
export const typography = {
  title: { fontFamily: fonts.display, fontSize: 36, lineHeight: 36, textTransform: "uppercase" },
  headline: { fontFamily: fonts.extrabold, fontSize: 22, lineHeight: 28 },
  body: { fontFamily: fonts.medium, fontSize: 16, lineHeight: 23 },
  label: { fontFamily: fonts.bold, fontSize: 14, lineHeight: 19 },
  small: { fontFamily: fonts.semibold, fontSize: 13, lineHeight: 18 },
  button: { fontFamily: fonts.extrabold, fontSize: 17, lineHeight: 22 },
  number: { fontFamily: fonts.number, fontSize: 40, lineHeight: 42 },
} as const;

/** Alvo de toque mínimo (44 pt no iOS, 48 dp no Android): usamos o maior. */
export const MIN_TOUCH = 48;

/** Tema único (escuro). Mantido como hook para os componentes não dependerem do valor fixo. */
export function useColors(): Colors {
  return colors;
}
