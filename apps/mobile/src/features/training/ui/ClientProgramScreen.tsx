import { zodResolver } from "@hookform/resolvers/zod";
import { router, useLocalSearchParams } from "expo-router";
import { useState } from "react";
import { Controller, useForm, useWatch } from "react-hook-form";
import { Pressable, StyleSheet, Text, View } from "react-native";
import { z } from "zod";
import { toAppError } from "../../../shared/lib/http";
import { Button } from "../../../shared/ui/Button";
import { Chip } from "../../../shared/ui/Chip";
import { EmptyState } from "../../../shared/ui/EmptyState";
import { errorMessage } from "../../../shared/ui/error-messages";
import { Icon } from "../../../shared/ui/Icon";
import { ListRow } from "../../../shared/ui/ListRow";
import { Message } from "../../../shared/ui/Message";
import { RoundButton } from "../../../shared/ui/RoundButton";
import { Screen } from "../../../shared/ui/Screen";
import { Segmented } from "../../../shared/ui/Segmented";
import { Skeleton } from "../../../shared/ui/Skeleton";
import { TextField } from "../../../shared/ui/TextField";
import { MIN_TOUCH, fonts, palette, radius, spacing, typography } from "../../../shared/ui/theme";
import {
  letterOf,
  programWeek,
  weekdaysLabel,
  type Program,
  type ProgramWorkout,
  type ScheduleMode,
} from "../domain/program";
import { executionStrings } from "../../execution";
import { useStartPresencial } from "../hooks/use-presencial";
import {
  useActiveProgram,
  useAddProgramWorkout,
  useCreateProgram,
  useTemplates,
  useUpdateProgram,
} from "../hooks/use-training";
import { presencialInputOf } from "./presencial";
import { strings } from "./strings";

const t = strings.program;

/** "15/10/2026" → "2026-10-15"; vazio → null; inválido → undefined. */
export function parseDate(typed: string): string | null | undefined {
  const value = typed.trim();
  if (value === "") {
    return null;
  }
  const match = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(value);
  if (match === null) {
    return undefined;
  }
  const [, d, m, y] = match;
  const iso = `${String(y)}-${String(m)}-${String(d)}`;
  const date = new Date(`${iso}T00:00:00`);
  return Number.isNaN(date.getTime()) || date.getDate() !== Number(d) ? undefined : iso;
}

/** Rota /clients/[linkId]: programa do aluno (design 5.4). */
export function ClientProgramScreen() {
  const { linkId, name } = useLocalSearchParams<{ linkId: string; name?: string }>();
  const program = useActiveProgram(linkId);
  const back = (
    <RoundButton
      icon="back"
      label={t.back}
      onPress={() => {
        router.back();
      }}
    />
  );

  if (program.isPending) {
    return (
      <Screen header={back} title={name ?? ""}>
        <Skeleton width="100%" height={120} rounded={24} />
        <Skeleton width="100%" height={72} rounded={24} />
      </Screen>
    );
  }
  if (program.isError) {
    return (
      <Screen header={back} title={name ?? ""}>
        <Message text={errorMessage(toAppError(program.error))} />
        <Button
          label={t.retry}
          variant="secondary"
          icon="refresh"
          onPress={() => {
            void program.refetch();
          }}
        />
      </Screen>
    );
  }
  return program.data === null ? (
    <NewProgramForm linkId={linkId} clientName={name ?? ""} header={back} />
  ) : (
    <ProgramView key={program.data.revision} linkId={linkId} program={program.data} header={back} />
  );
}

const FormSchema = z
  .object({
    name: z.string().trim().min(1, { error: t.nameRequired }).max(80),
    goal: z.string().max(200),
    startsOn: z.string(),
    endsOn: z.string(),
    scheduleMode: z.enum(["fixed_days", "sequence"]),
    weeklyTarget: z.number().int().min(1).max(14),
  })
  .superRefine((form, ctx) => {
    for (const field of ["startsOn", "endsOn"] as const) {
      if (parseDate(form[field]) === undefined) {
        ctx.addIssue({ code: "custom", path: [field], message: t.dateInvalid });
      }
    }
  });
