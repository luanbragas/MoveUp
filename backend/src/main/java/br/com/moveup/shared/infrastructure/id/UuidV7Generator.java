package br.com.moveup.shared.infrastructure.id;

import br.com.moveup.shared.domain.IdGenerator;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.UUID;

/**
 * UUIDv7 (RFC 9562): 48 bits de milissegundos desde a época, versão 7, variante RFC e 74 bits
 * aleatórios de {@link SecureRandom}. Ids do mesmo milissegundo não têm ordem garantida entre si, o
 * que a RFC permite e basta para localidade de índice.
 */
public final class UuidV7Generator implements IdGenerator {

  private static final long MAX_TIMESTAMP = (1L << 48) - 1;

  private final Clock clock;
  private final SecureRandom random;

  public UuidV7Generator(Clock clock) {
    this(clock, new SecureRandom());
  }

  UuidV7Generator(Clock clock, SecureRandom random) {
    this.clock = clock;
    this.random = random;
  }

  @Override
  public UUID newId() {
    var millis = clock.millis();
    if (millis < 0 || millis > MAX_TIMESTAMP) {
      throw new IllegalStateException("relógio fora do intervalo do UUIDv7");
    }
    var randA = random.nextInt() & 0x0FFF; // 12 bits
    var randB = random.nextLong() & 0x3FFF_FFFF_FFFF_FFFFL; // 62 bits

    var mostSignificant = (millis << 16) | 0x7000L | randA;
    var leastSignificant = 0x8000_0000_0000_0000L | randB;
    return new UUID(mostSignificant, leastSignificant);
  }
}
