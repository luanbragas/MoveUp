import { router } from "expo-router";
import { Text, View } from "react-native";
import { Button } from "../../../shared/ui/Button";
import { Chevrons } from "../../../shared/ui/Chevrons";
import { Screen } from "../../../shared/ui/Screen";
import { TextLink } from "../../../shared/ui/TextLink";
import { Title } from "../../../shared/ui/Title";
import { palette, spacing, typography } from "../../../shared/ui/theme";
import { useAuthState } from "../hooks/use-auth-state";
import { useMe } from "../hooks/use-me";
import { strings } from "./strings";

const t = strings.ready;

/** Fim do cadastro do personal: comemora e leva direto ao primeiro convite. */
export function ReadyScreen() {
  const auth = useAuthState();
  const me = useMe(auth.status === "signed-in" ? auth.user.uid : null);
  const firstName = me.data?.name.trim().split(/\s+/)[0] ?? "";

  return (
    <Screen
      footer={
        <>
          <Button
            label={t.invite}
            icon="plus"
            onPress={() => {
              router.replace("/clients");
              router.push("/clients/new");
            }}
          />
          <TextLink
            label={t.explore}
            onPress={() => {
              router.replace("/dashboard");
            }}
          />
        </>
      }
    >
      <View style={{ marginTop: spacing.xl, gap: spacing.lg }}>
        <Chevrons size={96} count={3} />
        <Title size={46} color={palette.text}>
          {t.title(firstName)}
        </Title>
        <Text style={[typography.body, { color: palette.textSoft }]}>{t.description}</Text>
      </View>
    </Screen>
  );
}
