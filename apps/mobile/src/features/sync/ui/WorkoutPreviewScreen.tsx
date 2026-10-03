import { router, useLocalSearchParams } from "expo-router";
import { useState } from "react";
import { Linking, Pressable, StyleSheet, Text, View } from "react-native";
import { BodyMap } from "../../../shared/ui/body-map/BodyMap";
import type { MuscleLevels } from "../../../shared/ui/body-map/muscles";
import { Banner } from "../../../shared/ui/Banner";
import { Message } from "../../../shared/ui/Message";
import { RoundButton } from "../../../shared/ui/RoundButton";
import { Screen } from "../../../shared/ui/Screen";
import { Skeleton } from "../../../shared/ui/Skeleton";
import { TextLink } from "../../../shared/ui/TextLink";
import { MIN_TOUCH, palette, radius, spacing, typography } from "../../../shared/ui/theme";
import {
  blockTiming,
  summarize,
  trainingStrings,
  type BlockMethod,
  type TrackingType,
} from "../../training";
import type { ExerciseInfo, PlannedBlock, PlannedExercise } from "../domain/planned";
import { usePlannedSnapshot } from "../hooks/use-planned";
import { strings } from "./strings";

const t = strings.preview;

function levelsOf(info: ExerciseInfo | undefined): MuscleLevels {
  const levels: Record<string, 1 | 2> = {};
  for (const m of info?.secondaryMuscles ?? []) {
    levels[m] = 1;
  }
  if (info?.primaryMuscle != null) {
    levels[info.primaryMuscle] = 2;
  }
  return levels;
}

const SET_TYPES = ["warmup", "normal", "drop", "rest_pause", "failure"] as const;
function setType(value: string): (typeof SET_TYPES)[number] {
  return SET_TYPES.find((type) => type === value) ?? "normal";
}

function summaryOf(exercise: PlannedExercise, info: ExerciseInfo | undefined): string {
  return summarize({
    key: exercise.exerciseId,
    exerciseId: exercise.exerciseId,
    exerciseName: info?.name ?? "",
    trackingType: (info?.trackingType ?? "reps_load") as TrackingType,
    primaryMuscle: info?.primaryMuscle ?? null,
    restSeconds: exercise.restSeconds,
    notes: exercise.notes,
    sets: exercise.sets.map((s) => ({ ...s, type: setType(s.type) })),
  });
}

function timingOf(block: PlannedBlock): string | null {
  return blockTiming({
    key: "b",
    name: block.name,
    method: block.method as BlockMethod,
    preset: block.preset === "tabata" ? "tabata" : null,
    rounds: block.rounds,
    workSeconds: block.workSeconds,
    restSeconds: block.restSeconds,
    restBetweenRounds: block.restBetweenRounds,
    durationSeconds: block.durationSeconds,
    exercises: [],
  });
}

/** Rota /workout/[id] do aluno: o treino do dia, lido do aparelho (funciona sem internet). */
export function WorkoutPreviewScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const snapshot = usePlannedSnapshot();
  const [open, setOpen] = useState<string | null>(null);
  const back = (
    <RoundButton
      icon="back"
      label={t.back}
      onPress={() => {
        router.back();
      }}
    />
  );

  if (snapshot.data === undefined) {
    return (
      <Screen header={back}>
        <Skeleton width="70%" height={44} />
        <Skeleton width="100%" height={140} rounded={24} />
      </Screen>
    );
  }
  const workout = snapshot.data.workouts.find((w) => w.id === id);
  if (workout === undefined) {
    return (
      <Screen header={back}>
        <Message text={t.notFound} />
      </Screen>
    );
  }
  const exercises = snapshot.data.exercises;

  return (
    <Screen
      header={back}
      title={workout.goal ?? workout.name}
      {...(workout.goal === null ? {} : { subtitle: workout.name })}
    >
      <Banner icon="clock" text={t.soon} />
      {workout.notes === null ? null : (
        <Text style={[typography.body, { color: palette.textSoft }]}>{workout.notes}</Text>
      )}
      {workout.blocks.map((block, index) => {
        const timing = timingOf(block);
        const method =
          block.method in trainingStrings.methods
            ? trainingStrings.methods[block.method as BlockMethod].label
            : block.method;
        return (
          <View key={index} style={styles.block}>
            <Text style={[typography.small, { color: palette.muted }]}>{t.block(index + 1)}</Text>
            <Text style={[typography.headline, { color: palette.text }]}>
              {block.name ?? method}
              {timing === null ? "" : ` · ${timing}`}
            </Text>
            {block.exercises.map((exercise, i) => {
              const info = exercises.get(exercise.exerciseId);
              const key = `${String(index)}-${String(i)}`;
              const expanded = open === key;
              return (
                <View key={key} style={styles.exercise}>
                  <Pressable
                    accessibilityRole="button"
                    accessibilityState={{ expanded }}
                    accessibilityLabel={`${info?.name ?? ""}, ${summaryOf(exercise, info)}`}
                    onPress={() => {
                      setOpen(expanded ? null : key);
                    }}
                    style={styles.exerciseMain}
                  >
                    <Text style={[typography.label, { color: palette.text, fontSize: 16 }]}>
                      {info?.name ?? "—"}
                    </Text>
                    <Text style={[typography.small, { color: palette.textSoft }]}>
                      {summaryOf(exercise, info)}
                    </Text>
                    {exercise.notes === null ? null : (
                      <Text style={[typography.small, { color: palette.lime }]}>
                        {exercise.notes}
                      </Text>
                    )}
                  </Pressable>
                  {expanded ? (
                    <View style={styles.howTo}>
                      {info?.primaryMuscle == null ? null : (
                        <View style={{ alignItems: "center" }}>
                          <BodyMap levels={levelsOf(info)} width={260} height={180} />
                        </View>
                      )}
                      {info?.instructions == null ? null : (
                        <>
                          <Text style={[typography.label, { color: palette.text }]}>{t.howTo}</Text>
                          <Text style={[typography.body, { color: palette.textSoft }]}>
                            {info.instructions}
                          </Text>
                        </>
                      )}
                      {info?.mediaUrl == null ? null : (
                        <TextLink
                          label={t.video}
                          onPress={() => {
                            if (info.mediaUrl !== null) {
                              void Linking.openURL(info.mediaUrl);
                            }
                          }}
                        />
                      )}
                    </View>
                  ) : null}
                </View>
              );
            })}
          </View>
        );
      })}
    </Screen>
  );
}

const styles = StyleSheet.create({
  block: {
    backgroundColor: palette.surface,
    borderRadius: radius.lg,
    padding: spacing.md,
    gap: spacing.sm,
  },
  exercise: { backgroundColor: palette.surface2, borderRadius: radius.md, overflow: "hidden" },
  exerciseMain: { padding: spacing.md - 2, gap: 2, minHeight: MIN_TOUCH },
  howTo: { paddingHorizontal: spacing.md - 2, paddingBottom: spacing.md, gap: spacing.sm },
});
