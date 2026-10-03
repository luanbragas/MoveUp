package br.com.moveup.anamnesis.api;

import java.util.Optional;
import java.util.UUID;

/**
 * Para o módulo alerts (worker): a liberação médica está pendente na anamnese mais recente do
 * aluno? Só sim/não e ids; nenhuma resposta do aluno sai daqui.
 */
public interface ClearanceFacts {

  /** A partir de qualquer versão: olha a mais recente do mesmo aluno. */
  Optional<ClearanceState> latestFor(UUID anamnesisId);

  record ClearanceState(UUID clientId, UUID linkId, boolean pending) {}
}
