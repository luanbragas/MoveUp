import { router } from "expo-router";
import { Text, View } from "react-native";
import { ListRow } from "../../../shared/ui/ListRow";
import { palette, spacing, typography } from "../../../shared/ui/theme";
import { exerciseCount, type PlannedSnapshot } from "../domain/planned";
import { strings } from "./strings";

const t = strings.today;
const DAYS = ["dom", "seg", "ter", "qua", "qui", "sex", "sáb"] as const;

function daysLabel(weekdays: readonly number[]): string | null {
  if (weekdays.length === 0) {
    return null;
  }
  const names = [...weekdays].sort((a, b) => a - b).map((d) => DAYS[d] ?? "");
  return names.length === 1
    ? (names[0] ?? null)
    : `${names.slice(0, -1).join(", ")} e ${names.at(-1) ?? ""}`;
}

/** Todos os treinos do programa (A, B, C…), para o aluno ver e abrir qualquer um. */
export function ProgramWorkouts({ snapshot }: { readonly snapshot: PlannedSnapshot }) {
  if (snapshot.program === null || snapshot.workouts.length < 2) {
    return null;
  }
  return (
    <View style={{ gap: spacing.sm }}>
      <Text style={[typography.label, { color: palette.textSoft }]}>{t.otherWorkouts}</Text>
      {snapshot.workouts.map((workout, index) => (
        <ListRow
          key={workout.id}
          title={workout.name}
          subtitle={[
            daysLabel(workout.weekdays),
            t.meta(exerciseCount(workout), workout.estimatedMinutes),
          ]
            .filter((part) => part !== null)
            .join(" · ")}
          icon="dumbbell"
          last={index === snapshot.workouts.length - 1}
          onPress={() => {
            router.push({ pathname: "/workout/[id]", params: { id: workout.id } });
          }}
        />
      ))}
    </View>
  );
}
