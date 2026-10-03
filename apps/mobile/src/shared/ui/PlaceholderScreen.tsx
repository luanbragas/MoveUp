import type { ReactNode } from "react";
import { EmptyState } from "./EmptyState";
import type { IconName } from "./Icon";
import { Screen } from "./Screen";

interface Props {
  readonly title: string;
  readonly emptyTitle: string;
  readonly description: string;
  readonly icon: IconName;
  readonly children?: ReactNode;
}

/** Aba que ainda não tem feature: o título da aba e o estado vazio do que vai aparecer ali. */
export function PlaceholderScreen({ title, emptyTitle, description, icon, children }: Props) {
  return (
    <Screen title={title}>
      <EmptyState icon={icon} title={emptyTitle} text={description} />
      {children}
    </Screen>
  );
}
