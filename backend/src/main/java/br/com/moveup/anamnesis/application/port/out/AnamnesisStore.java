package br.com.moveup.anamnesis.application.port.out;

import br.com.moveup.anamnesis.domain.model.Anamnesis;
import br.com.moveup.anamnesis.domain.model.AnamnesisTemplate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Anamneses (RLS: o aluno a própria; o personal, a dos alunos dele). Respostas cifradas. */
public interface AnamnesisStore {

  /** Modelo ativo do sistema. */
  AnamnesisTemplate currentTemplate();

  Optional<Anamnesis> latest(UUID clientId);

  Optional<Anamnesis> version(UUID clientId, int versionNumber);

  List<VersionSummary> versions(UUID clientId);

  void insert(Anamnesis anamnesis);

  /** Só versão ainda não revisada (o banco recusa mudar revisada). */
  void update(Anamnesis anamnesis);

  /** Aviso para o worker ({@code anamnesis.submitted}, {@code anamnesis.reviewed}): só ids. */
  void announce(UUID anamnesisId, UUID clientId, String eventType);

  record VersionSummary(
      int versionNumber, Instant createdAt, Instant reviewedAt, String clearance, boolean parq) {}
}
