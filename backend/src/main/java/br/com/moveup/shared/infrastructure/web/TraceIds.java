package br.com.moveup.shared.infrastructure.web;

import io.micrometer.tracing.Tracer;
import java.security.SecureRandom;
import java.util.HexFormat;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * {@code traceId} da requisição atual (o mesmo que o Micrometer Tracing põe no MDC dos logs). Sem
 * span ativo, gera um id aleatório: o {@code ProblemDetail} sempre tem {@code traceId}.
 */
@Component
public class TraceIds {

  private static final SecureRandom RANDOM = new SecureRandom();

  private final ObjectProvider<Tracer> tracer;

  public TraceIds(ObjectProvider<Tracer> tracer) {
    this.tracer = tracer;
  }

  public String current() {
    var active = tracer.getIfAvailable();
    if (active != null && active.currentSpan() != null) {
      return active.currentSpan().context().traceId();
    }
    var bytes = new byte[16];
    RANDOM.nextBytes(bytes);
    return HexFormat.of().formatHex(bytes);
  }
}
