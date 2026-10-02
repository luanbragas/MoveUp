import { Redirect } from "expo-router";
import type { ReactNode } from "react";
import { ActivityIndicator, StyleSheet, View } from "react-native";
import { useColors } from "../../../shared/ui/theme";
import { useEntry } from "../hooks/use-entry";

interface Props {
  /** Só renderiza o grupo quando o destino da conta é este; senão volta para "/". */
  readonly home: "professional-home" | "client-home";
  readonly children: ReactNode;
}

/**
 * Porteiro dos grupos (client) e (professional): um papel nunca abre as rotas do outro, nem
 * antes de terminar o onboarding (SCREEN-FLOWS 0.1).
 */
export function RoleGate({ home, children }: Props) {
  const { destination } = useEntry();
  const colors = useColors();

  if (destination === "loading") {
    return (
      <View style={[styles.center, { backgroundColor: colors.background }]}>
        <ActivityIndicator accessibilityLabel="Carregando" color={colors.primary} />
      </View>
    );
  }
  if (destination !== home) {
    return <Redirect href="/" />;
  }
  return children;
}

const styles = StyleSheet.create({
  center: { flex: 1, alignItems: "center", justifyContent: "center" },
});
