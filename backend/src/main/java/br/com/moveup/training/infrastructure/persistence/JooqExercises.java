package br.com.moveup.training.infrastructure.persistence;

import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.EXERCISE;

import br.com.moveup.shared.infrastructure.persistence.jooq.tables.records.ExerciseRecord;
import br.com.moveup.training.application.port.in.ManageExercises.ExerciseView;
import br.com.moveup.training.application.port.out.Exercises;
import br.com.moveup.training.domain.model.Exercise;
import br.com.moveup.training.domain.model.Muscle;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

/**
 * Biblioteca de exercícios (sem RLS: não é dado de aluno). A organização vem sempre do caso de uso;
 * a base é {@code organization_id is null}.
 */
@Repository
public class JooqExercises implements Exercises {

  /** Nome sem acento e em minúsculas, igual ao índice trigram da V5. */
  private static final Field<String> PLAIN_NAME =
      DSL.field("immutable_unaccent(lower({0}))", String.class, EXERCISE.NAME);

  private final DSLContext dsl;

  public JooqExercises(DSLContext dsl) {
    this.dsl = dsl;
  }

  @Override
  public List<ExerciseView> search(UUID organizationId, String query, Muscle muscle, int limit) {
    Condition visible =
        EXERCISE
            .ORGANIZATION_ID
            .isNull()
            .or(EXERCISE.ORGANIZATION_ID.eq(organizationId))
            .and(EXERCISE.ARCHIVED_AT.isNull());
    if (muscle != null) {
      visible =
          visible.and(
              EXERCISE
                  .PRIMARY_MUSCLE
                  .eq(muscle.code())
                  .or(
                      DSL.condition(
                          "{0} = any({1})", DSL.val(muscle.code()), EXERCISE.SECONDARY_MUSCLES)));
    }
    var select = dsl.selectFrom(EXERCISE);
    if (query.isEmpty()) {
      return select.where(visible).orderBy(PLAIN_NAME).limit(limit).fetch(JooqExercises::toView);
    }
    var typed = DSL.field("immutable_unaccent(lower({0}))", String.class, DSL.val(query));
    var position = DSL.position(PLAIN_NAME, typed);
    // parecido com alguma palavra do nome (erro de digitação); a biblioteca é pequena, então o
    // limite explícito (abaixo do padrão 0,6 do pg_trgm) não precisa do índice
    var similar =
        DSL.condition("word_similarity({0}, {1}) >= {2}", typed, PLAIN_NAME, DSL.inline(0.45));
    return select
        .where(visible.and(position.gt(0).or(similar)))
        .orderBy(
            DSL.when(position.eq(1), 0).when(position.gt(0), 1).otherwise(2),
            DSL.field("word_similarity({0}, {1})", Double.class, typed, PLAIN_NAME).desc(),
            PLAIN_NAME)
        .limit(limit)
        .fetch(JooqExercises::toView);
  }

  @Override
  public Optional<Exercise> find(UUID exerciseId) {
    return dsl.selectFrom(EXERCISE)
        .where(EXERCISE.ID.eq(exerciseId))
        .fetchOptional(JooqExercises::toDomain);
  }

  @Override
  public boolean nameTaken(UUID organizationId, String name) {
    return dsl.fetchExists(
        EXERCISE,
        EXERCISE.ORGANIZATION_ID.isNull().or(EXERCISE.ORGANIZATION_ID.eq(organizationId)),
        EXERCISE.ARCHIVED_AT.isNull(),
        PLAIN_NAME.eq(DSL.field("immutable_unaccent(lower({0}))", String.class, DSL.val(name))));
  }

  @Override
  public void insert(Exercise e, UUID createdBy) {
    dsl.insertInto(EXERCISE)
        .set(EXERCISE.ID, e.id())
        .set(EXERCISE.ORGANIZATION_ID, e.organizationId())
        .set(EXERCISE.NAME, e.name())
        .set(EXERCISE.MODALITY, e.modality().code())
        .set(EXERCISE.TRACKING_TYPE, e.trackingType().code())
        .set(EXERCISE.PRIMARY_MUSCLE, e.primaryMuscle() == null ? null : e.primaryMuscle().code())
        .set(
            EXERCISE.SECONDARY_MUSCLES,
            e.secondaryMuscles().stream().map(Muscle::code).toArray(String[]::new))
        .set(EXERCISE.EQUIPMENT, e.equipment())
        .set(EXERCISE.IS_UNILATERAL, e.unilateral())
        .set(EXERCISE.INSTRUCTIONS, e.instructions())
        .set(EXERCISE.MEDIA_URL, e.mediaUrl())
        .set(EXERCISE.CREATED_BY, createdBy)
        .execute();
  }

  @Override
  public void archive(UUID exerciseId) {
    dsl.update(EXERCISE)
        .set(EXERCISE.ARCHIVED_AT, DSL.currentOffsetDateTime())
        .where(EXERCISE.ID.eq(exerciseId))
        .and(EXERCISE.ARCHIVED_AT.isNull())
        .execute();
  }

  private static List<Muscle> muscles(String[] codes) {
    return codes == null
        ? List.of()
        : Arrays.stream(codes).map(Muscle::fromCode).flatMap(Optional::stream).toList();
  }

  private static Exercise toDomain(ExerciseRecord r) {
    return new Exercise(
        r.getId(),
        r.getOrganizationId(),
        r.getName(),
        Exercise.Modality.fromCode(r.getModality()),
        Exercise.TrackingType.fromCode(r.getTrackingType()),
        r.getPrimaryMuscle() == null ? null : Muscle.fromCode(r.getPrimaryMuscle()).orElse(null),
        muscles(r.getSecondaryMuscles()),
        r.getEquipment(),
        Boolean.TRUE.equals(r.getIsUnilateral()),
        r.getInstructions(),
        r.getMediaUrl());
  }

  private static ExerciseView toView(ExerciseRecord r) {
    return new ExerciseView(
        r.getId(),
        r.getName(),
        r.getModality(),
        r.getTrackingType(),
        r.getPrimaryMuscle(),
        r.getSecondaryMuscles() == null ? List.of() : List.of(r.getSecondaryMuscles()),
        r.getEquipment(),
        Boolean.TRUE.equals(r.getIsUnilateral()),
        r.getInstructions(),
        r.getMediaUrl(),
        r.getOrganizationId() != null);
  }
}
