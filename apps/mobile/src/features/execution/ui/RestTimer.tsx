import { useEffect, useState } from "react";
import { Modal, Pressable, StyleSheet, Text, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { Button } from "../../../shared/ui/Button";
import { fonts, palette, spacing, typography } from "../../../shared/ui/theme";
import { strings } from "./strings";

const t = strings.rest;

function clock(seconds: number): string {
  const s = Math.max(0, seconds);
  return `${String(Math.floor(s / 60))}:${String(s % 60).padStart(2, "0")}`;
}

interface Props {
  /** Fim do descanso (ms): o relógio é por horário, então voltar ao app mostra o certo. */
  readonly endsAt: number;
  readonly nextLabel: string | null;
  readonly onDone: () => void;
  readonly onExtend: (seconds: number) => void;
}

/** Descanso em tela cheia lima (design 3.4): número grande, pular ou +30 s. */
export function RestTimer({ endsAt, nextLabel, onDone, onExtend }: Props) {
  const [now, setNow] = useState(() => Date.now());
  const left = Math.ceil((endsAt - now) / 1000);

  useEffect(() => {
    const timer = setInterval(() => {
      setNow(Date.now());
    }, 250);
    return () => {
      clearInterval(timer);
    };
  }, []);

  useEffect(() => {
    if (left <= 0) {
      onDone();
    }
  }, [left, onDone]);

  return (
    <Modal animationType="fade" statusBarTranslucent onRequestClose={onDone}>
      <SafeAreaView style={styles.screen}>
        <Text style={[typography.label, styles.ink]}>{t.title}</Text>
        <Text
          accessibilityRole="timer"
          accessibilityLabel={`${t.title}: ${String(Math.max(0, left))} segundos`}
          style={styles.clock}
        >
          {clock(left)}
        </Text>
        {nextLabel === null ? null : (
          <Text style={[typography.body, styles.ink]}>{t.next(nextLabel)}</Text>
        )}
        <View style={styles.actions}>
          <Pressable
            accessibilityRole="button"
            accessibilityLabel={t.more}
            onPress={() => {
              onExtend(30);
            }}
            style={styles.more}
          >
            <Text style={[typography.button, styles.ink]}>{t.more}</Text>
          </Pressable>
          <View style={{ flex: 1 }}>
            <Button label={t.skip} onLime icon="arrow" onPress={onDone} />
          </View>
        </View>
      </SafeAreaView>
    </Modal>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: palette.lime,
    alignItems: "center",
    justifyContent: "center",
    gap: spacing.md,
    padding: spacing.lg,
  },
  ink: { color: palette.onLime },
  clock: { fontFamily: fonts.number, fontSize: 120, lineHeight: 124, color: palette.onLime },
  actions: {
    position: "absolute",
    left: spacing.lg,
    right: spacing.lg,
    bottom: spacing.xl,
    flexDirection: "row",
    gap: spacing.sm,
    alignItems: "center",
  },
  more: {
    height: 64,
    paddingHorizontal: spacing.lg,
    borderRadius: 32,
    borderWidth: 2,
    borderColor: palette.onLime,
    alignItems: "center",
    justifyContent: "center",
  },
});
