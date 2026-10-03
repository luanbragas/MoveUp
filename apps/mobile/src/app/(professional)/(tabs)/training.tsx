import { router } from "expo-router";
import { ListRow } from "../../../shared/ui/ListRow";
import { PlaceholderScreen } from "../../../shared/ui/PlaceholderScreen";
import { navigationStrings } from "../../../shared/ui/navigation-strings";

const t = navigationStrings.soon.training;

export default function Training() {
  return (
    <PlaceholderScreen
      title={t.title}
      emptyTitle={t.emptyTitle}
      description={t.description}
      icon={t.icon}
    >
      <ListRow
        title={navigationStrings.library.title}
        subtitle={navigationStrings.library.subtitle}
        icon="search"
        last
        onPress={() => {
          router.push("/exercises");
        }}
      />
    </PlaceholderScreen>
  );
}
