import { router, useLocalSearchParams } from "expo-router";
import { useEffect, useState } from "react";
import { Alert, Pressable, StyleSheet, Text, View } from "react-native";
import { toAppError } from "../../../shared/lib/http";
import { Banner } from "../../../shared/ui/Banner";
import { BottomSheet } from "../../../shared/ui/BottomSheet";
import { Button } from "../../../shared/ui/Button";
import { EmptyState } from "../../../shared/ui/EmptyState";
import { errorMessage } from "../../../shared/ui/error-messages";
import { Icon, type IconName } from "../../../shared/ui/Icon";
import { Message } from "../../../shared/ui/Message";
import { ReorderList, type DragHandle } from "../../../shared/ui/ReorderList";
import { RoundButton } from "../../../shared/ui/RoundButton";
import { Screen } from "../../../shared/ui/Screen";
import { Skeleton } from "../../../shared/ui/Skeleton";
import { TextField } from "../../../shared/ui/TextField";
import { TextLink } from "../../../shared/ui/TextLink";
import { Title } from "../../../shared/ui/Title";
import { MIN_TOUCH, palette, radius, spacing, typography } from "../../../shared/ui/theme";
import { RestrictionBanner, RestrictionWarning } from "../../anamnesis";
import { ExerciseLibraryScreen } from "../../exercise-library";
import type { StoredDraft } from "../domain/ports";
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
import { useDraftWriter, useStoredDraft } from "../hooks/use-draft";
import { useSaveWorkout, useWorkout } from "../hooks/use-training";
import { NumberField } from "./NumberField";
import { SetsEditor } from "./SetsEditor";
import { strings } from "./strings";

const t = strings.editor;
const METHODS = Object.keys(strings.methods) as BlockMethod[];
/** Espera depois da última mudança para gravar o rascunho no celular. */
const DRAFT_DEBOUNCE_MS = 600;

type Mode =
  | { readonly kind: "edit" }
  | { readonly kind: "method" }
  | { readonly kind: "pick"; readonly blockKey: string }
  | { readonly kind: "sets"; readonly blockKey: string; readonly exercise: ExerciseDraft }
  | { readonly kind: "conflict" };

