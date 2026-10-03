import { Linking, Pressable, StyleSheet, Text, View } from "react-native";
import { BodyMap } from "../../../shared/ui/body-map/BodyMap";
import { Icon } from "../../../shared/ui/Icon";
import { TextLink } from "../../../shared/ui/TextLink";
import { MIN_TOUCH, palette, radius, spacing, typography } from "../../../shared/ui/theme";
import { muscleLevels, type Exercise } from "../domain/exercise";
import { strings } from "./strings";

const t = strings.library;

interface Props {
  readonly exercise: Exercise;
  readonly expanded: boolean;
  readonly onToggle: () => void;
  /** Modo escolha (editor de treino): caixa à direita. */
  readonly selected?: boolean;
  readonly onSelect?: () => void;
  readonly onArchive?: () => void;
}

export function subtitleOf(exercise: Exercise): string {
  const muscle = exercise.primaryMuscle === null ? null : strings.muscles[exercise.primaryMuscle];
  return [muscle ?? strings.modalities[exercise.modality], exercise.equipment]
    .filter((part) => part !== null)
    .join(" · ");
}

/** Um exercício na lista: toque abre "como fazer" com o mapa muscular. */
export function ExerciseRow({
  exercise,
  expanded,
  onToggle,
  selected,
  onSelect,
  onArchive,
}: Props) {
  const picking = onSelect !== undefined;
  return (
    <View style={[styles.card, selected === true ? styles.cardSelected : null]}>
      <View style={styles.row}>
        <Pressable
          accessibilityRole="button"
          accessibilityLabel={`${exercise.name}, ${subtitleOf(exercise)}`}
          accessibilityState={{ expanded }}
          onPress={onToggle}
          style={styles.main}
        >
          <View style={styles.texts}>
            <View style={styles.nameRow}>
              <Text style={[typography.label, styles.name]} numberOfLines={2}>
                {exercise.name}
              </Text>
              {exercise.custom ? (
                <View style={styles.tag}>
                  <Text style={[typography.small, { color: palette.onLime, fontSize: 11 }]}>
                    {t.custom}
                  </Text>
                </View>
              ) : null}
            </View>
            <Text style={[typography.small, { color: palette.muted }]}>{subtitleOf(exercise)}</Text>
          </View>
          {picking ? null : <Icon name="chevron" size={18} color={palette.muted} />}
        </Pressable>
        {picking ? (
          <Pressable
            accessibilityRole="checkbox"
            accessibilityLabel={exercise.name}
            accessibilityState={{ checked: selected === true }}
            onPress={onSelect}
            style={styles.checkArea}
          >
            <View style={[styles.check, selected === true ? styles.checkOn : null]}>
              {selected === true ? (
                <Icon name="check" size={16} color={palette.onLime} strokeWidth={3} />
              ) : null}
            </View>
          </Pressable>
        ) : null}
      </View>
      {expanded ? (
        <View style={styles.detail}>
          {exercise.primaryMuscle === null ? null : (
            <View style={styles.map}>
              <BodyMap levels={muscleLevels(exercise)} width={260} height={180} />
            </View>
          )}
          {exercise.instructions === null ? null : (
            <>
              <Text style={[typography.label, { color: palette.text }]}>{t.howTo}</Text>
              <Text style={[typography.body, { color: palette.textSoft }]}>
                {exercise.instructions}
              </Text>
            </>
          )}
          <View style={styles.actions}>
            {exercise.mediaUrl === null ? null : (
              <TextLink
                label={t.video}
                onPress={() => {
                  if (exercise.mediaUrl !== null) {
                    void Linking.openURL(exercise.mediaUrl);
                  }
                }}
              />
            )}
            {onArchive === undefined || !exercise.custom ? null : (
              <TextLink label={t.archive} onPress={onArchive} />
            )}
          </View>
        </View>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  card: { backgroundColor: palette.surface, borderRadius: radius.lg, overflow: "hidden" },
  cardSelected: { borderWidth: 2, borderColor: palette.lime },
  row: { flexDirection: "row", alignItems: "center" },
  main: {
    flex: 1,
    minHeight: MIN_TOUCH + 16,
    flexDirection: "row",
    alignItems: "center",
    gap: spacing.sm,
    paddingVertical: spacing.sm + 4,
    paddingLeft: spacing.md,
    paddingRight: spacing.sm,
  },
  texts: { flex: 1, gap: 2 },
  nameRow: { flexDirection: "row", alignItems: "center", gap: spacing.sm },
  name: { color: palette.text, fontSize: 16, flexShrink: 1 },
  tag: {
    backgroundColor: palette.lime,
    borderRadius: radius.pill,
    paddingHorizontal: 8,
    paddingVertical: 1,
  },
  checkArea: {
    width: MIN_TOUCH + 8,
    alignSelf: "stretch",
    alignItems: "center",
    justifyContent: "center",
  },
  check: {
    width: 28,
    height: 28,
    borderRadius: 9,
    borderWidth: 2,
    borderColor: palette.line,
    alignItems: "center",
    justifyContent: "center",
  },
  checkOn: { backgroundColor: palette.lime, borderColor: palette.lime },
  detail: { paddingHorizontal: spacing.md, paddingBottom: spacing.md, gap: spacing.sm },
  map: { alignItems: "center", paddingVertical: spacing.sm },
  actions: { flexDirection: "row", gap: spacing.md },
});
