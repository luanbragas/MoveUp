import { useState, type ReactNode } from "react";
import { Animated, PanResponder, Vibration, View } from "react-native";

interface Layout {
  readonly y: number;
  readonly height: number;
}

/** O que cada item recebe para virar alça: segurar começa, soltar sem arrastar cancela. */
export interface DragHandle {
  readonly start: () => void;
  readonly cancel: () => void;
  readonly dragging: boolean;
}

/**
 * Para onde vai o item arrastado: passa da metade de um vizinho, troca de lugar com ele. Pura
 * (testável sem gesto).
 */
export function targetIndex(layouts: readonly Layout[], from: number, dy: number): number {
  const moving = layouts[from];
  if (moving === undefined) {
    return from;
  }
  const center = moving.y + moving.height / 2 + dy;
  let to = from;
  layouts.forEach((l, i) => {
    const mid = l.y + l.height / 2;
    if (i > from && center > mid) {
      to = Math.max(to, i);
    }
    if (i < from && center < mid) {
      to = Math.min(to, i);
    }
  });
  return to;
}

interface Props<T> {
  readonly items: readonly T[];
  readonly keyOf: (item: T) => string;
  readonly renderItem: (item: T, index: number, drag: DragHandle) => ReactNode;
  readonly onReorder: (from: number, to: number) => void;
  /** Avisa para a tela travar a rolagem enquanto arrasta. */
  readonly onDragChange?: (dragging: boolean) => void;
  readonly gap?: number;
}

interface Drag {
  readonly from: number;
  readonly to: number;
  /** Altura do item arrastado + espaço: quanto os vizinhos andam para abrir lugar. */
  readonly step: number;
}

/** Estado do gesto fora do React (o PanResponder lê e escreve durante o movimento). */
interface Gesture {
  layouts: Layout[];
  count: number;
  from: number | null;
  granted: boolean;
  onReorder: (from: number, to: number) => void;
  onDragChange: ((dragging: boolean) => void) | undefined;
  setDrag: (drag: Drag | null) => void;
}

/**
 * Lista curta reordenável com toque longo (blocos e exercícios do editor). Sem virtualização e
 * sem biblioteca de gestos: PanResponder do React Native assume o toque depois do toque longo.
 */
export function ReorderList<T>({
  items,
  keyOf,
  renderItem,
  onReorder,
  onDragChange,
  gap = 0,
}: Props<T>) {
  const [drag, setDrag] = useState<Drag | null>(null);
  const [dy] = useState(() => new Animated.Value(0));
  // objeto mutável do gesto: criado uma vez e só escrito nos eventos (os callbacks atuais chegam
  // no começo do arrasto, que é um evento)
  const [gesture] = useState<Gesture>(() => ({
    layouts: [],
    count: items.length,
    from: null,
    granted: false,
    onReorder,
    onDragChange,
    setDrag,
  }));

  const [responder] = useState(() => {
    const g = () => gesture;
    const layouts = () => g().layouts.slice(0, g().count);
    const finish = (moved: number) => {
      const { from } = g();
      g().from = null;
      g().granted = false;
      g().setDrag(null);
      dy.setValue(0);
      g().onDragChange?.(false);
      if (from !== null) {
        const to = targetIndex(layouts(), from, moved);
        if (to !== from) {
          g().onReorder(from, to);
        }
      }
    };
    return {
      finish,
      begin(
        index: number,
        step: number,
        latest: Pick<Gesture, "count" | "onReorder" | "onDragChange">,
      ) {
        g().count = latest.count;
        g().onReorder = latest.onReorder;
        g().onDragChange = latest.onDragChange;
        g().from = index;
        g().granted = false;
        g().setDrag({ from: index, to: index, step });
        Vibration.vibrate(15);
        g().onDragChange?.(true);
      },
      cancel() {
        // soltou sem arrastar (o PanResponder não chegou a assumir)
        if (g().from !== null && !g().granted) {
          finish(0);
        }
      },
      measure(index: number, layout: Layout) {
        g().layouts[index] = layout;
      },
      height(index: number) {
        return g().layouts[index]?.height ?? 0;
      },
      pan: PanResponder.create({
        onStartShouldSetPanResponderCapture: () => g().from !== null,
        onMoveShouldSetPanResponderCapture: () => g().from !== null,
        onPanResponderGrant: () => {
          g().granted = true;
        },
        onPanResponderTerminationRequest: () => false,
        onPanResponderMove: (_, state) => {
          dy.setValue(state.dy);
          const { from } = g();
          if (from !== null) {
            const to = targetIndex(layouts(), from, state.dy);
            g().setDrag({ from, to, step: (layouts()[from]?.height ?? 0) + gap });
          }
        },
        onPanResponderRelease: (_, state) => {
          finish(state.dy);
        },
        onPanResponderTerminate: (_, state) => {
          finish(state.dy);
        },
      }),
    };
  });

  return (
    <View {...responder.pan.panHandlers} style={{ gap }}>
      {items.map((item, index) => {
        let shift = 0;
        if (drag !== null && index !== drag.from) {
          if (drag.from < drag.to && index > drag.from && index <= drag.to) {
            shift = -drag.step;
          }
          if (drag.from > drag.to && index < drag.from && index >= drag.to) {
            shift = drag.step;
          }
        }
        const lifted = drag?.from === index;
        return (
          <Animated.View
            key={keyOf(item)}
            onLayout={(e) => {
              responder.measure(index, {
                y: e.nativeEvent.layout.y,
                height: e.nativeEvent.layout.height,
              });
            }}
            style={
              lifted
                ? { transform: [{ translateY: dy }], zIndex: 10, elevation: 8, opacity: 0.92 }
                : shift === 0
                  ? null
                  : { transform: [{ translateY: shift }] }
            }
          >
            {renderItem(item, index, {
              start: () => {
                responder.begin(index, responder.height(index) + gap, {
                  count: items.length,
                  onReorder,
                  onDragChange,
                });
              },
              cancel: () => {
                responder.cancel();
              },
              dragging: lifted,
            })}
          </Animated.View>
        );
      })}
    </View>
  );
}
