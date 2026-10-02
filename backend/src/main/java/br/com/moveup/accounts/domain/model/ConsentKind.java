package br.com.moveup.accounts.domain.model;

/** Tipos de consentimento (tabela {@code consent}), cada um com versão do texto aceito. */
public enum ConsentKind {
  TERMS("terms"),
  PRIVACY("privacy"),
  HEALTH_DATA("health_data"),
  PHOTOS("photos");

  private final String code;

  ConsentKind(String code) {
    this.code = code;
  }

  public String code() {
    return code;
  }

  public static ConsentKind fromCode(String code) {
    for (var kind : values()) {
      if (kind.code.equals(code)) {
        return kind;
      }
    }
    throw new IllegalArgumentException("consentimento desconhecido: " + code);
  }
}
