package br.com.moveup.training.application.port.in;

import br.com.moveup.training.application.port.in.ManageExercises.ExerciseView;
import br.com.moveup.training.application.port.in.ManageWorkouts.ContentView;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Recebimento do sync do aluno (ARQUITETURA 4, "Sincronização offline"): o planejado que mudou
 * desde o cursor. A unidade é o programa inteiro (treinos, agenda e exercícios usados); programa
 * arquivado vai como tombstone para o app apagar.
 */
public interface PlannedSync {

  /** Janela de sobreposição: commit atrasado não se perde (o app aplica por upsert). */
  long OVERLAP_SECONDS = 120;

  /**
   * @param since cursor da última sincronização; nulo = tudo
   */
  SyncPage changesSince(UUID userId, Instant since);

  /**
   * @param cursor mande de volta no próximo GET
   */
  record SyncPage(Instant cursor, List<ProgramSnapshot> programs, List<ExerciseView> exercises) {}

  /**
   * @param deleted tombstone: o app apaga o programa e os treinos dele
   * @param workouts todos os treinos ativos, na ordem da agenda (substituem os do app)
   */
  record ProgramSnapshot(
      UUID id,
      UUID linkId,
      String name,
      String goal,
      LocalDate startsOn,
      LocalDate endsOn,
      String scheduleMode,
      Integer weeklyTarget,
      boolean deleted,
      List<WorkoutSnapshot> workouts) {}

  /**
   * @param versionId vai na sessão para comparar com o planejado daquele dia
   */
  record WorkoutSnapshot(
      UUID id,
      String name,
      int position,
      Set<Integer> weekdays,
      UUID versionId,
      int versionNumber,
      ContentView content) {}
}
