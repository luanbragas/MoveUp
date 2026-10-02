import { PlaceholderScreen } from "../../../shared/ui/PlaceholderScreen";
import { strings } from "./strings";

/** Boas-vindas (SCREEN-FLOWS 0.2). Os botões "Sou personal" e "Tenho um convite" entram na Fase 1. */
export function WelcomeScreen() {
  return (
    <PlaceholderScreen title={strings.welcome.title} description={strings.welcome.description} />
  );
}
