import { zodResolver } from "@hookform/resolvers/zod";
import { Redirect, router } from "expo-router";
import { useState } from "react";
import { Controller, useForm, useWatch, type FieldPath } from "react-hook-form";
import { StyleSheet, Text, View } from "react-native";
import { z } from "zod";
import { Button } from "../../../shared/ui/Button";
import { ChoiceGroup } from "../../../shared/ui/ChoiceGroup";
import { Message } from "../../../shared/ui/Message";
import { RoundButton } from "../../../shared/ui/RoundButton";
import { Screen } from "../../../shared/ui/Screen";
import { Steps } from "../../../shared/ui/Steps";
import { TextField } from "../../../shared/ui/TextField";
import { TextLink } from "../../../shared/ui/TextLink";
import { palette, radius, spacing, typography } from "../../../shared/ui/theme";
import { parseBirthDate } from "../domain/birth-date";
import type { AccountRole } from "../domain/me";
import { setPendingRole, usePendingRole } from "../hooks/pending-role";
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
type Step = "role" | "name" | "business";

const ROLES: readonly { value: AccountRole; label: string }[] = [
  { value: "professional", label: t.professional },
  { value: "client", label: t.client },
];

const FIELDS: Readonly<Record<Step, readonly FieldPath<Form>[]>> = {
  role: ["role"],
  name: ["name", "birthDate"],
  business: ["businessName", "registryNumber"],
};

function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  const first = parts[0]?.[0] ?? "";
  const last = parts.length > 1 ? (parts[parts.length - 1]?.[0] ?? "") : "";
  return (first + last).toUpperCase();
}

/**
 * Cadastro depois do primeiro login (SCREEN-FLOWS 0.2), uma pergunta por tela: papel (só se não
 * veio das boas-vindas), nome e nascimento, e o negócio do personal (opcional). Os termos são o
 * último passo, na tela seguinte.
 */
export function RegisterScreen() {
  const auth = useAuthState();
  const uid = auth.status === "signed-in" ? auth.user.uid : "";
  const pendingRole = usePendingRole();
  const register = useRegisterAccount(uid);
  const signOut = useSignOut();
  const [askRole] = useState(pendingRole === null);
  const [step, setStep] = useState<Step>(pendingRole === null ? "role" : "name");
  const { control, handleSubmit, trigger, formState } = useForm<Form>({
    resolver: zodResolver(FormSchema),
    defaultValues: {
      role: pendingRole,
      name: "",
      birthDate: "",
      businessName: "",
      registryNumber: "",
    },
  });
  const role = useWatch({ control, name: "role" });
  const name = useWatch({ control, name: "name" });
  const businessName = useWatch({ control, name: "businessName" });

  if (auth.status === "signed-out") {
    return <Redirect href="/" />;
  }

  const steps: readonly Step[] = [
    ...(askRole ? (["role"] as const) : []),
    "name",
    ...(role === "professional" ? (["business"] as const) : []),
  ];
  const position = steps.indexOf(step);
  const total = steps.length + 1; // + termos, na tela seguinte

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
          setPendingRole(null);
          router.replace("/");
        },
      },
    );
  });

  const next = async () => {
    if (!(await trigger([...FIELDS[step]]))) {
      return;
    }
    const following = steps[position + 1];
    if (following === undefined) {
      void submit();
    } else {
      setStep(following);
    }
  };

  const back = () => {
    const previous = steps[position - 1];
    if (previous !== undefined) {
      setStep(previous);
      return;
    }
    signOut.mutate(undefined, {
      onSuccess: () => {
        router.replace("/");
      },
    });
  };

  const header = (
    <>
      <RoundButton
        icon={position === 0 ? "close" : "back"}
        label={position === 0 ? t.signOut : t.back}
        onPress={back}
      />
      <View style={styles.steps}>
        <Steps current={position + 1} total={total} />
      </View>
      {step === "business" ? (
        <TextLink
          label={t.skip}
          onPress={() => {
            void submit();
          }}
        />
      ) : null}
    </>
  );

  const footer = (
    <Button
      label={t.next}
      loading={register.isPending}
      onPress={() => {
        void next();
      }}
    />
  );

  if (step === "role") {
    return (
      <Screen header={header} title={t.roleTitle} subtitle={t.roleSubtitle} footer={footer}>
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
      </Screen>
    );
  }

  if (step === "business") {
    return (
      <Screen header={header} title={t.businessTitle} subtitle={t.businessSubtitle} footer={footer}>
        <Controller
          control={control}
          name="businessName"
          render={({ field }) => (
            <TextField
              label={t.businessName}
              value={field.value}
              onChangeText={field.onChange}
              onBlur={field.onBlur}
              autoCapitalize="words"
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
              placeholder="000000-G/SP"
            />
          )}
        />
        <View style={styles.preview} accessible accessibilityLabel={`${t.preview}: ${name}`}>
          <Text style={[typography.small, { color: palette.muted }]}>{t.preview}</Text>
          <View style={styles.previewRow}>
            <View style={styles.avatar}>
              <Text style={[typography.label, { color: palette.lime }]}>{initials(name)}</Text>
            </View>
            <View>
              <Text style={[typography.label, { color: palette.text }]}>{name.trim()}</Text>
              {businessName.trim() === "" ? null : (
                <Text style={[typography.small, { color: palette.lime }]}>
                  {businessName.trim()}
                </Text>
              )}
            </View>
          </View>
        </View>
        {register.isError ? <Message text={describeError(register.error)} /> : null}
      </Screen>
    );
  }

  return (
    <Screen
      header={header}
      title={t.nameTitle}
      subtitle={role === "professional" ? t.nameSubtitleProfessional : t.nameSubtitleClient}
      footer={footer}
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
            autoComplete="name"
            textContentType="name"
            autoCapitalize="words"
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
      {register.isError ? <Message text={describeError(register.error)} /> : null}
    </Screen>
  );
}

const styles = StyleSheet.create({
  steps: { flex: 1 },
  preview: {
    backgroundColor: palette.surface,
    borderRadius: radius.lg,
    padding: spacing.md,
    gap: spacing.sm + 2,
  },
  previewRow: { flexDirection: "row", alignItems: "center", gap: spacing.sm + 4 },
  avatar: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: palette.limeDark,
    alignItems: "center",
    justifyContent: "center",
  },
});
