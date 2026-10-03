package br.com.moveup.training.application.port.out;

import br.com.moveup.training.domain.model.Program;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Programas (com RLS: o personal só alcança os dos alunos dele) e a agenda dos treinos. */
public interface Programs {

  Optional<Program> find(UUID programId);

  Optional<UUID> activeFor(UUID linkId);

  void insert(Program program);

  /** Grava cabeçalho, revisão, ordem dos treinos e dias fixos. */
  void save(Program program);

  /** Arquiva os programas ativos do vínculo (antes de ativar outro). */
  void archiveActive(UUID linkId);

  void saveArchive(Program program);

  /** Resumo dos treinos do programa: tempo estimado e quantidade de exercícios. */
  Map<UUID, WorkoutStats> workoutStats(UUID programId);

  record WorkoutStats(String name, Integer estimatedMinutes, int exercises) {}
}
