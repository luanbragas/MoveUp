package br.com.moveup.anamnesis.application.port.in;

import br.com.moveup.anamnesis.application.port.out.AnamnesisStore.VersionSummary;
import br.com.moveup.anamnesis.domain.model.AnamnesisTemplate;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Anamnese: o aluno preenche; o personal revisa, complementa e decide a liberação médica. */
public interface ManageAnamnesis {

  AnamnesisTemplate template();

  /** A anamnese mais recente do próprio aluno (vazia antes do primeiro envio). */
  Optional<AnamnesisView> mine(UUID userId);

  AnamnesisView submit(UUID userId, Map<String, Object> answers);

  ClientAnamnesis ofLink(UUID professionalId, UUID linkId);

  AnamnesisView version(UUID professionalId, UUID linkId, int versionNumber);

  /**
   * Revisa a versão mais recente: complementa as respostas, decide a liberação e trava. Se a mais
   * recente já estava revisada, grava uma versão nova já revisada.
   */
  AnamnesisView review(
      UUID professionalId,
      UUID linkId,
      Map<String, Object> answers,
      String clearance,
      LocalDate clearanceDate);

  record AnamnesisView(
      int versionNumber,
      Map<String, Object> answers,
      boolean parqPositive,
      String clearance,
      LocalDate clearanceDate,
      boolean reviewed,
      Instant reviewedAt,
      Instant createdAt) {}

  record ClientAnamnesis(Optional<AnamnesisView> latest, List<VersionSummary> versions) {}
}
