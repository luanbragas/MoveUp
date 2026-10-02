import type { ReactNode } from "react";
import { Screen } from "./Screen";

interface Props {
  readonly title: string;
  readonly description: string;
  readonly children?: ReactNode;
}

/** Tela provisória das rotas que ainda não têm feature. */
export function PlaceholderScreen({ title, description, children }: Props) {
  return (
    <Screen title={title} subtitle={description}>
      {children}
    </Screen>
  );
}
