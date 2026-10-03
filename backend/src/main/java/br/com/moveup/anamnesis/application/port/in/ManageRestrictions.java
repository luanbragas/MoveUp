package br.com.moveup.anamnesis.application.port.in;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Restrições do aluno, mantidas pelo personal (aviso no perfil e ao montar treino). */
public interface ManageRestrictions {

  List<RestrictionView> list(UUID professionalId, UUID linkId, boolean includeResolved);

  RestrictionView create(UUID professionalId, UUID linkId, RestrictionInput input);

  RestrictionView update(
      UUID professionalId, UUID linkId, UUID restrictionId, RestrictionInput input);

  void delete(UUID professionalId, UUID linkId, UUID restrictionId);

  record RestrictionInput(
      String kind, String bodyRegion, String description, Integer severity, LocalDate resolvedOn) {}

  record RestrictionView(
      UUID id,
      String kind,
      String bodyRegion,
      String description,
      Integer severity,
      LocalDate resolvedOn,
      boolean fromAnamnesis) {}
}
