import { router } from "expo-router";
import { useState } from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";
import { toAppError } from "../../../shared/lib/http";
import { Button } from "../../../shared/ui/Button";
import { errorMessage } from "../../../shared/ui/error-messages";
import { Icon } from "../../../shared/ui/Icon";
import { Message } from "../../../shared/ui/Message";
import { RoundButton } from "../../../shared/ui/RoundButton";
import { Screen } from "../../../shared/ui/Screen";
import { Skeleton } from "../../../shared/ui/Skeleton";
import { Toggle } from "../../../shared/ui/Toggle";
import { fonts, MIN_TOUCH, palette, radius, spacing, typography } from "../../../shared/ui/theme";
import { clampThreshold, THRESHOLD_RANGE, type AlertSetting } from "../domain/alert";
import { useAlertSettings, useUpdateAlertSettings } from "../hooks/use-alerts";
import { strings } from "./strings";

const t = strings.settings;

/** Passo do limite: % de 5 em 5; dias e esforço de 1 em 1. */
function step(setting: AlertSetting): number {
  return setting.type === "low_adherence" ? 5 : 1;
}

/** Rota /alert-settings: ligar, push e limite de cada tipo de alerta. */
export function AlertSettingsScreen() {
  const settings = useAlertSettings();
  const back = (
    <RoundButton
      icon="back"
      label={t.back}
      onPress={() => {
        router.back();
      }}
    />
  );
  if (settings.data === undefined) {
    return (
      <Screen header={back} title={t.title}>
        {settings.isError ? (
          <Message text={errorMessage(toAppError(settings.error))} />
        ) : (
          <Skeleton width="100%" height={200} rounded={24} />
        )}
      </Screen>
    );
  }
  return <SettingsEditor initial={settings.data} header={back} />;
}

function SettingsEditor({
  initial,
  header,
}: {
  readonly initial: readonly AlertSetting[];
  readonly header: React.ReactNode;
}) {
  const [draft, setDraft] = useState(initial);
  const [dirty, setDirty] = useState(false);
  const update = useUpdateAlertSettings();

  const change = (type: AlertSetting["type"], patch: Partial<AlertSetting>) => {
    setDraft((current) => current.map((s) => (s.type === type ? { ...s, ...patch } : s)));
    setDirty(true);
  };

  return (
    <Screen
      header={header}
      title={t.title}
      subtitle={t.subtitle}
      footer={
        <Button
          label={t.save}
          icon="check"
          disabled={!dirty}
          loading={update.isPending}
          onPress={() => {
            update.mutate(draft, {
              onSuccess: () => {
                setDirty(false);
              },
            });
          }}
        />
      }
    >
      {update.isSuccess && !dirty ? <Message tone="info" text={t.saved} /> : null}
      {update.isError ? <Message text={errorMessage(toAppError(update.error))} /> : null}
      <Text style={[typography.small, { color: palette.muted }]}>{t.pushHint}</Text>
      {draft.map((setting) => {
        const label = t.types[setting.type].label;
        const unit = t.types[setting.type].unit;
        const range = THRESHOLD_RANGE[setting.type];
        return (
          <View key={setting.type} style={styles.card}>
            <Text accessibilityRole="header" style={[typography.headline, styles.title]}>
              {label}
            </Text>
            <Toggle
              label={t.enabled}
              value={setting.enabled}
              onChange={(enabled) => {
                change(setting.type, { enabled });
              }}
            />
            {setting.enabled ? (
              <Toggle
                label={t.push}
                value={setting.push}
                onChange={(push) => {
                  change(setting.type, { push });
                }}
              />
            ) : null}
            {setting.enabled &&
            unit !== null &&
            range !== undefined &&
            setting.threshold !== null ? (
              <View style={styles.stepper}>
                <Text style={[typography.small, { color: palette.textSoft, flex: 1 }]}>{unit}</Text>
                <Pressable
                  accessibilityRole="button"
                  accessibilityLabel={`${t.less}: ${label}`}
                  disabled={setting.threshold <= range[0]}
                  onPress={() => {
                    if (setting.threshold !== null) {
                      change(setting.type, {
                        threshold: clampThreshold(setting.type, setting.threshold - step(setting)),
                      });
                    }
                  }}
                  style={styles.stepButton}
                >
                  <Icon name="down" size={18} color={palette.text} />
                </Pressable>
                <Text
                  accessibilityLabel={`${unit}: ${String(setting.threshold)}`}
                  style={styles.value}
                >
                  {setting.threshold}
                </Text>
                <Pressable
                  accessibilityRole="button"
                  accessibilityLabel={`${t.more}: ${label}`}
                  disabled={setting.threshold >= range[1]}
                  onPress={() => {
                    if (setting.threshold !== null) {
                      change(setting.type, {
                        threshold: clampThreshold(setting.type, setting.threshold + step(setting)),
                      });
                    }
                  }}
                  style={styles.stepButton}
                >
                  <Icon name="up" size={18} color={palette.text} />
                </Pressable>
              </View>
            ) : null}
          </View>
        );
      })}
    </Screen>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: palette.surface,
    borderRadius: radius.lg,
    padding: spacing.md,
    gap: spacing.xs,
  },
  title: { color: palette.text, marginBottom: spacing.xs },
  stepper: { flexDirection: "row", alignItems: "center", gap: spacing.sm },
  stepButton: {
    width: MIN_TOUCH,
    height: MIN_TOUCH,
    borderRadius: MIN_TOUCH / 2,
    backgroundColor: palette.surface2,
    alignItems: "center",
    justifyContent: "center",
  },
  value: {
    fontFamily: fonts.number,
    fontSize: 24,
    color: palette.text,
    minWidth: 44,
    textAlign: "center",
  },
});
