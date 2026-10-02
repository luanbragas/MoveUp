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
import { spacing, typography, useColors } from "./theme";

interface Props {
  readonly title: string;
  readonly subtitle?: string;
  readonly children?: ReactNode;
  /** Puxar para atualizar (listas). */
  readonly refresh?: { readonly refreshing: boolean; readonly onRefresh: () => void };
}

/** Tela de formulário: área segura, rolagem e teclado sem cobrir os campos. */
export function Screen({ title, subtitle, children, refresh }: Props) {
  const colors = useColors();
  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]}>
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
                tintColor={colors.primary}
              />
            )
          }
        >
          <Text accessibilityRole="header" style={[typography.title, { color: colors.text }]}>
            {title}
          </Text>
          {subtitle === undefined ? null : (
            <Text style={[typography.body, { color: colors.textMuted }]}>{subtitle}</Text>
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
  content: { padding: spacing.lg, gap: spacing.sm, flexGrow: 1 },
  body: { marginTop: spacing.lg, gap: spacing.md },
});
