package br.com.moveup.accounts.domain.model;

import br.com.moveup.accounts.domain.exception.ConsentVersionOutdated;
import br.com.moveup.accounts.domain.exception.GuardianAuthorizationNotFound;
import br.com.moveup.accounts.domain.exception.GuardianConsentNotAllowed;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Consentimento de um dos pais ou do responsável legal pelo aluno menor (LGPD, art. 14). O menor
 * <em>pede</em>, indicando quem é o responsável; só vale quando o próprio responsável autoriza pelo
 * link que recebe. Imutável: cada passo devolve o novo estado.
 *
 * @param linkExpiresAt validade do link em aberto; nulo se ainda não há link ou já decidiu
 * @param revokedAt fim do pedido: cancelado pelo menor, recusado ou revogado
 */
public record GuardianConsent(
    UUID id,
    UUID userId,
    PersonName guardianName,
    GuardianRelationship relationship,
    String docVersion,
    Instant requestedAt,
    Instant linkExpiresAt,
    Instant verifiedAt,
    Instant declinedAt,
    Instant revokedAt) {

  public enum Status {
    PENDING,
    VERIFIED,
    DECLINED,
    CANCELLED
  }

  /** O menor indica o responsável. Só para conta de menor e com o texto vigente. */
  public static GuardianConsent request(
      UUID id,
      AccountProfile minor,
      PersonName guardianName,
      GuardianRelationship relationship,
      String docVersion,
      LegalVersions current,
      Instant now,
      LocalDate today) {
    if (!minor.isMinorOn(today)) {
      throw GuardianConsentNotAllowed.notRequired();
    }
    requireCurrent(docVersion, current);
    return new GuardianConsent(
        id, minor.userId(), guardianName, relationship, docVersion, now, null, null, null, null);
  }

  public Status status() {
    if (verifiedAt != null && revokedAt == null) {
      return Status.VERIFIED;
    }
    if (declinedAt != null) {
      return Status.DECLINED;
    }
    return revokedAt == null ? Status.PENDING : Status.CANCELLED;
  }

  /** Pedido aberto ou consentimento vigente: impede um segundo pedido. */
  public boolean isOpen() {
    return revokedAt == null;
  }

  /** Novo link (o anterior deixa de valer). Só com o pedido aguardando o responsável. */
  public GuardianConsent withLink(Instant expiresAt) {
    requirePending();
    return new GuardianConsent(
        id,
        userId,
        guardianName,
        relationship,
        docVersion,
        requestedAt,
        expiresAt,
        null,
        null,
        null);
  }

  /** O responsável autoriza, aceitando o texto vigente, com o link dentro da validade. */
  public GuardianConsent approve(String acceptedVersion, LegalVersions current, Instant now) {
    requireOpenLink(now);
    requireCurrent(acceptedVersion, current);
    return new GuardianConsent(
        id,
        userId,
        guardianName,
        relationship,
        acceptedVersion,
        requestedAt,
        null,
        now,
        null,
        null);
  }

  public GuardianConsent decline(Instant now) {
    requireOpenLink(now);
    return new GuardianConsent(
        id, userId, guardianName, relationship, docVersion, requestedAt, null, null, now, now);
  }

  /** O menor desiste do pedido (ex.: indicou a pessoa errada). */
  public GuardianConsent cancel(Instant now) {
    requirePending();
    return new GuardianConsent(
        id, userId, guardianName, relationship, docVersion, requestedAt, null, null, null, now);
  }

  private void requirePending() {
    if (status() != Status.PENDING) {
      throw GuardianConsentNotAllowed.notPending();
    }
  }

  /** O responsável ainda pode decidir: pedido aguardando e link dentro da validade. */
  public boolean linkOpenAt(Instant now) {
    return status() == Status.PENDING && linkExpiresAt != null && now.isBefore(linkExpiresAt);
  }

  private void requireOpenLink(Instant now) {
    if (!linkOpenAt(now)) {
      throw new GuardianAuthorizationNotFound();
    }
  }

  private static void requireCurrent(String docVersion, LegalVersions current) {
    if (!current.guardianConsent().equals(docVersion)) {
      throw new ConsentVersionOutdated();
    }
  }
}
