import { router, useLocalSearchParams } from "expo-router";
import { useState } from "react";
import { Alert, Pressable, StyleSheet, Text, View } from "react-native";
import { toAppError } from "../../../shared/lib/http";
import { Banner } from "../../../shared/ui/Banner";
import { Button } from "../../../shared/ui/Button";
import { EmptyState } from "../../../shared/ui/EmptyState";
import { errorMessage } from "../../../shared/ui/error-messages";
import { Icon, type IconName } from "../../../shared/ui/Icon";
import { Message } from "../../../shared/ui/Message";
import { RoundButton } from "../../../shared/ui/RoundButton";
import { Screen } from "../../../shared/ui/Screen";
import { Skeleton } from "../../../shared/ui/Skeleton";
import { TextField } from "../../../shared/ui/TextField";
import { TextLink } from "../../../shared/ui/TextLink";
import { Title } from "../../../shared/ui/Title";
import { MIN_TOUCH, palette, radius, spacing, typography } from "../../../shared/ui/theme";
import { ExerciseLibraryScreen } from "../../exercise-library";
import {
  blockTiming,
  edit,
  problemsOf,
  summarize,
  totals,
  type BlockDraft,
  type BlockMethod,
  type ExerciseDraft,
  type Workout,
  type WorkoutDraft,
} from "../domain/workout";
import { useSaveWorkout, useWorkout } from "../hooks/use-training";
import { NumberField } from "./NumberField";
import { SetsEditor } from "./SetsEditor";
import { strings } from "./strings";

const t = strings.editor;
const METHODS = Object.keys(strings.methods) as BlockMethod[];

type Mode =
  | { readonly kind: "edit" }
  | { readonly kind: "method" }
  | { readonly kind: "pick"; readonly blockKey: string }
  | { readonly kind: "sets"; readonly blockKey: string; readonly exercise: ExerciseDraft }
  | { readonly kind: "conflict" };

/** Rota /workouts/[id]: carrega e abre o editor. */
export function WorkoutEditorRoute() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const workout = useWorkout(id);

  if (workout.data === undefined) {
    return (
      <Screen
        header={
          <RoundButton
            icon="back"
            label={t.back}
            onPress={() => {
              router.back();
            }}
          />
        }
      >
        {workout.isError ? (
          <>
            <Message text={errorMessage(toAppError(workout.error))} />
            <Button
              label={t.retry}
              variant="secondary"
              icon="refresh"
              onPress={() => {
                void workout.refetch();
              }}
            />
          </>
        ) : (
          <>
            <Skeleton width="70%" height={44} />
            <Skeleton width="100%" height={160} rounded={24} />
          </>
        )}
      </Screen>
    );
  }
  // a chave recria o editor quando chega outra revisão (ex.: "usar a versão salva")
  return (
    <WorkoutEditor
      key={`${workout.data.id}-${String(workout.data.revision)}`}
      workout={workout.data}
      reload={() => workout.refetch()}
    />
  );
}

function SmallIcon({
  icon,
  label,
  onPress,
  disabled = false,
}: {
  readonly icon: IconName;
  readonly label: string;
  readonly onPress: () => void;
  readonly disabled?: boolean;
}) {
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={label}
      disabled={disabled}
      onPress={onPress}
      style={styles.smallIcon}
    >
      <Icon name={icon} size={18} color={disabled ? palette.line : palette.muted} />
    </Pressable>
  );
}

/**
 * Editor de treino (design 5.5): um bloco por cartão, exercícios com o resumo das séries, séries
 * numa tela própria. Grava tudo de uma vez no Salvar, com a revisão lida (If-Match).
 */
