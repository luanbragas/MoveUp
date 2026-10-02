import { useEffect, useState } from "react";
import { ActivityIndicator, StyleSheet, Text, View } from "react-native";
import { toAppError } from "../../../shared/lib/http";
import { Button } from "../../../shared/ui/Button";
import { errorMessage } from "../../../shared/ui/error-messages";
import { Message } from "../../../shared/ui/Message";
import { TextField } from "../../../shared/ui/TextField";
import { spacing, typography, useColors } from "../../../shared/ui/theme";
import { normalizeInviteCode } from "../domain/invite";
import { setPendingInviteCode, usePendingInviteCode } from "../hooks/pending-code";
import { useAcceptInvite, useInvitePreview } from "../hooks/use-invite";
import { strings } from "./strings";

const t = strings.code;
const p = strings.preview;

/** Digitar (ou receber pelo link) o código, ver quem convida e aceitar (SCREEN-FLOWS 1.2). */
export function InviteCodeForm() {
  const colors = useColors();
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

  if (code !== null && preview.isSuccess) {
    const invite = preview.data;
    return (
      <View style={styles.box}>
        <Text accessibilityRole="header" style={[typography.label, { color: colors.text }]}>
          {p.wantsToCoach(invite.professionalName)}
        </Text>
        <Text style={[typography.body, { color: colors.textMuted }]}>
          {p.organization(invite.organizationName)}
        </Text>
        <Text style={[typography.small, { color: colors.textMuted }]}>
          {p.expires(invite.expiresAt.toLocaleDateString("pt-BR"))}
        </Text>
        {accept.isError ? <Message text={errorMessage(toAppError(accept.error))} /> : null}
        <Button
          label={p.accept}
          loading={accept.isPending}
          onPress={() => {
            accept.mutate(code);
          }}
        />
        <Button
          label={p.other}
          variant="secondary"
          onPress={() => {
            accept.reset();
            setCode(null);
            setTyped("");
          }}
        />
      </View>
    );
  }

  return (
    <View style={styles.box}>
      <TextField
        label={t.label}
        value={typed}
        onChangeText={(text) => {
          setTyped(text);
          setInvalid(false);
        }}
        autoCapitalize="characters"
        autoComplete="off"
        error={invalid ? t.invalid : undefined}
      />
      {preview.isError ? <Message text={errorMessage(toAppError(preview.error))} /> : null}
      {code !== null && preview.isPending ? (
        <ActivityIndicator accessibilityLabel="Carregando" color={colors.primary} />
      ) : null}
      <Button
        label={t.submit}
        onPress={() => {
          const normalized = normalizeInviteCode(typed);
          setInvalid(normalized === null);
          setCode(normalized);
          if (normalized !== null && normalized === code) {
            void preview.refetch(); // mesmo código de novo (ex.: depois de erro de rede)
          }
        }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  box: { gap: spacing.md },
});
