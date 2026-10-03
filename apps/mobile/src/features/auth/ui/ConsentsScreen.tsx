import { Redirect, router } from "expo-router";
import { useState } from "react";
import { StyleSheet, Text, View } from "react-native";
import { Button } from "../../../shared/ui/Button";
import { Message } from "../../../shared/ui/Message";
import { Screen } from "../../../shared/ui/Screen";
import { Skeleton } from "../../../shared/ui/Skeleton";
import { Steps } from "../../../shared/ui/Steps";
import { Toggle } from "../../../shared/ui/Toggle";
import { palette, radius, spacing, typography } from "../../../shared/ui/theme";
import type { ConsentKind } from "../domain/me";
import { useAuthState } from "../hooks/use-auth-state";
import { useMe } from "../hooks/use-me";
import { useGrantConsents, useLegalVersions } from "../hooks/use-onboarding";
import { describeError } from "./describe-error";
import { strings } from "./strings";

const t = strings.consents;

/**
 * Último passo do cadastro: aceite dos textos que faltam, na versão vigente. Cada aceite é um
 * interruptor que começa desligado (consentimento explícito, LGPD); dados de saúde têm o próprio,
 * separado. Fotos de evolução são opcionais e só aparecem para o aluno.
 */
export function ConsentsScreen() {
  const auth = useAuthState();
  const uid = auth.status === "signed-in" ? auth.user.uid : null;
  const me = useMe(uid);
  const versions = useLegalVersions();
  const grant = useGrantConsents(uid ?? "");
  const [checked, setChecked] = useState<ReadonlySet<ConsentKind>>(new Set());

  if (auth.status === "signed-out") {
    return <Redirect href="/" />;
  }
  if (me.data === undefined || versions.data === undefined) {
    return (
      <Screen title={t.title}>
        {versions.isError ? (
          <Message text={describeError(versions.error)} />
        ) : (
          <>
            <Skeleton height={64} width="100%" />
            <Skeleton height={64} width="100%" />
            <Skeleton height={120} width="100%" />
          </>
        )}
      </Screen>
    );
  }

  const account = me.data;
  const legal = versions.data;
  const required = account.onboarding.missingConsents;
  const optional: readonly ConsentKind[] =
    account.role === "client" && !required.includes("photos") ? ["photos"] : [];
  const firstMissing = required.find((kind) => !checked.has(kind));
  const total = account.role === "professional" ? 3 : 2;

  const set = (kind: ConsentKind, value: boolean) => {
    const next = new Set(checked);
    if (value) {
      next.add(kind);
    } else {
      next.delete(kind);
    }
    setChecked(next);
  };

  const row = (kind: ConsentKind) => (
    <Toggle
      key={kind}
      label={t.rows[kind].label}
      description={t.rows[kind].description}
      value={checked.has(kind)}
      onChange={(value) => {
        set(kind, value);
      }}
    />
  );

  return (
    <Screen
      header={
        <View style={styles.steps}>
          <Steps current={total} total={total} />
        </View>
      }
      title={t.title}
      footer={
        <>
          {firstMissing === undefined ? null : (
            <Text style={[typography.small, styles.missing]}>
              {t.missing(t.rows[firstMissing].label)}
            </Text>
          )}
          <Button
            label={t.submit}
            disabled={firstMissing !== undefined}
            loading={grant.isPending}
            onPress={() => {
              const kinds = [...required, ...optional.filter((kind) => checked.has(kind))];
              grant.mutate(
                { kinds, versions: legal },
                {
                  onSuccess: () => {
                    router.replace(
                      account.role === "professional" && !account.isMinor ? "/ready" : "/",
                    );
                  },
                },
              );
            }}
          />
        </>
      }
    >
      <View style={styles.group}>{required.filter((kind) => kind !== "health_data").map(row)}</View>
      {required.includes("health_data") ? (
        <View style={[styles.group, styles.health]}>{row("health_data")}</View>
      ) : null}
      {optional.length > 0 ? <View style={styles.group}>{optional.map(row)}</View> : null}
      <Text style={[typography.small, { color: palette.muted }]}>{t.draftNotice}</Text>
      {grant.isError ? <Message text={describeError(grant.error)} /> : null}
    </Screen>
  );
}

const styles = StyleSheet.create({
  steps: { flex: 1 },
  group: {
    backgroundColor: palette.surface,
    borderRadius: radius.lg,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.xs,
  },
  health: { borderWidth: 1, borderColor: palette.line },
  missing: { color: palette.muted, textAlign: "center" },
});
