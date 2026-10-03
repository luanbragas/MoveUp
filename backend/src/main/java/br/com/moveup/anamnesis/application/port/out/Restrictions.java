package br.com.moveup.anamnesis.application.port.out;

import br.com.moveup.anamnesis.domain.model.HealthRestriction;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Restrições de saúde do aluno (descrição cifrada; excluir = tombstone para o sync). */
public interface Restrictions {

  List<HealthRestriction> list(UUID clientId, boolean includeResolved);

  Optional<HealthRestriction> find(UUID clientId, UUID restrictionId);

  void insert(HealthRestriction restriction, UUID createdBy);

  void update(HealthRestriction restriction);

  void delete(UUID clientId, UUID restrictionId);
}
