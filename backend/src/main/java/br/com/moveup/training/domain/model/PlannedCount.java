package br.com.moveup.training.domain.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Quantos treinos a agenda previa num período (base da adesão). */
public final class PlannedCount {

  private PlannedCount() {}

  /**
   * @param weekdays dias fixos: um item por treino agendado (0 = domingo … 6 = sábado), repetidos
   *     se dois treinos caem no mesmo dia
   * @param weeklyTarget sequência: treinos por semana (nulo em dias fixos)
   */
  public static int between(
      LocalDate from,
      LocalDate to,
      LocalDate startsOn,
      LocalDate endsOn,
      List<Integer> weekdays,
      Integer weeklyTarget) {
    var start = startsOn != null && startsOn.isAfter(from) ? startsOn : from;
    var end = endsOn != null && endsOn.isBefore(to) ? endsOn : to;
    if (end.isBefore(start)) {
      return 0;
    }
    if (weeklyTarget != null) {
      var days = ChronoUnit.DAYS.between(start, end) + 1;
      return (int) Math.round(weeklyTarget * days / 7.0);
    }
    var count = 0;
    for (var day = start; !day.isAfter(end); day = day.plusDays(1)) {
      var weekday = day.getDayOfWeek().getValue() % 7; // segunda = 1 … domingo = 0
      for (var scheduled : weekdays) {
        if (scheduled == weekday) {
          count++;
        }
      }
    }
    return count;
  }
}
