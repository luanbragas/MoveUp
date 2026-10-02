// API pública da feature auth: o que outras features e as rotas podem importar.
export { useMe } from "./hooks/use-me";
export { useAuthState, type AuthState } from "./hooks/use-auth-state";
export { authKeys } from "./hooks/auth-keys";
export { isOnboardingComplete } from "./domain/me";
export type {
  AccountRole,
  ConsentKind,
  LengthUnit,
  Me,
  Onboarding,
  UserId,
  WeightUnit,
} from "./domain/me";
export type { AccountRepository, MeRepository } from "./domain/ports";
export type { AuthSession, AuthUser } from "./domain/session";
export { EntryScreen } from "./ui/EntryScreen";
export { WelcomeScreen } from "./ui/WelcomeScreen";
export { SignInScreen } from "./ui/SignInScreen";
export { RegisterScreen } from "./ui/RegisterScreen";
export { ConsentsScreen } from "./ui/ConsentsScreen";
export { GuardianScreen } from "./ui/GuardianScreen";
export { RoleGate } from "./ui/RoleGate";
export { SignOutButton } from "./ui/SignOutButton";
