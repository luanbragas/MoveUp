import { Redirect } from "expo-router";
import type { ReactNode } from "react";
import { StyleSheet, View } from "react-native";
import { Chevrons } from "../../../shared/ui/Chevrons";
import { palette } from "../../../shared/ui/theme";
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

  if (destination === "loading") {
    return (
      <View
        accessibilityLabel="Carregando"
        style={[styles.center, { backgroundColor: palette.background }]}
      >
        <Chevrons size={72} count={3} />
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
