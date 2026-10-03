package br.com.moveup.training.infrastructure.persistence;

import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.EXERCISE;

import br.com.moveup.training.application.port.out.ExerciseCatalog;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/** Exercícios da base ou da organização, inclusive arquivados (treino antigo continua válido). */
@Repository
public class JooqExerciseCatalog implements ExerciseCatalog {

  private final DSLContext dsl;

  public JooqExerciseCatalog(DSLContext dsl) {
    this.dsl = dsl;
  }

  @Override
  public Map<UUID, ExerciseRef> refs(UUID organizationId, Collection<UUID> exerciseIds) {
    if (exerciseIds.isEmpty()) {
      return Map.of();
    }
    return dsl
        .select(EXERCISE.ID, EXERCISE.NAME, EXERCISE.TRACKING_TYPE, EXERCISE.PRIMARY_MUSCLE)
        .from(EXERCISE)
        .where(EXERCISE.ID.in(exerciseIds))
        .and(EXERCISE.ORGANIZATION_ID.isNull().or(EXERCISE.ORGANIZATION_ID.eq(organizationId)))
        .fetch()
        .stream()
        .collect(
            Collectors.toMap(
                r -> r.get(EXERCISE.ID),
                r ->
                    new ExerciseRef(
                        r.get(EXERCISE.NAME),
                        r.get(EXERCISE.TRACKING_TYPE),
                        r.get(EXERCISE.PRIMARY_MUSCLE))));
  }
}
