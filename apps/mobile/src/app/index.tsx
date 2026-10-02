import { Redirect } from "expo-router";

// Fase 1: decidir o grupo pelo papel da conta (SCREEN-FLOWS 0.2). Sem login ainda, vai para as boas-vindas.
export default function Index() {
  return <Redirect href="/welcome" />;
}
