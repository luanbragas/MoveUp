import { zodResolver } from "@hookform/resolvers/zod";
import { router } from "expo-router";
import { Controller, useForm, useWatch } from "react-hook-form";
import { StyleSheet, Text, View } from "react-native";
import { z } from "zod";
import { toAppError } from "../../../shared/lib/http";
import { Button } from "../../../shared/ui/Button";
import { Chip } from "../../../shared/ui/Chip";
import { errorMessage } from "../../../shared/ui/error-messages";
import { Message } from "../../../shared/ui/Message";
import { RoundButton } from "../../../shared/ui/RoundButton";
import { Screen } from "../../../shared/ui/Screen";
import { TextField } from "../../../shared/ui/TextField";
import { Toggle } from "../../../shared/ui/Toggle";
import { palette, spacing, typography } from "../../../shared/ui/theme";
import { MUSCLE_CODES, type Modality, type TrackingType } from "../domain/exercise";
import { useCreateExercise } from "../hooks/use-exercises";
import { strings } from "./strings";

const t = strings.form;
const MODALITIES = Object.keys(strings.modalities) as Modality[];
const TRACKING = Object.keys(strings.tracking) as TrackingType[];

const FormSchema = z.object({
  name: z.string().trim().min(2, { error: t.nameInvalid }).max(120, { error: t.nameInvalid }),
  modality: z.enum(["strength", "cardio", "conditioning", "complementary"]),
  trackingType: z.enum(["reps_load", "reps_only", "time", "distance_time"]),
  primaryMuscle: z.enum(MUSCLE_CODES).nullable(),
  secondaryMuscles: z.array(z.enum(MUSCLE_CODES)),
  equipment: z.string(),
  unilateral: z.boolean(),
  instructions: z.string().max(2000),
  mediaUrl: z.union([z.literal(""), z.url({ protocol: /^https$/, error: t.mediaInvalid })]),
});
type Form = z.infer<typeof FormSchema>;

function Label({ text }: { readonly text: string }) {
  return <Text style={[typography.label, { color: palette.textSoft }]}>{text}</Text>;
}

/** Exercício próprio do personal (fica só na biblioteca dele). */
export function NewExerciseScreen() {
  const create = useCreateExercise();
  const { control, handleSubmit, formState } = useForm<Form>({
    resolver: zodResolver(FormSchema),
    defaultValues: {
      name: "",
      modality: "strength",
      trackingType: "reps_load",
      primaryMuscle: null,
      secondaryMuscles: [],
      equipment: "",
      unilateral: false,
      instructions: "",
      mediaUrl: "",
    },
  });
  const primary = useWatch({ control, name: "primaryMuscle" });

  const submit = handleSubmit((form) => {
    create.mutate(
      {
        ...form,
        secondaryMuscles: form.secondaryMuscles.filter((m) => m !== form.primaryMuscle),
        equipment: form.equipment,
        instructions: form.instructions,
        mediaUrl: form.mediaUrl,
      },
      {
        onSuccess: () => {
          router.back();
        },
      },
    );
  });

  return (
    <Screen
      header={
        <RoundButton
          icon="close"
          label={t.close}
          onPress={() => {
            router.back();
          }}
        />
      }
      title={t.title}
      subtitle={t.subtitle}
      footer={
        <Button
          label={t.submit}
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
            onBlur={field.onBlur}
            autoCapitalize="sentences"
            error={formState.errors.name?.message}
          />
        )}
      />
      <Label text={t.modality} />
      <Controller
        control={control}
        name="modality"
        render={({ field }) => (
          <View accessibilityRole="radiogroup" accessibilityLabel={t.modality} style={styles.chips}>
            {MODALITIES.map((value) => (
              <Chip
                key={value}
                label={strings.modalities[value]}
                role="radio"
                selected={field.value === value}
                onPress={() => {
                  field.onChange(value);
                }}
              />
            ))}
          </View>
        )}
      />
      <Label text={t.tracking} />
      <Controller
        control={control}
        name="trackingType"
        render={({ field }) => (
          <View accessibilityRole="radiogroup" accessibilityLabel={t.tracking} style={styles.chips}>
            {TRACKING.map((value) => (
              <Chip
                key={value}
                label={strings.tracking[value]}
                role="radio"
                selected={field.value === value}
                onPress={() => {
                  field.onChange(value);
                }}
              />
            ))}
          </View>
        )}
      />
      <Label text={t.primary} />
      <Controller
        control={control}
        name="primaryMuscle"
        render={({ field }) => (
          <View accessibilityRole="radiogroup" accessibilityLabel={t.primary} style={styles.chips}>
            {MUSCLE_CODES.map((code) => (
              <Chip
                key={code}
                label={strings.muscles[code]}
                role="radio"
                selected={field.value === code}
                onPress={() => {
                  field.onChange(field.value === code ? null : code);
                }}
              />
            ))}
          </View>
        )}
      />
      <Label text={t.secondary} />
      <Controller
        control={control}
        name="secondaryMuscles"
        render={({ field }) => (
          <View style={styles.chips}>
            {MUSCLE_CODES.filter((code) => code !== primary).map((code) => {
              const on = field.value.includes(code);
              return (
                <Chip
                  key={code}
                  label={strings.muscles[code]}
                  selected={on}
                  onPress={() => {
                    field.onChange(
                      on
                        ? field.value.filter((m) => m !== code)
                        : [...field.value, code].slice(0, 6),
                    );
                  }}
                />
              );
            })}
          </View>
        )}
      />
      <Controller
        control={control}
        name="unilateral"
        render={({ field }) => (
          <Toggle
            label={t.unilateral}
            description={t.unilateralHint}
            value={field.value}
            onChange={field.onChange}
          />
        )}
      />
      <Controller
        control={control}
        name="equipment"
        render={({ field }) => (
          <TextField label={t.equipment} value={field.value} onChangeText={field.onChange} />
        )}
      />
      <Controller
        control={control}
        name="instructions"
        render={({ field }) => (
          <TextField label={t.instructions} value={field.value} onChangeText={field.onChange} />
        )}
      />
      <Controller
        control={control}
        name="mediaUrl"
        render={({ field }) => (
          <TextField
            label={t.mediaUrl}
            value={field.value}
            onChangeText={field.onChange}
            keyboardType="url"
            autoCapitalize="none"
            placeholder="https://"
            error={formState.errors.mediaUrl?.message}
          />
        )}
      />
      {create.isError ? <Message text={errorMessage(toAppError(create.error))} /> : null}
    </Screen>
  );
}

const styles = StyleSheet.create({
  chips: { flexDirection: "row", flexWrap: "wrap", gap: spacing.sm },
});
