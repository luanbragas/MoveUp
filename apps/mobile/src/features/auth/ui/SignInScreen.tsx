import { zodResolver } from "@hookform/resolvers/zod";
import { router } from "expo-router";
import { useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { z } from "zod";
import { Button } from "../../../shared/ui/Button";
import { Message } from "../../../shared/ui/Message";
import { Screen } from "../../../shared/ui/Screen";
import { TextField } from "../../../shared/ui/TextField";
import { useSendPasswordReset, useSignIn, useSignUp } from "../hooks/use-auth-actions";
import { describeError } from "./describe-error";
import { strings } from "./strings";

const t = strings.signIn;

const FormSchema = z.object({
  email: z.email({ error: t.emailInvalid }),
  password: z.string().min(8, { error: t.passwordShort }),
});
type Form = z.infer<typeof FormSchema>;

/** Login e criação de conta por e-mail e senha (Firebase). */
export function SignInScreen() {
  const [mode, setMode] = useState<"sign-in" | "sign-up">("sign-in");
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

  return (
    <Screen title={mode === "sign-in" ? t.titleSignIn : t.titleSignUp} subtitle={t.subtitle}>
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
          />
        )}
      />
      {action.isError ? <Message text={describeError(action.error)} /> : null}
      {reset.isSuccess ? <Message tone="info" text={t.resetSent} /> : null}
      <Button
        label={mode === "sign-in" ? t.submitSignIn : t.submitSignUp}
        loading={action.isPending}
        onPress={() => {
          void submit();
        }}
      />
      <Button
        label={mode === "sign-in" ? t.toggleToSignUp : t.toggleToSignIn}
        variant="secondary"
        onPress={() => {
          action.reset();
          setMode(mode === "sign-in" ? "sign-up" : "sign-in");
        }}
      />
      {mode === "sign-in" ? (
        <Button
          label={t.forgot}
          variant="secondary"
          loading={reset.isPending}
          onPress={() => {
            const email = getValues("email");
            if (FormSchema.shape.email.safeParse(email).success) {
              reset.mutate(email);
            } else {
              void trigger("email"); // só mostra o erro do e-mail
            }
          }}
        />
      ) : null}
    </Screen>
  );
}
