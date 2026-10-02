import { Stack } from "expo-router";
import { RoleGate } from "../../features/auth";

// Perfil profissional: só entra quem tem papel de profissional e terminou o onboarding.
export default function ProfessionalLayout() {
  return (
    <RoleGate home="professional-home">
      <Stack screenOptions={{ headerShown: false }} />
    </RoleGate>
  );
}
