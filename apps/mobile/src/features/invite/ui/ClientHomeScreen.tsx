import type { ReactNode } from "react";
import { ActivityIndicator, Alert, StyleSheet, Text, View } from "react-native";
import { toAppError } from "../../../shared/lib/http";
import { Button } from "../../../shared/ui/Button";
import { errorMessage } from "../../../shared/ui/error-messages";
import { Message } from "../../../shared/ui/Message";
import { Screen } from "../../../shared/ui/Screen";
import { spacing, typography, useColors } from "../../../shared/ui/theme";
import { currentLink, type MyLink } from "../domain/invite";
import { useEndMyLink, useMyLinks } from "../hooks/use-invite";
import { InviteCodeForm } from "./InviteCodeForm";
import { strings } from "./strings";

interface Props {
  /** Rodapé da tela (ex.: sair), até existir a aba de perfil. */
  readonly footer?: ReactNode;
}

/**
 * Casa do aluno nesta fase: o personal atual ou, sem vínculo ativo, o código do convite
 * (SCREEN-FLOWS 1.2: no MVP o aluno não usa o app sem vínculo).
 */
export function ClientHomeScreen({ footer }: Props) {
  const colors = useColors();
  const links = useMyLinks();
  const link = links.data === undefined ? null : currentLink(links.data);

  if (links.isPending) {
    return (
      <Screen title={strings.home.title}>
        <ActivityIndicator accessibilityLabel="Carregando" color={colors.primary} />
      </Screen>
    );
  }
  if (links.isError) {
    return (
      <Screen title={strings.home.title}>
        <Message text={strings.home.error} />
        <Button
          label={strings.home.retry}
          onPress={() => {
            void links.refetch();
          }}
        />
        {footer}
      </Screen>
    );
  }
  if (link?.status === "active") {
    return (
      <Screen
        title={strings.home.title}
        refresh={{
          refreshing: links.isRefetching,
          onRefresh: () => {
            void links.refetch();
          },
        }}
      >
        <LinkCard link={link} />
        <Message tone="info" text={strings.home.trainingSoon} />
        {footer}
      </Screen>
    );
  }
  return (
    <Screen title={strings.code.title} subtitle={strings.code.subtitle}>
      {link === null ? null : <LinkCard link={link} />}
      <InviteCodeForm />
      {footer}
    </Screen>
  );
}

function LinkCard({ link }: { readonly link: MyLink }) {
  const colors = useColors();
  const end = useEndMyLink();
  const t = strings.link;

  return (
    <View style={[styles.card, { borderColor: colors.border, backgroundColor: colors.surface }]}>
      <Text style={[typography.small, { color: colors.textMuted }]}>{t.yourProfessional}</Text>
      <Text style={[typography.label, { color: colors.text }]}>{link.professionalName}</Text>
      <Text style={[typography.body, { color: colors.textMuted }]}>{link.organizationName}</Text>
      {link.startedAt === null ? null : (
        <Text style={[typography.small, { color: colors.textMuted }]}>
          {t.since(link.startedAt.toLocaleDateString("pt-BR"))}
        </Text>
      )}
      {link.status === "inactive" ? <Message tone="info" text={t.paused} /> : null}
      {end.isError ? <Message text={errorMessage(toAppError(end.error))} /> : null}
      <Button
        label={t.end}
        variant="secondary"
        loading={end.isPending}
        onPress={() => {
          Alert.alert(t.confirmTitle, t.confirmMessage(link.professionalName), [
            { text: t.confirmBack, style: "cancel" },
            {
              text: t.end,
              style: "destructive",
              onPress: () => {
                end.mutate(link.linkId);
              },
            },
          ]);
        }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  card: { borderWidth: 1, borderRadius: 12, padding: spacing.md, gap: spacing.sm },
});
