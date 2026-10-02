import { isOnboardingComplete, type Me } from "./me";

// Para onde o app vai ao abrir ou depois de cada passo do onboarding (SCREEN-FLOWS 0.2 e 1.2).

export type Destination =
  | "loading"
  | "error"
  | "welcome"
  | "register"
  | "consents"
  | "guardian"
  | "professional-home"
  | "client-home";

export type AccountLookup =
  | { readonly kind: "loading" }
  | { readonly kind: "not-registered" }
  | { readonly kind: "failed" }
  | { readonly kind: "found"; readonly me: Me };

export function entryDestination(signedIn: boolean | null, account: AccountLookup): Destination {
  if (signedIn === null) {
    return "loading";
  }
  if (!signedIn) {
    return "welcome";
  }
  switch (account.kind) {
    case "loading":
      return "loading";
    case "failed":
      return "error";
    case "not-registered":
      return "register";
    case "found":
      return afterRegistration(account.me);
  }
}

function afterRegistration(me: Me): Destination {
  if (me.role === null) {
    return "register"; // conta antiga sem papel
  }
  if (!isOnboardingComplete(me)) {
    return me.onboarding.missingConsents.length > 0 ? "consents" : "guardian";
  }
  return me.role === "professional" ? "professional-home" : "client-home";
}
