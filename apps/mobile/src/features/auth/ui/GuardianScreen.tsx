import { zodResolver } from "@hookform/resolvers/zod";
import { Redirect, router } from "expo-router";
import { useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { Share, StyleSheet, Text, View } from "react-native";
import Svg, { Circle } from "react-native-svg";
import { z } from "zod";
import { Banner } from "../../../shared/ui/Banner";
import { Button } from "../../../shared/ui/Button";
import { Chevrons } from "../../../shared/ui/Chevrons";
import { ChoiceGroup } from "../../../shared/ui/ChoiceGroup";
import { Icon } from "../../../shared/ui/Icon";
import { Message } from "../../../shared/ui/Message";
import { RoundButton } from "../../../shared/ui/RoundButton";
import { Screen } from "../../../shared/ui/Screen";
import { Skeleton } from "../../../shared/ui/Skeleton";
import { TextField } from "../../../shared/ui/TextField";
import { TextLink } from "../../../shared/ui/TextLink";
import { Title } from "../../../shared/ui/Title";
import { palette, spacing, typography } from "../../../shared/ui/theme";
import type { GuardianRequest } from "../domain/me";
import type { GuardianLink, GuardianRelationship } from "../domain/ports";
import { useAuthState } from "../hooks/use-auth-state";
import { useEntry } from "../hooks/use-entry";
import {
  useCancelGuardianRequest,
  useLegalVersions,
  useRequestGuardian,
  useResendGuardianLink,
} from "../hooks/use-onboarding";
import { describeError } from "./describe-error";
import { strings } from "./strings";

const t = strings.guardian;

const FormSchema = z.object({
  guardianName: z.string().trim().min(2, { error: t.nameRequired }),
  relationship: z.enum(["mother", "father", "legal_guardian", "other"]).nullable(),
});
type Form = z.infer<typeof FormSchema>;

const RELATIONSHIPS: readonly { value: GuardianRelationship; label: string }[] = (
  Object.keys(t.relationships) as GuardianRelationship[]
).map((value) => ({ value, label: t.relationships[value] }));

/**
 * Abre o compartilhamento do sistema (WhatsApp, SMS...) com o link para o responsável. Se a pessoa
 * fechar sem mandar, a tela de espera tem "mandar o link de novo".
 */
function shareLink(guardianName: string, link: GuardianLink): void {
  Share.share({ message: t.shareMessage(firstName(guardianName), link.url) }).catch(
    () => undefined,
  );
}

function firstName(name: string): string {
  return name.trim().split(/\s+/)[0] ?? name;
}

function BackButton() {
  if (!router.canGoBack()) {
    return null;
  }
  return (
    <RoundButton
      icon="back"
      label={t.back}
      onPress={() => {
        router.back();
      }}
    />
  );
}

/**
 * Autorização do responsável pelo aluno menor (LGPD, art. 14). O menor só indica quem é; quem
 * autoriza é o responsável, pelo link que recebe no celular dele. Esta tela libera sozinha quando a
 * autorização chega (o hook da conta confere a cada 10 s enquanto o pedido está aberto).
 */
export function GuardianScreen() {
  const auth = useAuthState();
  const uid = auth.status === "signed-in" ? auth.user.uid : "";
  const entry = useEntry();

  if (auth.status === "signed-out") {
    return <Redirect href="/" />;
  }
  if (entry.destination !== "guardian" && entry.destination !== "loading") {
    return <Redirect href="/" />; // autorizado (ou não precisa): segue o fluxo de entrada
  }
  const request = entry.me?.onboarding.guardianRequest ?? null;
  if (entry.me === null) {
    return (
      <Screen>
        <Skeleton height={44} width="70%" />
        <Skeleton height={68} width="100%" />
      </Screen>
    );
  }
  return request?.status === "pending" ? (
    <WaitingForGuardian uid={uid} request={request} />
  ) : (
    <GuardianRequestForm uid={uid} declined={request} />
  );
}

function GuardianRequestForm({
  uid,
  declined,
}: {
  readonly uid: string;
  readonly declined: GuardianRequest | null;
}) {
  const versions = useLegalVersions();
  const requestGuardian = useRequestGuardian(uid);
  const { control, handleSubmit, formState } = useForm<Form>({
    resolver: zodResolver(FormSchema),
    defaultValues: { guardianName: "", relationship: null },
  });

  const submit = handleSubmit((form) => {
    if (form.relationship === null || versions.data === undefined) {
      return;
    }
    requestGuardian.mutate(
      {
        input: { guardianName: form.guardianName, relationship: form.relationship },
        versions: versions.data,
      },
      {
        onSuccess: (link) => {
          shareLink(form.guardianName, link);
        },
      },
    );
  });

  return (
    <Screen
      header={<BackButton />}
      title={t.title}
      subtitle={t.subtitle}
      footer={
        <Button
          label={t.submit}
          loading={requestGuardian.isPending}
          disabled={versions.data === undefined}
          onPress={() => {
            void submit();
          }}
        />
      }
    >
      {declined === null ? null : (
        <Banner icon="alert" text={t.declinedBanner(firstName(declined.guardianName))} />
      )}
      <Controller
        control={control}
        name="guardianName"
        render={({ field }) => (
          <TextField
            label={t.name}
            value={field.value}
            onChangeText={field.onChange}
            onBlur={field.onBlur}
            autoComplete="off"
            autoCapitalize="words"
            error={formState.errors.guardianName?.message}
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
      <View style={styles.how}>
        <Icon name="chat" size={22} color={palette.lime} strokeWidth={2.2} />
        <Text style={[typography.small, styles.howText]}>{t.howItWorks}</Text>
      </View>
      {versions.isError ? <Message text={describeError(versions.error)} /> : null}
      {requestGuardian.isError ? <Message text={describeError(requestGuardian.error)} /> : null}
    </Screen>
  );
}

const timeFormat = new Intl.DateTimeFormat("pt-BR", { hour: "2-digit", minute: "2-digit" });
const dayFormat = new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit" });

function WaitingForGuardian({
  uid,
  request,
}: {
  readonly uid: string;
  readonly request: GuardianRequest;
}) {
  const resend = useResendGuardianLink(uid);
  const cancel = useCancelGuardianRequest(uid);
  const name = firstName(request.guardianName);
  // hora de abertura da tela: o hook da conta recarrega a cada 10 s e traz a validade nova
  const [openedAt] = useState(() => Date.now());
  const expired = request.linkExpiresAt !== null && request.linkExpiresAt.getTime() <= openedAt;
  const error = resend.error ?? cancel.error;

  return (
    <Screen
      header={<BackButton />}
      footer={
        <>
          <Button
            label={t.resend}
            variant={expired ? "primary" : "secondary"}
            icon="chat"
            loading={resend.isPending}
            onPress={() => {
              resend.mutate(undefined, {
                onSuccess: (link) => {
                  shareLink(request.guardianName, link);
                },
              });
            }}
          />
          <TextLink
            before={t.wrongPerson}
            label={t.changeGuardian}
            onPress={() => {
              cancel.mutate();
            }}
          />
        </>
      }
    >
      <View style={styles.hero}>
        <View style={styles.ring} accessibilityElementsHidden importantForAccessibility="no">
          <Svg width={150} height={150} viewBox="0 0 150 150">
            <Circle cx={75} cy={75} r={66} fill="none" stroke={palette.line} strokeWidth={8} />
            <Circle
              cx={75}
              cy={75}
              r={66}
              fill="none"
              stroke={palette.lime}
              strokeWidth={8}
              strokeLinecap="round"
              strokeDasharray="120 415"
              transform="rotate(-90 75 75)"
            />
          </Svg>
          <View style={styles.ringCenter}>
            <Chevrons size={64} count={3} />
          </View>
        </View>
        <Title size={46}>{t.waitingTitle(name)}</Title>
        <Text style={[typography.body, { color: palette.textSoft }]}>{t.waitingLead(name)}</Text>
        <Text style={[typography.small, { color: palette.muted }]}>
          {expired
            ? t.expired
            : t.waitingSince(
                timeFormat.format(request.requestedAt),
                request.linkExpiresAt === null ? "-" : dayFormat.format(request.linkExpiresAt),
              )}
        </Text>
      </View>
      {error === null ? null : <Message text={describeError(error)} />}
    </Screen>
  );
}

const styles = StyleSheet.create({
  how: { flexDirection: "row", alignItems: "center", gap: spacing.sm + 4 },
  howText: { flex: 1, color: palette.textSoft, fontSize: 14 },
  hero: { gap: spacing.md + 2, marginTop: spacing.lg },
  ring: { width: 150, height: 150 },
  ringCenter: {
    position: "absolute",
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    alignItems: "center",
    justifyContent: "center",
  },
});
