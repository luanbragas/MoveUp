import { Stack } from "expo-router";

// Perfil aluno. Fase 1: só entra quem tem papel de aluno (o layout raiz redireciona).
export default function ClientLayout() {
  return <Stack screenOptions={{ headerShown: false }} />;
}
