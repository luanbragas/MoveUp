import { router } from "expo-router";
import { useState } from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";
import { toAppError } from "../../../shared/lib/http";
import { BottomSheet } from "../../../shared/ui/BottomSheet";
import { Button } from "../../../shared/ui/Button";
import { EmptyState } from "../../../shared/ui/EmptyState";
import { errorMessage } from "../../../shared/ui/error-messages";
import { Message } from "../../../shared/ui/Message";
import { RoundButton } from "../../../shared/ui/RoundButton";
import { Screen } from "../../../shared/ui/Screen";
import { Segmented } from "../../../shared/ui/Segmented";
import { Skeleton } from "../../../shared/ui/Skeleton";
import { MIN_TOUCH, palette, radius, spacing, typography } from "../../../shared/ui/theme";
import { byUrgency, type Alert, type AlertStatus } from "../domain/alert";
import { useAlerts, useResolveAlert, useSnoozeAlert } from "../hooks/use-alerts";
import { AlertCard } from "./AlertCard";
import { strings } from "./strings";

const t = strings.list;
const SNOOZE_DAYS = [1, 3, 7] as const;

const EMPTY: Record<AlertStatus, { title: string; text: string }> = {
  open: { title: t.emptyOpenTitle, text: t.emptyOpenText },
  snoozed: { title: t.emptySnoozedTitle, text: t.emptySnoozedText },
  resolved: { title: t.emptyResolvedTitle, text: t.emptyResolvedText },
};

/** Aba Atenção do profissional: abertos (urgentes primeiro), adiados e resolvidos. */
export function AlertsScreen() {
  const [status, setStatus] = useState<AlertStatus>("open");
  const alerts = useAlerts(status);
  const resolve = useResolveAlert();
  const snooze = useSnoozeAlert();
  const [snoozing, setSnoozing] = useState<Alert | null>(null);

  const items = alerts.data?.pages.flatMap((p) => p.items) ?? [];
  const openCount = alerts.data?.pages[0]?.openCount ?? 0;
  const shown = status === "open" ? byUrgency(items) : items;
  const urgent = status === "open" ? shown.filter((a) => a.severity === "urgent") : [];
  const rest = status === "open" ? shown.filter((a) => a.severity !== "urgent") : shown;

  const card = (alert: Alert) => (
    <AlertCard
      key={alert.id}
      alert={alert}
      onOpen={
        alert.linkId === null
          ? undefined
          : () => {
              if (alert.linkId !== null) {
                const params = { linkId: alert.linkId, name: alert.clientName ?? "" };
                // liberação médica abre direto a anamnese; o resto, o perfil do aluno
                router.push(
                  alert.type === "clearance_pending"
                    ? { pathname: "/anamnesis/[linkId]", params }
                    : { pathname: "/clients/[linkId]", params },
                );
              }
            }
      }
      onResolve={
        alert.status === "resolved"
          ? undefined
          : () => {
              resolve.mutate({ alertId: alert.id, days: 0 });
            }
      }
      onSnooze={
        alert.status === "open"
          ? () => {
              setSnoozing(alert);
            }
          : undefined
      }
    />
  );

  return (
    <Screen
      title={t.title}
      header={
        <>
          <View style={styles.flex} />
          <RoundButton
            icon="sliders"
            label={t.settings}
            onPress={() => {
              router.push("/alert-settings");
            }}
          />
        </>
      }
      refresh={{
        refreshing: alerts.isRefetching,
        onRefresh: () => {
          void alerts.refetch();
        },
      }}
    >
      <Segmented
        label={t.filter}
        value={status}
        onChange={setStatus}
        options={[
          { value: "open", label: t.open(openCount) },
          { value: "snoozed", label: t.snoozed },
          { value: "resolved", label: t.resolved },
        ]}
      />
      {alerts.isPending ? (
        <>
          <Skeleton width="100%" height={112} rounded={24} />
          <Skeleton width="100%" height={112} rounded={24} />
        </>
      ) : null}
      {alerts.isError ? (
        <>
          <Message text={errorMessage(toAppError(alerts.error))} />
          <Button
            label={t.retry}
            variant="secondary"
            icon="refresh"
            onPress={() => {
              void alerts.refetch();
            }}
          />
        </>
      ) : null}
      {alerts.isSuccess && items.length === 0 ? (
        <EmptyState icon="bell" title={EMPTY[status].title} text={EMPTY[status].text} />
      ) : null}
      {urgent.length > 0 ? (
        <Text accessibilityRole="header" style={[typography.label, styles.urgentLabel]}>
          {t.urgent}
        </Text>
      ) : null}
      {urgent.map(card)}
      {urgent.length > 0 && rest.length > 0 ? (
        <Text accessibilityRole="header" style={[typography.label, styles.restLabel]}>
          {t.rest}
        </Text>
      ) : null}
      {rest.map(card)}
      {alerts.hasNextPage ? (
        <Button
          label={t.more}
          variant="secondary"
          icon={null}
          loading={alerts.isFetchingNextPage}
          onPress={() => {
            void alerts.fetchNextPage();
          }}
        />
      ) : null}

      <BottomSheet
        visible={snoozing !== null}
        title={strings.snooze.title}
        subtitle={strings.snooze.subtitle}
        onClose={() => {
          setSnoozing(null);
        }}
      >
        {SNOOZE_DAYS.map((days) => (
          <Pressable
            key={days}
            accessibilityRole="button"
            accessibilityLabel={strings.snooze.days(days)}
            onPress={() => {
              if (snoozing !== null) {
                snooze.mutate({ alertId: snoozing.id, days });
              }
              setSnoozing(null);
            }}
            style={styles.option}
          >
            <Text style={[typography.label, { color: palette.text, fontSize: 16 }]}>
              {strings.snooze.days(days)}
            </Text>
          </Pressable>
        ))}
      </BottomSheet>
    </Screen>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  urgentLabel: { color: palette.red, marginTop: spacing.xs },
  restLabel: { color: palette.muted, marginTop: spacing.sm },
  option: {
    minHeight: MIN_TOUCH + 8,
    justifyContent: "center",
    paddingHorizontal: spacing.md,
    borderRadius: radius.md,
    backgroundColor: palette.surface2,
  },
});
