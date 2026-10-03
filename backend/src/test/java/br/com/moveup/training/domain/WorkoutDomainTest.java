package br.com.moveup.training.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.moveup.shared.domain.DomainException;
import br.com.moveup.shared.domain.ResourceNotFound;
import br.com.moveup.shared.domain.VersionMismatch;
import br.com.moveup.training.domain.model.PrescribedExercise;
import br.com.moveup.training.domain.model.PrescribedSet;
import br.com.moveup.training.domain.model.PrescribedSet.SetType;
import br.com.moveup.training.domain.model.Workout;
import br.com.moveup.training.domain.model.WorkoutBlock;
import br.com.moveup.training.domain.model.WorkoutBlock.Method;
import br.com.moveup.training.domain.model.WorkoutContent;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Regras do treino planejado (Fase 2, F2-2). */
class WorkoutDomainTest {

  static final UUID ORG = UUID.randomUUID();
  static final UUID SQUAT = UUID.randomUUID();
  static final UUID BENCH = UUID.randomUUID();

  static PrescribedSet set(int min, int max, String kg) {
    return new PrescribedSet(SetType.NORMAL, min, max, new BigDecimal(kg), null, null, null, 2, 90);
  }

  static PrescribedExercise exercise(UUID id) {
    return new PrescribedExercise(id, 90, null, List.of(set(8, 12, "60"), set(8, 12, "60")));
  }

  static WorkoutBlock block(Method method, PrescribedExercise... exercises) {
    return new WorkoutBlock(
        null, method, null, null, null, null, null, null, null, List.of(exercises));
  }

  static String codeOf(Throwable e) {
    return ((DomainException) e).code();
  }

  @Test
  void serieComFaixaDeRepsCargaEEsforco() {
    assertThat(set(8, 12, "62.5").loadKg()).isEqualByComparingTo("62.5");
    assertThatThrownBy(() -> set(12, 8, "60"))
        .satisfies(e -> assertThat(codeOf(e)).isEqualTo("reps-invalid"));
    assertThatThrownBy(() -> set(8, 12, "-1"))
        .satisfies(e -> assertThat(codeOf(e)).isEqualTo("load-invalid"));
    assertThatThrownBy(
            () ->
                new PrescribedSet(
                    SetType.NORMAL, 8, 8, null, null, null, new BigDecimal("8"), 2, null))
        .satisfies(e -> assertThat(codeOf(e)).isEqualTo("effort-invalid"));
    assertThat(new PrescribedSet(null, 10, null, null, null, null, null, null, null).type())
        .isEqualTo(SetType.NORMAL);
  }

  @Test
  void cadaMetodoExigeOQueEleUsa() {
    assertThat(block(Method.SEQUENTIAL, exercise(SQUAT)).exercises()).hasSize(1);
    assertThatThrownBy(() -> block(Method.SUPERSET, exercise(SQUAT)))
        .satisfies(e -> assertThat(codeOf(e)).isEqualTo("superset-needs-two"));
    assertThatThrownBy(() -> block(Method.CIRCUIT, exercise(SQUAT), exercise(BENCH)))
        .satisfies(e -> assertThat(codeOf(e)).isEqualTo("rounds-required"));
    assertThatThrownBy(() -> block(Method.AMRAP, exercise(SQUAT)))
        .satisfies(e -> assertThat(codeOf(e)).isEqualTo("duration-required"));
    assertThatThrownBy(() -> block(Method.SEQUENTIAL))
        .satisfies(e -> assertThat(codeOf(e)).isEqualTo("block-empty"));
    var noSets = new PrescribedExercise(SQUAT, null, null, List.of());
    assertThatThrownBy(() -> block(Method.SEQUENTIAL, noSets))
        .satisfies(e -> assertThat(codeOf(e)).isEqualTo("sets-required"));
    // em bloco por tempo, exercício sem série é válido (o relógio manda)
    var emom =
        new WorkoutBlock(
            null, Method.EMOM, null, null, null, null, null, 600, null, List.of(noSets));
    assertThat(emom.durationSeconds()).isEqualTo(600);
  }

