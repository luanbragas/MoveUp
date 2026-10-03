package br.com.moveup.coaching.api;

import java.util.Optional;
import java.util.UUID;

/** Para outros módulos (training): o vínculo, se for do profissional informado. */
public interface LinkDirectory {

  /** Vazio se o vínculo não existe ou é de outro profissional (responda 404). */
  Optional<LinkRef> ofProfessional(UUID professionalId, UUID linkId);

  /**
   * @param status {@code pending}, {@code active}, {@code inactive} ou {@code ended}
   */
  record LinkRef(UUID linkId, UUID clientId, UUID organizationId, String status) {

    /** Programa e treino só para vínculo pendente ou ativo (o aluno inativo fica congelado). */
    public boolean acceptsTraining() {
      return "pending".equals(status) || "active".equals(status);
    }
  }
}
