import { useEffect, useState } from "react";
import { Alert, StyleSheet, Text, View } from "react-native";
import { toAppError } from "../../../shared/lib/http";
import { Button } from "../../../shared/ui/Button";
import { CodeInput } from "../../../shared/ui/CodeInput";
import { errorMessage } from "../../../shared/ui/error-messages";
import { Icon } from "../../../shared/ui/Icon";
import { Message } from "../../../shared/ui/Message";
import { RoundButton } from "../../../shared/ui/RoundButton";
import { Screen } from "../../../shared/ui/Screen";
import { TextLink } from "../../../shared/ui/TextLink";
import { Title } from "../../../shared/ui/Title";
import { fonts, palette, radius, spacing, typography } from "../../../shared/ui/theme";
import { normalizeInviteCode, type InvitePreview } from "../domain/invite";
import { setPendingInviteCode, usePendingInviteCode } from "../hooks/pending-code";
import { useAcceptInvite, useInvitePreview } from "../hooks/use-invite";
import { strings } from "./strings";

const dayFormat = new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit" });

function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  return (
    (parts[0]?.[0] ?? "") + (parts.length > 1 ? (parts.at(-1)?.[0] ?? "") : "")
  ).toUpperCase();
}

interface Props {
  /** Convite aceito: a casa do aluno mostra as boas-vindas ao time. */
  readonly onAccepted: (professionalName: string) => void;
}

/**
 * Sem vínculo ativo, o aluno entra pelo convite (SCREEN-FLOWS 1.2): código (digitado ou vindo do
 * link), prévia de quem convida e o que essa pessoa vai poder ver, e o aceite. Convite vencido ou
 * usado tem tela própria.
 */
export function InviteFlow({ onAccepted }: Props) {
  const pending = usePendingInviteCode();
  const [typed, setTyped] = useState(pending ?? "");
  const [code, setCode] = useState(pending === null ? null : normalizeInviteCode(pending));
  const [invalid, setInvalid] = useState(false);
  const preview = useInvitePreview(code);
  const accept = useAcceptInvite();

  // o código do link já foi para o formulário: não reaproveitar depois (convite é de uso único)
  useEffect(() => {
    if (pending !== null) {
      setPendingInviteCode(null);
    }
  }, [pending]);

  const restart = () => {
    accept.reset();
    setCode(null);
    setTyped("");
  };

  if (code !== null && preview.isSuccess) {
    return (
      <PreviewStep
        invite={preview.data}
        accepting={accept.isPending}
        error={accept.isError ? errorMessage(toAppError(accept.error)) : null}
        onAccept={() => {
          accept.mutate(code, {
            onSuccess: () => {
              onAccepted(preview.data.professionalName);
            },
          });
        }}
        onBack={restart}
      />
    );
  }

  const failure = preview.isError ? toAppError(preview.error) : null;
  if (code !== null && failure?.kind === "problem" && failure.code === "invite-expired") {
    return <ExpiredStep code={code} onRetry={restart} />;
  }

  const t = strings.code;
  return (
    <Screen
      title={t.title}
      subtitle={t.subtitle}
      footer={
        <>
          <Button
            label={t.submit}
            loading={code !== null && preview.isFetching}
            disabled={typed.length < 8}
            onPress={() => {
              const normalized = normalizeInviteCode(typed);
              setInvalid(normalized === null);
              setCode(normalized);
              if (normalized !== null && normalized === code) {
                void preview.refetch(); // mesmo código de novo (ex.: depois de erro de rede)
              }
            }}
          />
          <TextLink
            before={t.noCode}
            label={t.askLink}
            onPress={() => {
              Alert.alert(t.askLink, t.askLinkHelp);
            }}
          />
        </>
      }
    >
      <CodeInput
        label={t.label}
        value={typed}
        length={8}
        onChangeText={(text) => {
          setTyped(text);
          setInvalid(false);
        }}
        error={invalid ? t.invalid : undefined}
      />
      <View style={styles.hint}>
        <Icon name="share" size={18} color={palette.muted} />
        <Text style={[typography.small, { color: palette.muted }]}>{t.fromLink}</Text>
      </View>
      {failure === null ? null : <Message text={errorMessage(failure)} />}
    </Screen>
  );
}

function PreviewStep({
  invite,
  accepting,
  error,
  onAccept,
  onBack,
}: {
  readonly invite: InvitePreview;
  readonly accepting: boolean;
  readonly error: string | null;
  readonly onAccept: () => void;
  readonly onBack: () => void;
}) {
  const t = strings.preview;
  return (
    <Screen
      header={<RoundButton icon="back" label={t.back} onPress={onBack} />}
      footer={
        <>
          <Button label={t.accept} icon="check" loading={accepting} onPress={onAccept} />
          <TextLink label={t.other} onPress={onBack} />
        </>
      }
    >
      <View style={styles.previewHead}>
        <View style={styles.avatar}>
          <Text style={styles.avatarText}>{initials(invite.professionalName)}</Text>
        </View>
        <Title size={40} accent={t.wantsToCoach.toUpperCase()}>
          {invite.professionalName}
        </Title>
        <Text style={[typography.small, { color: palette.muted }]}>
          {t.organization(invite.organizationName, dayFormat.format(invite.expiresAt))}
        </Text>
      </View>
      <View style={styles.card} accessibilityLabel={t.can} accessible>
        {t.abilities.map((ability) => (
          <View key={ability} style={styles.ability}>
            <Icon name="check" size={18} color={palette.lime} strokeWidth={2.8} />
            <Text style={[typography.body, { color: palette.textSoft, flex: 1 }]}>{ability}</Text>
          </View>
        ))}
      </View>
      {error === null ? null : <Message text={error} />}
    </Screen>
  );
}

function ExpiredStep({ code, onRetry }: { readonly code: string; readonly onRetry: () => void }) {
  const t = strings.expired;
  return (
    <Screen footer={<Button label={t.other} variant="secondary" icon={null} onPress={onRetry} />}>
      <View style={styles.expiredCode} accessibilityElementsHidden importantForAccessibility="no">
        <Text style={styles.strike}>{`${code.slice(0, 4)} ${code.slice(4)}`}</Text>
      </View>
      <Title size={42}>{t.title}</Title>
      <Text style={[typography.body, { color: palette.textSoft }]}>{t.text}</Text>
    </Screen>
  );
}

const styles = StyleSheet.create({
  hint: { flexDirection: "row", alignItems: "center", gap: spacing.sm },
  previewHead: { gap: spacing.sm + 4, marginTop: spacing.md },
  avatar: {
    width: 72,
    height: 72,
    borderRadius: 36,
    backgroundColor: palette.limeDark,
    alignItems: "center",
    justifyContent: "center",
  },
  avatarText: { fontFamily: fonts.number, fontSize: 26, color: palette.lime },
  card: {
    backgroundColor: palette.surface,
    borderRadius: radius.lg,
    padding: spacing.md,
    gap: spacing.sm + 4,
  },
  ability: { flexDirection: "row", alignItems: "center", gap: spacing.sm + 4 },
  expiredCode: {
    alignSelf: "flex-start",
    backgroundColor: palette.surface,
    borderRadius: radius.md,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    marginTop: spacing.lg,
  },
  strike: {
    fontFamily: fonts.number,
    fontSize: 26,
    letterSpacing: 4,
    color: palette.muted,
    textDecorationLine: "line-through",
  },
});
