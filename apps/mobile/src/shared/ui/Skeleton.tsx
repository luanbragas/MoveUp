import { useEffect, useState } from "react";
import { Animated, type DimensionValue } from "react-native";

interface Props {
  readonly width: DimensionValue;
  readonly height: number;
  readonly rounded?: number;
}

/** Bloco de carregamento no formato do conteúdo (no lugar de rodinha girando). */
export function Skeleton({ width, height, rounded = 12 }: Props) {
  const [pulse] = useState(() => new Animated.Value(0.55));

  useEffect(() => {
    const loop = Animated.loop(
      Animated.sequence([
        Animated.timing(pulse, { toValue: 1, duration: 700, useNativeDriver: true }),
        Animated.timing(pulse, { toValue: 0.55, duration: 700, useNativeDriver: true }),
      ]),
    );
    loop.start();
    return () => {
      loop.stop();
    };
  }, [pulse]);

  return (
    <Animated.View
      accessibilityElementsHidden
      importantForAccessibility="no-hide-descendants"
      style={{ width, height, borderRadius: rounded, backgroundColor: "#2A2A2F", opacity: pulse }}
    />
  );
}
