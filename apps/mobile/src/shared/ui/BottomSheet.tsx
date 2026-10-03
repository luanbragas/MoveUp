import { useState, type ReactNode } from "react";
import {
  KeyboardAvoidingView,
  Modal,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { RoundButton } from "./RoundButton";
import { palette, radius, spacing, typography } from "./theme";

interface Props {
  readonly visible: boolean;
  readonly title: string;
  readonly subtitle?: string;
  readonly onClose: () => void;
  /** Ação fixa embaixo (fora da rolagem e acima do teclado). */
  readonly footer?: ReactNode;
  readonly children?: ReactNode;
}

const CLOSE = "Fechar";
/** Arrastar a alça para baixo além disso fecha a folha. */
const DISMISS_DISTANCE = 80;

/**
 * Folha que sobe de baixo sobre a tela atual (editar séries, escolher método, como fazer). Fecha
 * no X, tocando fora, arrastando a alça para baixo ou no voltar do Android.
 */
export function BottomSheet({ visible, title, subtitle, onClose, footer, children }: Props) {
  // arrastar a alça para baixo fecha: eventos de toque da própria view (sem gesto global)
  const [touchY, setTouchY] = useState<number | null>(null);

  return (
    <Modal
      visible={visible}
      animationType="slide"
      transparent
      statusBarTranslucent
      onRequestClose={onClose}
    >
      <KeyboardAvoidingView
        style={styles.backdrop}
        behavior={Platform.OS === "ios" ? "padding" : undefined}
      >
        <Pressable
          accessibilityElementsHidden
          importantForAccessibility="no"
          style={StyleSheet.absoluteFill}
          onPress={onClose}
        />
        <SafeAreaView edges={["bottom"]} style={styles.sheet}>
          <View
            onTouchStart={(e) => {
              setTouchY(e.nativeEvent.pageY);
            }}
            onTouchEnd={(e) => {
              if (touchY !== null && e.nativeEvent.pageY - touchY > DISMISS_DISTANCE) {
                onClose();
              }
              setTouchY(null);
            }}
            style={styles.grab}
          >
            <View style={styles.handle} />
            <View style={styles.head}>
              <View style={{ flex: 1, gap: 2 }}>
                <Text accessibilityRole="header" style={[typography.headline, styles.title]}>
                  {title}
                </Text>
                {subtitle === undefined ? null : (
                  <Text style={[typography.small, { color: palette.muted }]}>{subtitle}</Text>
                )}
              </View>
              <RoundButton icon="close" label={CLOSE} onPress={onClose} />
            </View>
          </View>
          <ScrollView
            keyboardShouldPersistTaps="handled"
            contentContainerStyle={styles.content}
            style={styles.scroll}
          >
            {children}
          </ScrollView>
          {footer === undefined ? null : <View style={styles.footer}>{footer}</View>}
        </SafeAreaView>
      </KeyboardAvoidingView>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: { flex: 1, backgroundColor: "rgba(0,0,0,0.6)", justifyContent: "flex-end" },
  sheet: {
    backgroundColor: palette.surface,
    borderTopLeftRadius: radius.xl,
    borderTopRightRadius: radius.xl,
    paddingHorizontal: spacing.md + 4,
    maxHeight: "92%",
  },
  grab: { paddingTop: spacing.sm, gap: spacing.sm },
  handle: {
    alignSelf: "center",
    width: 40,
    height: 4,
    borderRadius: 2,
    backgroundColor: palette.line,
  },
  head: { flexDirection: "row", alignItems: "center", gap: spacing.sm },
  title: { color: palette.text },
  scroll: { flexGrow: 0 },
  content: { gap: spacing.sm, paddingTop: spacing.sm, paddingBottom: spacing.lg },
  footer: { paddingBottom: spacing.md, gap: spacing.sm },
});
