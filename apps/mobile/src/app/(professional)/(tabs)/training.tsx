import { PlaceholderScreen } from "../../../shared/ui/PlaceholderScreen";
import { navigationStrings } from "../../../shared/ui/navigation-strings";

const t = navigationStrings.soon.training;

export default function Soon() {
  return <PlaceholderScreen title={t.title} description={t.description}></PlaceholderScreen>;
}
