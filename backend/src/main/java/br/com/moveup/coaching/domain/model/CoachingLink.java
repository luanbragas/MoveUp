package br.com.moveup.coaching.domain.model;

import br.com.moveup.coaching.domain.exception.CoachingConflict;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Vínculo profissional–aluno. A ativação acontece no aceite do convite (função do banco, por causa
 * da trava do limite); aqui ficam as transições que o profissional faz depois.
 */
public final class CoachingLink {

  private final UUID id;
  private final UUID organizationId;
  private final UUID professionalId;
  private final UUID clientId;
  private LinkStatus status;
  private Instant endedAt; // nulo enquanto não encerrado

  public CoachingLink(
      UUID id,
      UUID organizationId,
      UUID professionalId,
      UUID clientId,
      LinkStatus status,
      Instant endedAt) {
    this.id = id;
    this.organizationId = organizationId;
    this.professionalId = professionalId;
    this.clientId = clientId;
    this.status = status;
    this.endedAt = endedAt;
  }

  public boolean belongsTo(UUID professional) {
    return professionalId.equals(professional);
  }

  /** Pausa o aluno: libera a vaga do plano, mantém o histórico. */
  public void inactivate() {
    if (status != LinkStatus.ACTIVE) {
      throw CoachingConflict.linkStateInvalid();
    }
    status = LinkStatus.INACTIVE;
  }

  /**
   * Volta a ativar, se o plano tiver vaga. {@code limit} vem com a assinatura travada e {@code
   * activeNow} contado depois da trava, então duas reativações simultâneas não passam do limite.
   */
  public void reactivate(int activeNow, OptionalInt limit) {
    if (status != LinkStatus.INACTIVE) {
      throw CoachingConflict.linkStateInvalid();
    }
    if (limit.isEmpty() || activeNow >= limit.getAsInt()) {
      throw CoachingConflict.planLimitReached();
    }
    status = LinkStatus.ACTIVE;
  }

  /** Encerra de vez. Pendente encerrado = convite cancelado sem chance de reenviar. */
  public void end(Instant now) {
    if (status == LinkStatus.ENDED) {
      throw CoachingConflict.linkStateInvalid();
    }
    status = LinkStatus.ENDED;
    endedAt = now;
  }

  /** Reenviar ou cancelar convite só faz sentido enquanto o aluno não aceitou. */
  public void requirePending() {
    if (status != LinkStatus.PENDING) {
      throw CoachingConflict.linkStateInvalid();
    }
  }

  public UUID id() {
    return id;
  }

  public UUID organizationId() {
    return organizationId;
  }

  public UUID professionalId() {
    return professionalId;
  }

  public UUID clientId() {
    return clientId;
  }

  public LinkStatus status() {
    return status;
  }

  public Optional<Instant> endedAt() {
    return Optional.ofNullable(endedAt);
  }
}
