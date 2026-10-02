package br.com.moveup.coaching.domain.model;

/** Estado do vínculo profissional–aluno. */
public enum LinkStatus {
  /** Convite enviado, aluno ainda não aceitou. */
  PENDING("pending"),
  ACTIVE("active"),
  /** Pausado pelo profissional: não conta no limite do plano; histórico continua visível. */
  INACTIVE("inactive"),
  ENDED("ended");

  private final String code;

  LinkStatus(String code) {
    this.code = code;
  }

  public String code() {
    return code;
  }

  public static LinkStatus fromCode(String code) {
    for (var status : values()) {
      if (status.code.equals(code)) {
        return status;
      }
    }
    throw new IllegalArgumentException("status de vínculo desconhecido: " + code);
  }
}
