package br.com.moveup.anamnesis.domain.model;

import br.com.moveup.anamnesis.domain.exception.InvalidAnamnesisData;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

/**
 * Uma versão da anamnese do aluno. Enquanto não revisada, o aluno pode reenviar e ela é
 * substituída; depois de revisada pelo personal é imutável e qualquer mudança vira versão nova.
 */
public record Anamnesis(
    UUID id,
    UUID clientId,
    UUID linkId,
    int versionNumber,
    UUID templateId,
    Answers answers,
    Clearance clearance,
    UUID filledBy,
    UUID reviewedBy,
    Instant reviewedAt,
    Instant createdAt) {

  public enum ClearanceStatus {
    NOT_REQUIRED,
    PENDING,
    CLEARED;

    public String code() {
      return name().toLowerCase(Locale.ROOT);
    }

    public static ClearanceStatus fromCode(String code) {
      for (var s : values()) {
        if (s.code().equals(code)) {
          return s;
        }
      }
      throw new InvalidAnamnesisData("clearance-invalid", "Situação da liberação inválida.");
    }
  }

  /** Liberação médica: liberada precisa da data do atestado. */
  public record Clearance(ClearanceStatus status, LocalDate date) {
    public Clearance {
      if (status == ClearanceStatus.CLEARED && date == null) {
        throw new InvalidAnamnesisData(
            "clearance-date-required", "Informe a data da liberação médica.");
      }
      if (status != ClearanceStatus.CLEARED) {
        date = null;
      }
    }

    /** No envio do aluno: PAR-Q com "sim" deixa a liberação pendente. */
    static Clearance fromParq(boolean positive) {
      return new Clearance(positive ? ClearanceStatus.PENDING : ClearanceStatus.NOT_REQUIRED, null);
    }
  }

  public boolean reviewed() {
    return reviewedAt != null;
  }

  /** Primeira versão ou nova versão depois de uma revisada. */
  public static Anamnesis submitted(
      UUID id,
      UUID clientId,
      UUID linkId,
      int versionNumber,
      AnamnesisTemplate template,
      Answers answers,
      UUID filledBy,
      Instant now) {
    return new Anamnesis(
        id,
        clientId,
        linkId,
        versionNumber,
        template.id(),
        answers,
        Clearance.fromParq(answers.parqPositive()),
        filledBy,
        null,
        null,
        now);
  }

  /** O aluno reenviou antes da revisão: substitui as respostas da mesma versão. */
  public Anamnesis resubmitted(Answers newAnswers, UUID by) {
    if (reviewed()) {
      throw new InvalidAnamnesisData(
          "anamnesis-reviewed", "Esta versão já foi revisada; envie uma nova.");
    }
    return new Anamnesis(
        id,
        clientId,
        linkId,
        versionNumber,
        templateId,
        newAnswers,
        Clearance.fromParq(newAnswers.parqPositive()),
        by,
        null,
        null,
        createdAt);
  }

  /** Revisão do personal: complementa as respostas, decide a liberação e trava a versão. */
  public Anamnesis reviewedBy(
      UUID professional, Answers complemented, Clearance decision, Instant now) {
    if (reviewed()) {
      throw new InvalidAnamnesisData(
          "anamnesis-reviewed", "Esta versão já foi revisada; envie uma nova.");
    }
    return new Anamnesis(
        id,
        clientId,
        linkId,
        versionNumber,
        templateId,
        complemented,
        decision,
        filledBy,
        professional,
        now,
        createdAt);
  }
}