export function WorkoutEditor({
  workout,
  reload,
}: {
  readonly workout: Workout;
  readonly reload: () => Promise<unknown>;
}) {
  const [draft, setDraft] = useState<WorkoutDraft>(workout.draft);
  const [dirty, setDirty] = useState(false);
  const [mode, setMode] = useState<Mode>({ kind: "edit" });
  const save = useSaveWorkout(workout.id);
  const problems = problemsOf(draft);
  const sum = totals(draft);

  const change = (next: WorkoutDraft) => {
    setDraft(next);
    setDirty(true);
  };

  const persist = (revision: number) => {
    save.mutate(
      { revision, draft },
      {
        onSuccess: () => {
          setDirty(false);
        },
        onError: (error) => {
          const failure = toAppError(error);
          if (failure.kind === "problem" && failure.code === "version-mismatch") {
            setMode({ kind: "conflict" });
          }
        },
      },
    );
  };

  const leave = () => {
    if (!dirty) {
      router.back();
      return;
    }
    Alert.alert(t.discardTitle, t.discardMessage, [
      { text: t.discardStay, style: "cancel" },
      {
        text: t.discardLeave,
        style: "destructive",
        onPress: () => {
          router.back();
        },
      },
    ]);
  };

  if (mode.kind === "pick") {
    return (
      <ExerciseLibraryScreen
        onPick={(picked) => {
          change(
            edit.addExercises(
              draft,
              mode.blockKey,
              picked.map((e) => ({
                id: e.id,
                name: e.name,
                trackingType: e.trackingType,
                primaryMuscle: e.primaryMuscle,
              })),
            ),
          );
          setMode({ kind: "edit" });
        }}
        onClose={() => {
          setMode({ kind: "edit" });
        }}
      />
    );
  }

  if (mode.kind === "sets") {
    return (
      <SetsEditor
        exercise={mode.exercise}
        onBack={() => {
          setMode({ kind: "edit" });
        }}
        onDone={(sets, notes) => {
          change(edit.updateExercise(draft, mode.blockKey, mode.exercise.key, { sets, notes }));
          setMode({ kind: "edit" });
        }}
      />
    );
  }

  if (mode.kind === "method") {
    return (
      <Screen
        header={
          <RoundButton
            icon="close"
            label={t.back}
            onPress={() => {
              setMode({ kind: "edit" });
            }}
          />
        }
        title={strings.pickMethod.title}
        subtitle={strings.pickMethod.subtitle}
      >
        {METHODS.map((method) => (
          <Pressable
            key={method}
            accessibilityRole="button"
            accessibilityLabel={`${strings.methods[method].label}: ${strings.methods[method].hint}`}
            onPress={() => {
              change(edit.addBlock(draft, method));
              setMode({ kind: "edit" });
            }}
            style={styles.methodRow}
          >
            <View style={{ flex: 1, gap: 2 }}>
              <Text style={[typography.label, { color: palette.text, fontSize: 16 }]}>
                {strings.methods[method].label}
              </Text>
              <Text style={[typography.small, { color: palette.muted }]}>
                {strings.methods[method].hint}
              </Text>
            </View>
            <Icon name="plus" size={20} color={palette.lime} />
          </Pressable>
        ))}
      </Screen>
    );
  }

  if (mode.kind === "conflict") {
    const c = strings.conflict;
    return (
      <Screen
        title={c.title}
        footer={
          <>
            <Button
              label={c.useSaved}
              variant="secondary"
              icon={null}
              onPress={() => {
                save.reset();
                void reload();
              }}
            />
            <Button
              label={c.useMine}
              loading={save.isPending}
              onPress={() => {
                void reload().then((fresh) => {
                  const data = (fresh as { data?: Workout }).data;
                  if (data !== undefined) {
                    setMode({ kind: "edit" });
                    persist(data.revision);
                  }
                });
              }}
            />
          </>
        }
      >
        <Text style={[typography.body, { color: palette.textSoft }]}>{c.text}</Text>
      </Screen>
    );
  }

  const failure = save.isError ? toAppError(save.error) : null;
  return (
    <Screen
      header={
        <>
          <RoundButton icon="back" label={t.back} onPress={leave} />
          <View style={{ flex: 1 }}>
            <Text style={[typography.small, { color: dirty ? palette.lime : palette.muted }]}>
              {dirty ? t.unsaved : `${t.saved} · ${t.version(workout.versionNumber)}`}
            </Text>
          </View>
        </>
      }
      footer={
        <>
          {problems[0] === undefined || !dirty ? null : (
            <Text style={[typography.small, styles.problem]}>{t.problems[problems[0].code]}</Text>
          )}
          <View style={styles.footerRow}>
            <Text style={[typography.small, { color: palette.muted, flex: 1 }]}>
              {t.totals(sum.exercises, sum.sets)}
            </Text>
          </View>
          <Button
            label={t.save}
            icon="check"
            disabled={!dirty || problems.length > 0}
            loading={save.isPending}
            onPress={() => {
              persist(workout.revision);
            }}
          />
        </>
      }
    >
      <Title size={36}>{draft.name.trim() === "" ? t.name : draft.name}</Title>
      <TextField
        label={t.name}
        value={draft.name}
        onChangeText={(name) => {
          change({ ...draft, name });
        }}
        autoCapitalize="sentences"
      />
      <View style={styles.pair}>
        <View style={{ flex: 2 }}>
          <TextField
            label={t.goal}
            value={draft.goal ?? ""}
            placeholder={t.goalPlaceholder}
            onChangeText={(goal) => {
              change({ ...draft, goal: goal.trim() === "" ? null : goal });
            }}
          />
        </View>
        <NumberField
          label={t.minutes}
          value={draft.estimatedMinutes}
          onChange={(estimatedMinutes) => {
            change({ ...draft, estimatedMinutes });
          }}
        />
      </View>
      {failure !== null && failure.kind === "network" ? <Banner text={t.offline} /> : null}
      {failure !== null &&
      failure.kind !== "network" &&
      !(failure.kind === "problem" && failure.code === "version-mismatch") ? (
        <Message text={errorMessage(failure)} />
      ) : null}
      {draft.blocks.length === 0 ? (
        <EmptyState icon="dumbbell" title={t.emptyTitle} text={t.emptyText} />
      ) : null}
      {draft.blocks.map((block, index) => (
        <BlockCard
          key={block.key}
          block={block}
          index={index}
          last={index === draft.blocks.length - 1}
          problem={problems.find((p) => p.blockKey === block.key)?.code ?? null}
          onChange={change}
          draft={draft}
          onAddExercise={() => {
            setMode({ kind: "pick", blockKey: block.key });
          }}
          onEditSets={(exercise) => {
            setMode({ kind: "sets", blockKey: block.key, exercise });
          }}
        />
      ))}
      <Button
        label={t.addBlock}
        variant="secondary"
        icon="plus"
        onPress={() => {
          setMode({ kind: "method" });
        }}
      />
    </Screen>
  );
}

