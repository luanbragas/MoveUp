package br.com.moveup.execution.infrastructure.persistence;

import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.WORKOUT_SESSION;

import br.com.moveup.execution.api.PlannedVersionUsage;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/**
 * Sessões têm RLS: o personal enxerga as dos alunos dele, que são as únicas que importam para um
 * treino da organização dele.
 */
@Repository
public class JooqPlannedVersionUsage implements PlannedVersionUsage {

  private final DSLContext dsl;

  public JooqPlannedVersionUsage(DSLContext dsl) {
    this.dsl = dsl;
  }

  @Override
  public boolean isUsed(UUID workoutVersionId) {
    return dsl.fetchExists(
        WORKOUT_SESSION, WORKOUT_SESSION.WORKOUT_VERSION_ID.eq(workoutVersionId));
  }
}
