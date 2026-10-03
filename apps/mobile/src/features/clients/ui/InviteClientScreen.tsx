import { zodResolver } from "@hookform/resolvers/zod";
import { router } from "expo-router";
import { useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { StyleSheet, View } from "react-native";
import { z } from "zod";
import { toAppError } from "../../../shared/lib/http";
import { Button } from "../../../shared/ui/Button";
import { Chip } from "../../../shared/ui/Chip";
import { errorMessage } from "../../../shared/ui/error-messages";
import { Message } from "../../../shared/ui/Message";
import { RoundButton } from "../../../shared/ui/RoundButton";
import { Screen } from "../../../shared/ui/Screen";
import { TextField } from "../../../shared/ui/TextField";
import { spacing } from "../../../shared/ui/theme";
import { isFull } from "../domain/client";
import { useInviteClient, useSeats } from "../hooks/use-clients";
import { PlanFullScreen } from "./PlanFullScreen";
import { openShare } from "./share-params";
import { strings } from "./strings";

const t = strings.form;

const optional = (text: string) => (text.trim() === "" ? null : text.trim());

// Mesmos limites do backend (InvalidClientData); o backend continua sendo a regra final.
const FormSchema = z.object({
  name: z.string().trim().min(2, { error: t.nameInvalid }).max(200, { error: t.nameInvalid }),
  email: z.union([z.literal(""), z.email({ error: t.emailInvalid })]),
  phone: z.string().refine(
    (value) => {
      const digits = value.replace(/\D/g, "");
      return value.trim() === "" || (digits.length >= 10 && digits.length <= 15);
    },
    { error: t.phoneInvalid },
  ),
  goal: z.string().max(500, { error: t.goalTooLong }),
});
type Form = z.infer<typeof FormSchema>;

/** Pré-cadastro do aluno; ao criar, abre o compartilhamento do convite. */
export function InviteClientScreen() {
  const invite = useInviteClient();
  const seats = useSeats();
  const [released, setReleased] = useState(false);
  const { control, handleSubmit, formState } = useForm<Form>({
    resolver: zodResolver(FormSchema),
    defaultValues: { name: "", email: "", phone: "", goal: "" },
  });

  const submit = handleSubmit((form) => {
    const input = {
      name: form.name,
      email: optional(form.email),
      phone: optional(form.phone),
      goal: optional(form.goal),
    };
    invite.mutate(input, {
      onSuccess: (invitation) => {
        openShare(invitation, { name: input.name, phone: input.phone }, "replace");
      },
    });
  });

  const failure = invite.isError ? toAppError(invite.error) : null;
  const limitHit = failure?.kind === "problem" && failure.code === "plan-limit-reached";
  if (seats.data !== undefined && !released && (isFull(seats.data) || limitHit)) {
    return (
      <PlanFullScreen
        seats={seats.data}
        onReleased={() => {
          invite.reset();
          setReleased(true);
        }}
      />
    );
  }

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
          loading={invite.isPending}
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
            autoCapitalize="words"
            autoComplete="off"
            error={formState.errors.name?.message}
          />
        )}
      />
      <Controller
        control={control}
        name="phone"
        render={({ field }) => (
          <TextField
            label={t.phone}
            value={field.value}
            onChangeText={field.onChange}
            onBlur={field.onBlur}
            keyboardType="phone-pad"
            autoComplete="off"
            placeholder={t.phonePlaceholder}
            error={formState.errors.phone?.message}
          />
        )}
      />
      <Controller
        control={control}
        name="email"
        render={({ field }) => (
          <TextField
            label={t.email}
            value={field.value}
            onChangeText={field.onChange}
            onBlur={field.onBlur}
            keyboardType="email-address"
            autoCapitalize="none"
            autoComplete="off"
            error={formState.errors.email?.message}
          />
        )}
      />
      <Controller
        control={control}
        name="goal"
        render={({ field }) => (
          <View accessibilityRole="radiogroup" accessibilityLabel={t.goal} style={styles.goals}>
            {t.goals.map((goal) => (
              <Chip
                key={goal}
                label={goal}
                role="radio"
                selected={field.value === goal}
                onPress={() => {
                  field.onChange(field.value === goal ? "" : goal);
                }}
              />
            ))}
          </View>
        )}
      />
      {released ? <Message tone="info" text={strings.full.released} /> : null}
      {failure === null ? null : <Message text={errorMessage(failure)} />}
    </Screen>
  );
}

const styles = StyleSheet.create({
  goals: { flexDirection: "row", flexWrap: "wrap", gap: spacing.sm },
});
