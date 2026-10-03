import { Tabs } from "expo-router/js-tabs";
import { AutoSync } from "../../../features/sync";
import { PillTabBar, TabBarInsetProvider } from "../../../shared/ui/PillTabBar";
import { navigationStrings } from "../../../shared/ui/navigation-strings";
import { palette } from "../../../shared/ui/theme";

const t = navigationStrings.client;
const ICONS = {
  home: "home",
  training: "dumbbell",
  history: "clock",
  progress: "trend",
  profile: "user",
} as const;

// Abas do perfil aluno: barra em pílula flutuante (vidro no iOS).
export default function TabsLayout() {
  return (
    <TabBarInsetProvider>
      <AutoSync />
      <Tabs
        screenOptions={{ headerShown: false, sceneStyle: { backgroundColor: palette.background } }}
        tabBar={(props) => <PillTabBar {...props} icons={ICONS} />}
      >
        <Tabs.Screen name="home" options={{ title: t.home }} />
        <Tabs.Screen name="training" options={{ title: t.training }} />
        <Tabs.Screen name="history" options={{ title: t.history }} />
        <Tabs.Screen name="progress" options={{ title: t.progress }} />
        <Tabs.Screen name="profile" options={{ title: t.profile }} />
      </Tabs>
    </TabBarInsetProvider>
  );
}
