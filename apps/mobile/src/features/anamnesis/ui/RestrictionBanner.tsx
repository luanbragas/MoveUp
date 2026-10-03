import { router } from "expo-router";
import { Pressable, StyleSheet, Text, View } from "react-native";
import { Icon } from "../../../shared/ui/Icon";
import { MIN_TOUCH, palette, radius, spacing, typography } from "../../../shared/ui/theme";
import { active, restrictionsFor } from "../domain/anamnesis";
import { useRestrictions } from "../hooks/use-anamnesis";
import { restrictionTitle } from "./RestrictionsSection";
import { strings } from "./strings";

const t = strings.restrictions;

/**
 * Aviso no perfil do aluno e no editor (SCREEN-FLOWS 2.3): as restrições ativas, com atalho para a
 * anamnese. Sem restrição ativa, só o atalho.
 */
export function RestrictionBanner({
  linkId,
  name,
  compact = false,
}: {
  readonly linkId: string;
  readonly name?: string;
  readonly compact?: boolean;
}) {
  const restrictions = useRestrictions(linkId);
  const list = active(restrictions.data ?? []);
  const open = () => {
    router.push({ pathname: "/anamnesis/[linkId]", params: { linkId, name: name ?? "" } });
  };
  if (list.length === 0) {
    return compact ? null : (
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={t.open}
        onPress={open}
        style={styles.link}
      >
        <Icon name="doc" size={18} color={palette.lime} />
        <Text style={[typography.label, { color: palette.lime }]}>{t.open}</Text>
      </Pressable>
    );
  }
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`${t.banner(list.length)}: ${list.map(restrictionTitle).join(", ")}. ${t.open}`}
      onPress={open}
      style={styles.card}
    >
      <View style={styles.row}>
        <Icon name="alert" size={18} color={palette.red} />
        <Text style={[typography.label, { color: palette.red, flex: 1 }]}>
          {t.banner(list.length)}
        </Text>
        <Icon name="chevron" size={18} color={palette.muted} />
      </View>
      {list.map((r) => (
        <Text key={r.id} style={[typography.small, { color: palette.textSoft }]}>
          {restrictionTitle(r)}: {r.description}
        </Text>
      ))}
    </Pressable>
  );
}

/** Uma linha de aviso para o exercício que envolve região com restrição ativa (não bloqueia). */
export function RestrictionWarning({
  linkId,
  muscle,
}: {
  readonly linkId: string;
  readonly muscle: string | null;
}) {
  const restrictions = useRestrictions(linkId);
  const hits = restrictionsFor(muscle, restrictions.data ?? []);
  if (hits.length === 0) {
    return null;
  }
  return (
    <View style={styles.warning}>
      <Icon name="alert" size={14} color={palette.red} />
      <Text style={[typography.small, { color: palette.red, flex: 1 }]}>
        {t.warning} {hits.map(restrictionTitle).join(", ")}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: palette.redSoft,
    borderRadius: radius.lg,
    padding: spacing.md,
    gap: spacing.xs,
  },
  row: { flexDirection: "row", alignItems: "center", gap: spacing.sm },
  link: { flexDirection: "row", alignItems: "center", gap: spacing.sm, minHeight: MIN_TOUCH },
  warning: { flexDirection: "row", alignItems: "center", gap: 6 },
});
