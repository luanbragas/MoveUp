import { router } from "expo-router";
import { StyleSheet, Text, View } from "react-native";
import { Button } from "../../../shared/ui/Button";
import { Chevrons } from "../../../shared/ui/Chevrons";
import { Icon } from "../../../shared/ui/Icon";
import { Screen } from "../../../shared/ui/Screen";
import { TextLink } from "../../../shared/ui/TextLink";
import { fonts, palette, radius, spacing, typography } from "../../../shared/ui/theme";
import { setPendingRole } from "../hooks/pending-role";
import { strings } from "./strings";

const t = strings.welcome;

/**
 * Boas-vindas (SCREEN-FLOWS 0.2): o produto como herói (série feita, descanso, mensagem do
 * personal) e os dois caminhos. O papel escolhido aqui já abre o cadastro certo.
 */
export function WelcomeScreen() {
  return (
    <Screen
      footer={
        <>
          <Button
            label={t.professional}
            onPress={() => {
              setPendingRole("professional");
              router.push({ pathname: "/sign-in", params: { mode: "sign-up" } });
            }}
          />
          <Button
            label={t.client}
            variant="secondary"
            icon={null}
            onPress={() => {
              setPendingRole("client");
              router.push({ pathname: "/sign-in", params: { mode: "sign-up" } });
            }}
          />
          <TextLink
            before={t.hasAccount}
            label={t.signIn}
            onPress={() => {
              router.push({ pathname: "/sign-in", params: { mode: "sign-in" } });
            }}
          />
        </>
      }
    >
      <View style={styles.brand}>
        <Chevrons size={26} count={2} />
        <Text style={styles.brandText}>
          MOVE<Text style={{ color: palette.lime }}>UP</Text>
        </Text>
      </View>

      <View style={styles.collage} accessible accessibilityLabel={t.collage}>
        <View style={styles.bigChevrons}>
          <Chevrons size={260} count={3} opacity={0.12} />
        </View>
        <View style={[styles.card, styles.setCard]}>
          <View style={styles.setHead}>
            <View style={styles.check}>
              <Icon name="check" size={14} color={palette.lime} strokeWidth={3.2} />
            </View>
            <Text style={[typography.label, { color: palette.onLime }]}>{t.setDone}</Text>
          </View>
          <Text style={styles.setValue}>
            {t.setValue} <Text style={styles.setReps}>{t.setReps}</Text>
          </Text>
        </View>
        <View style={[styles.card, styles.restCard]}>
          <Text style={[typography.small, { color: palette.muted }]}>{t.rest}</Text>
          <Text style={styles.restValue}>{t.restValue}</Text>
        </View>
        <View style={[styles.card, styles.chatCard]}>
          <View style={styles.avatar}>
            <Text style={[typography.label, { color: palette.lime }]}>AS</Text>
          </View>
          <View>
            <Text style={[typography.small, { color: palette.muted, fontSize: 12 }]}>
              {t.coachName}
            </Text>
            <Text style={[typography.label, { color: palette.text }]}>{t.coachMessage}</Text>
          </View>
        </View>
      </View>

      <Text accessibilityRole="header" style={styles.title}>
        {t.title.toUpperCase()}
      </Text>
      <Text style={[typography.body, { color: palette.textSoft }]}>{t.description}</Text>
    </Screen>
  );
}

const styles = StyleSheet.create({
  brand: { flexDirection: "row", alignItems: "center", gap: spacing.sm },
  brandText: { fontFamily: fonts.display, fontSize: 18, color: palette.text, letterSpacing: 0.4 },
  collage: { height: 300, marginTop: spacing.md },
  bigChevrons: { position: "absolute", right: -40, top: -10 },
  card: { position: "absolute", borderRadius: radius.lg, padding: spacing.md },
  setCard: {
    top: 20,
    left: 0,
    width: 230,
    backgroundColor: palette.lime,
    transform: [{ rotate: "-5deg" }],
    gap: 4,
  },
  setHead: { flexDirection: "row", alignItems: "center", gap: spacing.sm },
  check: {
    width: 26,
    height: 26,
    borderRadius: 13,
    backgroundColor: palette.onLime,
    alignItems: "center",
    justifyContent: "center",
  },
  setValue: { fontFamily: fonts.number, fontSize: 32, color: palette.onLime },
  setReps: { fontFamily: fonts.bold, fontSize: 17 },
  restCard: {
    top: 118,
    right: 0,
    width: 200,
    backgroundColor: palette.surface2,
    transform: [{ rotate: "4deg" }],
  },
  restValue: { fontFamily: fonts.number, fontSize: 46, lineHeight: 50, color: palette.text },
  chatCard: {
    top: 222,
    left: 24,
    flexDirection: "row",
    alignItems: "center",
    gap: spacing.sm + 4,
    backgroundColor: palette.surface,
    borderRadius: radius.pill,
    paddingVertical: spacing.sm + 2,
    paddingRight: spacing.lg,
  },
  avatar: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: palette.limeDark,
    alignItems: "center",
    justifyContent: "center",
  },
  title: {
    fontFamily: fonts.display,
    fontSize: 44,
    lineHeight: 42,
    color: palette.text,
    marginTop: spacing.md,
  },
});