type Form = z.infer<typeof FormSchema>;

function NewProgramForm({
  linkId,
  clientName,
  header,
}: {
  readonly linkId: string;
  readonly clientName: string;
  readonly header: React.ReactNode;
}) {
  const [open, setOpen] = useState(false);
  const create = useCreateProgram(linkId);
  const { control, handleSubmit, formState } = useForm<Form>({
    resolver: zodResolver(FormSchema),
    defaultValues: {
      name: "",
      goal: "",
      startsOn: "",
      endsOn: "",
      scheduleMode: "fixed_days",
      weeklyTarget: 3,
    },
  });
  const mode = useWatch({ control, name: "scheduleMode" });

  if (!open) {
    return (
      <Screen header={header} title={clientName}>
        <EmptyState
          icon="dumbbell"
          title={t.noneTitle}
          text={t.noneText}
          action={
            <Button
              label={t.create}
              icon="plus"
              onPress={() => {
                setOpen(true);
              }}
            />
          }
        />
      </Screen>
    );
  }

  const submit = handleSubmit((form) => {
    create.mutate({
      name: form.name,
      goal: form.goal.trim() === "" ? null : form.goal.trim(),
      startsOn: parseDate(form.startsOn) ?? null,
      endsOn: parseDate(form.endsOn) ?? null,
      scheduleMode: form.scheduleMode,
      weeklyTarget: form.scheduleMode === "sequence" ? form.weeklyTarget : null,
    });
  });

  return (
    <Screen
      header={header}
      title={t.newTitle}
      subtitle={clientName}
      footer={
        <Button
          label={t.create}
          loading={create.isPending}
          onPress={() => {
            void submit();
          }}
        />
      }
    >
      <Controller
        control={control}
        name="name"
        render={({ field }) => (
          <TextField
            label={t.name}
            value={field.value}
            onChangeText={field.onChange}
            placeholder={t.namePlaceholder}
            error={formState.errors.name?.message}
          />
        )}
      />
      <Controller
        control={control}
        name="goal"
        render={({ field }) => (
          <TextField label={t.goal} value={field.value} onChangeText={field.onChange} />
        )}
      />
      <View style={styles.pair}>
        <View style={{ flex: 1 }}>
          <Controller
            control={control}
            name="startsOn"
            render={({ field }) => (
              <TextField
                label={t.startsOn}
                value={field.value}
                onChangeText={field.onChange}
                keyboardType="numbers-and-punctuation"
                placeholder="DD/MM/AAAA"
                error={formState.errors.startsOn?.message}
              />
            )}
          />
        </View>
        <View style={{ flex: 1 }}>
          <Controller
            control={control}
            name="endsOn"
            render={({ field }) => (
              <TextField
                label={t.endsOn}
                value={field.value}
                onChangeText={field.onChange}
                keyboardType="numbers-and-punctuation"
                placeholder="DD/MM/AAAA"
                error={formState.errors.endsOn?.message}
              />
            )}
          />
        </View>
      </View>
      <Text style={[typography.label, { color: palette.textSoft }]}>{t.mode}</Text>
      <Controller
        control={control}
        name="scheduleMode"
        render={({ field }) => (
          <Segmented<ScheduleMode>
            label={t.mode}
            value={field.value}
            onChange={field.onChange}
            options={[
              { value: "fixed_days", label: t.modes.fixed_days },
              { value: "sequence", label: t.modes.sequence },
            ]}
          />
        )}
      />
      <Text style={[typography.small, { color: palette.muted }]}>{t.modeHint[mode]}</Text>
      {mode === "sequence" ? (
        <>
          <Text style={[typography.label, { color: palette.textSoft }]}>{t.weeklyTarget}</Text>
          <Controller
            control={control}
            name="weeklyTarget"
            render={({ field }) => (
              <View style={styles.chips}>
                {[1, 2, 3, 4, 5, 6, 7].map((n) => (
                  <Chip
                    key={n}
                    label={String(n)}
                    role="radio"
                    selected={field.value === n}
                    onPress={() => {
                      field.onChange(n);
                    }}
                  />
                ))}
              </View>
            )}
          />
        </>
      ) : null}
      {create.isError ? <Message text={errorMessage(toAppError(create.error))} /> : null}
    </Screen>
  );
}

