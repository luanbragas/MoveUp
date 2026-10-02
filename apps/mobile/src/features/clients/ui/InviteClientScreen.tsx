import { zodResolver } from "@hookform/resolvers/zod";
import { router } from "expo-router";
import { Controller, useForm } from "react-hook-form";
import { z } from "zod";
import { toAppError } from "../../../shared/lib/http";
import { Button } from "../../../shared/ui/Button";
import { errorMessage } from "../../../shared/ui/error-messages";
import { Message } from "../../../shared/ui/Message";
import { Screen } from "../../../shared/ui/Screen";
import { TextField } from "../../../shared/ui/TextField";
import { useInviteClient } from "../hooks/use-clients";
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

  return (
    <Screen title={t.title} subtitle={t.subtitle}>
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
        name="goal"
        render={({ field }) => (
          <TextField
            label={t.goal}
            value={field.value}
            onChangeText={field.onChange}
            onBlur={field.onBlur}
            error={formState.errors.goal?.message}
          />
        )}
      />
      {invite.isError ? <Message text={errorMessage(toAppError(invite.error))} /> : null}
      <Button
        label={t.submit}
        loading={invite.isPending}
        onPress={() => {
          void submit();
        }}
      />
      <Button
        label={t.cancel}
        variant="secondary"
        onPress={() => {
          router.back();
        }}
      />
    </Screen>
  );
}
