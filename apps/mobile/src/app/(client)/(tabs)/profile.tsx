import { router } from "expo-router";
import { SignOutButton } from "../../../features/auth";
import { Button } from "../../../shared/ui/Button";
import { PlaceholderScreen } from "../../../shared/ui/PlaceholderScreen";
import { navigationStrings } from "../../../shared/ui/navigation-strings";

const t = navigationStrings.soon.clientProfile;

export default function Soon() {
  return (
    <PlaceholderScreen
      title={t.title}
      emptyTitle={t.emptyTitle}
      description={t.description}
      icon={t.icon}
    >
      <Button
        label="Minha anamnese"
        variant="secondary"
        icon="doc"
        onPress={() => {
          router.push("/anamnesis");
        }}
      />
      <SignOutButton />
    </PlaceholderScreen>
  );
}
