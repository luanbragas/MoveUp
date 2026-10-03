import { router } from "expo-router";
import { Pressable, StyleSheet, Text, View } from "react-native";
import { Button } from "../../../shared/ui/Button";
import { Icon } from "../../../shared/ui/Icon";
import { fonts, palette, radius, spacing, typography } from "../../../shared/ui/theme";
import {
  exerciseCount,
  todayOf,
  type PlannedSnapshot,
  type PlannedWorkout,
} from "../domain/planned";
import { strings } from "./strings";

const t = strings.today;

function letter(position: number): string {
  return String.fromCharCode(64 + Math.min(Math.max(position, 1), 26));
}

function open(workout: PlannedWorkout) {
  router.push({ pathname: "/workout/[id]", params: { id: workout.id } });
}

/** Card da aba Hoje (design 3.1): o treino do dia, o próximo da sequência ou o descanso. */
export function TodayCard({
  snapshot,
  date,
}: {
  readonly snapshot: PlannedSnapshot;
  readonly date: Date;
}) {
  const today = todayOf(snapshot, date);
  if (today.kind === "none" || snapshot.program === null) {
    return null;
  }
  const program = snapshot.program;

  if (today.kind === "rest") {
    return (
      <View style={styles.restCard}>
        <Icon name="clock" size={24} color={palette.lime} />
        <Text style={[typography.headline, { color: palette.text }]}>{t.rest}</Text>
        {today.next === null || today.nextWeekday === null ? null : (
          <Pressable
            accessibilityRole="button"
            accessibilityLabel={t.restNext(t.days[today.nextWeekday] ?? "", today.next.name)}
            onPress={() => {
              if (today.next !== null) {
                open(today.next);
              }
            }}
          >
            <Text style={[typography.body, { color: palette.textSoft }]}>
              {t.restNext(t.days[today.nextWeekday] ?? "", today.next.name)}
            </Text>
          </Pressable>
        )}
      </View>
    );
  }

  const workout = today.workout;
  const label = today.kind === "next" ? t.next : t.label;
  const sub =
    program.scheduleMode === "sequence" && program.weeklyTarget !== null
      ? t.perWeek(program.weeklyTarget)
      : program.name;
  return (
    <View style={styles.card}>
      <View style={styles.head}>
        <View style={styles.letter}>
          <Text style={styles.letterText}>{letter(workout.position)}</Text>
        </View>
        <View style={{ flex: 1 }}>
          <Text style={[typography.label, { color: palette.onLime }]}>{label}</Text>
          <Text style={[typography.small, { color: palette.onLime, opacity: 0.75 }]}>{sub}</Text>
        </View>
      </View>
      <Text accessibilityRole="header" style={styles.title}>
        {(workout.goal ?? workout.name).toUpperCase()}
      </Text>
      <Text style={[typography.label, { color: palette.onLime }]}>
        {workout.goal === null ? null : `${workout.name} · `}
        {t.meta(exerciseCount(workout), workout.estimatedMinutes)}
      </Text>
      <Button
        label={t.open}
        onLime
        onPress={() => {
          open(workout);
        }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: palette.lime,
    borderRadius: radius.xl,
    padding: spacing.lg,
    gap: spacing.sm + 2,
  },
  head: { flexDirection: "row", alignItems: "center", gap: spacing.sm + 4 },
  letter: {
    width: 40,
    height: 40,
    borderRadius: 13,
    backgroundColor: palette.onLime,
    alignItems: "center",
    justifyContent: "center",
  },
  letterText: { fontFamily: fonts.display, fontSize: 18, color: palette.lime },
  title: { fontFamily: fonts.display, fontSize: 34, lineHeight: 34, color: palette.onLime },
  restCard: {
    backgroundColor: palette.surface,
    borderRadius: radius.xl,
    padding: spacing.lg,
    gap: spacing.sm,
  },
});