function ProgramView({
  linkId,
  program,
  header,
}: {
  readonly linkId: string;
  readonly program: Program;
  readonly header: React.ReactNode;
}) {
  const [days, setDays] = useState<Record<string, readonly number[]>>(
    Object.fromEntries(program.workouts.map((w) => [w.id, w.weekdays])),
  );
  const [order, setOrder] = useState<readonly ProgramWorkout[]>(program.workouts);
  const [dirty, setDirty] = useState(false);
  const [adding, setAdding] = useState(false);
  const update = useUpdateProgram(linkId);
  const startPresencial = useStartPresencial((workout) => presencialInputOf(workout, linkId));
  const add = useAddProgramWorkout(linkId);
  const templates = useTemplates();
  const week = programWeek(program, new Date());
  const fixed = program.scheduleMode === "fixed_days";

  const openEditor = (workoutId: string) => {
    router.push({ pathname: "/workouts/[id]", params: { id: workoutId } });
  };

  const presencial = (workoutId: string) => {
    startPresencial.mutate(workoutId, {
      onSuccess: (session) => {
        router.push({ pathname: "/session/[id]", params: { id: session.id } });
      },
    });
  };

  const addWorkout = (templateId: string | null) => {
    add.mutate(
      {
        programId: program.id,
        templateId,
        name: templateId === null ? t.workoutName(letterOf(program.workouts.length + 1)) : null,
      },
      {
        onSuccess: (workout) => {
          setAdding(false);
          openEditor(workout.id);
        },
      },
    );
  };

  const meta = [
    week === null ? null : t.week(week.current, week.total),
    fixed ? t.modes.fixed_days : t.perWeek(program.weeklyTarget ?? 0),
  ]
    .filter((part) => part !== null)
    .join(" · ");

  return (
    <Screen
      header={header}
      title={program.name}
      subtitle={[program.goal, meta].filter((part) => part !== null && part !== "").join(" · ")}
      footer={
        dirty ? (
          <Button
            label={t.saveSchedule}
            icon="check"
            loading={update.isPending}
            onPress={() => {
              update.mutate({
                program,
                input: {
                  name: program.name,
                  goal: program.goal,
                  startsOn: program.startsOn,
                  endsOn: program.endsOn,
                  scheduleMode: program.scheduleMode,
                  weeklyTarget: program.weeklyTarget,
                },
                schedule: order.map((w) => ({
                  workoutId: w.id,
                  weekdays: fixed ? (days[w.id] ?? []) : [],
                })),
              });
            }}
          />
        ) : undefined
      }
    >
      {order.map((workout, index) => (
        <View key={workout.id} style={styles.workout}>
          <Pressable
            accessibilityRole="button"
            accessibilityLabel={`${workout.name}, ${String(workout.exercises)} exercícios`}
            onPress={() => {
              openEditor(workout.id);
            }}
            style={styles.workoutMain}
          >
            <View style={styles.letter}>
              <Text style={styles.letterText}>{letterOf(index + 1)}</Text>
            </View>
            <View style={{ flex: 1, gap: 2 }}>
              <Text style={[typography.label, { color: palette.text, fontSize: 16 }]}>
                {workout.name}
              </Text>
              <Text style={[typography.small, { color: palette.muted }]}>
                {[
                  fixed
                    ? (days[workout.id] ?? []).length === 0
                      ? t.noDays
                      : weekdaysLabel(days[workout.id] ?? [])
                    : null,
                  strings.templates.meta(workout.exercises, workout.estimatedMinutes),
                ]
                  .filter((part) => part !== null)
                  .join(" · ")}
              </Text>
            </View>
            <Icon name="chevron" size={18} color={palette.muted} />
          </Pressable>
          {fixed ? (
            <View style={styles.days}>
              {t.days.map((letter, day) => {
                const on = (days[workout.id] ?? []).includes(day);
                return (
                  <Pressable
                    key={day}
                    accessibilityRole="checkbox"
                    accessibilityLabel={`${workout.name} na ${t.dayNames[day] ?? ""}`}
                    accessibilityState={{ checked: on }}
                    onPress={() => {
                      const current = days[workout.id] ?? [];
                      setDays({
                        ...days,
                        [workout.id]: on ? current.filter((d) => d !== day) : [...current, day],
                      });
                      setDirty(true);
                    }}
                    style={[styles.day, on ? styles.dayOn : null]}
                  >
                    <Text
                      style={[typography.label, { color: on ? palette.onLime : palette.muted }]}
                    >
                      {letter}
                    </Text>
                  </Pressable>
                );
              })}
            </View>
          ) : null}
          <Pressable
            accessibilityRole="button"
            accessibilityLabel={`${executionStrings.start.presencial}: ${workout.name}`}
            onPress={() => {
              presencial(workout.id);
            }}
            style={styles.presencial}
          >
            <Icon name="play" size={16} color={palette.lime} />
            <Text style={[typography.label, { color: palette.lime }]}>
              {executionStrings.start.presencial}
            </Text>
          </Pressable>
          {!fixed && index > 0 ? (
            <Pressable
              accessibilityRole="button"
              accessibilityLabel={`${strings.editor.moveUp}: ${workout.name}`}
              onPress={() => {
                const next = [...order];
                const [item] = next.splice(index, 1);
                if (item !== undefined) {
                  next.splice(index - 1, 0, item);
                }
                setOrder(next);
                setDirty(true);
              }}
              style={styles.up}
            >
              <Icon name="up" size={18} color={palette.muted} />
            </Pressable>
          ) : null}
        </View>
      ))}
      {update.isError ? <Message text={errorMessage(toAppError(update.error))} /> : null}
      {adding ? (
        <View style={styles.addBox}>
          <Text style={[typography.label, { color: palette.textSoft }]}>{t.fromTemplate}</Text>
          {(templates.data ?? []).map((template, index, all) => (
            <ListRow
              key={template.id}
              title={template.name}
              subtitle={strings.templates.meta(template.exercises, template.estimatedMinutes)}
              icon="copy"
              last={index === all.length - 1}
              onPress={() => {
                addWorkout(template.id);
              }}
            />
          ))}
          <Button
            label={t.blank}
            variant="secondary"
            icon="plus"
            loading={add.isPending}
            onPress={() => {
              addWorkout(null);
            }}
          />
        </View>
      ) : (
        <Button
          label={t.addWorkout}
          variant="secondary"
          icon="plus"
          onPress={() => {
            setAdding(true);
          }}
        />
      )}
      {add.isError ? <Message text={errorMessage(toAppError(add.error))} /> : null}
    </Screen>
  );
}

