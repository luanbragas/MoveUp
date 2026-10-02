package br.com.moveup.accounts.domain.model;

import java.util.EnumSet;
import java.util.Set;

/** Papel da conta. No MVP uma conta tem um papel só (SCREEN-FLOWS 0.1). */
public enum AccountRole {
  PROFESSIONAL("professional", EnumSet.of(ConsentKind.TERMS, ConsentKind.PRIVACY)),
  // aluno lida com dado de saúde desde a anamnese: o consentimento específico é obrigatório
  CLIENT("client", EnumSet.of(ConsentKind.TERMS, ConsentKind.PRIVACY, ConsentKind.HEALTH_DATA));

  private final String code;
  private final Set<ConsentKind> requiredConsents;

  AccountRole(String code, Set<ConsentKind> requiredConsents) {
    this.code = code;
    this.requiredConsents = requiredConsents;
  }

  public String code() {
    return code;
  }

  /** Consentimentos sem os quais a conta não segue o onboarding. Fotos é opcional para todos. */
  public Set<ConsentKind> requiredConsents() {
    return EnumSet.copyOf(requiredConsents);
  }

  public static AccountRole fromCode(String code) {
    for (var role : values()) {
      if (role.code.equals(code)) {
        return role;
      }
    }
    throw new IllegalArgumentException("papel desconhecido: " + code);
  }
}
