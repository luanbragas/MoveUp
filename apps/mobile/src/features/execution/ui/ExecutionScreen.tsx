import { useEffect, useState } from "react";
import { Alert, Linking, Modal, Pressable, ScrollView, StyleSheet, Text, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { uuidV7 } from "../../../shared/lib/uuid-v7";
import { BodyMap } from "../../../shared/ui/body-map/BodyMap";
import type { MuscleLevels } from "../../../shared/ui/body-map/muscles";
import { Button } from "../../../shared/ui/Button";
import { Chip } from "../../../shared/ui/Chip";
import { Icon } from "../../../shared/ui/Icon";
import { RoundButton } from "../../../shared/ui/RoundButton";
import { Screen } from "../../../shared/ui/Screen";
import { TextField } from "../../../shared/ui/TextField";
import { TextLink } from "../../../shared/ui/TextLink";
import { Title } from "../../../shared/ui/Title";
import { Toggle } from "../../../shared/ui/Toggle";
import { MIN_TOUCH, fonts, palette, radius, spacing, typography } from "../../../shared/ui/theme";
import {
  act,
  compare,
  durationSeconds,
  isComplete,
  musclesWorked,
  volumeKg,
  type PainEntry,
  type Session,
  type SessionExercise,
  type SessionSet,
} from "../domain/session";
import { usePreviousSession, useSaveSession } from "../hooks/use-sessions";
import { NumberInput } from "./NumberInput";
import { RestTimer } from "./RestTimer";
import { strings } from "./strings";
import { TimedBlock } from "./TimedBlock";

const t = strings.run;
const TIMED = new Set(["hiit", "intervals", "emom", "amrap"]);
const REGIONS = Object.keys(strings.feedback.regions) as (keyof typeof strings.feedback.regions)[];

/** O que a tela sabe de cada exercício além da sessão (instruções e vídeo, quando houver). */
export interface ExerciseInfoLite {
  readonly id: string;
  readonly name: string;
  readonly trackingType: string;
  readonly primaryMuscle: string | null;
  readonly secondaryMuscles: readonly string[];
  readonly instructions: string | null;
  readonly mediaUrl: string | null;
}

interface Props {
  readonly session: Session;
  /** Biblioteca conhecida no aparelho (como fazer e troca de exercício). */
  readonly exercises: ReadonlyMap<string, ExerciseInfoLite>;
  readonly firstName: string;
  readonly onClose: () => void;
}

function levelsOf(e: Pick<SessionExercise, "primaryMuscle" | "secondaryMuscles">): MuscleLevels {
  const levels: Record<string, 1 | 2> = {};
  e.secondaryMuscles.forEach((m) => (levels[m] = 1));
  if (e.primaryMuscle !== null) {
    levels[e.primaryMuscle] = 2;
  }
  return levels;
}

function clock(seconds: number): string {
  const s = Math.max(0, Math.floor(seconds));
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  const mm = String(m).padStart(h > 0 ? 2 : 1, "0");
  return `${h > 0 ? `${String(h)}:` : ""}${mm}:${String(s % 60).padStart(2, "0")}`;
}

const fmtKg = (kg: number) => String(Math.round(kg * 10) / 10).replace(".", ",");

/**
 * Execução do treino (design 3.2): a tabela de séries do exercício atual, pré-preenchida com o
 * planejado; cada toque grava no aparelho na hora. Biset passa para o próximo do grupo sem
 * descanso; blocos por tempo abrem o relógio. Depois: feedback e resumo.
 */
export function ExecutionScreen({ session: initial, exercises, firstName, onClose }: Props) {
  const [session, setSession] = useState(initial);
  const [index, setIndex] = useState(() => {
    const next = initial.exercises.findIndex(
      (e) => e.status !== "skipped" && e.sets.some((s) => !s.completed),
    );
    return Math.max(0, next);
  });
  const [step, setStep] = useState<"run" | "feedback" | "summary">(
    initial.status === "in_progress" ? "run" : "summary",
  );
  const [restUntil, setRestUntil] = useState<number | null>(null);
  const [sheet, setSheet] = useState<"howto" | "substitute" | null>(null);
  const [now, setNow] = useState(() => Date.now());
  const save = useSaveSession();

  useEffect(() => {
    if (step !== "run") {
      return undefined;
    }
    const timer = setInterval(() => {
      setNow(Date.now());
    }, 1000);
    return () => {
      clearInterval(timer);
    };
  }, [step]);

  const commit = (next: Session) => {
    setSession(next);
    save.mutate(next);
  };

  if (step === "feedback") {
    return (
      <FeedbackStep
        session={session}
        onBack={() => {
          setStep("run");
        }}
        onSave={(effort, comment, pains) => {
          commit(act.finish(session, { effort, comment, pains }, new Date()));
          setStep("summary");
        }}
      />
    );
  }
  if (step === "summary") {
    return <SummaryStep session={session} firstName={firstName} onDone={onClose} />;
  }

  const exercise = session.exercises[index];
  if (exercise === undefined) {
    return null;
  }
  const block = session.blocks[exercise.blockIndex];
  const blockExercises = session.exercises.filter((e) => e.blockIndex === exercise.blockIndex);
  const timed = block !== undefined && TIMED.has(block.method);
  const info = exercises.get(exercise.exerciseId);
  const nextExercise = session.exercises[index + 1];

  const goTo = (target: number) => {
    setIndex(Math.min(Math.max(target, 0), session.exercises.length - 1));
  };

  const confirm = (set: SessionSet) => {
    const done = !set.completed;
    const next = act.setSet(session, exercise.id, set.id, { completed: done }, new Date());
    commit(next);
    if (!done) {
      return;
    }
    // biset/circuito: vai para o próximo do grupo na mesma volta; descanso no fim da volta
    const inGroup = block?.method === "superset" || block?.method === "circuit";
    const groupNext = blockExercises[blockExercises.findIndex((e) => e.id === exercise.id) + 1];
    if (inGroup && groupNext !== undefined) {
      goTo(session.exercises.findIndex((e) => e.id === groupNext.id));
      return;
    }
    if (inGroup && blockExercises[0] !== undefined) {
      const first = session.exercises.findIndex((e) => e.id === blockExercises[0]?.id);
      const remaining = next.exercises[first]?.sets.some((s) => !s.completed) ?? false;
      if (remaining) {
        goTo(first);
      }
    }
    const rest =
      (inGroup ? block.restBetweenRounds : null) ??
      set.planned?.restSeconds ??
      exercise.restSeconds ??
      60;
    const exerciseDone = next.exercises[index]?.sets.every((s) => s.completed) ?? false;
    if (exerciseDone && !inGroup && nextExercise !== undefined) {
      goTo(index + 1);
    }
    if (rest > 0) {
      setRestUntil(Date.now() + rest * 1000);
    }
  };

  const finish = () => {
    if (isComplete(session)) {
      setStep("feedback");
      return;
    }
    Alert.alert(t.finishPendingTitle, t.finishPendingMessage, [
      { text: t.finishPendingBack, style: "cancel" },
      {
        text: t.finishPendingGo,
        onPress: () => {
          setStep("feedback");
        },
      },
    ]);
  };

  const candidates = [...exercises.values()].filter(
    (e) =>
      e.id !== exercise.exerciseId &&
      e.primaryMuscle !== null &&
      e.primaryMuscle ===
        (exercises.get(exercise.substitutedFrom ?? exercise.exerciseId)?.primaryMuscle ??
          exercise.primaryMuscle),
  );

  return (
    <Screen
      header={
        <>
          <RoundButton icon="down" label={t.back} onPress={onClose} />
          <View style={{ flex: 1, alignItems: "center" }}>
            <Text style={[typography.small, { color: palette.muted }]}>{t.elapsed}</Text>
            <Text style={styles.elapsed}>
              {clock((now - Date.parse(session.startedAt)) / 1000)}
            </Text>
          </View>
          <Pressable
            accessibilityRole="button"
            accessibilityLabel={t.finish}
            onPress={finish}
            style={styles.finish}
          >
            <Text style={[typography.label, { color: palette.onLime }]}>{t.finish}</Text>
          </Pressable>
        </>
      }
    >
      <View
        accessibilityRole="adjustable"
        accessibilityLabel={t.exercise(index + 1, session.exercises.length)}
        style={styles.segments}
      >
        {session.exercises.map((e, i) => {
          const done = e.status === "skipped" || e.sets.every((s) => s.completed);
          return (
            <Pressable
              key={e.id}
              accessibilityRole="button"
              accessibilityLabel={`${e.name}${done ? ", feito" : ""}`}
              onPress={() => {
                goTo(i);
              }}
              style={styles.segmentHit}
            >
              <View
                style={[
                  styles.segment,
                  done ? styles.segmentDone : null,
                  i === index ? styles.segmentNow : null,
                ]}
              />
            </Pressable>
          );
        })}
      </View>

      <View style={styles.titleRow}>
        <RoundButton
          icon="back"
          label={t.previous}
          onPress={() => {
            goTo(index - 1);
          }}
        />
        <View style={{ flex: 1 }}>
          <Text style={[typography.small, { color: palette.muted }]}>
            {t.exercise(index + 1, session.exercises.length)}
          </Text>
          <Title size={28}>{exercise.name}</Title>
          {exercise.substitutedFrom === null ? null : (
            <Text style={[typography.small, { color: palette.muted }]}>
              {t.substitutedFrom(exercises.get(exercise.substitutedFrom)?.name ?? "")}
            </Text>
          )}
        </View>
        <RoundButton
          icon="arrow"
          label={t.next}
          onPress={() => {
            goTo(index + 1);
          }}
        />
      </View>

      {exercise.notes === null ? null : (
        <View style={styles.note}>
          <Icon name="chat" size={18} color={palette.lime} />
          <Text style={[typography.small, { color: palette.textSoft, flex: 1 }]}>
            {exercise.notes}
          </Text>
        </View>
      )}

      {timed ? (
        <TimedBlock
          key={exercise.blockIndex}
          block={block}
          exercises={blockExercises}
          onDone={(elapsed, rounds) => {
            let next = session;
            for (const e of blockExercises) {
              for (const s of e.sets) {
                next = act.setSet(
                  next,
                  e.id,
                  s.id,
                  {
                    completed: true,
                    durationSeconds: elapsed,
                    ...(rounds === null ? {} : { reps: rounds }),
                  },
                  new Date(),
                );
              }
            }
            commit(next);
            const after = session.exercises.findIndex((e) => e.blockIndex > exercise.blockIndex);
            if (after >= 0) {
              goTo(after);
            }
          }}
        />
      ) : exercise.status === "skipped" ? (
        <View style={styles.skipped}>
          <Text style={[typography.headline, { color: palette.muted }]}>{t.skipped}</Text>
          <TextLink
            label={t.unskip}
            onPress={() => {
              commit(act.setSet(session, exercise.id, exercise.sets[0]?.id ?? "", {}, new Date()));
            }}
          />
        </View>
      ) : (
        <SetTable
          exercise={exercise}
          onChange={(set, values) => {
            commit(act.setSet(session, exercise.id, set.id, values, new Date()));
          }}
          onConfirm={confirm}
        />
      )}

      {timed || exercise.status === "skipped" ? null : (
        <TextLink
          label={t.addSet}
          onPress={() => {
            commit(act.addSet(session, exercise.id, new Date(), uuidV7));
          }}
        />
      )}
      <View style={styles.tools}>
        <Chip
          label={t.howTo}
          icon="play"
          onPress={() => {
            setSheet("howto");
          }}
        />
        {timed ? null : (
          <Chip
            label={t.substitute}
            icon="refresh"
            onPress={() => {
              setSheet("substitute");
            }}
          />
        )}
        {timed || exercise.status === "skipped" ? null : (
          <Chip
            label={t.skip}
            onPress={() => {
              commit(act.skip(session, exercise.id, new Date()));
              goTo(index + 1);
            }}
          />
        )}
      </View>

      {restUntil === null ? null : (
        <RestTimer
          endsAt={restUntil}
          nextLabel={session.exercises[index]?.name ?? null}
          onDone={() => {
            setRestUntil(null);
          }}
          onExtend={(seconds) => {
            setRestUntil((current) => (current ?? Date.now()) + seconds * 1000);
          }}
        />
      )}

      <Modal
        visible={sheet !== null}
        animationType="slide"
        transparent
        onRequestClose={() => {
          setSheet(null);
        }}
      >
        <View style={styles.sheetBackdrop}>
          <SafeAreaView edges={["bottom"]} style={styles.sheet}>
            <View style={styles.sheetHead}>
              <Text style={[typography.headline, { color: palette.text, flex: 1 }]}>
                {sheet === "howto" ? t.howTo : t.substituteTitle}
              </Text>
              <RoundButton
                icon="close"
                label={t.close}
                onPress={() => {
                  setSheet(null);
                }}
              />
            </View>
            <ScrollView contentContainerStyle={{ gap: spacing.sm, paddingBottom: spacing.lg }}>
              {sheet === "howto" ? (
                <>
                  <View style={{ alignItems: "center" }}>
                    <BodyMap levels={levelsOf(exercise)} width={280} height={200} />
                  </View>
                  {info?.instructions == null ? null : (
                    <Text style={[typography.body, { color: palette.textSoft }]}>
                      {info.instructions}
                    </Text>
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
                </>
              ) : candidates.length === 0 ? (
                <Text style={[typography.body, { color: palette.muted }]}>{t.noSubstitute}</Text>
              ) : (
                <>
                  <Text style={[typography.small, { color: palette.muted }]}>
                    {t.substituteHint}
                  </Text>
                  {candidates.map((c) => (
                    <Pressable
                      key={c.id}
                      accessibilityRole="button"
                      accessibilityLabel={c.name}
                      onPress={() => {
                        commit(
                          act.substitute(
                            session,
                            exercise.id,
                            {
                              exerciseId: c.id,
                              name: c.name,
                              trackingType: c.trackingType,
                              primaryMuscle: c.primaryMuscle,
                              secondaryMuscles: c.secondaryMuscles,
                            },
                            new Date(),
                          ),
                        );
                        setSheet(null);
                      }}
                      style={styles.candidate}
                    >
                      <Text style={[typography.label, { color: palette.text }]}>{c.name}</Text>
                    </Pressable>
                  ))}
                </>
              )}
            </ScrollView>
          </SafeAreaView>
        </View>
      </Modal>
    </Screen>
  );
}

export function SetTable({
  exercise,
  onChange,
  onConfirm,
}: {
  readonly exercise: SessionExercise;
  readonly onChange: (set: SessionSet, values: Partial<SessionSet>) => void;
  readonly onConfirm: (set: SessionSet) => void;
}) {
  const byTime = exercise.trackingType === "time" || exercise.trackingType === "distance_time";
  const withLoad = exercise.trackingType === "reps_load";
  return (
    <View style={styles.table}>
      <View style={styles.row} accessibilityElementsHidden importantForAccessibility="no">
        <Text style={[styles.th, styles.num]}>{t.set}</Text>
        {withLoad ? <Text style={styles.th}>{t.kg}</Text> : null}
        <Text style={styles.th}>{byTime ? t.seconds : t.reps}</Text>
        <View style={styles.check} />
      </View>
      {exercise.sets.map((set) => {
        const planned = set.planned;
        const hint =
          planned === null
            ? null
            : byTime
              ? planned.durationSeconds
              : planned.repsMin !== null &&
                  planned.repsMax !== null &&
                  planned.repsMin !== planned.repsMax
                ? `${String(planned.repsMin)}–${String(planned.repsMax)}`
                : (planned.repsMax ?? planned.repsMin);
        return (
          <View key={set.id} style={[styles.row, set.completed ? styles.rowDone : null]}>
            <View style={styles.num}>
              <Text style={[styles.setNumber, set.completed ? styles.inkDone : null]}>
                {set.setNumber}
              </Text>
              {set.type === "normal" ? null : (
                <Text style={[typography.small, { color: palette.muted, fontSize: 10 }]}>
                  {set.type}
                </Text>
              )}
            </View>
            {withLoad ? (
              <NumberInput
                label={`${t.kg}, ${t.set} ${String(set.setNumber)}`}
                value={set.loadKg}
                decimal
                done={set.completed}
                onChange={(loadKg) => {
                  onChange(set, { loadKg });
                }}
              />
            ) : null}
            <NumberInput
              label={`${byTime ? t.seconds : t.reps}, ${t.set} ${String(set.setNumber)}`}
              value={byTime ? set.durationSeconds : set.reps}
              hint={hint === null ? null : `${String(hint)} ${t.planned}`}
              done={set.completed}
              onChange={(v) => {
                onChange(set, byTime ? { durationSeconds: v } : { reps: v });
              }}
            />
            <Pressable
              accessibilityRole="button"
              accessibilityLabel={set.completed ? t.undo(set.setNumber) : t.confirm(set.setNumber)}
              accessibilityState={{ checked: set.completed }}
              onPress={() => {
                onConfirm(set);
              }}
              style={[styles.check, set.completed ? styles.checkOn : null]}
            >
              <Icon
                name="check"
                size={22}
                color={set.completed ? palette.onLime : palette.muted}
                strokeWidth={3}
              />
            </Pressable>
          </View>
        );
      })}
    </View>
  );
}

function FeedbackStep({
  session,
  onBack,
  onSave,
}: {
  readonly session: Session;
  readonly onBack: () => void;
  readonly onSave: (effort: number, comment: string | null, pains: readonly PainEntry[]) => void;
}) {
  const f = strings.feedback;
  const [effort, setEffort] = useState<number | null>(null);
  const [comment, setComment] = useState("");
  const [hurt, setHurt] = useState(false);
  const [region, setRegion] = useState<string | null>(null);
  const [intensity, setIntensity] = useState<number | null>(null);
  const [exerciseId, setExerciseId] = useState<string | null>(null);
  const done = session.exercises.filter((e) => e.status !== "skipped");

  return (
    <Screen
      header={<RoundButton icon="back" label={strings.history.back} onPress={onBack} />}
      title={f.title}
      footer={
        <Button
          label={f.save}
          icon="check"
          disabled={effort === null || (hurt && region === null)}
          onPress={() => {
            onSave(
              effort ?? 0,
              comment.trim() === "" ? null : comment.trim(),
              hurt && region !== null
                ? [{ id: uuidV7(), bodyRegion: region, exerciseId, intensity }]
                : [],
            );
          }}
        />
      }
    >
      <Text style={[typography.label, { color: palette.textSoft }]}>{f.effort}</Text>
      <View accessibilityRole="radiogroup" accessibilityLabel={f.effort} style={styles.effort}>
        {Array.from({ length: 11 }, (_, n) => (
          <Pressable
            key={n}
            accessibilityRole="radio"
            accessibilityLabel={f.effortValue(n)}
            accessibilityState={{ checked: effort === n }}
            onPress={() => {
              setEffort(n);
            }}
            style={[styles.effortDot, effort === n ? styles.effortOn : null]}
          >
            <Text
              style={[
                typography.label,
                { color: effort === n ? palette.onLime : palette.textSoft },
              ]}
            >
              {n}
            </Text>
          </Pressable>
        ))}
      </View>
      <TextField label={f.comment} value={comment} onChangeText={setComment} />
      <Toggle label={f.pain} description={f.painHint} value={hurt} onChange={setHurt} />
      {hurt ? (
        <View style={styles.painBox}>
          <Text style={[typography.label, { color: palette.text }]}>{f.where}</Text>
          <View style={styles.chips}>
            {REGIONS.map((r) => (
              <Chip
                key={r}
                label={f.regions[r]}
                role="radio"
                selected={region === r}
                onPress={() => {
                  setRegion(r);
                }}
              />
            ))}
          </View>
          <Text style={[typography.label, { color: palette.text }]}>{f.intensity}</Text>
          <View style={styles.chips}>
            {Array.from({ length: 11 }, (_, n) => (
              <Chip
                key={n}
                label={String(n)}
                role="radio"
                selected={intensity === n}
                onPress={() => {
                  setIntensity(n);
                }}
              />
            ))}
          </View>
          <Text style={[typography.label, { color: palette.text }]}>{f.whichExercise}</Text>
          <View style={styles.chips}>
            <Chip
              label={f.none}
              role="radio"
              selected={exerciseId === null}
              onPress={() => {
                setExerciseId(null);
              }}
            />
            {done.map((e) => (
              <Chip
                key={e.id}
                label={e.name}
                role="radio"
                selected={exerciseId === e.exerciseId}
                onPress={() => {
                  setExerciseId(e.exerciseId);
                }}
              />
            ))}
          </View>
        </View>
      ) : null}
    </Screen>
  );
}

function SummaryStep({
  session,
  firstName,
  onDone,
}: {
  readonly session: Session;
  readonly firstName: string;
  readonly onDone: () => void;
}) {
  const s = strings.summary;
  const previous = usePreviousSession(session);
  const comparison = compare(session, previous.data ?? null);
  const duration = durationSeconds(session) ?? 0;
  const sets = session.exercises.flatMap((e) => e.sets).filter((x) => x.completed).length;

  return (
    <Screen footer={<Button label={s.done} icon="check" onPress={onDone} />}>
      <Title size={42}>{s.title(firstName)}</Title>
      <View style={styles.stats}>
        <View style={styles.stat}>
          <Text style={styles.statValue}>{clock(duration)}</Text>
          <Text style={[typography.small, { color: palette.muted }]}>{s.duration}</Text>
        </View>
        <View style={styles.stat}>
          <Text style={styles.statValue}>{fmtKg(volumeKg(session))} kg</Text>
          <Text style={[typography.small, { color: palette.muted }]}>{s.volume}</Text>
        </View>
        <View style={styles.stat}>
          <Text style={styles.statValue}>{sets}</Text>
          <Text style={[typography.small, { color: palette.muted }]}>{s.sets}</Text>
        </View>
      </View>
      <Text style={[typography.label, { color: palette.textSoft }]}>{s.worked}</Text>
      <View style={styles.map}>
        <BodyMap levels={musclesWorked(session)} width={300} height={220} />
      </View>
      <Text style={[typography.label, { color: palette.textSoft }]}>{s.compare}</Text>
      {previous.data == null ? (
        <Text style={[typography.small, { color: palette.muted }]}>{s.first}</Text>
      ) : (
        comparison
          .filter((c) => c.topLoadKg !== null)
          .map((c) => {
            const diff =
              c.previousTopLoadKg === null || c.topLoadKg === null
                ? null
                : c.topLoadKg - c.previousTopLoadKg;
            return (
              <View key={c.exerciseId} style={styles.compareRow}>
                <Text style={[typography.body, { color: palette.text, flex: 1 }]}>{c.name}</Text>
                <Text
                  style={[
                    typography.label,
                    { color: diff !== null && diff > 0 ? palette.lime : palette.muted },
                  ]}
                >
                  {diff === null
                    ? "—"
                    : diff > 0
                      ? s.up(fmtKg(diff))
                      : diff < 0
                        ? s.down(fmtKg(-diff))
                        : s.same}
                </Text>
              </View>
            );
          })
      )}
      <Text style={[typography.small, { color: palette.muted }]}>
        {session.syncStatus === "synced" ? s.synced : s.pending}
      </Text>
    </Screen>
  );
}

const styles = StyleSheet.create({
  elapsed: { fontFamily: fonts.number, fontSize: 20, color: palette.text },
  finish: {
    minHeight: MIN_TOUCH,
    paddingHorizontal: spacing.md,
    borderRadius: radius.pill,
    backgroundColor: palette.lime,
    justifyContent: "center",
  },
  segments: { flexDirection: "row", gap: 4 },
  segmentHit: { flex: 1, height: 24, justifyContent: "center" },
  segment: { height: 5, borderRadius: 3, backgroundColor: palette.line },
  segmentDone: { backgroundColor: palette.limeDark },
  segmentNow: { backgroundColor: palette.lime, height: 7 },
  titleRow: { flexDirection: "row", alignItems: "center", gap: spacing.sm },
  note: {
    flexDirection: "row",
    gap: spacing.sm,
    backgroundColor: palette.surface,
    borderRadius: radius.md,
    padding: spacing.sm + 4,
    alignItems: "center",
  },
  table: { gap: 6 },
  row: { flexDirection: "row", alignItems: "center", gap: spacing.sm },
  rowDone: { opacity: 0.85 },
  th: { flex: 1, textAlign: "center", ...typography.small, color: palette.muted },
  num: { width: 44, alignItems: "center" },
  setNumber: { fontFamily: fonts.number, fontSize: 20, color: palette.text },
  inkDone: { color: palette.lime },
  check: {
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: palette.surface2,
    alignItems: "center",
    justifyContent: "center",
  },
  checkOn: { backgroundColor: palette.lime },
  skipped: {
    backgroundColor: palette.surface,
    borderRadius: radius.lg,
    padding: spacing.lg,
    alignItems: "center",
    gap: spacing.sm,
  },
  tools: { flexDirection: "row", flexWrap: "wrap", gap: spacing.sm },
  sheetBackdrop: { flex: 1, backgroundColor: "rgba(0,0,0,0.6)", justifyContent: "flex-end" },
  sheet: {
    backgroundColor: palette.surface,
    borderTopLeftRadius: radius.xl,
    borderTopRightRadius: radius.xl,
    padding: spacing.md + 4,
    maxHeight: "80%",
    gap: spacing.sm,
  },
  sheetHead: { flexDirection: "row", alignItems: "center" },
  candidate: {
    minHeight: MIN_TOUCH + 8,
    justifyContent: "center",
    paddingHorizontal: spacing.md,
    borderRadius: radius.md,
    backgroundColor: palette.surface2,
  },
  effort: { flexDirection: "row", flexWrap: "wrap", gap: 6 },
  effortDot: {
    width: 48,
    height: 48,
    borderRadius: 24,
    backgroundColor: palette.surface2,
    alignItems: "center",
    justifyContent: "center",
  },
  effortOn: { backgroundColor: palette.lime },
  painBox: {
    gap: spacing.sm,
    backgroundColor: palette.redSoft,
    borderRadius: radius.lg,
    padding: spacing.md,
  },
  chips: { flexDirection: "row", flexWrap: "wrap", gap: spacing.sm },
  stats: { flexDirection: "row", gap: spacing.sm },
  stat: {
    flex: 1,
    backgroundColor: palette.surface,
    borderRadius: radius.lg,
    padding: spacing.md,
    gap: 2,
  },
  statValue: { fontFamily: fonts.number, fontSize: 22, color: palette.text },
  map: { alignItems: "center" },
  compareRow: { flexDirection: "row", alignItems: "center", gap: spacing.sm },
});
