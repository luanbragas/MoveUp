package br.com.moveup.execution.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Recordes pessoais com histórico: percorre as séries feitas em ordem de tempo e registra cada vez
 * que um valor supera o melhor anterior do mesmo tipo (e da mesma carga, no caso de reps). O último
 * de cada chave é o vigente. O cálculo é determinístico: refazer com os mesmos dados dá o mesmo
 * resultado, por isso editar uma sessão é só recalcular.
 */
public final class PersonalRecords {

  private PersonalRecords() {}

  public enum Type {
    MAX_LOAD,
    MAX_REPS_AT_LOAD,
    MAX_DISTANCE,
    BEST_PACE,
    MAX_DURATION;

    public String code() {
      return name().toLowerCase(Locale.ROOT);
    }

    /** Pace (s/km) é melhor quando menor; o resto, quando maior. */
    boolean better(BigDecimal candidate, BigDecimal best) {
      return this == BEST_PACE ? candidate.compareTo(best) < 0 : candidate.compareTo(best) > 0;
    }
  }

  /** Uma série feita (concluída), de uma sessão finalizada. */
  public record SetPerformance(
      UUID sessionId,
      UUID setId,
      UUID exerciseId,
      Instant achievedAt,
      Integer reps,
      BigDecimal loadKg,
      Integer durationSeconds,
      Integer distanceM) {}

  /**
   * @param loadKg só em {@link Type#MAX_REPS_AT_LOAD} (a carga em que as reps valem)
   * @param reps reps da série (informativo em {@link Type#MAX_LOAD})
   */
  public record Record(
      UUID exerciseId,
      Type type,
      BigDecimal value,
      BigDecimal loadKg,
      Integer reps,
      UUID sessionId,
      UUID setId,
      Instant achievedAt,
      boolean current) {}

  private record Key(UUID exerciseId, Type type, BigDecimal loadKg) {}

  public static List<Record> history(List<SetPerformance> sets) {
    var ordered =
        sets.stream()
            .sorted(
                Comparator.comparing(SetPerformance::achievedAt)
                    .thenComparing(SetPerformance::setId))
            .toList();
    var best = new HashMap<Key, BigDecimal>();
    var records = new ArrayList<Record>();
    var lastIndex = new HashMap<Key, Integer>();
    for (var set : ordered) {
      for (var c : candidates(set)) {
        var key = new Key(set.exerciseId(), c.type(), c.loadKg());
        var previous = best.get(key);
        if (previous == null || c.type().better(c.value(), previous)) {
          best.put(key, c.value());
          lastIndex.put(key, records.size());
          records.add(
              new Record(
                  set.exerciseId(),
                  c.type(),
                  c.value(),
                  c.loadKg(),
                  set.reps(),
                  set.sessionId(),
                  set.setId(),
                  set.achievedAt(),
                  false));
        }
      }
    }
    var current = Map.copyOf(lastIndex).values();
    for (var i : current) {
      var r = records.get(i);
      records.set(
          i,
          new Record(
              r.exerciseId(),
              r.type(),
              r.value(),
              r.loadKg(),
              r.reps(),
              r.sessionId(),
              r.setId(),
              r.achievedAt(),
              true));
    }
    return List.copyOf(records);
  }

  private record Candidate(Type type, BigDecimal value, BigDecimal loadKg) {}

  private static List<Candidate> candidates(SetPerformance s) {
    var out = new ArrayList<Candidate>();
    var reps = s.reps() == null ? 0 : s.reps();
    var load = s.loadKg() == null ? null : s.loadKg().setScale(3, RoundingMode.HALF_UP);
    if (load != null && load.signum() > 0 && reps >= 1) {
      out.add(new Candidate(Type.MAX_LOAD, load, null));
      out.add(new Candidate(Type.MAX_REPS_AT_LOAD, BigDecimal.valueOf(reps), load));
    }
    var distance = s.distanceM() == null ? 0 : s.distanceM();
    var duration = s.durationSeconds() == null ? 0 : s.durationSeconds();
    if (distance > 0) {
      out.add(new Candidate(Type.MAX_DISTANCE, BigDecimal.valueOf(distance), null));
      if (duration > 0) {
        var pace =
            BigDecimal.valueOf(duration * 1000L)
                .divide(BigDecimal.valueOf(distance), 3, RoundingMode.HALF_UP);
        out.add(new Candidate(Type.BEST_PACE, pace, null));
      }
    } else if (duration > 0 && load == null && reps == 0) {
      // exercício por tempo (prancha, isometria): só o tempo conta
      out.add(new Candidate(Type.MAX_DURATION, BigDecimal.valueOf(duration), null));
    }
    return out;
  }
}
