package br.com.moveup.training.application.port.in;

import br.com.moveup.training.application.port.in.ManageWorkouts.WorkoutView;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Programa do aluno (um ativo por vínculo) com os treinos e a agenda. */
public interface ManagePrograms {

  /** Cria e ativa; o programa ativo anterior do vínculo é arquivado. */
  ProgramView create(UUID professionalId, UUID linkId, ProgramInput input);

  Optional<ProgramView> active(UUID professionalId, UUID linkId);

  ProgramView get(UUID professionalId, UUID programId);

  /**
   * @param schedule todos os treinos do programa, na ordem; dias só no modo dias fixos
   */
  ProgramView update(
      UUID professionalId,
      UUID programId,
      int expectedRevision,
      ProgramInput input,
      List<SlotInput> schedule);

  /** Treino novo no fim da agenda: cópia do modelo ou vazio (aí abre o editor). */
  WorkoutView addWorkout(UUID professionalId, UUID programId, UUID templateId, String name);

  void archive(UUID professionalId, UUID programId, int expectedRevision);

  record ProgramInput(
      String name,
      String goal,
      LocalDate startsOn,
      LocalDate endsOn,
      String scheduleMode,
      Integer weeklyTarget) {}

  record SlotInput(UUID workoutId, Set<Integer> weekdays) {}

  record ProgramView(
      UUID id,
      UUID linkId,
      String name,
      String goal,
      LocalDate startsOn,
      LocalDate endsOn,
      String scheduleMode,
      Integer weeklyTarget,
      int revision,
      List<ProgramWorkoutView> workouts) {}

  /**
   * @param position ordem na agenda (A = 1)
   * @param weekdays 0 = domingo; vazio no modo sequência
   */
  record ProgramWorkoutView(
      UUID id,
      String name,
      int position,
      Set<Integer> weekdays,
      Integer estimatedMinutes,
      int exercises) {}
}
