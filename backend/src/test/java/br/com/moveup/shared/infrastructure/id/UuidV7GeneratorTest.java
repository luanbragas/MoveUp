package br.com.moveup.shared.infrastructure.id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidV7GeneratorTest {

  private static final Instant NOW = Instant.parse("2026-10-02T12:00:00.123Z");

  @Test
  void deveGerarVersao7ComVarianteRfc() {
    var id = new UuidV7Generator(Clock.fixed(NOW, ZoneOffset.UTC)).newId();

    assertThat(id.version()).isEqualTo(7);
    assertThat(id.variant()).isEqualTo(2);
  }

  @Test
  void deveCarregarOsMilissegundosDoRelogioNos48BitsIniciais() {
    var id = new UuidV7Generator(Clock.fixed(NOW, ZoneOffset.UTC)).newId();

    assertThat(timestampOf(id)).isEqualTo(NOW.toEpochMilli());
  }

  @Test
  void idsDeMilissegundosDiferentesFicamEmOrdem() {
    var earlier = new UuidV7Generator(Clock.fixed(NOW, ZoneOffset.UTC)).newId();
    var later = new UuidV7Generator(Clock.fixed(NOW.plusMillis(1), ZoneOffset.UTC)).newId();

    // comparação como texto: é a ordem do índice uuid do Postgres
    assertThat(earlier.toString()).isLessThan(later.toString());
  }

  @Test
  void naoRepeteIdNoMesmoMilissegundo() {
    var generator = new UuidV7Generator(Clock.fixed(NOW, ZoneOffset.UTC));
    var ids = new HashSet<UUID>();

    for (int i = 0; i < 10_000; i++) {
      ids.add(generator.newId());
    }

    assertThat(ids).hasSize(10_000);
  }

  @Test
  void relogioAntesDe1970EhRejeitado() {
    var generator = new UuidV7Generator(Clock.fixed(Instant.EPOCH.minusMillis(1), ZoneOffset.UTC));

    assertThatThrownBy(generator::newId).isInstanceOf(IllegalStateException.class);
  }

  private static long timestampOf(UUID id) {
    return id.getMostSignificantBits() >>> 16;
  }
}
