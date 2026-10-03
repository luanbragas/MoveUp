import { useEffect, useRef, useState } from "react";
import { Pressable, StyleSheet, Text, Vibration, View } from "react-native";
import { Button } from "../../../shared/ui/Button";
import { Icon } from "../../../shared/ui/Icon";
import { fonts, palette, radius, spacing, typography } from "../../../shared/ui/theme";
import type { BlockTiming, SessionExercise } from "../domain/session";
import { strings } from "./strings";

const t = strings.timed;

function clock(seconds: number): string {
  const s = Math.max(0, Math.round(seconds));
  return `${String(Math.floor(s / 60))}:${String(s % 60).padStart(2, "0")}`;
}

/** Fase atual de um bloco por tempo, a partir dos segundos corridos. Pura (testável). */
export function phaseAt(block: BlockTiming, elapsed: number) {
  if (block.method === "hiit" || block.method === "intervals") {
    const work = block.workSeconds ?? 20;
    const rest = block.restSeconds ?? 10;
    const rounds = block.rounds ?? 8;
    const cycle = work + rest;
    const total = cycle * rounds - rest; // sem descanso depois da última
    const round = Math.min(rounds, Math.floor(elapsed / cycle) + 1);
    const inCycle = elapsed - (round - 1) * cycle;
    const working = inCycle < work;
    return {
      done: elapsed >= total,
      label: working ? t.work : t.rest,
      working,
      left: working ? work - inCycle : cycle - inCycle,
      progress: t.round(round, rounds),
      total,
    };
  }
  const duration = block.durationSeconds ?? 600;
  if (block.method === "emom") {
    const minutes = Math.ceil(duration / 60);
    const minute = Math.min(minutes, Math.floor(elapsed / 60) + 1);
    return {
      done: elapsed >= duration,
      label: t.minute(minute, minutes),
      working: true,
      left: 60 - (elapsed % 60),
      progress: t.minute(minute, minutes),
      total: duration,
    };
  }
  return {
    done: elapsed >= duration,
    label: t.remaining,
    working: true,
    left: duration - elapsed,
    progress: t.remaining,
    total: duration,
  };
}

/** Rodadas de um bloco que terminou no tempo (HIIT e intervalado: as do bloco; EMOM: minutos). */
function roundsOf(block: BlockTiming): number | null {
  if (block.method === "hiit" || block.method === "intervals") {
    return block.rounds ?? 8;
  }
  if (block.method === "emom") {
    return Math.ceil((block.durationSeconds ?? 600) / 60);
  }
  return null;
}

interface Props {
  readonly block: BlockTiming;
  readonly exercises: readonly SessionExercise[];
  /** Rodadas feitas (AMRAP: contadas; demais: as do bloco) e reps da rodada incompleta (AMRAP). */
  readonly onDone: (
    elapsedSeconds: number,
    rounds: number | null,
    extraReps: number | null,
  ) => void;
}

