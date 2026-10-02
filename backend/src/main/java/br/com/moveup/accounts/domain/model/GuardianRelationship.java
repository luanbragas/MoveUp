package br.com.moveup.accounts.domain.model;

/** Parentesco de quem consente pelo menor. */
public enum GuardianRelationship {
  MOTHER("mother"),
  FATHER("father"),
  LEGAL_GUARDIAN("legal_guardian"),
  OTHER("other");

  private final String code;

  GuardianRelationship(String code) {
    this.code = code;
  }

  public String code() {
    return code;
  }

  public static GuardianRelationship fromCode(String code) {
    for (var relationship : values()) {
      if (relationship.code.equals(code)) {
        return relationship;
      }
    }
    throw new IllegalArgumentException("parentesco desconhecido: " + code);
  }
}
