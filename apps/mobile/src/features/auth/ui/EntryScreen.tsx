import { Redirect, type Href } from "expo-router";
import { StyleSheet, View } from "react-native";
import { Button } from "../../../shared/ui/Button";
import { Chevrons } from "../../../shared/ui/Chevrons";
import { Screen } from "../../../shared/ui/Screen";
import { TextLink } from "../../../shared/ui/TextLink";
import { palette } from "../../../shared/ui/theme";
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

  if (destination === "loading") {
    return (
      <View
        accessibilityLabel={strings.entry.loading}
        style={[styles.center, { backgroundColor: palette.background }]}
      >
        <Chevrons size={72} count={3} />
      </View>
    );
  }
  if (destination === "error") {
    return (
      <Screen
        title={strings.entry.errorTitle}
        subtitle={strings.entry.error}
        footer={
          <>
            <Button label={strings.entry.retry} icon="refresh" onPress={retry} />
            <TextLink
              label={strings.entry.signOut}
              onPress={() => {
                signOut.mutate();
              }}
            />
          </>
        }
      />
    );
  }
  return <Redirect href={ROUTES[destination]} />;
}

const styles = StyleSheet.create({
  center: { flex: 1, alignItems: "center", justifyContent: "center" },
});
