package br.com.moveup.execution;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.moveup.execution.domain.model.PersonalRecords;
import br.com.moveup.execution.domain.model.PersonalRecords.SetPerformance;
import br.com.moveup.execution.domain.model.PersonalRecords.Type;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PersonalRecordsTest {

  private static final UUID BENCH = UUID.randomUUID();
  private static final Instant T0 = Instant.parse("2026-10-01T12:00:00Z");

  private static SetPerformance set(int day, Integer reps, String kg) {
    return new SetPerformance(
        UUID.randomUUID(),
        UUID.randomUUID(),
        BENCH,
        T0.plusSeconds(day * 86_400L),
        reps,
        kg == null ? null : new BigDecimal(kg),
        null,
        null);
  }

  @Test
  void historicoComVigenteSoNoUltimoRecordeDeCadaTipo() {
    var records =
        PersonalRecords.history(
            List.of(set(0, 10, "50"), set(1, 8, "55"), set(2, 10, "50"), set(3, 12, "50")));

    var maxLoad = records.stream().filter(r -> r.type() == Type.MAX_LOAD).toList();
    assertThat(maxLoad)
        .extracting(r -> r.value().toPlainString())
        .containsExactly("50.000", "55.000");
    assertThat(maxLoad).extracting(PersonalRecords.Record::current).containsExactly(false, true);

    // reps a 50 kg: 10 (dia 0), igualar no dia 2 não conta, 12 no dia 3 é recorde
    var repsAt50 =
        records.stream()
            .filter(r -> r.type() == Type.MAX_REPS_AT_LOAD && r.loadKg().intValue() == 50)
            .toList();
    assertThat(repsAt50).extracting(r -> r.value().intValue()).containsExactly(10, 12);
    assertThat(records.stream().filter(PersonalRecords.Record::current)).hasSize(3);
  }

  @Test
  void deterministicoNaoImportaAOrdemDeEntrada() {
    var sets = List.of(set(2, 6, "60"), set(0, 10, "50"), set(1, 8, "55"));
    assertThat(PersonalRecords.history(sets))
        .extracting(PersonalRecords.Record::value)
        .isEqualTo(
            PersonalRecords.history(List.of(sets.get(1), sets.get(2), sets.get(0))).stream()
                .map(PersonalRecords.Record::value)
                .toList());
  }

  @Test
  void corridaGeraDistanciaEPaceMenorEMelhor() {
    var slow =
        new SetPerformance(UUID.randomUUID(), UUID.randomUUID(), BENCH, T0, null, null, 1500, 5000);
    var fast =
        new SetPerformance(
            UUID.randomUUID(),
            UUID.randomUUID(),
            BENCH,
            T0.plusSeconds(86_400),
            null,
            null,
            1400,
            5000);
    var records = PersonalRecords.history(List.of(slow, fast));

    assertThat(records.stream().filter(r -> r.type() == Type.BEST_PACE && r.current()))
        .singleElement()
        .extracting(r -> r.value().toPlainString())
        .isEqualTo("280.000");
    assertThat(records.stream().filter(r -> r.type() == Type.MAX_DISTANCE)).hasSize(1);
  }
}