/** Rota /workouts/[id]: carrega o treino e o rascunho do celular, e abre o editor. */
export function WorkoutEditorRoute() {
  const { id, linkId } = useLocalSearchParams<{ id: string; linkId?: string }>();
  const workout = useWorkout(id);
  const stored = useStoredDraft(id);

  if (workout.data === undefined || stored.isPending) {
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
      recovered={stored.data ?? null}
      linkId={linkId ?? null}
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

/** Ações de leitor de tela no lugar do arrastar: subir e descer. */
function moveActions(index: number, last: boolean, label: string) {
  return [
    ...(index > 0 ? [{ name: "moveUp", label: `${t.moveUp}: ${label}` }] : []),
    ...(last ? [] : [{ name: "moveDown", label: `${t.moveDown}: ${label}` }]),
  ];
}

/**
 * Editor de treino (design 5.5): um bloco por cartão, exercícios com o resumo das séries; séries e
 * método em folhas que sobem de baixo; reordenar segurando e arrastando. Grava tudo de uma vez no
 * Salvar, com a revisão lida (If-Match). Cada mudança fica num rascunho no celular até salvar.
 */
export function WorkoutEditor({
  workout,
  recovered = null,
  linkId = null,
  reload,
}: {
  readonly workout: Workout;
  /** Treino de um aluno (aberto pelo perfil dele): avisa das restrições ativas. */
  readonly linkId?: string | null;
  /** Rascunho deste celular (o app fechou ou caiu a internet antes de salvar). */
  readonly recovered?: StoredDraft | null;
  readonly reload: () => Promise<unknown>;
}) {
  const sameBase = recovered !== null && recovered.baseRevision === workout.revision;
  const [draft, setDraft] = useState<WorkoutDraft>(sameBase ? recovered.draft : workout.draft);
  const [dirty, setDirty] = useState(sameBase);
  const [notice, setNotice] = useState<"restored" | "older" | null>(
    recovered === null ? null : sameBase ? "restored" : "older",
  );
  const [mode, setMode] = useState<Mode>({ kind: "edit" });
  const [dragging, setDragging] = useState(false);
  const save = useSaveWorkout(workout.id);
  const drafts = useDraftWriter(workout.id);
  const problems = problemsOf(draft);
  const sum = totals(draft);

  const change = (next: WorkoutDraft) => {
    setDraft(next);
    setDirty(true);
    if (notice === "older") {
      setNotice(null);
    }
  };

  // rascunho no celular: grava um pouco depois da última mudança
  useEffect(() => {
    if (!dirty) {
      return undefined;
    }
    const timer = setTimeout(() => {
      drafts.save(draft, workout.revision);
    }, DRAFT_DEBOUNCE_MS);
    return () => {
      clearTimeout(timer);
    };
  }, [dirty, draft, drafts, workout.revision]);

  const persist = (revision: number) => {
    save.mutate(
      { revision, draft },
      {
        onSuccess: () => {
          setDirty(false);
          drafts.remove();
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
        text: t.discardKeep,
        onPress: () => {
          drafts.save(draft, workout.revision);
          router.back();
        },
      },
      {
        text: t.discardLeave,
        style: "destructive",
        onPress: () => {
          drafts.remove();
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
                drafts.remove();
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
      scrollEnabled={!dragging}
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
      {linkId === null ? null : <RestrictionBanner linkId={linkId} compact />}
      {notice === "restored" ? (
        <View style={styles.notice}>
          <Text style={[typography.small, { color: palette.textSoft }]}>{t.draftRestored}</Text>
          <TextLink
            label={t.draftDiscard}
            onPress={() => {
              setDraft(workout.draft);
              setDirty(false);
              setNotice(null);
              drafts.remove();
            }}
          />
        </View>
      ) : null}
      {notice === "older" && recovered !== null ? (
        <View style={styles.notice}>
          <Text style={[typography.small, { color: palette.textSoft }]}>{t.draftOlder}</Text>
          <Text style={[typography.small, { color: palette.muted }]}>{t.draftOlderHint}</Text>
          <View style={styles.noticeActions}>
            <TextLink
              label={t.draftRecover}
              onPress={() => {
                change(recovered.draft);
                setNotice(null);
              }}
            />
            <TextLink
              label={t.draftDiscard}
              onPress={() => {
                setNotice(null);
                drafts.remove();
              }}
            />
          </View>
        </View>
      ) : null}
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
      {draft.blocks.length > 1 || draft.blocks.some((b) => b.exercises.length > 1) ? (
        <Text style={[typography.small, { color: palette.muted }]}>{t.reorderHint}</Text>
      ) : null}
      <ReorderList
        items={draft.blocks}
        keyOf={(block) => block.key}
        gap={spacing.md}
        onDragChange={setDragging}
        onReorder={(from, to) => {
          change(edit.moveBlockTo(draft, from, to));
        }}
        renderItem={(block, index, drag) => (
          <BlockCard
            block={block}
            index={index}
            last={index === draft.blocks.length - 1}
            drag={drag}
            linkId={linkId}
            problem={problems.find((p) => p.blockKey === block.key)?.code ?? null}
            onChange={change}
            onDragChange={setDragging}
            draft={draft}
            onAddExercise={() => {
              setMode({ kind: "pick", blockKey: block.key });
            }}
            onEditSets={(exercise) => {
              setMode({ kind: "sets", blockKey: block.key, exercise });
            }}
          />
        )}
      />
      <Button
        label={t.addBlock}
        variant="secondary"
        icon="plus"
        onPress={() => {
          setMode({ kind: "method" });
        }}
      />

      <BottomSheet
        visible={mode.kind === "method"}
        title={strings.pickMethod.title}
        subtitle={strings.pickMethod.subtitle}
        onClose={() => {
          setMode({ kind: "edit" });
        }}
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
      </BottomSheet>
      {mode.kind === "sets" ? (
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
      ) : null}
    </Screen>
  );
}

function BlockCard({
  block,
  index,
  last,
  drag,
  linkId,
  problem,
  draft,
  onChange,
  onDragChange,
  onAddExercise,
  onEditSets,
}: {
  readonly block: BlockDraft;
  readonly index: number;
  readonly last: boolean;
  readonly drag: DragHandle;
  readonly linkId: string | null;
  readonly problem: keyof typeof t.problems | null;
  readonly draft: WorkoutDraft;
  readonly onChange: (draft: WorkoutDraft) => void;
  readonly onDragChange: (dragging: boolean) => void;
  readonly onAddExercise: () => void;
  readonly onEditSets: (exercise: ExerciseDraft) => void;
}) {
  const timing = blockTiming(block);
  const set = (patch: Partial<Omit<BlockDraft, "key" | "exercises">>) => {
    onChange(edit.updateBlock(draft, block.key, patch));
  };
  return (
    <View
      style={[
        styles.block,
        problem === null ? null : styles.blockProblem,
        drag.dragging ? styles.lifted : null,
      ]}
    >
      <View style={styles.blockHead}>
        <Pressable
          accessibilityRole="header"
          accessibilityHint={t.reorderHint}
          accessibilityActions={moveActions(index, last, t.block(index + 1))}
          onAccessibilityAction={(e) => {
            onChange(
              edit.moveBlock(draft, block.key, e.nativeEvent.actionName === "moveUp" ? -1 : 1),
            );
          }}
          delayLongPress={300}
          onLongPress={drag.start}
          onPressOut={drag.cancel}
          style={styles.blockTitle}
        >
          <Icon name="grip" size={18} color={drag.dragging ? palette.lime : palette.line} />
          <View style={{ flex: 1 }}>
            <Text style={[typography.small, { color: palette.muted }]}>{t.block(index + 1)}</Text>
            <Text style={[typography.headline, { color: palette.text }]}>
              {strings.methods[block.method].label}
              {timing === null ? "" : ` · ${timing}`}
            </Text>
          </View>
        </Pressable>
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

      <ReorderList
        items={block.exercises}
        keyOf={(exercise) => exercise.key}
        gap={spacing.sm}
        onDragChange={onDragChange}
        onReorder={(from, to) => {
          onChange(edit.moveExerciseTo(draft, block.key, from, to));
        }}
        renderItem={(exercise, i, exerciseDrag) => (
          <View style={[styles.exercise, exerciseDrag.dragging ? styles.lifted : null]}>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel={`${exercise.exerciseName}, ${summarize(exercise)}. ${t.editSets}`}
              accessibilityActions={moveActions(
                i,
                i === block.exercises.length - 1,
                exercise.exerciseName,
              )}
              onAccessibilityAction={(e) => {
                onChange(
                  edit.moveExercise(
                    draft,
                    block.key,
                    exercise.key,
                    e.nativeEvent.actionName === "moveUp" ? -1 : 1,
                  ),
                );
              }}
              onPress={() => {
                onEditSets(exercise);
              }}
              delayLongPress={300}
              onLongPress={exerciseDrag.start}
              onPressOut={exerciseDrag.cancel}
              style={styles.exerciseMain}
            >
              <Icon
                name="grip"
                size={16}
                color={exerciseDrag.dragging ? palette.lime : palette.line}
              />
              <View style={{ flex: 1, gap: 2 }}>
                <Text style={[typography.label, { color: palette.text, fontSize: 16 }]}>
                  {exercise.exerciseName}
                </Text>
                <Text style={[typography.small, { color: palette.textSoft }]}>
                  {summarize(exercise)}
                </Text>
                {linkId === null ? null : (
                  <RestrictionWarning linkId={linkId} muscle={exercise.primaryMuscle} />
                )}
              </View>
            </Pressable>
            <SmallIcon
              icon="close"
              label={`${t.remove}: ${exercise.exerciseName}`}
              onPress={() => {
                onChange(edit.removeExercise(draft, block.key, exercise.key));
              }}
            />
          </View>
        )}
      />
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
    backgroundColor: palette.surface2,
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
  blockTitle: {
    flex: 1,
    flexDirection: "row",
    alignItems: "center",
    gap: spacing.sm,
    minHeight: MIN_TOUCH,
  },
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
    paddingLeft: spacing.sm + 2,
  },
  exerciseMain: {
    flex: 1,
    flexDirection: "row",
    alignItems: "center",
    gap: spacing.sm + 2,
    paddingVertical: spacing.sm + 2,
    minHeight: MIN_TOUCH,
  },
  lifted: { borderWidth: 1, borderColor: palette.lime },
  notice: {
    backgroundColor: palette.surface,
    borderRadius: radius.md,
    padding: spacing.md,
    gap: spacing.sm,
  },
  noticeActions: { flexDirection: "row", gap: spacing.md, flexWrap: "wrap" },
});
