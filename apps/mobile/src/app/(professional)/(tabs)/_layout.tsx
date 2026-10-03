import { Tabs } from "expo-router/js-tabs";
import { AutoPush } from "../../../features/execution";
import { PillTabBar, TabBarInsetProvider } from "../../../shared/ui/PillTabBar";
import { navigationStrings } from "../../../shared/ui/navigation-strings";
import { palette } from "../../../shared/ui/theme";

const t = navigationStrings.professional;
const ICONS = {
  dashboard: "chart",
  clients: "users",
  training: "dumbbell",
  alerts: "bell",
  settings: "sliders",
} as const;

// Abas do perfil profissional: barra em pílula flutuante (vidro no iOS).
export default function TabsLayout() {
  return (
    <TabBarInsetProvider>
      <AutoPush />
      <Tabs
        screenOptions={{ headerShown: false, sceneStyle: { backgroundColor: palette.background } }}
        tabBar={(props) => <PillTabBar {...props} icons={ICONS} />}
      >
        <Tabs.Screen name="dashboard" options={{ title: t.dashboard }} />
        <Tabs.Screen name="clients" options={{ title: t.clients }} />
        <Tabs.Screen name="training" options={{ title: t.training }} />
        <Tabs.Screen name="alerts" options={{ title: t.alerts }} />
        <Tabs.Screen name="settings" options={{ title: t.settings }} />
      </Tabs>
    </TabBarInsetProvider>
  );
}
