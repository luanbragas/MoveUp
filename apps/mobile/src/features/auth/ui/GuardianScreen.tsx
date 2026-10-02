import { zodResolver } from "@hookform/resolvers/zod";
import { Redirect, router } from "expo-router";
import { Controller, useForm } from "react-hook-form";
import { ActivityIndicator } from "react-native";
import { z } from "zod";
import { Button } from "../../../shared/ui/Button";
import { Checkbox } from "../../../shared/ui/Checkbox";
import { ChoiceGroup } from "../../../shared/ui/ChoiceGroup";
import { Message } from "../../../shared/ui/Message";
import { Screen } from "../../../shared/ui/Screen";
import { TextField } from "../../../shared/ui/TextField";
import type { GuardianRelationship } from "../domain/ports";
import { useAuthState } from "../hooks/use-auth-state";
import { useDeclareGuardian, useLegalVersions } from "../hooks/use-onboarding";
import { describeError } from "./describe-error";
import { strings } from "./strings";

const t = strings.guardian;

const FormSchema = z.object({
  guardianName: z.string().trim().min(2, { error: t.nameRequired }),
  guardianEmail: z.email({ error: t.emailInvalid }),
  relationship: z.enum(["mother", "father", "legal_guardian", "other"]).nullable(),
  declared: z.boolean().refine((value) => value, { error: t.mustDeclare }),
});
type Form = z.infer<typeof FormSchema>;

const RELATIONSHIPS: readonly { value: GuardianRelationship; label: string }[] = (
  Object.keys(t.relationships) as GuardianRelationship[]
).map((value) => ({ value, label: t.relationships[value] }));

/**
 * Autorização do responsável pelo aluno menor (LGPD, art. 14). Pensada para ser preenchida pelo
 * próprio responsável, no aparelho do aluno.
 */
export function GuardianScreen() {
  const auth = useAuthState();
  const uid = auth.status === "signed-in" ? auth.user.uid : "";
  const versions = useLegalVersions();
  const declare = useDeclareGuardian(uid);
  const { control, handleSubmit, formState } = useForm<Form>({
    resolver: zodResolver(FormSchema),
    defaultValues: { guardianName: "", guardianEmail: "", relationship: null, declared: false },
  });

  if (auth.status === "signed-out") {
    return <Redirect href="/" />;
  }
  if (versions.data === undefined) {
    return (
      <Screen title={t.title}>
        {versions.isError ? (
          <Message text={describeError(versions.error)} />
        ) : (
          <ActivityIndicator accessibilityLabel="Carregando" />
        )}
      </Screen>
    );
  }
  const legal = versions.data;

  const submit = handleSubmit((form) => {
    if (form.relationship === null) {
      return;
    }
    declare.mutate(
      {
        input: {
          guardianName: form.guardianName,
          guardianEmail: form.guardianEmail,
          relationship: form.relationship,
        },
        versions: legal,
      },
      {
        onSuccess: () => {
          router.replace("/");
        },
      },
    );
  });

  return (
    <Screen title={t.title} subtitle={t.subtitle}>
      <Controller
        control={control}
        name="guardianName"
        render={({ field }) => (
          <TextField
            label={t.name}
            value={field.value}
            onChangeText={field.onChange}
            onBlur={field.onBlur}
            autoComplete="name"
            error={formState.errors.guardianName?.message}
          />
        )}
      />
      <Controller
        control={control}
        name="guardianEmail"
        render={({ field }) => (
          <TextField
            label={t.email}
            value={field.value}
            onChangeText={field.onChange}
            onBlur={field.onBlur}
            keyboardType="email-address"
            autoCapitalize="none"
            error={formState.errors.guardianEmail?.message}
          />
        )}
      />
      <Controller
        control={control}
        name="relationship"
        render={({ field }) => (
          <ChoiceGroup
            label={t.relationshipLabel}
            options={RELATIONSHIPS}
            value={field.value}
            onChange={field.onChange}
            error={
              field.value === null && formState.isSubmitted ? t.relationshipRequired : undefined
            }
          />
        )}
      />
      <Controller
        control={control}
        name="declared"
        render={({ field }) => (
          <Checkbox label={t.declaration} checked={field.value} onChange={field.onChange} />
        )}
      />
      {formState.errors.declared?.message === undefined ? null : (
        <Message text={formState.errors.declared.message} />
      )}
      {declare.isError ? <Message text={describeError(declare.error)} /> : null}
      <Button
        label={t.submit}
        loading={declare.isPending}
        onPress={() => {
          void submit();
        }}
      />
    </Screen>
  );
}
