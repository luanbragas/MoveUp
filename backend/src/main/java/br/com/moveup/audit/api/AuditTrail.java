package br.com.moveup.audit.api;

import java.util.UUID;

/**
 * Porta pública do audit: grava no {@code audit_log} na mesma transação da mudança (ARQUITETURA,
 * seção 5). Nunca recebe dado de saúde: só ids e a ação.
 */
public interface AuditTrail {

  void record(Entry entry);

  /**
   * @param clientId aluno afetado (sem FK: sobrevive à anonimização)
   */
  record Entry(UUID actorId, String entity, UUID entityId, UUID clientId, Action action) {}

  enum Action {
    LINK_CREATED("link_created"),
    LINK_CHANGED("link_changed"),
    LINK_ENDED("link_ended"),
    /** O personal abriu a anamnese ou as restrições de um aluno (dado de saúde). */
    VIEW_ANAMNESIS("view_anamnesis");

    private final String code;

    Action(String code) {
      this.code = code;
    }

    public String code() {
      return code;
    }
  }
}
