package br.com.moveup.training.api;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Para a adesão (alerts, worker): quantos treinos a agenda do programa ativo previa. */
public interface TrainingSchedule {

  /**
   * Treinos previstos para cada vínculo nos dias [from, to] (inclusive), respeitando as datas do
   * programa. Dias fixos: cada treino agendado no dia da semana conta um; sequência: a meta semanal
   * proporcional aos dias. Vínculo sem programa ativo fica de fora.
   */
  Map<UUID, Integer> plannedSessions(Collection<UUID> linkIds, LocalDate from, LocalDate to);
}
