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
  readonly title: string;
  readonly subtitle?: string;
  readonly children?: ReactNode;
  /** Puxar para atualizar (listas). */
  readonly refresh?: { readonly refreshing: boolean; readonly onRefresh: () => void };
  /** Linha acima do título (voltar, passos, ação à direita). */
  readonly header?: ReactNode;
}

/** Tela de formulário: área segura, rolagem e teclado sem cobrir os campos. */
export function Screen({ title, subtitle, children, refresh, header }: Props) {
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
          {header}
          <Title size={36}>{title}</Title>
          {subtitle === undefined ? null : (
            <Text style={[typography.body, { color: palette.muted }]}>{subtitle}</Text>
          )}
          <View style={styles.body}>{children}</View>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1 },
  flex: { flex: 1 },
  content: {
    paddingHorizontal: spacing.md + 4,
    paddingTop: spacing.lg,
    paddingBottom: spacing.xl,
    gap: spacing.sm + 2,
    flexGrow: 1,
  },
  body: { marginTop: spacing.lg, gap: spacing.md },
});
