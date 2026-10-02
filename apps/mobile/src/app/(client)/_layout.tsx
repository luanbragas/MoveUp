import { Stack } from "expo-router";
import { RoleGate } from "../../features/auth";

// Perfil aluno: só entra quem tem papel de aluno e terminou o onboarding.
export default function ClientLayout() {
  return (
    <RoleGate home="client-home">
      <Stack screenOptions={{ headerShown: false }} />
    </RoleGate>
  );
}
