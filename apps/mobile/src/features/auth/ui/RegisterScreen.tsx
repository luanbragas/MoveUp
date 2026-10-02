import { zodResolver } from "@hookform/resolvers/zod";
import { Redirect, router } from "expo-router";
import { Controller, useForm, useWatch } from "react-hook-form";
import { z } from "zod";
import { Button } from "../../../shared/ui/Button";
import { ChoiceGroup } from "../../../shared/ui/ChoiceGroup";
import { Message } from "../../../shared/ui/Message";
import { Screen } from "../../../shared/ui/Screen";
import { TextField } from "../../../shared/ui/TextField";
import { parseBirthDate } from "../domain/birth-date";
import type { AccountRole } from "../domain/me";
import { useSignOut } from "../hooks/use-auth-actions";
import { useAuthState } from "../hooks/use-auth-state";
import { useRegisterAccount } from "../hooks/use-onboarding";
import { describeError } from "./describe-error";
import { strings } from "./strings";

const t = strings.register;

const FormSchema = z
  .object({
    role: z.enum(["professional", "client"]).nullable(),
    name: z.string().trim().min(2, { error: t.nameRequired }),
    birthDate: z.string(),
    businessName: z.string(),
    registryNumber: z.string(),
  })
  .superRefine((form, ctx) => {
    if (form.role === null) {
      ctx.addIssue({ code: "custom", path: ["role"], message: t.roleRequired });
    }
    const typed = form.birthDate.trim();
    if (typed === "") {
      if (form.role === "client") {
        ctx.addIssue({ code: "custom", path: ["birthDate"], message: t.birthDateRequired });
      }
    } else if (parseBirthDate(typed) === null) {
      ctx.addIssue({ code: "custom", path: ["birthDate"], message: t.birthDateInvalid });
    }
  });
type Form = z.infer<typeof FormSchema>;

const ROLES: readonly { value: AccountRole; label: string }[] = [
  { value: "professional", label: t.professional },
  { value: "client", label: t.client },
];

/** Cadastro depois do primeiro login (SCREEN-FLOWS 0.2): papel, nome e dados do papel. */
export function RegisterScreen() {
  const auth = useAuthState();
  const uid = auth.status === "signed-in" ? auth.user.uid : "";
  const register = useRegisterAccount(uid);
  const signOut = useSignOut();
  const { control, handleSubmit, formState } = useForm<Form>({
    resolver: zodResolver(FormSchema),
    defaultValues: { role: null, name: "", birthDate: "", businessName: "", registryNumber: "" },
  });
  const role = useWatch({ control, name: "role" });

  if (auth.status === "signed-out") {
    return <Redirect href="/" />;
  }

  const submit = handleSubmit((form) => {
    if (form.role === null) {
      return;
    }
    const birthDate = form.birthDate.trim() === "" ? null : parseBirthDate(form.birthDate);
    register.mutate(
      {
        role: form.role,
        name: form.name,
        birthDate,
        businessName: form.role === "professional" ? form.businessName : null,
        registryNumber: form.role === "professional" ? form.registryNumber : null,
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
        name="role"
        render={({ field }) => (
          <ChoiceGroup
            label={t.roleLabel}
            options={ROLES}
            value={field.value}
            onChange={field.onChange}
            error={formState.errors.role?.message}
          />
        )}
      />
      <Controller
        control={control}
        name="name"
        render={({ field }) => (
          <TextField
            label={t.name}
            value={field.value}
            onChangeText={field.onChange}
            onBlur={field.onBlur}
            autoComplete="name"
            textContentType="name"
            error={formState.errors.name?.message}
          />
        )}
      />
      <Controller
        control={control}
        name="birthDate"
        render={({ field }) => (
          <TextField
            label={role === "professional" ? t.birthDateOptional : t.birthDate}
            value={field.value}
            onChangeText={field.onChange}
            onBlur={field.onBlur}
            keyboardType="numbers-and-punctuation"
            placeholder="DD/MM/AAAA"
            error={formState.errors.birthDate?.message}
          />
        )}
      />
      {role === "professional" ? (
        <>
          <Controller
            control={control}
            name="businessName"
            render={({ field }) => (
              <TextField
                label={t.businessName}
                value={field.value}
                onChangeText={field.onChange}
                onBlur={field.onBlur}
              />
            )}
          />
          <Controller
            control={control}
            name="registryNumber"
            render={({ field }) => (
              <TextField
                label={t.registryNumber}
                value={field.value}
                onChangeText={field.onChange}
                onBlur={field.onBlur}
                autoCapitalize="characters"
              />
            )}
          />
        </>
      ) : null}
      {register.isError ? <Message text={describeError(register.error)} /> : null}
      <Button
        label={t.submit}
        loading={register.isPending}
        onPress={() => {
          void submit();
        }}
      />
      <Button
        label={strings.entry.signOut}
        variant="secondary"
        onPress={() => {
          signOut.mutate(undefined, {
            onSuccess: () => {
              router.replace("/");
            },
          });
        }}
      />
    </Screen>
  );
}
