import { router } from "expo-router";
import { Button } from "../../../shared/ui/Button";
import { Screen } from "../../../shared/ui/Screen";
import { strings } from "./strings";

/**
 * Boas-vindas (SCREEN-FLOWS 0.2). Os dois caminhos passam pelo login; o papel é escolhido no
 * cadastro. "Tenho um convite" ganha o campo do código na F1-5.
 */
export function WelcomeScreen() {
  return (
    <Screen title={strings.welcome.title} subtitle={strings.welcome.description}>
      <Button
        label={strings.welcome.professional}
        onPress={() => {
          router.push("/sign-in");
        }}
      />
      <Button
        label={strings.welcome.client}
        variant="secondary"
        onPress={() => {
          router.push("/sign-in");
        }}
      />
    </Screen>
  );
}
