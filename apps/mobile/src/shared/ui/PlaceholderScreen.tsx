import { StyleSheet, Text, View, useColorScheme } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

interface Props {
  readonly title: string;
  readonly description: string;
}

/** Tela provisória das rotas que ainda não têm feature (Fase 0). */
export function PlaceholderScreen({ title, description }: Props) {
  const dark = useColorScheme() === "dark";
  return (
    <SafeAreaView style={[styles.safe, dark ? styles.bgDark : styles.bgLight]}>
      <View style={styles.content}>
        <Text accessibilityRole="header" style={[styles.title, dark && styles.textDark]}>
          {title}
        </Text>
        <Text style={[styles.description, dark && styles.mutedDark]}>{description}</Text>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1 },
  bgLight: { backgroundColor: "#FFFFFF" },
  bgDark: { backgroundColor: "#0B0B0C" },
  content: { flex: 1, justifyContent: "center", paddingHorizontal: 24, gap: 12 },
  title: { fontSize: 28, fontWeight: "700", color: "#111114" },
  description: { fontSize: 17, lineHeight: 24, color: "#55555C" },
  textDark: { color: "#F5F5F7" },
  mutedDark: { color: "#A1A1A8" },
});