function BlockCard({
  block,
  index,
  last,
  problem,
  draft,
  onChange,
  onAddExercise,
  onEditSets,
}: {
  readonly block: BlockDraft;
  readonly index: number;
  readonly last: boolean;
  readonly problem: keyof typeof t.problems | null;
  readonly draft: WorkoutDraft;
  readonly onChange: (draft: WorkoutDraft) => void;
  readonly onAddExercise: () => void;
  readonly onEditSets: (exercise: ExerciseDraft) => void;
}) {
  const timing = blockTiming(block);
  const set = (patch: Partial<Omit<BlockDraft, "key" | "exercises">>) => {
    onChange(edit.updateBlock(draft, block.key, patch));
  };
  return (
    <View style={[styles.block, problem === null ? null : styles.blockProblem]}>
      <View style={styles.blockHead}>
        <View style={{ flex: 1 }}>
          <Text style={[typography.small, { color: palette.muted }]}>{t.block(index + 1)}</Text>
          <Text style={[typography.headline, { color: palette.text }]}>
            {strings.methods[block.method].label}
            {timing === null ? "" : ` · ${timing}`}
          </Text>
        </View>
        <SmallIcon
          icon="up"
          label={`${t.moveUp}: ${t.block(index + 1)}`}
          disabled={index === 0}
          onPress={() => {
            onChange(edit.moveBlock(draft, block.key, -1));
          }}
        />
        <SmallIcon
          icon="down"
          label={`${t.moveDown}: ${t.block(index + 1)}`}
          disabled={last}
          onPress={() => {
            onChange(edit.moveBlock(draft, block.key, 1));
          }}
        />
        <SmallIcon
          icon="trash"
          label={`${t.remove}: ${t.block(index + 1)}`}
          onPress={() => {
            onChange(edit.removeBlock(draft, block.key));
          }}
        />
      </View>

      {block.method === "hiit" || block.method === "intervals" ? (
        <View style={styles.pair}>
          <NumberField
            label={t.rounds}
            value={block.rounds}
            onChange={(rounds) => {
              set({ rounds, preset: null });
            }}
          />
          <NumberField
            label={t.work}
            value={block.workSeconds}
            onChange={(workSeconds) => {
              set({ workSeconds, preset: null });
            }}
          />
          <NumberField
            label={t.rest}
            value={block.restSeconds}
            onChange={(restSeconds) => {
              set({ restSeconds, preset: null });
            }}
          />
        </View>
      ) : null}
      {block.method === "emom" || block.method === "amrap" ? (
        <View style={styles.pair}>
          <NumberField
            label={t.duration}
            value={block.durationSeconds === null ? null : Math.round(block.durationSeconds / 60)}
            onChange={(minutes) => {
              set({ durationSeconds: minutes === null ? null : minutes * 60 });
            }}
          />
        </View>
      ) : null}
      {block.method === "circuit" ? (
        <View style={styles.pair}>
          <NumberField
            label={t.rounds}
            value={block.rounds}
            onChange={(rounds) => {
              set({ rounds });
            }}
          />
          <NumberField
            label={t.restRounds}
            value={block.restBetweenRounds}
            onChange={(restBetweenRounds) => {
              set({ restBetweenRounds });
            }}
          />
        </View>
      ) : null}

      {block.exercises.map((exercise, i) => (
        <View key={exercise.key} style={styles.exercise}>
          <Pressable
            accessibilityRole="button"
            accessibilityLabel={`${exercise.exerciseName}, ${summarize(exercise)}. ${t.editSets}`}
            onPress={() => {
              onEditSets(exercise);
            }}
            style={styles.exerciseMain}
          >
            <Text style={[typography.label, { color: palette.text, fontSize: 16 }]}>
              {exercise.exerciseName}
            </Text>
            <Text style={[typography.small, { color: palette.textSoft }]}>
              {summarize(exercise)}
            </Text>
          </Pressable>
          <SmallIcon
            icon="up"
            label={`${t.moveUp}: ${exercise.exerciseName}`}
            disabled={i === 0}
            onPress={() => {
              onChange(edit.moveExercise(draft, block.key, exercise.key, -1));
            }}
          />
          <SmallIcon
            icon="close"
            label={`${t.remove}: ${exercise.exerciseName}`}
            onPress={() => {
              onChange(edit.removeExercise(draft, block.key, exercise.key));
            }}
          />
        </View>
      ))}
      {problem === null ? null : (
        <Text style={[typography.small, { color: palette.red }]}>{t.problems[problem]}</Text>
      )}
      <TextLink label={t.addExercise} onPress={onAddExercise} />
    </View>
  );
}

const styles = StyleSheet.create({
  pair: { flexDirection: "row", gap: spacing.sm, alignItems: "flex-end" },
  footerRow: { flexDirection: "row", alignItems: "center" },
  problem: { color: palette.muted, textAlign: "center" },
  methodRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: spacing.md,
    backgroundColor: palette.surface,
    borderRadius: radius.lg,
    padding: spacing.md,
    minHeight: MIN_TOUCH + 20,
  },
  block: {
    backgroundColor: palette.surface,
    borderRadius: radius.lg,
    padding: spacing.md,
    gap: spacing.sm + 2,
  },
  blockProblem: { borderWidth: 1, borderColor: palette.red },
  blockHead: { flexDirection: "row", alignItems: "center" },
  smallIcon: {
    width: 40,
    height: MIN_TOUCH,
    alignItems: "center",
    justifyContent: "center",
  },
  exercise: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: palette.surface2,
    borderRadius: radius.md,
    paddingLeft: spacing.md,
  },
  exerciseMain: { flex: 1, paddingVertical: spacing.sm + 2, gap: 2, minHeight: MIN_TOUCH },
});
