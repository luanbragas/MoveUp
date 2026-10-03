import type { ReactNode } from "react";
import {
  KeyboardAvoidingView,
  Platform,
  RefreshControl,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { palette, spacing, typography } from "./theme";
import { Title } from "./Title";

interface Props {
  /** Caixa alta na Archivo larga; quebre a linha com "\n" (até ~10 letras por linha). */
  readonly title?: string;
  /** Final do título em lima, na linha de baixo. */
  readonly titleAccent?: string;
  readonly subtitle?: string;
  readonly children?: ReactNode;
  /** Puxar para atualizar (listas). */
  readonly refresh?: { readonly refreshing: boolean; readonly onRefresh: () => void };
  /** Linha acima do título (voltar, passos, ação à direita). */
  readonly header?: ReactNode;
  /** Ações fixas embaixo (botão principal, link): ficam fora da rolagem e acima do teclado. */
  readonly footer?: ReactNode;
}

/** Tela base: área segura, rolagem, teclado sem cobrir os campos e rodapé fixo opcional. */
export function Screen({ title, titleAccent, subtitle, children, refresh, header, footer }: Props) {
  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: palette.background }]}>
      <KeyboardAvoidingView
        style={styles.flex}
        behavior={Platform.OS === "ios" ? "padding" : undefined}
      >
        <ScrollView
          contentContainerStyle={styles.content}
          keyboardShouldPersistTaps="handled"
          refreshControl={
            refresh === undefined ? undefined : (
              <RefreshControl
                refreshing={refresh.refreshing}
                onRefresh={refresh.onRefresh}
                tintColor={palette.lime}
              />
            )
          }
        >
          {header === undefined ? null : <View style={styles.header}>{header}</View>}
          {title === undefined ? null : (
            <Title size={40} {...(titleAccent === undefined ? {} : { accent: titleAccent })}>
              {title}
            </Title>
          )}
          {subtitle === undefined ? null : (
            <Text style={[typography.body, { color: palette.muted }]}>{subtitle}</Text>
          )}
          <View style={title === undefined && subtitle === undefined ? styles.bare : styles.body}>
            {children}
          </View>
        </ScrollView>
        {footer === undefined ? null : <View style={styles.footer}>{footer}</View>}
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1 },
  flex: { flex: 1 },
  content: {
    paddingHorizontal: spacing.md + 4,
    paddingTop: spacing.md,
    paddingBottom: spacing.xl,
    gap: spacing.sm + 2,
    flexGrow: 1,
  },
  header: { flexDirection: "row", alignItems: "center", gap: spacing.sm, marginBottom: spacing.sm },
  body: { marginTop: spacing.md, gap: spacing.md },
  bare: { gap: spacing.md, flexGrow: 1 },
  footer: {
    paddingHorizontal: spacing.md + 4,
    paddingTop: spacing.sm,
    paddingBottom: spacing.md,
    gap: spacing.sm,
    backgroundColor: palette.background,
  },
});
