import { PlaceholderScreen } from "../../../shared/ui/PlaceholderScreen";
import { navigationStrings } from "../../../shared/ui/navigation-strings";

const t = navigationStrings.soon.clientProgress;

export default function Soon() {
  return (
    <PlaceholderScreen
      title={t.title}
      emptyTitle={t.emptyTitle}
      description={t.description}
      icon={t.icon}
    ></PlaceholderScreen>
  );
}