/** Bloco por tempo (HIIT/Tabata, intervalado, EMOM, AMRAP) em tela cheia. */
export function TimedBlock({ block, exercises, onDone }: Props) {
  const [startedAt, setStartedAt] = useState<number | null>(null);
  const [pausedElapsed, setPausedElapsed] = useState(0);
  const [running, setRunning] = useState(false);
  const [now, setNow] = useState(() => Date.now());
  const [rounds, setRounds] = useState(0);
  const [extraReps, setExtraReps] = useState(0);
  const lastWorking = useRef<boolean | null>(null);

  const elapsed =
    pausedElapsed + (running && startedAt !== null ? Math.floor((now - startedAt) / 1000) : 0);
  const phase = phaseAt(block, elapsed);

  useEffect(() => {
    if (!running) {
      return undefined;
    }
    const timer = setInterval(() => {
      setNow(Date.now());
    }, 250);
    return () => {
      clearInterval(timer);
    };
  }, [running]);

  useEffect(() => {
    if (lastWorking.current !== null && lastWorking.current !== phase.working) {
      Vibration.vibrate(phase.working ? [0, 250, 120, 250] : 400);
    }
    lastWorking.current = phase.working;
  }, [phase.working]);

  const amrap = block.method === "amrap";
  const current =
    block.method === "hiit" || block.method === "intervals" || block.method === "emom"
      ? exercises[
          Math.floor(
            elapsed /
              (block.method === "emom"
                ? 60
                : (block.workSeconds ?? 20) + (block.restSeconds ?? 10)),
          ) % Math.max(exercises.length, 1)
        ]
      : null;

  return (
    <View style={[styles.card, phase.working ? styles.cardWork : styles.cardRest]}>
      <Text style={[typography.label, phase.working ? styles.inkOnLime : styles.ink]}>
        {phase.progress}
      </Text>
      <Text
        accessibilityRole="timer"
        accessibilityLabel={`${phase.label}: ${String(Math.round(phase.left))} segundos`}
        style={[styles.clock, phase.working ? styles.inkOnLime : styles.ink]}
      >
        {clock(phase.done ? 0 : phase.left)}
      </Text>
      <Text style={[typography.headline, phase.working ? styles.inkOnLime : styles.ink]}>
        {phase.done
          ? t.done
          : current === null || current === undefined
            ? phase.label
            : current.name}
      </Text>
      {amrap ? (
        <View style={styles.rounds}>
          <Pressable
            accessibilityRole="button"
            accessibilityLabel={t.removeRound}
            onPress={() => {
              setRounds(Math.max(0, rounds - 1));
            }}
            style={styles.roundButton}
          >
            <Icon name="close" size={20} color={palette.onLime} />
          </Pressable>
          <View style={{ alignItems: "center" }}>
            <Text style={[styles.roundsValue, styles.inkOnLime]}>{rounds}</Text>
            <Text style={[typography.small, styles.inkOnLime]}>{t.rounds}</Text>
          </View>
          <Pressable
            accessibilityRole="button"
            accessibilityLabel={t.addRound}
            onPress={() => {
              setRounds(rounds + 1);
            }}
            style={styles.roundButton}
          >
            <Icon name="plus" size={20} color={palette.onLime} />
          </Pressable>
        </View>
      ) : null}
      {amrap ? (
        <View style={styles.extra}>
          <Text style={[typography.small, styles.inkOnLime]}>{t.extraReps}</Text>
          <View style={styles.extraControls}>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel={t.removeExtraRep}
              onPress={() => {
                setExtraReps(Math.max(0, extraReps - 1));
              }}
              style={styles.smallButton}
            >
              <Icon name="close" size={16} color={palette.onLime} />
            </Pressable>
            <Text style={[styles.extraValue, styles.inkOnLime]}>{extraReps}</Text>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel={t.addExtraRep}
              onPress={() => {
                setExtraReps(extraReps + 1);
              }}
              style={styles.smallButton}
            >
              <Icon name="plus" size={16} color={palette.onLime} />
            </Pressable>
          </View>
        </View>
      ) : null}
      {amrap || block.method === "emom" ? (
        <View style={{ gap: 2 }}>
          {exercises.map((e) => (
            <Text key={e.id} style={[typography.small, styles.inkOnLime]}>
              • {e.name}
            </Text>
          ))}
        </View>
      ) : null}
      <View style={styles.actions}>
        {phase.done ? (
          <Button
            label={t.done}
            icon="check"
            onLime={phase.working}
            onPress={() => {
              onDone(
                Math.min(elapsed, phase.total),
                amrap ? rounds : roundsOf(block),
                amrap ? extraReps : null,
              );
            }}
          />
        ) : (
          <Button
            label={running ? t.pause : startedAt === null ? t.start : t.resume}
            icon={running ? null : "play"}
            onLime={phase.working}
            onPress={() => {
              if (running) {
                setPausedElapsed(elapsed);
                setRunning(false);
              } else {
                setStartedAt(Date.now());
                setNow(Date.now());
                setRunning(true);
              }
            }}
          />
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    borderRadius: radius.xl,
    padding: spacing.lg,
    gap: spacing.sm + 2,
    alignItems: "stretch",
  },
  cardWork: { backgroundColor: palette.lime },
  cardRest: { backgroundColor: palette.surface2 },
  ink: { color: palette.text },
  inkOnLime: { color: palette.onLime },
  clock: { fontFamily: fonts.number, fontSize: 88, lineHeight: 92 },
  rounds: { flexDirection: "row", alignItems: "center", justifyContent: "space-between" },
  roundButton: {
    width: 56,
    height: 56,
    borderRadius: 28,
    borderWidth: 2,
    borderColor: palette.onLime,
    alignItems: "center",
    justifyContent: "center",
  },
  roundsValue: { fontFamily: fonts.number, fontSize: 48 },
  extra: { flexDirection: "row", alignItems: "center", justifyContent: "space-between" },
  extraControls: { flexDirection: "row", alignItems: "center", gap: spacing.sm },
  extraValue: { fontFamily: fonts.number, fontSize: 24, minWidth: 32, textAlign: "center" },
  smallButton: {
    width: 44,
    height: 44,
    borderRadius: 22,
    borderWidth: 2,
    borderColor: palette.onLime,
    alignItems: "center",
    justifyContent: "center",
  },
  actions: { marginTop: spacing.sm },
});
