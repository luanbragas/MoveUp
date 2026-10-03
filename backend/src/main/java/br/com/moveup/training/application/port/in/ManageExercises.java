package br.com.moveup.training.application.port.in;

import java.util.List;
import java.util.UUID;

/** Biblioteca de exercícios do personal: a base do MoveUp mais os exercícios próprios dele. */
public interface ManageExercises {

  int MAX_RESULTS = 100;

  /**
   * Busca por nome sem acento (parte do nome ou parecido), opcionalmente por músculo. Sem texto,
   * lista em ordem alfabética.
   */
  List<ExerciseView> search(UUID professionalId, String query, String muscle, int limit);

  ExerciseView create(UUID professionalId, NewExercise exercise);

  /** Arquiva um exercício próprio: some da busca, mas treinos antigos continuam mostrando. */
  void archive(UUID professionalId, UUID exerciseId);

  record NewExercise(
      String name,
      String modality,
      String trackingType,
      String primaryMuscle,
      List<String> secondaryMuscles,
      String equipment,
      boolean unilateral,
      String instructions,
      String mediaUrl) {}

  /**
   * @param custom exercício próprio da organização (pode arquivar); falso = biblioteca base
   */
  record ExerciseView(
      UUID id,
      String name,
      String modality,
      String trackingType,
      String primaryMuscle,
      List<String> secondaryMuscles,
      String equipment,
      boolean unilateral,
      String instructions,
      String mediaUrl,
      boolean custom) {}
}
