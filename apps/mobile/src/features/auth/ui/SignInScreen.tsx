import { zodResolver } from "@hookform/resolvers/zod";
import { router, useLocalSearchParams } from "expo-router";
import { useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { Pressable, Text } from "react-native";
import { z } from "zod";
import { Button } from "../../../shared/ui/Button";
import { Message } from "../../../shared/ui/Message";
import { RoundButton } from "../../../shared/ui/RoundButton";
import { Screen } from "../../../shared/ui/Screen";
import { TextField } from "../../../shared/ui/TextField";
import { TextLink } from "../../../shared/ui/TextLink";
import { MIN_TOUCH, palette, typography } from "../../../shared/ui/theme";
import { useSendPasswordReset, useSignIn, useSignUp } from "../hooks/use-auth-actions";
import { describeError } from "./describe-error";
import { strings } from "./strings";

const t = strings.signIn;

const FormSchema = z.object({
  email: z.email({ error: t.emailInvalid }),
  password: z.string().min(8, { error: t.passwordShort }),
});
type Form = z.infer<typeof FormSchema>;

/**
 * Entrar ou criar conta por e-mail e senha (Firebase). Google e Apple entram quando o login social
 * for configurado; o modo inicial vem das boas-vindas (`?mode=sign-up`).
 */
export function SignInScreen() {
  const params = useLocalSearchParams<{ mode?: string }>();
  const [mode, setMode] = useState<"sign-in" | "sign-up">(
    params.mode === "sign-up" ? "sign-up" : "sign-in",
  );
  const signIn = useSignIn();
  const signUp = useSignUp();
  const reset = useSendPasswordReset();
  const { control, handleSubmit, getValues, trigger, formState } = useForm<Form>({
    resolver: zodResolver(FormSchema),
    defaultValues: { email: "", password: "" },
  });

  const action = mode === "sign-in" ? signIn : signUp;
  const submit = handleSubmit((form) => {
    action.mutate(form, {
      onSuccess: () => {
        router.replace("/");
      },
    });
  });

  const forgot = (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={t.forgot}
      disabled={reset.isPending}
      onPress={() => {
        const email = getValues("email");
        if (FormSchema.shape.email.safeParse(email).success) {
          reset.mutate(email);
        } else {
          void trigger("email"); // só mostra o erro do e-mail
        }
      }}
      style={{ minHeight: MIN_TOUCH, justifyContent: "center", paddingLeft: 8 }}
    >
      <Text style={[typography.label, { color: palette.lime }]}>{t.forgot}</Text>
    </Pressable>
  );

  return (
    <Screen
      header={
        router.canGoBack() ? (
          <RoundButton
            icon="back"
            label={t.back}
            onPress={() => {
              router.back();
            }}
          />
        ) : undefined
      }
      title={mode === "sign-in" ? t.titleSignIn : t.titleSignUp}
      subtitle={t.subtitle}
      footer={
        <>
          <Button
            label={mode === "sign-in" ? t.submitSignIn : t.submitSignUp}
            loading={action.isPending}
            onPress={() => {
              void submit();
            }}
          />
          <TextLink
            before={mode === "sign-in" ? t.noAccount : t.hasAccount}
            label={mode === "sign-in" ? t.toSignUp : t.toSignIn}
            onPress={() => {
              action.reset();
              setMode(mode === "sign-in" ? "sign-up" : "sign-in");
            }}
          />
        </>
      }
    >
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
            autoComplete="email"
            textContentType="emailAddress"
            error={formState.errors.email?.message}
          />
        )}
      />
      <Controller
        control={control}
        name="password"
        render={({ field }) => (
          <TextField
            label={t.password}
            value={field.value}
            onChangeText={field.onChange}
            onBlur={field.onBlur}
            secureTextEntry
            autoCapitalize="none"
            autoComplete={mode === "sign-in" ? "current-password" : "new-password"}
            textContentType={mode === "sign-in" ? "password" : "newPassword"}
            error={formState.errors.password?.message}
            trailing={mode === "sign-in" ? forgot : undefined}
          />
        )}
      />
      {action.isError ? <Message text={describeError(action.error)} /> : null}
      {reset.isSuccess ? <Message tone="info" text={t.resetSent} /> : null}
      {reset.isError ? <Message text={describeError(reset.error)} /> : null}
    </Screen>
  );
}
