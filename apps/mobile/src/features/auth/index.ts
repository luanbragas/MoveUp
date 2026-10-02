// API pública da feature auth: o que outras features e as rotas podem importar.
export { useMe } from "./hooks/use-me";
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
export type { MeRepository } from "./domain/ports";
export { WelcomeScreen } from "./ui/WelcomeScreen";
