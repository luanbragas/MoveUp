import { SignOutButton } from "../../../features/auth";
import { PlaceholderScreen } from "../../../shared/ui/PlaceholderScreen";
import { navigationStrings } from "../../../shared/ui/navigation-strings";

const t = navigationStrings.soon.settings;

export default function Soon() {
  return (
    <PlaceholderScreen title={t.title} description={t.description}>
      <SignOutButton />
    </PlaceholderScreen>
  );
}
