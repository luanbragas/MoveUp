import { useState } from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";
import { Button } from "../../../shared/ui/Button";
import { Icon } from "../../../shared/ui/Icon";
import { RoundButton } from "../../../shared/ui/RoundButton";
import { Screen } from "../../../shared/ui/Screen";
import { TextField } from "../../../shared/ui/TextField";
import { TextLink } from "../../../shared/ui/TextLink";
import { MIN_TOUCH, palette, radius, spacing, typography } from "../../../shared/ui/theme";
import type { ExerciseDraft, SetDraft, SetType } from "../domain/workout";
import { NumberField } from "./NumberField";
import { strings } from "./strings";

const t = strings.sets;
const TYPES: readonly SetType[] = ["normal", "warmup", "drop", "rest_pause", "failure"];

interface Props {
  readonly exercise: ExerciseDraft;
  readonly onDone: (sets: readonly SetDraft[], notes: string | null) => void;
  readonly onBack: () => void;
}

/** Séries do exercício: uma linha por série (pirâmide, drop, aquecimento) e o esforço alvo. */
export function SetsEditor({ exercise, onDone, onBack }: Props) {
  const [sets, setSets] = useState<readonly SetDraft[]>(exercise.sets);
  const [notes, setNotes] = useState(exercise.notes ?? "");
  const byTime = exercise.trackingType === "time" || exercise.trackingType === "distance_time";
  const withLoad = exercise.trackingType === "reps_load";

  const update = (index: number, patch: Partial<SetDraft>) => {
    setSets((current) => current.map((s, i) => (i === index ? { ...s, ...patch } : s)));
  };

  return (
    <Screen
      header={<RoundButton icon="back" label={strings.editor.back} onPress={onBack} />}
      title={exercise.exerciseName}
      subtitle={t.title}
      footer={
        <Button
          label={t.done}
          icon="check"
          onPress={() => {
            onDone(sets, notes.trim() === "" ? null : notes.trim());
          }}
        />
      }
    >
      <View style={styles.head} accessibilityElementsHidden importantForAccessibility="no">
        <Text style={[styles.th, styles.num]}>{t.number}</Text>
        <Text style={[styles.th, styles.type]}>{t.type}</Text>
        {byTime ? (
          <Text style={styles.th}>{t.time}</Text>
        ) : (
          <>
            <Text style={styles.th}>{t.reps}</Text>
            <Text style={styles.th}>{t.repsMax}</Text>
          </>
        )}
        {withLoad ? <Text style={styles.th}>{t.kg}</Text> : null}
        {exercise.trackingType === "distance_time" ? (
          <Text style={styles.th}>{t.distance}</Text>
        ) : null}
        <Text style={styles.th}>{t.rest}</Text>
        <View style={styles.removeCol} />
      </View>
      {sets.map((set, index) => {
        const n = index + 1;
        const nextType = TYPES[(TYPES.indexOf(set.type) + 1) % TYPES.length] ?? "normal";
        return (
          <View key={index} style={styles.row}>
            <Text style={[typography.label, styles.num, { color: palette.muted }]}>{n}</Text>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel={`${t.type} da série ${String(n)}: ${t.types[set.type]}`}
              onPress={() => {
                update(index, { type: nextType });
              }}
              style={[styles.typeChip, set.type === "normal" ? null : styles.typeChipOn]}
            >
              <Text
                numberOfLines={1}
                style={[
                  typography.small,
                  { color: set.type === "normal" ? palette.textSoft : palette.onLime },
                ]}
              >
                {t.types[set.type]}
              </Text>
            </Pressable>
            {byTime ? (
              <NumberField
                compact
                label={`${t.time}, série ${String(n)}`}
                value={set.durationSeconds}
                onChange={(v) => {
                  update(index, { durationSeconds: v });
                }}
              />
            ) : (
              <>
                <NumberField
                  compact
                  label={`${t.reps}, série ${String(n)}`}
                  value={set.repsMin}
                  onChange={(v) => {
                    update(index, { repsMin: v });
                  }}
                />
                <NumberField
                  compact
                  label={`${t.repsMax}, série ${String(n)}`}
                  value={set.repsMax}
                  onChange={(v) => {
                    update(index, { repsMax: v });
                  }}
                />
              </>
            )}
            {withLoad ? (
              <NumberField
                compact
                decimal
                label={`${t.kg}, série ${String(n)}`}
                value={set.loadKg}
                onChange={(v) => {
                  update(index, { loadKg: v });
                }}
              />
            ) : null}
            {exercise.trackingType === "distance_time" ? (
              <NumberField
                compact
                label={`${t.distance}, série ${String(n)}`}
                value={set.distanceM}
                onChange={(v) => {
                  update(index, { distanceM: v });
                }}
              />
            ) : null}
            <NumberField
              compact
              label={`${t.rest}, série ${String(n)}`}
              value={set.restSeconds}
              onChange={(v) => {
                update(index, { restSeconds: v });
              }}
            />
            <Pressable
              accessibilityRole="button"
              accessibilityLabel={t.removeSet(n)}
              disabled={sets.length === 1}
              onPress={() => {
                setSets((current) => current.filter((_, i) => i !== index));
              }}
              style={styles.removeCol}
            >
              <Icon
                name="close"
                size={18}
                color={sets.length === 1 ? palette.line : palette.muted}
              />
            </Pressable>
          </View>
        );
      })}
      <View style={styles.actions}>
        <TextLink
          label={t.add}
          onPress={() => {
            setSets((current) => {
              const last = current.at(-1);
              return last === undefined ? current : [...current, { ...last, type: "normal" }];
            });
          }}
        />
        {sets.length > 1 ? (
          <TextLink
            label={t.applyAll}
            onPress={() => {
              setSets((current) => {
                const first = current[0];
                return first === undefined
                  ? current
                  : current.map((s) => ({ ...first, type: s.type }));
              });
            }}
          />
        ) : null}
      </View>
      {byTime ? null : (
        <View style={styles.rir}>
          <View style={{ flex: 2 }}>
            <Text style={[typography.label, { color: palette.text }]}>{t.rir}</Text>
            <Text style={[typography.small, { color: palette.muted }]}>{t.rirHint}</Text>
          </View>
          <NumberField
            label={t.rir}
            compact
            value={sets[0]?.targetRir ?? null}
            onChange={(v) => {
              setSets((current) => current.map((s) => ({ ...s, targetRir: v })));
            }}
          />
        </View>
      )}
      <TextField label={t.notes} value={notes} onChangeText={setNotes} />
    </Screen>
  );
}

const styles = StyleSheet.create({
  head: { flexDirection: "row", alignItems: "center", gap: 6 },
  th: { flex: 1, textAlign: "center", ...typography.small, color: palette.muted },
  num: { width: 22, flex: 0, textAlign: "center" },
  type: { width: 76, flex: 0 },
  row: { flexDirection: "row", alignItems: "center", gap: 6 },
  typeChip: {
    width: 76,
    minHeight: 48,
    borderRadius: radius.sm,
    backgroundColor: palette.surface2,
    alignItems: "center",
    justifyContent: "center",
    paddingHorizontal: 4,
  },
  typeChipOn: { backgroundColor: palette.lime },
  removeCol: { width: 32, minHeight: MIN_TOUCH, alignItems: "center", justifyContent: "center" },
  actions: { flexDirection: "row", gap: spacing.md, flexWrap: "wrap" },
  rir: {
    flexDirection: "row",
    alignItems: "center",
    gap: spacing.md,
    backgroundColor: palette.surface,
    borderRadius: radius.lg,
    padding: spacing.md,
  },
});
