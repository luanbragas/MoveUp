import { Redirect, router } from "expo-router";
import { useState } from "react";
import { ActivityIndicator } from "react-native";
import { Button } from "../../../shared/ui/Button";
import { Checkbox } from "../../../shared/ui/Checkbox";
import { Message } from "../../../shared/ui/Message";
import { Screen } from "../../../shared/ui/Screen";
import type { ConsentKind } from "../domain/me";
import { useAuthState } from "../hooks/use-auth-state";
import { useMe } from "../hooks/use-me";
import { useGrantConsents, useLegalVersions } from "../hooks/use-onboarding";
import { describeError } from "./describe-error";
import { strings } from "./strings";

const t = strings.consents;

/** Aceite dos textos obrigatórios que faltam (versão vigente), um por um. */
export function ConsentsScreen() {
  const auth = useAuthState();
  const uid = auth.status === "signed-in" ? auth.user.uid : null;
  const me = useMe(uid);
  const versions = useLegalVersions();
  const grant = useGrantConsents(uid ?? "");
  const [checked, setChecked] = useState<ReadonlySet<ConsentKind>>(new Set());
  const [incomplete, setIncomplete] = useState(false);

  if (auth.status === "signed-out") {
    return <Redirect href="/" />;
  }
  if (me.data === undefined || versions.data === undefined) {
    return (
      <Screen title={t.title}>
        {versions.isError ? (
          <Message text={describeError(versions.error)} />
        ) : (
          <ActivityIndicator accessibilityLabel="Carregando" />
        )}
      </Screen>
    );
  }

  const missing = me.data.onboarding.missingConsents;
  const legal = versions.data;

  return (
    <Screen title={t.title} subtitle={t.subtitle}>
      <Message tone="info" text={t.draftNotice} />
      {missing.map((kind) => (
        <Checkbox
          key={kind}
          label={t.labels[kind]}
          checked={checked.has(kind)}
          onChange={(value) => {
            const next = new Set(checked);
            if (value) {
              next.add(kind);
            } else {
              next.delete(kind);
            }
            setChecked(next);
            setIncomplete(false);
          }}
        />
      ))}
      {incomplete ? <Message text={t.mustAcceptAll} /> : null}
      {grant.isError ? <Message text={describeError(grant.error)} /> : null}
      <Button
        label={t.submit}
        loading={grant.isPending}
        onPress={() => {
          if (!missing.every((kind) => checked.has(kind))) {
            setIncomplete(true);
            return;
          }
          grant.mutate(
            { kinds: missing, versions: legal },
            {
              onSuccess: () => {
                router.replace("/");
              },
            },
          );
        }}
      />
    </Screen>
  );
}
