package br.com.moveup.training;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.moveup.training.domain.model.PlannedCount;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Base da adesão: quantos treinos a agenda previa. */
class PlannedCountTest {

  @Test
  void agendaPrevistaPorDiasFixosESequencia() {
    // 2026-10-05 é segunda; duas semanas com treino seg (1) e qui (4)
    var from = LocalDate.parse("2026-10-05");
    var to = LocalDate.parse("2026-10-18");
    assertThat(PlannedCount.between(from, to, null, null, List.of(1, 4), null)).isEqualTo(4);
    // programa começou no meio da janela
    assertThat(
            PlannedCount.between(
                from, to, LocalDate.parse("2026-10-12"), null, List.of(1, 4), null))
        .isEqualTo(2);
    assertThat(PlannedCount.between(from, to, null, null, List.of(), 3)).isEqualTo(6);
  }
}
