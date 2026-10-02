import { Stack } from "expo-router";

// Perfil profissional. Fase 1: só entra quem tem papel de profissional (o layout raiz redireciona).
export default function ProfessionalLayout() {
  return <Stack screenOptions={{ headerShown: false }} />;
}
