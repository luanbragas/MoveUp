import { Redirect, type Href } from "expo-router";
import { ActivityIndicator, StyleSheet, View } from "react-native";
import { Button } from "../../../shared/ui/Button";
import { Message } from "../../../shared/ui/Message";
import { Screen } from "../../../shared/ui/Screen";
import { useColors } from "../../../shared/ui/theme";
import type { Destination } from "../domain/entry";
import { useSignOut } from "../hooks/use-auth-actions";
import { useEntry } from "../hooks/use-entry";
import { strings } from "./strings";

const ROUTES: Readonly<Record<Exclude<Destination, "loading" | "error">, Href>> = {
  welcome: "/welcome",
  register: "/register",
  consents: "/consents",
  guardian: "/guardian",
  "professional-home": "/dashboard",
  "client-home": "/home",
};

/** Rota "/": decide para onde ir conforme sessão, cadastro e onboarding. */
export function EntryScreen() {
  const { destination, retry } = useEntry();
  const signOut = useSignOut();
  const colors = useColors();

  if (destination === "loading") {
    return (
      <View style={[styles.center, { backgroundColor: colors.background }]}>
        <ActivityIndicator accessibilityLabel="Carregando" color={colors.primary} />
      </View>
    );
  }
  if (destination === "error") {
    return (
      <Screen title="MoveUp">
        <Message text={strings.entry.error} />
        <Button label={strings.entry.retry} onPress={retry} />
        <Button
          label={strings.entry.signOut}
          variant="secondary"
          onPress={() => {
            signOut.mutate();
          }}
        />
      </Screen>
    );
  }
  return <Redirect href={ROUTES[destination]} />;
}

const styles = StyleSheet.create({
  center: { flex: 1, alignItems: "center", justifyContent: "center" },
});