  @Test
  void tabataEhHiitVinteDezOitoRodadas() {
    var tabata =
        new WorkoutBlock(
            "Final",
            Method.HIIT,
            "Tabata",
            3,
            40,
            40,
            null,
            null,
            null,
            List.of(new PrescribedExercise(SQUAT, null, null, List.of())));

    assertThat(tabata.preset()).isEqualTo("tabata");
    assertThat(tabata.rounds()).isEqualTo(8);
    assertThat(tabata.workSeconds()).isEqualTo(20);
    assertThat(tabata.restSeconds()).isEqualTo(10);
    assertThatThrownBy(
            () ->
                new WorkoutBlock(
                    null,
                    Method.SEQUENTIAL,
                    "tabata",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    List.of(exercise(SQUAT))))
        .satisfies(e -> assertThat(codeOf(e)).isEqualTo("preset-invalid"));
  }

  @Test
  void salvarSemSessaoSubstituiComSessaoCriaVersaoNova() {
    var content =
        new WorkoutContent("Peito", 50, null, List.of(block(Method.SEQUENTIAL, exercise(BENCH))));
    var workout =
        Workout.forProgram(
            UUID.randomUUID(), UUID.randomUUID(), ORG, UUID.randomUUID(), null, "Treino A");
    var firstVersion = workout.versionId();

    assertThat(workout.edit("Treino A", content, 1, false, UUID.randomUUID())).isFalse();
    assertThat(workout.versionId()).isEqualTo(firstVersion);
    assertThat(workout.revision()).isEqualTo(2);

    var newVersion = UUID.randomUUID();
    assertThat(workout.edit("Treino A", content, 2, true, newVersion)).isTrue();
    assertThat(workout.versionId()).isEqualTo(newVersion);
    assertThat(workout.versionNumber()).isEqualTo(2);
    assertThat(workout.revision()).isEqualTo(3);
  }

  @Test
  void revisaoAntigaNaoSalvaEOutraOrganizacaoNaoVe() {
    var template =
        Workout.newTemplate(
            UUID.randomUUID(), UUID.randomUUID(), ORG, "  Full   body ", WorkoutContent.empty());

    assertThat(template.name()).isEqualTo("Full body");
    assertThatThrownBy(
            () -> template.edit("X", WorkoutContent.empty(), 7, false, UUID.randomUUID()))
        .isInstanceOf(VersionMismatch.class);
    assertThatThrownBy(() -> template.requireOwnedBy(UUID.randomUUID()))
        .isInstanceOf(ResourceNotFound.class);
    template.archive(1);
    assertThatThrownBy(() -> template.requireOwnedBy(ORG)).isInstanceOf(ResourceNotFound.class);
  }

  @Test
  void copiaDoModeloGuardaAOrigem() {
    var content =
        new WorkoutContent("Pernas", 60, null, List.of(block(Method.SEQUENTIAL, exercise(SQUAT))));
    var template =
        Workout.newTemplate(UUID.randomUUID(), UUID.randomUUID(), ORG, "Pernas", content);

    var copy =
        Workout.forProgram(
            UUID.randomUUID(), UUID.randomUUID(), ORG, UUID.randomUUID(), template, null);

    assertThat(copy.isTemplate()).isFalse();
    assertThat(copy.sourceTemplateId()).isEqualTo(template.id());
    assertThat(copy.name()).isEqualTo("Pernas");
    assertThat(copy.content()).isEqualTo(content);
    assertThatThrownBy(
            () ->
                Workout.forProgram(
                    UUID.randomUUID(), UUID.randomUUID(), ORG, UUID.randomUUID(), copy, null))
        .satisfies(e -> assertThat(codeOf(e)).isEqualTo("not-a-template"));
  }
}
