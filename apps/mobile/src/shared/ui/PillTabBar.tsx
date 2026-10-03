import { GlassView, isLiquidGlassAvailable } from "expo-glass-effect";
import type { Tabs } from "expo-router/js-tabs";
import { createContext, useContext, type ComponentProps, type ReactNode } from "react";
import { Platform, Pressable, StyleSheet, Text, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { Icon, type IconName } from "./Icon";
import { MIN_TOUCH, palette, typography } from "./theme";

type TabBarProps = Parameters<NonNullable<ComponentProps<typeof Tabs>["tabBar"]>>[0];

const BAR_HEIGHT = 68;
const BAR_GAP = 12;

/** Espaço que a barra flutuante ocupa embaixo; o Screen soma no fim da rolagem. */
const TabBarInsetContext = createContext(0);

export function useTabBarInset(): number {
  return useContext(TabBarInsetContext);
}

/** Envolve o navegador de abas: as telas de dentro sabem quanto deixar livre embaixo. */
export function TabBarInsetProvider({ children }: { readonly children: ReactNode }) {
  const insets = useSafeAreaInsets();
  return (
    <TabBarInsetContext.Provider value={BAR_HEIGHT + BAR_GAP + Math.max(insets.bottom, BAR_GAP)}>
      {children}
    </TabBarInsetContext.Provider>
  );
}

interface Props extends TabBarProps {
  /** Ícone de cada aba, pelo nome da rota. */
  readonly icons: Readonly<Record<string, IconName>>;
}

/**
 * Barra de abas em pílula flutuante. A aba ativa vira cápsula lima com o rótulo; as outras ficam só
 * com o ícone (o nome vai para o leitor de tela). No iOS com Liquid Glass, o fundo é o vidro nativo.
 */
export function PillTabBar({ state, descriptors, navigation, icons }: Props) {
  const insets = useSafeAreaInsets();
  const glass = Platform.OS === "ios" && isLiquidGlassAvailable();

  const items = state.routes.map((route, index) => {
    const focused = state.index === index;
    const options = descriptors[route.key]?.options;
    const label = options?.title ?? route.name;
    const icon = icons[route.name] ?? "home";
    return (
      <Pressable
        key={route.key}
        accessibilityRole="tab"
        accessibilityLabel={label}
        accessibilityState={{ selected: focused }}
        onPress={() => {
          const event = navigation.emit({
            type: "tabPress",
            target: route.key,
            canPreventDefault: true,
          });
          if (!focused && !event.defaultPrevented) {
            navigation.navigate(route.name, route.params);
          }
        }}
        style={focused ? styles.active : styles.idle}
      >
        <Icon
          name={icon}
          size={focused ? 20 : 22}
          color={focused ? palette.onLime : palette.muted}
          strokeWidth={focused ? 2.4 : 2}
        />
        {focused ? (
          <Text style={[typography.label, styles.activeLabel]} numberOfLines={1}>
            {label}
          </Text>
        ) : null}
      </Pressable>
    );
  });

  const bottom = Math.max(insets.bottom, BAR_GAP);
  return (
    <View pointerEvents="box-none" style={[styles.wrap, { bottom }]}>
      {glass ? (
        <GlassView
          accessibilityRole="tablist"
          glassEffectStyle="regular"
          colorScheme="dark"
          style={styles.bar}
        >
          {items}
        </GlassView>
      ) : (
        <View accessibilityRole="tablist" style={[styles.bar, styles.solid]}>
          {items}
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { position: "absolute", left: 16, right: 16 },
  bar: {
    height: BAR_HEIGHT,
    borderRadius: BAR_HEIGHT / 2,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingHorizontal: 8,
    overflow: "hidden",
  },
  solid: {
    backgroundColor: "rgba(28, 28, 31, 0.96)",
    borderWidth: 1,
    borderColor: "rgba(255, 255, 255, 0.07)",
  },
  idle: {
    width: MIN_TOUCH,
    height: MIN_TOUCH,
    alignItems: "center",
    justifyContent: "center",
  },
  active: {
    height: 52,
    paddingHorizontal: 18,
    borderRadius: 26,
    backgroundColor: palette.lime,
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
    maxWidth: "50%",
  },
  activeLabel: { color: palette.onLime },
});