const styles = StyleSheet.create({
  pair: { flexDirection: "row", gap: spacing.sm },
  chips: { flexDirection: "row", flexWrap: "wrap", gap: spacing.sm },
  workout: {
    backgroundColor: palette.surface,
    borderRadius: radius.lg,
    padding: spacing.sm + 4,
    gap: spacing.sm,
  },
  workoutMain: {
    flexDirection: "row",
    alignItems: "center",
    gap: spacing.sm + 4,
    minHeight: MIN_TOUCH,
  },
  letter: {
    width: 44,
    height: 44,
    borderRadius: 14,
    backgroundColor: palette.lime,
    alignItems: "center",
    justifyContent: "center",
  },
  letterText: { fontFamily: fonts.display, fontSize: 20, color: palette.onLime },
  days: { flexDirection: "row", gap: 6 },
  day: {
    flex: 1,
    height: MIN_TOUCH - 4,
    borderRadius: radius.sm,
    backgroundColor: palette.surface2,
    alignItems: "center",
    justifyContent: "center",
  },
  dayOn: { backgroundColor: palette.lime },
  presencial: {
    flexDirection: "row",
    alignItems: "center",
    gap: 6,
    minHeight: MIN_TOUCH,
    alignSelf: "flex-start",
  },
  up: {
    alignSelf: "flex-end",
    width: MIN_TOUCH,
    height: 36,
    alignItems: "center",
    justifyContent: "center",
  },
  addBox: { gap: spacing.sm },
});
