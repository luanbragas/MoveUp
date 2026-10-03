import { router } from "expo-router";
import { StyleSheet, Text, View } from "react-native";
import { Button } from "../../../shared/ui/Button";
import { Icon } from "../../../shared/ui/Icon";
import { palette, radius, spacing, typography } from "../../../shared/ui/theme";
import { useAnamnesisDraft, useMyAnamnesis } from "../hooks/use-anamnesis";
import { strings } from "./strings";

const t = strings.banner;

/** Aviso fixo na Home do aluno enquanto a anamnese não foi enviada (SCREEN-FLOWS 1.2). */
export function AnamnesisBanner() {
  const mine = useMyAnamnesis();
  const draft = useAnamnesisDraft();
  if (!mine.isSuccess || mine.data !== null) {
    return null;
  }
  const started = draft.data != null && Object.keys(draft.data).length > 0;
  return (
    <View style={styles.card}>
      <View style={styles.row}>
        <Icon name="doc" size={20} color={palette.onLime} />
        <Text style={[typography.headline, styles.ink]}>{t.title}</Text>
      </View>
      <Text style={[typography.small, styles.ink]}>{started ? t.draft : t.text}</Text>
      <Button
        label={started ? t.resume : t.action}
        onLime
        icon="arrow"
        onPress={() => {
          router.push("/anamnesis");
        }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: palette.lime,
    borderRadius: radius.lg,
    padding: spacing.md,
    gap: spacing.sm,
  },
  row: { flexDirection: "row", alignItems: "center", gap: spacing.sm },
  ink: { color: palette.onLime },
});
